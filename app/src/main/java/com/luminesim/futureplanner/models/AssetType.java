package com.luminesim.futureplanner.models;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * An asset like a subscriber, user, computer, etc.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class AssetType {
    @NonNull
    private String name;
}
