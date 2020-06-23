package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

public class CurrencyMonad extends Monad<Monad.None, Number> {

    public static String INFO_CURRENCY_CODE = "com.luminesim.futureplanner.INFO_CURRENCY_CODE";
    private final Currency currency;

    public CurrencyMonad(@NonNull Currency currency) {
        super(
                Optional.of(Monad.None.class),
                Optional.of(Number.class),
                x -> true,
                new Class[]{Number.class},
                new String[]{currency.getCurrencyCode()});
        this.currency = currency;
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

    /**
     * Gets information about the monad.
     *
     * @return An info package with the type of currency appended as a currency code.
     */
    @Override
    public MonadInformation<Number> getInfo() {
        return super.getInfo().copyMutable(Number.class).setProperty(INFO_CURRENCY_CODE, currency.getCurrencyCode());
    }
}
