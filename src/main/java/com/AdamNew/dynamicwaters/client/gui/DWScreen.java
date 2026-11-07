package com.AdamNew.dynamicwaters.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class DWScreen extends Screen {
    protected DWScreen(Component title) {
        super(title);
    }

    public static float panoramaTurn() {
        //? fabric {
        /*return net.minecraft.client.Minecraft.getInstance().getTimer().getRealtimeDeltaTicks();
        *///?} else {
        return 1.0F;
        //?}
    }

    //? if <1.20.5 {
    /*@Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return mouseScrolled(mouseX, mouseY, 0.0, delta);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX, mouseY, scrollY);
    }
    *///?}
}
