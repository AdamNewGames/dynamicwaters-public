package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.platform.Registrar;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ModParticles {
    public static final Registrar<ParticleType<?>> PARTICLE_TYPES = Registrar.create(Registries.PARTICLE_TYPE);

    public static final Supplier<SimpleParticleType> RIVER_BUBBLE =
            //? !fabric {
            PARTICLE_TYPES.register("river_bubble", () -> new SimpleParticleType(false));
            //?} else {
            /*PARTICLE_TYPES.register("river_bubble", net.fabricmc.fabric.api.particle.v1.FabricParticleTypes::simple);
            *///?}

    public static final Supplier<SimpleParticleType> RIVER_FOAM =
            //? !fabric {
            PARTICLE_TYPES.register("river_foam", () -> new SimpleParticleType(false));
            //?} else {
            /*PARTICLE_TYPES.register("river_foam", net.fabricmc.fabric.api.particle.v1.FabricParticleTypes::simple);
            *///?}

    public static final Supplier<SimpleParticleType> RIVER_MIST =
            //? !fabric {
            PARTICLE_TYPES.register("river_mist", () -> new SimpleParticleType(false));
            //?} else {
            /*PARTICLE_TYPES.register("river_mist", net.fabricmc.fabric.api.particle.v1.FabricParticleTypes::simple);
            *///?}
}
