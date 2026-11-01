package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.CanyonCarverConfiguration;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(CanyonWorldCarver.class)
public class CanyonCarverMixin {
    @Inject(method = "carve", at = @At("HEAD"), cancellable = true)
    private void dynamicwaters$cancelCanyonCarving(CarvingContext context, CanyonCarverConfiguration config,
                                                    ChunkAccess chunk, Function<net.minecraft.core.BlockPos, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> biomeGetter,
                                                    net.minecraft.util.RandomSource random, net.minecraft.world.level.levelgen.Aquifer aquifer,
                                                    net.minecraft.world.level.ChunkPos chunkPos, CarvingMask carvingMask,
                                                    CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
