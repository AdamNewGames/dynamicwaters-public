package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseGeneratorSettings.class)
public class NoiseGeneratorSettingsMixin {
    @Inject(method = "noiseRouter", at = @At("RETURN"), cancellable = true)
    private void dynamicwaters$forceRidges(CallbackInfoReturnable<NoiseRouter> cir) {
        if (com.AdamNew.dynamicwaters.Config.vanillaNormalRivers()) {
            return;
        }
        NoiseRouter original = cir.getReturnValue();

        DensityFunction.Visitor riverFlattener = fn -> {
            if (fn instanceof DensityFunctions.HolderHolder holder && holder.function().unwrapKey().isPresent()) {
                String path = holder.function().unwrapKey().get().location().getPath();
                if (path.equals("overworld/ridges") || path.endsWith("/ridges") || path.endsWith("ridges_folded")) {
                    return dynamicwaters$flattenRidges(fn);
                }
            }
            return fn;
        };

        DensityFunction newRidges = dynamicwaters$flattenRidges(original.ridges());

        DensityFunction newInitialDensity = original.initialDensityWithoutJaggedness().mapAll(riverFlattener);
        DensityFunction newFinalDensity = original.finalDensity().mapAll(riverFlattener);

        NoiseRouter newRouter = new NoiseRouter(
                original.barrierNoise().mapAll(riverFlattener),
                original.fluidLevelFloodednessNoise(),
                original.fluidLevelSpreadNoise(),
                original.lavaNoise(),
                original.temperature(),
                original.vegetation(),
                original.continents(),
                original.erosion(),
                original.depth(),
                newRidges,
                newInitialDensity,
                newFinalDensity,
                original.veinToggle(),
                original.veinRidged(),
                original.veinGap());

        cir.setReturnValue(newRouter);
    }

    private DensityFunction dynamicwaters$flattenRidges(DensityFunction originalRidges) {
        DensityFunction arg2 = DensityFunctions.rangeChoice(
                originalRidges, -1000000.0, 0.0,
                DensityFunctions.constant(-1.0),
                DensityFunctions.constant(1.0));

        DensityFunction whenInRange = DensityFunctions.mul(DensityFunctions.constant(0.1), arg2);

        return DensityFunctions.rangeChoice(
                originalRidges, -0.15, 0.15,
                whenInRange,
                originalRidges);
    }
}
