package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SurfaceSystem.class)
public interface SurfaceSystemAccessor {
    @Invoker("getBand")
    BlockState dynamicwaters$getBand(int x, int y, int z);
}
