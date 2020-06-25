package com.luminesim.futureplanner.monad.types;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.MonadInformation;
import ca.anthrodynamics.indes.lang.Rate;
import ca.anthrodynamics.indes.lang.ScheduledRate;
import lombok.NonNull;

/**
 * Causes an amount to occur on a set date.
 */
public class OnDateMonad extends Monad<Number, OneOffAmount> {

    public static final String PROPERTY_HAS_DATE = "com.luminesim.futureplanner.PROPERTY_HAS_DATE";

    public OnDateMonad(@NonNull String localDateName) {
        super(
                getDefaultInfo(),
                // Input rate cannot already have a starting date.
                other -> !((boolean)other.getProperties().getOrDefault(PROPERTY_HAS_DATE, false)),
                new Class[] {LocalDateTime.class},
                new String[] {localDateName}
        );
    }

    private static MonadInformation<Number, OneOffAmount> getDefaultInfo() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_HAS_DATE, true);
        return new MonadInformation<>(properties, Optional.of(Number.class), Optional.of(OneOffAmount.class));
    }

    @Override
    protected OneOffAmount apply(Number number, Object[] params) {
        return new OneOffAmount() {
            @Override
            public DoubleSupplier getAmount() {
                return () -> number.doubleValue();
            }

            @Override
            public LocalDateTime getTime() {
                return ((LocalDateTime) params[0]);
            }
        };
    }
}
