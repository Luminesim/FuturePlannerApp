package com.luminesim.futureplanner.monad.types;

import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

/**
 * Assigns an income type to a monad.
 */
public class IncomeTypeMonad extends Monad<Number, Number> {

    public static String INFO_INCOME_TYPE = "com.luminesim.futureplanner.INFO_INCOME_TYPE";
    private IncomeType incomeType;

    /**
     * Can only apply this monad to numbers without an income type.
     *
     * @param incomeType
     */
    public IncomeTypeMonad(@NonNull IncomeType incomeType) {
        super(Optional.of(Number.class),
                Optional.of(Number.class),
                monad -> !monad.getInfo().getProperties().containsKey(INFO_INCOME_TYPE),
                new Class[0],
                new String[0]);
        this.incomeType = incomeType;
    }

    @Override
    protected Number apply(Number number, Object[] objects) {
        return number;
    }

    /**
     * Gets information about the monad.
     *
     * @return An info package with the type of income appended.
     */
    @Override
    public MonadInformation<Number> getInfo() {
        return super.getInfo().copyMutable(Number.class).setProperty(INFO_INCOME_TYPE, incomeType);
    }
}
