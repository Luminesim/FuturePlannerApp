package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.NonNull;

public class CurrencyMonad extends Monad<SuppliesRootModel, MonetaryAmount> {

    public static String INFO_CURRENCY_CODE = "com.luminesim.futureplanner.INFO_CURRENCY_CODE";

    public CurrencyMonad(@NonNull Currency currency) {
        super(
                SuppliesRootModel.class,
                MonetaryAmount.class,
                Number.class,
                currency.getCurrencyCode()
        );
    }

    /**
     * Returns the value.
     *
     * @param in
     * @return
     */
    @Override
    protected Traits apply(Traits in, Object[] params) {
        // Assign the money value.
        return in.andThen(new MonetaryAmount() {
            @Override
            public Currency getCurrency() {
                return Currency.getInstance((String) getParameterValues()[0]);
            }

            @Override
            public double getAsDouble() {
                return (Double) params[0];
            }
        });
    }
}
