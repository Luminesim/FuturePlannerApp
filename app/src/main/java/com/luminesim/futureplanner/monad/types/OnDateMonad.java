package com.luminesim.futureplanner.monad.types;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.HasTimeAssignment;
import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Traits;
import lombok.NonNull;

/**
 * Causes an amount to occur on a set date.
 */
public class OnDateMonad extends Monad<DoubleSupplier, OneOffAmount> {

    public static final String PROPERTY_HAS_DATE = "com.luminesim.futureplanner.PROPERTY_HAS_DATE";

    public OnDateMonad(@NonNull String localDateName) {
        super(
                getDefaultInfo(),
                // Input amount cannot already have a date.
                other -> !other.getProperties().canDuckTypeAs(HasTimeAssignment.class),
                new Class[] {LocalDateTime.class},
                new String[] {localDateName}
        );
    }

    private static MonadInformation<DoubleSupplier, OneOffAmount> getDefaultInfo() {
        return new MonadInformation<>(Traits.infoOnly(OneOffAmount.class), Optional.of(DoubleSupplier.class), Optional.of(OneOffAmount.class));
    }

    @Override
    protected Traits apply(Traits in, Object[] params) {
        return in.andThen(new OneOffAmount() {
            @Override
            public DoubleSupplier getAmount() {
                return () -> in.as(DoubleSupplier.class).getAsDouble();
            }

            @Override
            public LocalDateTime getTime() {
                return ((LocalDateTime) params[0]);
            }
        });
    }
}
