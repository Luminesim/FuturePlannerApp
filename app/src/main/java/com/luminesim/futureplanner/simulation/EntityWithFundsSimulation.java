package com.luminesim.futureplanner.simulation;

import android.content.Context;

import androidx.annotation.NonNull;

import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.models.Model;
import com.luminesim.futureplanner.models.ModelView;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.luminesim.futureplanner.monad.types.IncomeType;
import com.luminesim.futureplanner.monad.types.OneOffAmount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import ca.anthrodynamics.indes.Engine;
import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.HasEnd;
import ca.anthrodynamics.indes.lang.HasStart;
import ca.anthrodynamics.indes.lang.Rate;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.Getter;
import lombok.Setter;

public abstract class EntityWithFundsSimulation implements Runnable, ModelView {
    @Getter
    private TreeMap<LocalDate, Double> fundsDataset = new TreeMap<>();

    /**
     * @apiNote Setter available as a test hook.
     */
    @Setter
    private EntityRepository repo;
    /**
     * @apiNote Getter available as a test hook.
     */
    @Getter
    private Agent root;
    private MonadDatabase db;
    private long entityUid;
    private Lock workLock = new ReentrantLock();
    private Condition workDone = workLock.newCondition();

    @Getter
    private int numberOfDaysToRun;

    /**
     * @param appContext The application {@link Context}
     * @pre numberOfDaysToRun >= 1
     */
    public EntityWithFundsSimulation(int numberOfDaysToRun, @NonNull Context appContext, @NonNull long entityUid) {

        // Sanity check.
        if (numberOfDaysToRun <= 0) {
            throw new IllegalArgumentException("Number of days to run the simulation must be positive.");
        }

        // Record the entity and database.
        this.numberOfDaysToRun = numberOfDaysToRun;
        db = MonadDatabase.getDatabase(appContext);
        repo = new EntityRepository(appContext);
        this.entityUid = entityUid;
    }

    @Override
    public final synchronized void run() {

        // Get the entity and do work.
        LocalDateTime startTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        AtomicBoolean done = new AtomicBoolean(false);

        repo.getEntity(entityUid, entity -> {

            // Construct the simulation.
            Engine engine = new Engine();
            root = new Agent(engine);
            engine.setRoot(root);

            // Accumulate all income, expenses, etc.
            List<ComputableMonad> ongoingIncome = new ArrayList<>();
            List<ComputableMonad> oneOffIncome = new ArrayList<>();
            List<ComputableMonad> ongoingExpenses = new ArrayList<>();
            List<ComputableMonad> oneOffExpenses = new ArrayList<>();
            List<Model> submodels = new ArrayList<>();
            entity.getFacts().forEach(fact -> {
                // Accumulate the steps in an action / calculation.
                ArrayList<ComputableMonad> actions = new ArrayList<>(fact.getDetails().size());
                fact.getDetails().stream().sorted(Comparator.comparingInt(EntityFactDetail::getStepNumber)).forEach(detail -> {
                    try {
                        actions.add(db.makeComputable(detail.getMonadJson()));
                    } catch (Throwable t) {
                        throw new RuntimeException(String.format(
                                "Problem with Entity %s Fact %s Detail %s.",
                                "" + fact.getFact().getEntityUid(),
                                "" + fact.getFact().getUid(),
                                "" + detail.getUid()),
                                t
                        );
                    }
                });

                // Compose to one coherent action.
                ComputableMonad action = null;
                for (ComputableMonad next : actions) {
                    if (action == null) {
                        action = next;
                    } else {
                        action = action.compose(next);
                    }
                }

                // No action? Skip.
                if (action == null) {
                    return;
                }

                // Record in the appropriate collection.
                if (action.getInfo().getProperties().canDuckTypeAs(Rate.class)) {
                    switch (fact.getFact().getCategory()) {
                        case Income:
                            ongoingIncome.add(action);
                            break;
                        case Expenses:
                            ongoingExpenses.add(action);
                            break;
                        default:
                            throw new Error("Unhandled fact category: " + fact.getFact().getCategory());
                    }
                } else if (action.getInfo().getProperties().canDuckTypeAs(OneOffAmount.class)) {
                    switch (fact.getFact().getCategory()) {
                        case Income:
                            oneOffIncome.add(action);
                            break;
                        case Expenses:
                            oneOffExpenses.add(action);
                            break;
                        default:
                            throw new Error("Unhandled fact category: " + fact.getFact().getCategory());
                    }
                } else if (action.getInfo().getProperties().canDuckTypeAs(Model.class)) {
                    submodels.add(action.apply(ComputableMonad.NoInput).as(Model.class));
                } else {
                    throw new Error("Unhandled income/expense type: " + action.getInfo().getProperties());
                }
            });

            // Give each model access to the global view.
            submodels.forEach(model -> model.setRootModel(this));

            // Build the simulation.
            // NOTE: DT MUST BE EVEN -- SEE BELOW.
            double dt = 1;
            final double DAY = 1 * dt;
            double endTime = numberOfDaysToRun * DAY;

            // Construct the simulation.
            constructSimulation(entity, root, startTime, submodels, ongoingIncome, oneOffIncome, ongoingExpenses, oneOffExpenses);

            // Run the simulation.
            engine.start();
            for (double i = 0; i < endTime; i += dt) {
                engine.runUntil(i);
                fundsDataset.put(startTime.plusDays((long) i).toLocalDate(), getFunds());
            }

            workLock.lock();
            done.set(true);
            workDone.signal();
            workLock.unlock();
        });

        // Wait for work to be complete and finish.
        workLock.lock();
        try {
            if (!done.get()) {
                workDone.await();
            }
            return;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            workLock.unlock();
        }
    }

    /**
     * @return The amount of funds that the entity has at the current moment in the simulation.
     */
    protected abstract Double getFunds();

    /**
     * Constructs the root agent of the simulation.
     *
     * @param root
     * @param submodels
     */
    protected abstract void constructSimulation(@NonNull EntityWithFacts entity, @NonNull Agent root, @lombok.NonNull LocalDateTime startTime, @NonNull List<Model> submodels, @NonNull List<ComputableMonad> ongoingIncome, @NonNull List<ComputableMonad> oneOffIncome, @NonNull List<ComputableMonad> ongoingExpenses, @NonNull List<ComputableMonad> oneOffExpenses);

    /**
     * @param income
     * @return The type of income provided by the income.
     */
    protected IncomeType getIncomeType(ComputableMonad income) {
        return IncomeType.CADOtherIncome;
    }

    /**
     * Returns the sum of a list of flows applicable at this moment in time.
     *
     * @param engine
     * @param startTime
     * @param subFlows
     * @return
     */
    protected double getTotalFlow(Engine engine, LocalDateTime startTime, List<ComputableMonad> subFlows) {

        double sum = 0;
        for (ComputableMonad flow : subFlows) {
            Traits result = flow.apply(ComputableMonad.NoInput);
            Rate rate = result.as(Rate.class);
            boolean isAtOrAfterStart = true;
            boolean isAtOrBeforeEnd = true;
            if (result.canDuckTypeAs(HasStart.class) || result.canDuckTypeAs(HasEnd.class)) {
                LocalDate simulationTime = startTime.plusDays((long) engine.time()).toLocalDate();
                if (result.canDuckTypeAs(HasStart.class)) {
                    // REQUIRES EVEN DT
                    isAtOrAfterStart = !simulationTime.isBefore(result.as(HasStart.class).getStart());
                }
                if (result.canDuckTypeAs(HasEnd.class)) {
                    isAtOrBeforeEnd = !simulationTime.isAfter(result.as(HasEnd.class).getEnd());
                }
            }
            if (isAtOrAfterStart && isAtOrBeforeEnd) {
                sum += rate.getAsDouble();
            }
        }
        return sum;
    }
}
