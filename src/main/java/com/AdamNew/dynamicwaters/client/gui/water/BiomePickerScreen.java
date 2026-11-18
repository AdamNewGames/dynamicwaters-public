package com.AdamNew.dynamicwaters.client.gui.water;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class BiomePickerScreen extends com.AdamNew.dynamicwaters.client.gui.DWScreen {
    private static final int ROW = 14;

    private final Screen parent;
    private final Consumer<String> onPick;
    private final List<String> all = new ArrayList<>();
    private List<String> shown = List.of();
    private EditBox search;
    private int scroll;
    private int listX, listY, listW, visible;

    public BiomePickerScreen(Screen parent, Consumer<String> onPick) {
        super(Component.translatable("dynamicwaters.gui.waterColors.pickTitle"));
        this.parent = parent;
        this.onPick = onPick;
        final Registry<Biome> biomes = biomes();
        if (biomes != null) {
            biomes.keySet().stream().map(ResourceLocation::toString).sorted().forEach(all::add);
            biomes.getTagNames().map(t -> "#" + t.location()).sorted().forEach(all::add);
        }
    }

    private static Registry<Biome> biomes() {
        final var level = Minecraft.getInstance().level;
        return level == null ? null : level.registryAccess().registryOrThrow(Registries.BIOME);
    }

    static int ownColor(String name) {
        final Registry<Biome> biomes = biomes();
        if (biomes == null || name == null || name.startsWith("#")) return -1;
        final ResourceLocation id = ResourceLocation.tryParse(name);
        final Biome biome = id == null ? null : biomes.get(id);
        return biome == null ? -1 : biome.getWaterColor() & 0xFFFFFF;
    }

    @Override
    protected void init() {
        final String before = search != null ? search.getValue() : "";
        search = new EditBox(this.font, this.width / 2 - 100, 20, 200, 16, Component.translatable("dynamicwaters.gui.banks.search"));
        search.setHint(Component.translatable("dynamicwaters.gui.banks.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(before);
        search.setResponder(s -> refilter());
        addRenderableWidget(search);
        setInitialFocus(search);
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
        listW = Math.min(320, this.width - 40);
        listX = (this.width - listW) / 2;
        listY = 42;
        visible = Math.max(2, (this.height - listY - 34) / ROW);
        refilter();
    }

    private void refilter() {
        final String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        final List<String> out = new ArrayList<>();
        for (String id : all) {
            if (q.isEmpty() || id.contains(q)) out.add(id);
        }
        shown = out;
        scroll = 0;
    }

    private int maxScroll() {
        return Math.max(0, shown.size() - visible);
    }

    private String at(double mx, double my) {
        if (mx < listX || mx >= listX + listW || my < listY || my >= listY + visible * ROW) return null;
        final int i = scroll + (int) ((my - listY) / ROW);
        return i < shown.size() ? shown.get(i) : null;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        com.AdamNew.dynamicwaters.client.gui.DWConfigScreen.tintWater(g, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 7, 0xFFFFFF);
        g.fill(listX - 2, listY - 2, listX + listW + 2, listY + visible * ROW + 2, 0x80000000);
        if (all.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("dynamicwaters.gui.waterColors.noWorld"), this.width / 2, listY + 8, 0xA0A0A0);
            return;
        }
        final String hover = at(mouseX, mouseY);
        for (int k = 0; k < visible && scroll + k < shown.size(); k++) {
            final String id = shown.get(scroll + k);
            final int y = listY + k * ROW;
            if (id.equals(hover)) g.fill(listX, y, listX + listW, y + ROW, 0x50FFFFFF);
            final int own = ownColor(id);
            if (own >= 0) WaterColorsScreen.swatch(g, listX + 3, y + 2, 10, own);
            g.drawString(this.font, id, listX + 18, y + 3, id.startsWith("#") ? 0x9EFFFE : 0xFFFFFF);
        }
        if (shown.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("dynamicwaters.gui.waterColors.noMatch"), this.width / 2, listY + 8, 0xA0A0A0);
        }
        if (maxScroll() > 0) {
            final int barX = listX + listW + 3, total = visible * ROW;
            final int knob = Math.max(8, total * visible / (visible + maxScroll()));
            final int ky = listY + (total - knob) * scroll / maxScroll();
            g.fill(barX, listY, barX + 3, listY + total, 0x40FFFFFF);
            g.fill(barX, ky, barX + 3, ky + knob, 0xC0FFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        final String id = at(mx, my);
        if (id != null && button == 0) {
            onPick.accept(id);
            this.minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Mth.clamp(scroll - (int) Math.signum(sy), 0, maxScroll());
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
