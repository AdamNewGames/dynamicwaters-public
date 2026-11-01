package com.AdamNew.dynamicwaters.terrain;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

public interface WiredRouter {
    NoiseRouter dynamicwaters$wiredRouter();

    static NoiseRouter of(RandomState randomState) {
        if ((Object) randomState instanceof WiredRouter wired) {
            final NoiseRouter router = wired.dynamicwaters$wiredRouter();
            if (router != null) return router;
        }
        return randomState.router();
    }
}
