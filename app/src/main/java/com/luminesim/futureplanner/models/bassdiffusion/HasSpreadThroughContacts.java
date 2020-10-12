package com.luminesim.futureplanner.models.bassdiffusion;

import com.luminesim.futureplanner.models.MonadUtilities;
import com.luminesim.futureplanner.models.bassdiffusion.freemium.IsPartialFreemiumBassDiffusionModel;

import ca.anthrodynamics.indes.lang.Monad;

public interface HasSpreadThroughContacts {
    double getConversionsPerTimeUnit();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasSpreadThroughContacts.class,
                "Conversion",
                Double.class,
                params -> () -> (double) params[0]
        );
    }
}
