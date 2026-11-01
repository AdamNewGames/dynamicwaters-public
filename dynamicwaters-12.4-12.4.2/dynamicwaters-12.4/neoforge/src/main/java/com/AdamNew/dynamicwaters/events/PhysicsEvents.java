package com.AdamNew.dynamicwaters.events;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.SableCompat;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = DynamicWaters.MODID)
public class PhysicsEvents {
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SableCompat.tickPhysicsShips(serverLevel);
        }
    }
}
