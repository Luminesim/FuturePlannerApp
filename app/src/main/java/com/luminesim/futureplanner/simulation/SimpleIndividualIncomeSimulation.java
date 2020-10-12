package com.luminesim.futureplanner.simulation;

import android.content.Context;
import android.util.Log;

import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.models.AssetType;
import com.luminesim.futureplanner.models.Model;
import com.luminesim.futureplanner.models.Qualifier;
import com.luminesim.futureplanner.monad.types.OneOffAmount;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.sd.SDDiagram;
import lombok.NonNull;

/**
 * An individual with simple income / expenses. No currency type is involved,
 * and taxes must be selectively applied.
 */
public class SimpleIndividualIncomeSimulation extends EntityWithFundsSimulation {

    public static final String ENTITY_TYPE = "SimpleIndividual";
    public static String PARAMETER_INITIAL_FUNDS = "Initial Funds";
    private Model submodels;

    /**
     * @param appContext The application {@link Context}
     */
    public SimpleIndividualIncomeSimulation(int numberOfDaysToRun, @NonNull Context appContext, @NonNull long entityUid) {
        super(numberOfDaysToRun, appContext, entityUid);
    }

    @Override
    protected void constructSimulation(
            @NonNull EntityWithFacts entity,
            @NonNull Agent root,
            @NonNull LocalDateTime startTime,
            @NonNull List<Model> submodels,
            @NonNull List<ComputableMonad> ongoingIncome,
            @NonNull List<ComputableMonad> oneOffIncome,
            @NonNull List<ComputableMonad> ongoingExpenses,
            @NonNull List<ComputableMonad> oneOffExpenses) {

        // Build the simulation.
        // NOTE: DT MUST BE EVEN -- SEE BELOW.
        double dt = 1;
        final double DAY = 1 * dt;
        final double YEAR = 1 / (365.0 * DAY);

        // Pull out details.
        double initialFunds = Double.parseDouble(entity.getParameter(PARAMETER_INITIAL_FUNDS).get());

        // Create the basic model with rate of income and expenses.
        final String Funds = "Funds", RateOfIncome = "Income", RateOfExpenses = "Expenses";
        SDDiagram<Double> sd = root
                .addSDDiagram("Money", dt)
                .addStock(Funds, initialFunds)
                .addFlow(RateOfIncome)
                .fromVoid()
                .to(Funds)
                .at((nil, funds) -> getTotalFlow(root.getEngine(), startTime, ongoingIncome))
                .addFlow(RateOfExpenses)
                .from(Funds)
                .toVoid()
                .at((funds, nil) -> getTotalFlow(root.getEngine(), startTime, ongoingExpenses));

        // Add one-off events.
        oneOffIncome.forEach(incomeEvent -> {
            OneOffAmount amount = incomeEvent.apply(getBaseTraits()).as(OneOffAmount.class);
            double when = (double) startTime.until(amount.getTime(), ChronoUnit.DAYS);
            if (when >= 0) {
                root.addEvent(when, e -> sd.updateStock(Funds, old -> old + amount.getAmount().getAsDouble()));
            }
        });

        // One-off expenses.
        oneOffExpenses.forEach(expenseEvent -> {
            OneOffAmount amount = expenseEvent.apply(getBaseTraits()).as(OneOffAmount.class);
            double when = (double) startTime.until(amount.getTime(), ChronoUnit.DAYS);
            if (when >= 0) {
                root.addEvent(when, e -> sd.updateStock(Funds, old -> old - amount.getAmount().getAsDouble()));
            }
        });

        // Register submodels
        this.submodels = Model.compose(submodels);
        root.addPopulation("Submodels").addToPopulation(
                "Submodels",
                submodels.stream().map(m -> m.asAgents(root.getEngine(), DAY, dt)).flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    protected Double getFunds() {
        return (Double) getRoot().getNumericSDDiagram("Money").get("Funds");
    }

    @Override
    public Set<AssetType> getAssetTypes() {
        return submodels.getAssetTypes();
    }

    @Override
    public Map<String, Set<Qualifier>> getAssetQualifiers(@NonNull AssetType assetType) {
        return submodels.getAssetQualifiers(assetType);
    }

    @Override
    public double getCount(@NonNull AssetType assetType, @NonNull Map<String, Set<Qualifier>> qualifiers) {
        return submodels.getCount(assetType, qualifiers);
    }
}
