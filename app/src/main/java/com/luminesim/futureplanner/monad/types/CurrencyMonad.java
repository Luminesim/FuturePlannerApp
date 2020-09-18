package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.NonNull;

public class CurrencyMonad extends Monad<Monad.None, MonetaryAmount> {

    public static String INFO_CURRENCY_CODE = "com.luminesim.futureplanner.INFO_CURRENCY_CODE";

    public CurrencyMonad(@NonNull Currency currency) {
        super(
                getDefaultInfo(currency),
                x -> true,
                new Class[]{Number.class},
                new String[]{currency.getCurrencyCode()});
    }

    private static MonadInformation<None, MonetaryAmount> getDefaultInfo(Currency currency) {
        MonadInformation info = new MonadInformation(Traits.infoOnly(MonetaryAmount.class), Optional.of(None.class), Optional.of(MonetaryAmount.class));
        return info;
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
        return Traits.from(new MonetaryAmount() {
            @Override
            public Currency getCurrency() {
                return Currency.getInstance((String)getParameterValues()[0]);
            }

            @Override
            public double getAsDouble() {
                return (Double) params[0];
            }
        });
    }
}
