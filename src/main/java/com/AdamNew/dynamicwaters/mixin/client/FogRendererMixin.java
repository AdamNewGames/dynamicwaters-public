package com.AdamNew.dynamicwaters.mixin.client;

import com.AdamNew.dynamicwaters.client.ClearRiverWater;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void dynamicwaters$clearRiverWater(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance,
            boolean shouldCreateFog, float partialTick, CallbackInfo ci) {
        ClearRiverWater.apply(camera, farPlaneDistance);
    }
}
