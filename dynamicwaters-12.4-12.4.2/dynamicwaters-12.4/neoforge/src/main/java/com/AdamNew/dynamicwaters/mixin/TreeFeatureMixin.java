package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeFeature.class)
public class TreeFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void dynamicwaters$blockTreesInRivers(FeaturePlaceContext<TreeConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        if (!HydrologyManager.riversIn(context.level().getLevel())) {
            return;
        }
        BlockPos origin = context.origin();
        double x = origin.getX();
        double z = origin.getZ();

        com.AdamNew.dynamicwaters.river.RiverContext riverCtx = com.AdamNew.dynamicwaters.MainRivers.findNearest(x, z, HydrologyManager.getGlobalGenerator(), HydrologyManager.getDummyAccessor(), HydrologyManager.getGlobalRandomState());
        if (riverCtx != null && riverCtx.normDistSq() < 1.5) {
            if (origin.getY() < HydrologyManager.getSeaLevel() + 15) {
                cir.setReturnValue(false);
                return;
            }
        }

        HydrologyManager.WatershedContext mountainCtx = HydrologyManager.evaluateWatershedContext(x, z, HydrologyManager.getGlobalGenerator(), HydrologyManager.getDummyAccessor(), HydrologyManager.getGlobalRandomState(), false);
        if (mountainCtx.bestPath != null && mountainCtx.distance < mountainCtx.width + 2.0) {
            if (origin.getY() < mountainCtx.bestY + 15) {
                cir.setReturnValue(false);
            }
        }
    }
}
