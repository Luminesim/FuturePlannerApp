package com.luminesim.futureplanner.monad.types;

import java.util.Currency;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A type of income linked to a currency.
 */
@RequiredArgsConstructor
public enum IncomeType {
    /**
     * The default type of CAD income.
     */
    CADOtherIncome(true, Currency.getInstance("CAD")),

    /**
     * Income that is completely untaxed.
     * E.g. tax refund from previous year.
     */
    CADUntaxed(false, Currency.getInstance("CAD"))
    ;

    @Getter
    private boolean isTaxed;

    @Getter
    private Currency currency;
    IncomeType(boolean isTaxed, @NonNull Currency currency) {
        this.isTaxed = isTaxed;
        this.currency = currency;
    }

}
