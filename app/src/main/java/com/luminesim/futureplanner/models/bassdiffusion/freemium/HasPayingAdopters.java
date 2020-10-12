package com.luminesim.futureplanner.models.bassdiffusion.freemium;

import com.luminesim.futureplanner.models.MonadUtilities;

import ca.anthrodynamics.indes.lang.Monad;

public interface HasPayingAdopters {

    double getPayingAdopters();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasPayingAdopters.class,
                "Paying Adopters",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
