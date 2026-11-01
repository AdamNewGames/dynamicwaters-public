package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpringFeature.class)
public class SpringFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void dynamicwaters$noSpringsOnGorgeWalls(FeaturePlaceContext<SpringConfiguration> context,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (!HydrologyManager.riversIn(context.level().getLevel())) {
            return;
        }
        final BlockPos pos = context.origin();
        if (pos.getY() >= HydrologyManager.getSeaLevel() && (HydrologyManager.inGorgeReach(pos.getX(), pos.getZ())
                || HydrologyManager.inRavine(pos.getX(), pos.getZ()))) {
            cir.setReturnValue(false);
        }
    }
}
