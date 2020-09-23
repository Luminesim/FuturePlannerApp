package com.luminesim.futureplanner.models;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;

/**
 * An asset like a subscriber, user, computer, etc.
 */
@AllArgsConstructor
@Getter
public class AssetType {
    @NonNull
    private String name;
}
