package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.DynamicWaters;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    private static PanoramaRenderer DYNAMIC_WATERS_PANORAMA;

    @Inject(method = {"renderPanorama", "m_280039_"}, at = @At("HEAD"), cancellable = true)
    private void onRenderPanorama(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (Config.CUSTOM_MAIN_MENU_PANORAMA != null && Config.CUSTOM_MAIN_MENU_PANORAMA.get()) {
            if (DYNAMIC_WATERS_PANORAMA == null) {
                CubeMap customCubeMap = new CubeMap(ResourceLocation.fromNamespaceAndPath(DynamicWaters.MODID, "textures/gui/title/background/panorama"));
                DYNAMIC_WATERS_PANORAMA = new PanoramaRenderer(customCubeMap);
            }
            DYNAMIC_WATERS_PANORAMA.render(graphics, 0, 0, partialTick, 1.0F);
            ci.cancel();
        }
    }
}
