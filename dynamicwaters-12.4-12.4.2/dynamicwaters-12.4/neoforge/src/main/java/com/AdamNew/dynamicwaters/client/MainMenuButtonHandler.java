package com.AdamNew.dynamicwaters.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.AbstractWidget;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

@EventBusSubscriber(modid = "dynamicwaters", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class MainMenuButtonHandler {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            if (com.AdamNew.dynamicwaters.Config.SHOW_MAIN_MENU_BUTTON.get()) {
                int targetX = event.getScreen().width / 2 + 104 + 24;
                int targetY = event.getScreen().height / 4 + 48 + 24 * 2;

                for (net.minecraft.client.gui.components.events.GuiEventListener listener : event.getListenersList()) {
                    if (listener instanceof AbstractWidget widget) {
                        if (widget.getMessage().getString().equals(net.minecraft.client.resources.language.I18n.get("fml.menu.mods")) ||
                            widget.getMessage().getString().equals(net.minecraft.client.resources.language.I18n.get("fml.menu.mods.title"))) {
                            targetX = widget.getX() + widget.getWidth() + 4;
                            targetY = widget.getY();
                            break;
                        }
                    }
                }

                event.addListener(Button.builder(Component.literal("D").withStyle(net.minecraft.ChatFormatting.AQUA), (button) -> {
                    Minecraft.getInstance().setScreen(new com.AdamNew.dynamicwaters.client.gui.DWConfigScreen(event.getScreen()));
                })
                .bounds(targetX, targetY, 20, 20)
                .tooltip(Tooltip.create(Component.literal("Dynamic Waters Config")))
                .build());
            }
        }
    }
}
