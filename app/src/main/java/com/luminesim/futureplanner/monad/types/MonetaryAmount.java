package com.luminesim.futureplanner.monad.types;

import java.util.Currency;
import java.util.function.DoubleSupplier;

import lombok.NoArgsConstructor;

public interface MonetaryAmount extends DoubleSupplier {
    Currency getCurrency();
}
