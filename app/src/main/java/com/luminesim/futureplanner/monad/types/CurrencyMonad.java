package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

public class CurrencyMonad extends Monad<Monad.None, Number> {

    public static String INFO_CURRENCY_CODE = "com.luminesim.futureplanner.INFO_CURRENCY_CODE";

    public CurrencyMonad(@NonNull Currency currency) {
        super(
                getDefaultInfo(currency),
                x -> true,
                new Class[]{Number.class},
                new String[]{currency.getCurrencyCode()});
    }

    private static MonadInformation<None, Number> getDefaultInfo(Currency currency) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INFO_CURRENCY_CODE, currency.getCurrencyCode());
        MonadInformation info = new MonadInformation(properties, Optional.of(None.class), Optional.of(Number.class));
        return info;
    }

    /**
     * Returns the value.
     *
     * @param in
     * @return
     */
    @Override
    protected Double apply(Monad.None in, Object[] params) {
        // Assign the money value.
        return (Double) params[0];
    }
}
