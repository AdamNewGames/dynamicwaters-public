package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.fluid.RiverWaterFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, DynamicWaters.MODID);

    public static final DeferredHolder<Fluid, Fluid> RIVER_WATER = FLUIDS.register("river_water", RiverWaterFluid::new);
}
