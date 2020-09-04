package com.luminesim.futureplanner.monad.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import lombok.NonNull;

public class PercentAdditionMonad extends Monad<Number, Number> {

    public PercentAdditionMonad(@NonNull String additionParamName) {
        super(
                getDefaultInfo(),
                x -> true,
                new Class[]{Number.class},
                new String[]{additionParamName});
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
        return in.doubleValue() * (1 + (Double) params[0]/100.0);
    }
}
