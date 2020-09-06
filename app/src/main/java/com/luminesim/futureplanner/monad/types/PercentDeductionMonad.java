package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

public class PercentDeductionMonad extends Monad<Number, Number> {

    public PercentDeductionMonad(@NonNull String deductionParamName) {
        super(
                getDefaultInfo(),
                x -> true,
                new Class[]{Number.class},
                new String[]{deductionParamName});

        setParameterPrecondition(0, "percent must be in range 0-100", value -> 0 <= (double)value && (double)value <= 100);
    }

    private static MonadInformation<Number, Number> getDefaultInfo() {
        Map<String, Object> properties = new HashMap<>();
        MonadInformation info = new MonadInformation(properties, Optional.of(Number.class), Optional.of(Number.class));
        return info;
    }

    /**
     * Returns the value with deduction applied.
     *
     * @param in
     * @return
     */
    @Override
    protected Double apply(Number in, Object[] params) {

        // Assign the money value.
        return in.doubleValue() * (1 - (Double) params[0]/100.0);
    }
}
