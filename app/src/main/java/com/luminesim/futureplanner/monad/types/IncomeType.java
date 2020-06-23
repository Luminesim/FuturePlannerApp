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
    CADOtherIncome(Currency.getInstance("CAD"))
    ;

    @Getter
    private Currency currency;
    IncomeType(@NonNull Currency currency) {
        this.currency = currency;
    }

}
