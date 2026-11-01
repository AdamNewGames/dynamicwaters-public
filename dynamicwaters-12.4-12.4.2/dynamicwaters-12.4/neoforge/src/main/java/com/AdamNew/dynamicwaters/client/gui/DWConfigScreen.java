package com.AdamNew.dynamicwaters.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class DWConfigScreen extends Screen {
    private static final CubeMap CUBE_MAP = new CubeMap(
            ResourceLocation.fromNamespaceAndPath("dynamicwaters", "textures/gui/title/background/panorama"));
    private static final PanoramaRenderer PANORAMA = new PanoramaRenderer(CUBE_MAP);

    private static final int BOX_WIDTH = 400;
    private static final int BUTTON_WIDTH = 190;
    private static final int BUTTON_GAP = 6;
    private static final int SUBTITLE_Y = 30;
    private static final int SUBTITLE_LINE = 10;

    private final Screen lastScreen;
    private float time;

    public DWConfigScreen(Screen lastScreen) {
        super(Component.translatable("dynamicwaters.gui.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        this.lastScreen = lastScreen;
    }

    private List<net.minecraft.util.FormattedCharSequence> subtitle = List.of();

    @Override
    protected void init() {
        final int textWidth = Math.max(100, Math.min(BOX_WIDTH, this.width) - 20);
        final List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
        lines.addAll(this.font.split(Component.translatable("dynamicwaters.gui.subtitle1"), textWidth));
        lines.addAll(this.font.split(Component.translatable("dynamicwaters.gui.subtitle2"), textWidth));
        this.subtitle = lines;

        final List<DWConfigCategories.Category> categories = DWConfigCategories.all();
        final int left = this.width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        final int right = this.width / 2 + BUTTON_GAP / 2;

        int y = SUBTITLE_Y + SUBTITLE_LINE * lines.size() + 10;
        for (int i = 0; i < categories.size(); i++) {
            final DWConfigCategories.Category c = categories.get(i);

            final boolean alone = i == categories.size() - 1 && i % 2 == 0;
            final int x = alone ? this.width / 2 - BUTTON_WIDTH / 2 : (i % 2 == 0) ? left : right;
            addRenderableWidget(new CategoryButton(x, y, c, btn -> this.minecraft.setScreen(new DWCategoryScreen(this, c))));
            if (i % 2 == 1) y += Button.DEFAULT_HEIGHT + 4;
        }

        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.fullEditor"), btn -> openFullEditor(this.minecraft, this))
                .bounds(this.width / 2 - 100, this.height - 52, 200, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.fullEditor.tooltip")))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    static void openFullEditor(Minecraft mc, Screen parent) {
        net.neoforged.fml.ModList.get().getModContainerById("dynamicwaters").ifPresent(container ->
                mc.setScreen(new net.neoforged.neoforge.client.gui.ConfigurationScreen(container, parent,
                        (screen, type, modConfig, title) -> new DWConfigSectionScreen(screen, type, modConfig, title))));
    }

    public static void tintWater(GuiGraphics graphics, int width, int height) {
        graphics.fillGradient(0, 0, width, height, 0x66003366, 0xAA001133);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        time += partialTick;
        if (this.minecraft.level == null) {
            float blur = 0.85F;
            if (time < 20F) {
                blur = (time / 20F) * 0.75F + 0.1F;
            }
            PANORAMA.render(graphics, this.width, this.height, blur, partialTick);
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }

        final int x0 = this.width / 2 - BOX_WIDTH / 2, x1 = this.width / 2 + BOX_WIDTH / 2;
        graphics.fill(x0, 0, x1, this.height, 0x66000000);
        graphics.fill(x0 - 1, 0, x0, this.height, 0x66999999);
        graphics.fill(x1, 0, x1 + 1, this.height, 0x66999999);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        for (int i = 0; i < subtitle.size(); i++) {
            graphics.drawCenteredString(this.font, subtitle.get(i), this.width / 2, SUBTITLE_Y + SUBTITLE_LINE * i, 0x9EFFFE);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private static final class CategoryButton extends Button {
        private final DWConfigCategories.Category category;

        CategoryButton(int x, int y, DWConfigCategories.Category category, OnPress onPress) {
            super(x, y, BUTTON_WIDTH, DEFAULT_HEIGHT, category.title(), onPress, DEFAULT_NARRATION);
            this.category = category;
            setTooltip(Tooltip.create(category.description()));
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final Component saved = getMessage();
            setMessage(Component.empty());
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            setMessage(saved);

            final var font = Minecraft.getInstance().font;
            final int textY = getY() + (getHeight() - 8) / 2;

            graphics.renderItem(new net.minecraft.world.item.ItemStack(category.icon()), getX() + 3, getY() + 2);
            final boolean enabled = category.enabled().getAsBoolean();
            final int colour = !this.active ? 0xA0A0A0 : enabled ? 0xFFFFFF : 0x8A8A8A;
            graphics.drawString(font, category.title(), getX() + 23, textY, colour);
            Component status = category.status().get();
            if (!status.getString().isEmpty()) {
                final int nameEnd = getX() + 23 + font.width(category.title());
                final int avail = getX() + getWidth() - 6 - nameEnd - 8;
                if (font.width(status) > avail) {
                    final String cut = font.plainSubstrByWidth(status.getString(), Math.max(0, avail - font.width("...")));
                    status = Component.literal(cut.stripTrailing() + "...").withStyle(ChatFormatting.AQUA);
                }
                final int sw = font.width(status);
                graphics.drawString(font, status, getX() + getWidth() - 6 - sw, textY, colour);
            }
        }
    }
}
