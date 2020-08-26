package com.luminesim.futureplanner.simulation;

import android.content.Context;
import android.util.Log;

import com.luminesim.futureplanner.db.EntityWithFacts;
import com.luminesim.futureplanner.monad.types.CurrencyMonad;
import com.luminesim.futureplanner.monad.types.IncomeType;
import com.luminesim.futureplanner.monad.types.IncomeTypeMonad;
import com.luminesim.futureplanner.monad.types.OneOffAmount;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ca.anthrodynamics.indes.abm.Agent;
import ca.anthrodynamics.indes.lang.ComputableMonad;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.sd.SDDiagram;
import lombok.NonNull;

@Deprecated
public class CanadianIndividualIncomeSimulation extends EntityWithFundsSimulation {

    public static final String ENTITY_TYPE = "CanadianIndividual";

    public static String PARAMETER_PROVINCE = "Province";
    public static String PARAMETER_INITIAL_FUNDS = "Initial Funds";

    /**
     * @param appContext The application {@link Context}
     */
    public CanadianIndividualIncomeSimulation(@NonNull Context appContext, @NonNull long entityUid) {
        super(-1, appContext, entityUid);
    }

    @Override
    protected void constructSimulation(@NonNull EntityWithFacts entity, @NonNull Agent root, @NonNull LocalDateTime startTime, @NonNull List<ComputableMonad> ongoingIncome, @NonNull List<ComputableMonad> oneOffIncome, @NonNull List<ComputableMonad> ongoingExpenses, @NonNull List<ComputableMonad> oneOffExpenses) {
        // Build the simulation.
        // NOTE: DT MUST BE EVEN -- SEE BELOW.
        double dt = 1;
        final double DAY = 1 * dt;
        final double YEAR = 1 / (365.0 * DAY);
        double assumedTaxPercentForOneOffIncome = 33.0;

        // Pull out details.
        double initialFunds = Double.parseDouble(entity.getParameter(PARAMETER_INITIAL_FUNDS).get());
        if (1 == 1) {
            throw new RuntimeException("you updated this so entities have parameters, do check.");
        }

        // Create the basic model.
        final String Funds = "Funds";
        final String WithheldFunds = "Withheld Funds";
        SDDiagram<Double> sd = root.addSDDiagram("Money", dt)
                .addStock("Taxable Income: " + IncomeType.CADOtherIncome, 0.00)
                .addStock(WithheldFunds, 0.00)
                .addStock(Funds, initialFunds)
                .addFlow("Expenses").from("Funds").toVoid().at((funds, nil) -> getTotalFlow(root.getEngine(), startTime, ongoingExpenses));
        ;

        // Add stocks for taxes.
        Set<IncomeType> incomeTypes = new HashSet<>();
        oneOffIncome.stream().map(this::getIncomeType).forEach(incomeTypes::add);
        ongoingIncome.stream().map(this::getIncomeType).forEach(incomeTypes::add);
        incomeTypes.forEach(type -> sd.addStock(getTaxableIncomeStock(type), 0.0));

        // Add a flow for taxes.
        // Must be aggregated by type.
        Map<IncomeType, List<ComputableMonad>> incomeStreams = new HashMap<>();
        ongoingIncome.forEach(income ->
                incomeStreams.computeIfAbsent(getIncomeType(income), t -> new ArrayList<>()).add(income)
        );
        incomeStreams.forEach((type, list) -> {
            String incomeFlow = "incomeArrives-" + type;
            String withholdingFlow = "incomeWithheld-" + type;
            String taxFlow = "taxableIncomeArrives-" + type;

            // Add the income to our core reservoir. Taxes will be removed later.
            sd.addFlow(incomeFlow).fromVoid().to(Funds).at((nil, funds) -> getTotalFlow(root.getEngine(), startTime, ongoingIncome));

            // Withhold some taxes.
            sd.addFlow(withholdingFlow).from(Funds).to(WithheldFunds).at((funds, taxes) -> {
                Double currentIncome = sd.get(incomeFlow);
                return getPersonalTaxRatePerYear(
                        startTime.plusDays((long) root.getEngine().time()).toLocalDate(),
                        type.getCurrency(),
                        currentIncome / YEAR,
                        type
                ) * YEAR;
            });

            // Track taxable income.
            if (type.isTaxed()) {
                sd.addFlow(taxFlow).fromVoid().to(getTaxableIncomeStock(type)).at((nil, funds) -> sd.get(incomeFlow));
            }
        });

        // Add one-off events.
        oneOffIncome.forEach(incomeEvent -> {
            IncomeType incomeType = getIncomeType(incomeEvent);
            OneOffAmount amount = (OneOffAmount) incomeEvent.apply(Monad.NoInput);
            double when = (double) startTime.until(amount.getTime(), ChronoUnit.DAYS);
            if (when >= 0) {

                // Add to funds, possibly taxing.
                root.addEvent(when, e -> sd.updateStock(Funds, old -> {
                    double untaxedAmount = amount.getAmount().getAsDouble();

                    // Tax appropriately, if necessary.
                    if (incomeType.isTaxed()) {
                        sd.updateStock(
                                getTaxableIncomeStock(incomeType),
                                oldTaxStock -> oldTaxStock + untaxedAmount * assumedTaxPercentForOneOffIncome/100.0
                        );
                        return old + (untaxedAmount - untaxedAmount * assumedTaxPercentForOneOffIncome/100.0);
                    } else {
                        return old + untaxedAmount;
                    }
                }));
            }
        });

        // One-off expenses.
        oneOffExpenses.forEach(expenseEvent -> {
            OneOffAmount amount = (OneOffAmount) expenseEvent.apply(Monad.NoInput);
            double when = (double) startTime.until(amount.getTime(), ChronoUnit.DAYS);
            if (when >= 0) {
                root.addEvent(when, e -> sd.updateStock(Funds, old -> old - amount.getAmount().getAsDouble()));
            }
        });
    }

    @Override
    protected Double getFunds() {
        return (Double)getRoot().getNumericSDDiagram("Money").get("Funds");
    }


    /**
     * @param incomeType
     * @return The name for the year-to-date taxable income stock for income of the given type.
     */
    private String getTaxableIncomeStock(IncomeType incomeType) {
        return String.format("Taxable Income YTD (%s)", incomeType);
    }

    /**
     * Generates the taxable amount
     *
     * @param time
     * @param amountPerYear
     * @return
     */
    private double getPersonalTaxRatePerYear(LocalDate time, Currency currency, double amountPerYear, IncomeType incomeType) {

        // Handle foreign currency.
        if (!currency.getCurrencyCode().equals("CAD")) {
            throw new IllegalStateException(String.format("Unsupported currency %s", currency));
        }

        // Figure out the currency and income type.
        TreeMap<Double, Double> federalRatesUpTo = new TreeMap<>();
        TreeMap<Double, Double> provincialRatesUpTo = new TreeMap<>();

        // Figure out the correct rate.
        if (incomeType == IncomeType.CADOtherIncome) {
            if (time.getYear() >= 2020) {
                federalRatesUpTo.put(47_630.00, 15.0);
                federalRatesUpTo.put(95_259.00, 20.5);
                federalRatesUpTo.put(147_667.00, 26.0);
                federalRatesUpTo.put(210_371.00, 29.0);
                federalRatesUpTo.put(Double.MAX_VALUE, 33.0);

                provincialRatesUpTo.put(45_225.00, 10.5);
                provincialRatesUpTo.put(129_214.00, 12.5);
                provincialRatesUpTo.put(Double.MAX_VALUE, 14.5);
            } else {
                throw new IllegalStateException("Unsupported year: " + time.getYear());
            }

            // Tax appropriately.
            double sum = 0;
            Double floor = 0d;

            // CPP
            // https://www.canada.ca/en/revenue-agency/services/tax/businesses/topics/payroll/payroll-deductions-contributions/canada-pension-plan-cpp/cpp-contribution-rates-maximums-exemptions.html
            double cppBasicPayPeriodExemption = 3500.00;
            // TODO: DOUBLE THIS IF SELF-EMPLOYED
            double cppMaximumAnnualEmployeeAndEmployerContribution = 2_898.00;
            double cppPercent = 5.25;
            double cppMaximumAnnualPensionableEarnings = 58_700.00;
            double cppToPay = Math.min(cppMaximumAnnualEmployeeAndEmployerContribution, Math.min(amountPerYear, cppMaximumAnnualPensionableEarnings) * cppPercent/100.0);
            Log.i("TAX", "CPP is " + cppToPay);
            sum += cppToPay;

            // EI
            // https://www.canada.ca/en/revenue-agency/services/tax/businesses/topics/payroll/payroll-deductions-contributions/employment-insurance-ei/manual-calculation-ei.html
            double eiMaximumAnnualInsurableEarnings = 54_200.00;
            double eiRatePercent = 1.58;
            double eiMaximumAnnualEmployeePremium = 856.36;
            double eiMaximumAnnualEmployerPremium = 1_198.90;
            double eiToPay = Math.min(eiMaximumAnnualEmployeePremium + eiMaximumAnnualEmployerPremium, Math.min(amountPerYear, eiMaximumAnnualInsurableEarnings) * eiRatePercent/100.0);
            Log.i("TAX", "EI is " + eiToPay);
            sum += eiToPay;

            // Provincial rates.
            double provincialSum = 0d;
            // TODO: Doublecheck.
            double provincialExemptAmount = 16_065.00 + cppToPay + eiToPay;
            floor = provincialExemptAmount;
            for (Double ceiling : provincialRatesUpTo.keySet()) {
                double incomeInRange = Math.min(ceiling - floor, amountPerYear - floor);
                if (incomeInRange > 0) {
                    provincialSum += incomeInRange * provincialRatesUpTo.get(ceiling) / 100.0;
                }
                if (ceiling > floor) {
                    floor = ceiling;
                }
            }
            Log.d("TAX", "Provincial tax is " + provincialSum);
            sum += provincialSum;

            // Federal rates.
            double federalSum = 0;
            // TODO: Doublecheck.
            floor = cppToPay + eiToPay + 12_069.00;
            for (Double ceiling : federalRatesUpTo.keySet()) {
                double incomeInRange = Math.min(ceiling - floor, amountPerYear - floor);
                if (incomeInRange > 0) {
                    federalSum += incomeInRange * federalRatesUpTo.get(ceiling) / 100.0;
                }
                if (ceiling > floor) {
                    floor = ceiling;
                }
            }
            Log.d("TAX", "Federal tax is " + federalSum);
            sum += federalSum;

            return sum;
        }
        // Untaxed income requires no tax.
        else if (!incomeType.isTaxed()) {
            return 0;
        }
        else {
            throw new IllegalStateException(String.format("Unsupported income type %s for currency %s", incomeType, currency));
        }


    }
}
