package com.AdamNew.dynamicwaters.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SafeItemIcon {
    private static final Set<Item> BROKEN = ConcurrentHashMap.newKeySet();

    private SafeItemIcon() {
    }

    public static void render(GuiGraphics g, ItemStack stack, int x, int y, Runnable fallback) {
        if (stack.isEmpty() || BROKEN.contains(stack.getItem())) {
            if (fallback != null) fallback.run();
            return;
        }
        final PoseStack.Pose top = g.pose().last();
        try {
            g.renderItem(stack, x, y);
        } catch (Throwable t) {
            while (g.pose().last() != top && !g.pose().clear()) g.pose().popPose();
            com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
            BROKEN.add(stack.getItem());
            if (fallback != null) fallback.run();
        }
    }
}
