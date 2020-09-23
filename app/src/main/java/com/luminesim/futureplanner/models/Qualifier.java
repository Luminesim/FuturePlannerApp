package com.luminesim.futureplanner.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Qualifies an {@link #Asset}
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Qualifier {
    private final String label;
}
