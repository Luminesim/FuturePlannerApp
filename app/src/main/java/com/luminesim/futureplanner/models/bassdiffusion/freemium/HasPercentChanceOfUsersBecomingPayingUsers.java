package com.luminesim.futureplanner.models.bassdiffusion.freemium;

import com.luminesim.futureplanner.models.MonadUtilities;

import ca.anthrodynamics.indes.lang.Monad;

public interface HasPercentChanceOfUsersBecomingPayingUsers {

    double getPercentChanceOfBecomingPayingUser();

    static Monad create() {
        return MonadUtilities.addNonReplaceableProperty(
                IsPartialFreemiumBassDiffusionModel.class,
                HasPercentChanceOfUsersBecomingPayingUsers.class,
                "Chance",
                Double.class,
                params -> () -> (Double) params[0]);
    }
}
