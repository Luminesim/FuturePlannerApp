package com.luminesim.futureplanner.monad.types;

import java.time.LocalDateTime;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.HasTimeAssignment;

/**
 * An amount that should be processed exactly once.
 */
public interface OneOffAmount extends HasTimeAssignment {
    /**
     * @return
     *  The amount to use, calculated at the moment it is to be processed.
     */
    DoubleSupplier getAmount();

    /**
     * @return
     *  The time at which the amount is to be processed. This must be set once and not changed.
     */
    LocalDateTime getTime();
}
