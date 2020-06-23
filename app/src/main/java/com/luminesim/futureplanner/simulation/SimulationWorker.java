package com.luminesim.futureplanner.simulation;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.luminesim.futureplanner.db.EntityFactDetail;
import com.luminesim.futureplanner.db.EntityRepository;
import com.luminesim.futureplanner.monad.MonadDatabase;
import com.luminesim.futureplanner.monad.types.CurrencyMonad;
import com.luminesim.futureplanner.monad.types.IncomeTypeMonad;
import com.luminesim.futureplanner.monad.types.IncomeType;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ca.anthrodynamics.indes.Engine;
import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Rate;
import ca.anthrodynamics.indes.lang.ScheduledRate;
import ca.anthrodynamics.indes.sd.SDDiagram;
import lombok.Getter;
import lombok.Setter;

public class SimulationWorker extends Worker {

    /**
     *
     */
    public static final String DATA_ENTITY_UID = "com.luminesim.futureplanner.DATA_ENTITY_UID";

    /**
     * @apiNote Setter available as a test hook.
     */
    @Setter
    private EntityRepository repo;
    /**
     * @apiNote  Getter available as a test hook.
     */
    @Getter
    private Agent root;
    private MonadDatabase db;
    private long entityUid;
    private Lock workLock = new ReentrantLock();
    private Condition workDone = workLock.newCondition();

    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public SimulationWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);

        // Record the entity and database.
        db = MonadDatabase.getDatabase(appContext);
        repo = new EntityRepository(appContext);
        entityUid = workerParams.getInputData().getLong(DATA_ENTITY_UID, 0l);
    }

    @NonNull
    @Override
    public Result doWork() {

        // Get the entity and do work.
        LocalDate startTime = LocalDate.now();
        AtomicBoolean done = new AtomicBoolean(false);
        TreeMap<Double, Double> fundsDataset = new TreeMap<>();

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
            entity.getFacts().forEach(fact -> {
                // Accumulate the steps in an action / calculation.
                ArrayList<ComputableMonad> actions = new ArrayList<>(fact.getDetails().size());
                fact.getDetails().stream().sorted(Comparator.comparingInt(EntityFactDetail::getStepNumber)).forEach(detail -> {
                    try {
                        actions.add(db.makeComputable(detail.getMonadJson()));
                    } catch (IOException e) {
                        e.printStackTrace();
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

                // Record in the appropriate collection.
                if (Rate.class.isAssignableFrom((Class) action.getOutType().get())) {
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
                } else {
                    throw new Error("Unhandled income/expense type: " + action.getOutType().get());
                }
            });


            // Build the simulation.
            // NOTE: DT MUST BE EVEN -- SEE BELOW.
            double dt = 1;
            final double DAY = 1 * dt;
            final double YEAR = 1 / (365.0 * DAY);
            double endTime = 365.0 * DAY;
            Currency masterCurrency = Currency.getInstance("CAD");

            // Create the basic model.
            SDDiagram<Double> sd = root.addSDDiagram("Money", dt)
                    .addStock("Funds", 0.00)
                    .addFlow("Expenses").from("Funds").toVoid().at((funds, nil) -> getTotalFlow(engine, startTime, ongoingExpenses));

            // Add a flow for taxes.
            // Must be aggregated by type.
            Map<IncomeType, List<ComputableMonad>> incomeStreams = new HashMap<>();
            ongoingIncome.forEach(income ->
                incomeStreams.computeIfAbsent(getIncomeType(income), t -> new ArrayList<>()).add(income)
            );
            incomeStreams.forEach((type, list) -> {
                String incomeFlow = "Income: " + type.name();
                        sd.addFlow(incomeFlow).fromVoid().to("Funds").at((nil, funds) -> getTotalFlow(engine, startTime, ongoingIncome));
                        sd.addFlow("Taxes: " + type.name()).from("Funds").toVoid().at((funds, nil) -> {
                            Double currentIncome = sd.get(incomeFlow);
                            return getPersonalTaxRatePerYear(
                                    startTime.plusDays((long) engine.time()),
                                    type.getCurrency(),
                                    currentIncome / YEAR,
                                    type
                                    ) * YEAR;
                        });
                    });

            // Run the simulation.
            engine.start();
            for (double i = 0; i < endTime; i += dt) {
                engine.runUntil(i);
                Log.i("Simulation", "Running until " + i + "/" + endTime + ", funds are " + sd.get("Funds"));
                fundsDataset.put(i, sd.get("Funds"));
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
            return Result.success();
//                    (new Data.Builder())
//                            .putString("Funds", fundsDataset.toString())
//                            .putLong("StartEpochDay", startTime.toEpochDay())
//                            .putDouble("FinalFunds", fundsDataset.lastEntry().getValue())
//                            .build()
//            );
        } catch (Throwable t) {
            return Result.failure();
        } finally {
            workLock.unlock();
        }
    }

    /**
     * @param income
     * @return The type of income provided by the income.
     */
    private IncomeType getIncomeType(ComputableMonad income) {
        // Set up default income types.
        Map<Currency, IncomeType> unspecifiedTypes = new HashMap<>();
        unspecifiedTypes.put(Currency.getInstance("CAD"), IncomeType.CADOtherIncome);

        // Return the appropriate income type.
        Currency incomeCurrency = Currency.getInstance((String) income.getInfo().getProperties().get(CurrencyMonad.INFO_CURRENCY_CODE));
        return  (IncomeType)income.getInfo().getProperties().getOrDefault(IncomeTypeMonad.INFO_INCOME_TYPE, unspecifiedTypes.get(incomeCurrency));
    }

    /**
     * Generates the taxable amount
     *
     * @param time
     * @param amountPerYear
     * @return
     */
    private double getPersonalTaxRatePerYear(LocalDate time, Currency currency, double amountPerYear, IncomeType incomeType) {

        // Figure out the currency and income type.
        TreeMap<Double, Double> ratesUpTo = new TreeMap<>();
        if (currency.getCurrencyCode().equals("CAD")) {

            // Figure out the correct rate.
            if (incomeType == IncomeType.CADOtherIncome) {
                if (time.getYear() >= 2020) {
                    ratesUpTo.put(45_225.00, 25.50);
                    ratesUpTo.put(48_535.00, 27.50);
                    ratesUpTo.put(97_069.00, 33.00);
                    ratesUpTo.put(129_214.00, 38.50);
                    ratesUpTo.put(150_473.00, 40.50);
                    ratesUpTo.put(214_368.00, 43.72);
                    ratesUpTo.put(Double.MAX_VALUE, 47.50);
                } else {
                    throw new IllegalStateException("Unsupported year: " + time.getYear());
                }
            } else {
                throw new IllegalStateException(String.format("Unsupported income type %s for currency %s", incomeType, currency));
            }
        } else {
            throw new IllegalStateException(String.format("Unsupported currency %s", currency));
        }

        // Tax appropriately.
        double sum = 0;
        Double floor = 0d;
        for (Double ceiling : ratesUpTo.keySet()) {
            double incomeInRange = Math.min(ceiling - floor, amountPerYear - floor);
            if (incomeInRange > 0) {
                sum += incomeInRange * ratesUpTo.get(ceiling) / 100.0;
            }
            floor = ceiling;
        }

        return sum;
    }

    /**
     * Returns the sum of a list of flows applicable at this moment in time.
     *
     * @param engine
     * @param startTime
     * @param subFlows
     * @return
     */
    private double getTotalFlow(Engine engine, LocalDate startTime, List<ComputableMonad> subFlows) {

        double sum = 0;
        for (ComputableMonad flow : subFlows) {
            Rate rate = ((Rate) flow.apply(ComputableMonad.NoInput));
            boolean isAtOrAfterStart = true;
            boolean isAtOrBeforeEnd = true;
            if (rate instanceof ScheduledRate) {
                ScheduledRate scheduledRate = (ScheduledRate) rate;
                if (scheduledRate.getStart().isPresent()) {
                    // REQUIRES EVEN DT
                    LocalDate simulationTime = startTime.plusDays((long) engine.time());
                    isAtOrAfterStart = !simulationTime.isBefore(scheduledRate.getStart().get());
                    isAtOrBeforeEnd = !simulationTime.isAfter(scheduledRate.getEnd().get());
                }
            }
            if (isAtOrAfterStart && isAtOrBeforeEnd) {
                sum += rate.getAsDouble();
            }
        }
        return sum;
//        return subFlows
//                .stream()
//                .map(flow -> ((Rate) flow.apply(ComputableMonad.NoInput)))
//                .filter(rate -> {
//                    boolean isAtOrAfterStart = true;
//                    boolean isAtOrBeforeEnd = true;
//                    if (rate instanceof ScheduledRate) {
//                        ScheduledRate scheduledRate = (ScheduledRate) rate;
//                        if (scheduledRate.getStart().isPresent()) {
//                            // REQUIRES EVEN DT
//                            LocalDate simulationTime = startTime.plusDays((long) engine.time());
//                            isAtOrAfterStart = !simulationTime.isBefore(scheduledRate.getStart().get());
//                            isAtOrBeforeEnd = !simulationTime.isAfter(scheduledRate.getEnd().get());
//                        }
//                    }
//                    return isAtOrAfterStart && isAtOrBeforeEnd;
//                })
//                .mapToDouble(rate -> rate.getAsDouble())
//                .sum();
    }
}
