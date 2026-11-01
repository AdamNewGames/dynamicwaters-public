package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(WorldCarver.class)
public class WorldCarverMixin {
    @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
    private void dynamicwaters$protectRiversFromCarving(CarvingContext context, CarverConfiguration config,
                                                        ChunkAccess chunk, Function<BlockPos, Holder<Biome>> posToBiome,
                                                        CarvingMask mask, BlockPos.MutableBlockPos pos,
                                                        BlockPos.MutableBlockPos nextPos, Aquifer aquifer,
                                                        MutableBoolean isSurface, CallbackInfoReturnable<Boolean> cir) {
        if (HydrologyManager.isRiverOrWallBlock(pos.getX(), pos.getY(), pos.getZ())) {
            cir.setReturnValue(false);
        }
    }
}
