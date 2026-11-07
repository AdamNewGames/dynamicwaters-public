package com.AdamNew.dynamicwaters.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.DynamicWaters;

@Mixin(Screen.class)
public abstract class ConfigurationScreenMixin {
    private static PanoramaRenderer DYNAMIC_WATERS_PANORAMA;

    @Inject(method = {"renderBackground", "m_280273_"}, at = @At("HEAD"), cancellable = true)
    //? if >=1.20.5 {
    private void onRenderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    //?} else {
    /*private void onRenderBackground(net.minecraft.client.gui.GuiGraphics graphics, CallbackInfo ci) {
    *///?}
        Screen screen = (Screen) (Object) this;
        String className = screen.getClass().getSimpleName();
        if (className.contains("Config") || className.contains("Configuration")) {
            if (Config.CUSTOM_MAIN_MENU_PANORAMA != null && Config.CUSTOM_MAIN_MENU_PANORAMA.get()) {
                if (DYNAMIC_WATERS_PANORAMA == null) {
                    CubeMap customCubeMap = new CubeMap(ResourceLocation.fromNamespaceAndPath(DynamicWaters.MODID, "textures/gui/title/background/panorama"));
                    DYNAMIC_WATERS_PANORAMA = new PanoramaRenderer(customCubeMap);
                }

                //? if >=1.20.5 {
                DYNAMIC_WATERS_PANORAMA.render(graphics, 0, 0, partialTick, com.AdamNew.dynamicwaters.client.gui.DWScreen.panoramaTurn());
                //?} else {
                /*DYNAMIC_WATERS_PANORAMA.render(net.minecraft.client.Minecraft.getInstance().getDeltaFrameTime(), 1.0F);
                *///?}

                graphics.fill(0, 0, screen.width, screen.height, 0xAA000000);
                ci.cancel();
            }
        }
    }
}
