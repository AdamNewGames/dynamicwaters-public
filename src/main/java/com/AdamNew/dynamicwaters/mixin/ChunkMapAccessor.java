package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21 {
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
    //? if >=1.21 {
    @Accessor("worldGenContext")
    net.minecraft.world.level.chunk.status.WorldGenContext dynamicwaters$worldGenContext();
    //?}
}
