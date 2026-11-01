package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.client.particle.RiverBubbleParticle;
import com.AdamNew.dynamicwaters.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DynamicWaters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet((net.minecraft.core.particles.SimpleParticleType) ModParticles.RIVER_BUBBLE.get(), RiverBubbleParticle.Provider::new);
    }
}
