package com.luminesim.futureplanner.models.bassdiffusion;

import com.luminesim.futureplanner.models.MonadUtilities;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;

import ca.anthrodynamics.indes.lang.Monad;

/**
 * An entity with adopters who may or may not try more than once.
 */
public interface HasAdopterRetention {
    double getPercentFirstTimeUsersRetained();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasAdopterRetention.class,
                "Retention",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
