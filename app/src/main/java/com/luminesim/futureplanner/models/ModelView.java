package com.luminesim.futureplanner.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;

/**
 * A model.
 */
public interface ModelView {
    Set<AssetType> getAssetTypes();
    Map<String, Set<Qualifier>> getAssetQualifiers(@NonNull AssetType assetType);
    double getCount(@NonNull AssetType assetType, @NonNull Map<String, Set<Qualifier>> qualifiers);

    /**
     * @return
     *  The root model that this model can reference to gain whole-of-simulation information.
     */
    ModelView getRootModel();
}
