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
        super(
                getDefaultInfo(),
                x -> true,
                new Class[]{Number.class},
                new String[]{additionParamName});
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
        return in.andThen((DoubleSupplier)() -> in.as(DoubleSupplier.class).getAsDouble() * (1 + (Double) params[0]/100.0));
    }
}
