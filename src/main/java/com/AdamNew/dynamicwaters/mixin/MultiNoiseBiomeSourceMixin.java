package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import com.AdamNew.dynamicwaters.MainRivers;
import com.AdamNew.dynamicwaters.river.RiverContext;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiNoiseBiomeSource.class, priority = 900)
public class MultiNoiseBiomeSourceMixin {
    @Unique
    private volatile Holder<Biome> dynamicwaters$riverBiome;

    @Unique
    private volatile Holder<Biome> dynamicwaters$frozenRiverBiome;
    @Unique
    private volatile boolean dynamicwaters$riverBiomeFound;

    @Unique
    private static final float DYNAMICWATERS_FROZEN_BELOW = 0.15f;

    @Unique
    private static final int DYNAMICWATERS_FROZEN_REACH = 3;

    @Unique
    private boolean dynamicwaters$coldLand(int x, int z, net.minecraft.world.level.biome.Climate.Sampler sampler) {
        final MultiNoiseBiomeSource source = (MultiNoiseBiomeSource) (Object) this;
        final int r = DYNAMICWATERS_FROZEN_REACH;
        final int cell = com.AdamNew.dynamicwaters.terrain.CoarseHeightField.CELL_SIZE;
        int cold = 0;
        for (int k = 0; k < 5; k++) {
            final int qx = x + (k == 1 ? r : k == 2 ? -r : 0), qz = z + (k == 3 ? r : k == 4 ? -r : 0);
            final int ground = com.AdamNew.dynamicwaters.terrain.CoarseHeightField.sample(
                    Math.floorDiv(qx * 4 + 2, cell), Math.floorDiv(qz * 4 + 2, cell),
                    HydrologyManager.getGlobalGenerator(), HydrologyManager.getGlobalRandomState());
            if (source.getNoiseBiome(qx, Math.floorDiv(ground + 2, 4), qz, sampler).value().getBaseTemperature()
                    < DYNAMICWATERS_FROZEN_BELOW && ++cold >= 2) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void dynamicwaters$injectRiverBiome(int x, int y, int z, net.minecraft.world.level.biome.Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!com.AdamNew.dynamicwaters.Config.dynamicNormalRivers()) {
            return;
        }
        if (!HydrologyManager.isGeneratingRivers() && HydrologyManager.getGlobalGenerator() != null && HydrologyManager.getGlobalRandomState() != null) {
            if (!dynamicwaters$riverBiomeFound) {
                MultiNoiseBiomeSource source = (MultiNoiseBiomeSource)(Object)this;
                for (Holder<Biome> holder : source.possibleBiomes()) {
                    if (holder.unwrapKey().isEmpty()) continue;
                    final String path = holder.unwrapKey().get().location().getPath();
                    if (dynamicwaters$riverBiome == null && path.equals("river")) {
                        dynamicwaters$riverBiome = holder;
                    } else if (dynamicwaters$frozenRiverBiome == null && path.equals("frozen_river")) {
                        dynamicwaters$frozenRiverBiome = holder;
                    }
                }
                dynamicwaters$riverBiomeFound = true;
            }
            final Holder<Biome> cachedRiverBiome = dynamicwaters$riverBiome;

            if (cachedRiverBiome != null) {
                double worldX = x * 4.0 + 2.0;
                double worldY = y * 4.0 + 2.0;
                double worldZ = z * 4.0 + 2.0;

                if (worldY > 80) {
                    return;
                }

                HydrologyManager.setGeneratingRivers(true);
                try {
                    RiverContext info = MainRivers.findNearest(worldX, worldZ, HydrologyManager.getGlobalGenerator(), HydrologyManager.getDummyAccessor(), HydrologyManager.getGlobalRandomState());

                    if (info != null && info.normDistSq() < 0.25 && !HydrologyManager.riverSplitLand(worldX, worldZ)) {
                        final Holder<Biome> frozen = dynamicwaters$frozenRiverBiome;
                        cir.setReturnValue(frozen != null && dynamicwaters$coldLand(x, z, sampler) ? frozen : cachedRiverBiome);
                        return;
                    }
                } finally {
                    HydrologyManager.setGeneratingRivers(false);
                }
            }
        }
    }

    @Inject(method = "getNoiseBiome", at = @At("RETURN"), cancellable = true)
    private void dynamicwaters$surfaceBiomeInGorges(int x, int y, int z, net.minecraft.world.level.biome.Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
        if (com.AdamNew.dynamicwaters.Config.NORMAL_RIVER_TUNNELS.get() || !com.AdamNew.dynamicwaters.Config.dynamicNormalRivers()) {
            return;
        }
        final Holder<Biome> found = cir.getReturnValue();
        if (found == null || HydrologyManager.isGeneratingRivers()
                || !found.is(com.AdamNew.dynamicwaters.platform.ModTags.IS_UNDERGROUND)
                || y * 4 + 2 < HydrologyManager.getSeaLevel() - 4
                || HydrologyManager.getGlobalGenerator() == null || HydrologyManager.getGlobalRandomState() == null) {
            return;
        }
        HydrologyManager.setGeneratingRivers(true);
        try {
            final double worldX = x * 4.0 + 2.0, worldZ = z * 4.0 + 2.0;
            if (!HydrologyManager.inGorgeReach(worldX, worldZ)) return;
            final int ground = com.AdamNew.dynamicwaters.terrain.CoarseHeightField.sample(
                    Math.floorDiv(x * 4 + 2, com.AdamNew.dynamicwaters.terrain.CoarseHeightField.CELL_SIZE),
                    Math.floorDiv(z * 4 + 2, com.AdamNew.dynamicwaters.terrain.CoarseHeightField.CELL_SIZE),
                    HydrologyManager.getGlobalGenerator(), HydrologyManager.getGlobalRandomState());
            final int surface = Math.floorDiv(ground + 2, 4);
            if (y >= surface) return;
            cir.setReturnValue(((MultiNoiseBiomeSource) (Object) this).getNoiseBiome(x, surface, z, sampler));
        } finally {
            HydrologyManager.setGeneratingRivers(false);
        }
    }
}
