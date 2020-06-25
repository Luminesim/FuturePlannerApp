package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

/**
 * Assigns an income type to a monad.
 */
public class IncomeTypeMonad extends Monad<Number, Number> {

    public static String INFO_INCOME_TYPE = "com.luminesim.futureplanner.INFO_INCOME_TYPE";

    /**
     * Can only apply this monad to numbers without an income type.
     *
     * @param incomeType
     */
    public IncomeTypeMonad(@NonNull IncomeType incomeType) {
        super(
                getDefaultInfo(incomeType),
                monad -> !monad.getProperties().containsKey(INFO_INCOME_TYPE),
                new Class[0],
                new String[0]);
    }

    @Override
    protected Number apply(Number number, Object[] objects) {
        return number;
    }

    private static MonadInformation<Number, Number> getDefaultInfo(@NonNull IncomeType incomeType) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INFO_INCOME_TYPE, incomeType);
        MonadInformation info = new MonadInformation(properties, Optional.of(Number.class), Optional.of(Number.class));
        return info;
    }
}
