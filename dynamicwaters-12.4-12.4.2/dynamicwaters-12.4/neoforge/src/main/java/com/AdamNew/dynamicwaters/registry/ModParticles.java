package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.DynamicWaters;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DynamicWaters.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIVER_BUBBLE =
            PARTICLE_TYPES.register("river_bubble", () -> new SimpleParticleType(false));
}
