package com.luminesim.futureplanner.models.bassdiffusion;

import com.luminesim.futureplanner.models.MonadUtilities;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;

import ca.anthrodynamics.indes.lang.Monad;

/**
 * An entity with potential adopters.
 */
public interface HasPotentialAdopters {
    double getPotentialAdopters();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasPotentialAdopters.class,
                "Potential Adopters",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
