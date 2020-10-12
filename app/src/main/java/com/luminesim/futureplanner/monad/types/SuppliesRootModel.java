package com.luminesim.futureplanner.monad.types;

import com.luminesim.futureplanner.models.ModelView;

/**
 * Something that can supply the root model in the simulation.
 */
public interface SuppliesRootModel {
    ModelView getRootModel();
}
