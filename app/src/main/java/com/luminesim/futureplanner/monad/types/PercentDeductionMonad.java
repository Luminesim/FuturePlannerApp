package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.NonNull;

public class PercentDeductionMonad extends Monad<DoubleSupplier, DoubleSupplier> {

    public PercentDeductionMonad(@NonNull String deductionParamName) {
        super(
                getDefaultInfo(),
                x -> true,
                new Class[]{Number.class},
                new String[]{deductionParamName});

        setParameterPrecondition(0, "percent must be in range 0-100", value -> 0 <= (double)value && (double)value <= 100);
    }

    private static MonadInformation<DoubleSupplier, DoubleSupplier> getDefaultInfo() {
        MonadInformation info = new MonadInformation(Traits.infoOnly(DoubleSupplier.class), Optional.of(DoubleSupplier.class), Optional.of(DoubleSupplier.class));
        return info;
    }

    /**
     * Returns the value with deduction applied.
     *
     * @param in
     * @return
     */
    @Override
    protected Traits apply(Traits in, Object[] params) {
        return in.andThen((DoubleSupplier)() -> in.as(DoubleSupplier.class).getAsDouble() * (1 - (Double) params[0]/100.0));
    }
}
