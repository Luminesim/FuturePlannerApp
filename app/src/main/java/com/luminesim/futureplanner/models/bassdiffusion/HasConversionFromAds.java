package com.luminesim.futureplanner.models.bassdiffusion;

import com.luminesim.futureplanner.models.MonadUtilities;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;

import ca.anthrodynamics.indes.lang.Monad;

public interface HasConversionFromAds {
    double getConversionPercentPerTimeUnit();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasConversionFromAds.class,
                "Conversion Percent",
                Double.class,
                params -> () -> (double) params[0]
        );
    }
}
