package com.AdamNew.dynamicwaters.mixin.client;

import com.AdamNew.dynamicwaters.client.RiverWaterColor;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeColors.class)
public class BiomeColorsMixin {
    @Shadow
    @Final
    @Mutable
    public static ColorResolver WATER_COLOR_RESOLVER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void dynamicwaters$riverWaterColor(CallbackInfo ci) {
        WATER_COLOR_RESOLVER = new RiverWaterColor(WATER_COLOR_RESOLVER);
    }
}
