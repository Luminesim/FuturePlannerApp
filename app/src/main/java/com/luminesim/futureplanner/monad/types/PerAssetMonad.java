package com.luminesim.futureplanner.monad.types;

import com.luminesim.futureplanner.models.AssetType;
import com.luminesim.futureplanner.models.Qualifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;

import ca.anthrodynamics.indes.lang.Monad;
import ca.anthrodynamics.indes.lang.Traits;

/**
 * Args are (AssetType, Qualifier Name, Qualifier Value, Qualifier Name, Qualifier Option...)
 */
public class PerAssetMonad extends Monad<DoubleSupplier, DoubleSupplier> {

    public PerAssetMonad() {
        super(
                DoubleSupplier.class,
                DoubleSupplier.class,
                // Can only do work if we have access to the root model.
                //in -> in.getProperties().canDuckTypeAs(SuppliesRootModel.class)
                String.class,
                "asset type"
        );
    }

    @Override
    protected Traits apply(Traits in, Object[] objects) {
        // The asset type is the first argument.
        // Qualifiers follow.
        AssetType assetType = new AssetType((String) objects[0]);
        Map<String, Set<Qualifier>> qualifiers = new HashMap<>();
        for (int i = 1; i < objects.length - 1; i += 2) {
            String qualifierName = (String) objects[i];
            Qualifier qualifierValue = new Qualifier((String) objects[i + 1]);
            qualifiers.put(qualifierName, new HashSet<>(Arrays.asList(qualifierValue)));
        }

        // Existing value * count e.g. $10 * number of turkey sandwiches
        return in.andThen((DoubleSupplier) () -> {
                    double existingAmount = in.as(DoubleSupplier.class).getAsDouble();
                    double assetCount = in.as(SuppliesRootModel.class).getRootModel().getCount(assetType, qualifiers);
                    return existingAmount * assetCount;
                }
        );
    }
}
