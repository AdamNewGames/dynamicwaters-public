package com.AdamNew.dynamicwaters.client;

public final class LabClientQuit {
    private LabClientQuit() {}

    public static void quit() {
        net.minecraft.client.Minecraft.getInstance().stop();
    }
}
