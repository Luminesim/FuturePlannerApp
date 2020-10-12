package com.luminesim.futureplanner.models.bassdiffusion;

import com.luminesim.futureplanner.models.MonadUtilities;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;

import ca.anthrodynamics.indes.lang.Monad;

/**
 * An entity with adopters who eventually fall off the bandwagon.
 */
public interface HasAdopterLifespan {
    double getAverageMonthsThatAdoptersUseProduct();

    /**
     * A good default value for the average lifespan of a retained freemium app user.
     */
    double FREEMIUM_APP_LIFESPAN_MONTHS = 1;

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasAdopterLifespan.class,
                "Lifespan",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
