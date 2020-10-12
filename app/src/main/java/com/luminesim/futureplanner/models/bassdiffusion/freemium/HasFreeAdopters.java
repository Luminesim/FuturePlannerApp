package com.luminesim.futureplanner.models.bassdiffusion.freemium;

import com.luminesim.futureplanner.models.MonadUtilities;

import ca.anthrodynamics.indes.lang.Monad;

public interface HasFreeAdopters {

    double getFreeAdopters();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasFreeAdopters.class,
                "Free Adopters",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
