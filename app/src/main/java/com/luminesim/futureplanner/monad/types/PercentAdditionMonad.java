package com.luminesim.futureplanner.monad.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.NonNull;

public class PercentAdditionMonad extends Monad<DoubleSupplier, DoubleSupplier> {

    public PercentAdditionMonad(@NonNull String additionParamName) {
        super(DoubleSupplier.class, DoubleSupplier.class, Number.class, additionParamName);
    }

    /**
     * Returns the value with deduction applied.
     *
     * @param in
     * @return
     */
    @Override
    protected Traits apply(Traits in, Object[] params) {
        return in.andThen((DoubleSupplier)() -> in.as(DoubleSupplier.class).getAsDouble() * (1 + (Double) params[0]/100.0));
    }
}
