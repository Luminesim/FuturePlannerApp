package com.luminesim.futureplanner.models.bassdiffusion.freemium;

import com.luminesim.futureplanner.models.bassdiffusion.HasConversionFromAds;
import com.luminesim.futureplanner.models.bassdiffusion.HasPotentialAdopters;
import com.luminesim.futureplanner.models.bassdiffusion.HasSpreadThroughContacts;

/**
 * A complete bass diffusion has its potential adopters specified
 * and the power of ads and WOM.
 * All other options can be inferred from default parameters.
 */
public interface IsCompleteFreemiumBassDiffusionModel extends
        IsPartialFreemiumBassDiffusionModel,
        HasPotentialAdopters,
        HasConversionFromAds,
        HasSpreadThroughContacts {
}
