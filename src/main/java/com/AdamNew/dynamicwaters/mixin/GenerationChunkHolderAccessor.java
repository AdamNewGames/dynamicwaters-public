package com.AdamNew.dynamicwaters.mixin;

import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21 {
import org.spongepowered.asm.mixin.gen.Accessor;
//?}

//? if >=1.21 {
@Mixin(net.minecraft.server.level.GenerationChunkHolder.class)
public interface GenerationChunkHolderAccessor {
    @Accessor("futures")
    java.util.concurrent.atomic.AtomicReferenceArray<java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>>> dynamicwaters$futures();
}
//?} else {
/*@Mixin(net.minecraft.server.level.ChunkMap.class)
public interface GenerationChunkHolderAccessor {
}
*///?}
