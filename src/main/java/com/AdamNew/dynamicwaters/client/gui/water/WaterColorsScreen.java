package com.AdamNew.dynamicwaters.client.gui.water;

import com.AdamNew.dynamicwaters.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WaterColorsScreen extends com.AdamNew.dynamicwaters.client.gui.DWScreen {
    private static final int ROW = 22;

    enum Kind { MOUNTAIN, LOWLAND, BIOME, BUILT_IN }

    record Row(Kind kind, String name, int color) {
        Component label() {
            return switch (kind) {
                case MOUNTAIN -> Component.translatable("dynamicwaters.gui.waterColors.mountain");
                case LOWLAND -> Component.translatable("dynamicwaters.gui.waterColors.lowland");
                case BIOME, BUILT_IN -> Component.literal(name);
            };
        }

        int group() {
            return switch (kind) {
                case MOUNTAIN, LOWLAND -> 0;
                case BIOME -> 1;
                case BUILT_IN -> 2;
            };
        }
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private int selected = -1;
    private int scroll;
    private int listX, listY, listW, visible;
    private Button editButton, deleteButton;
    private long lastClick;

    public WaterColorsScreen(Screen parent) {
        super(Component.translatable("dynamicwaters.gui.waterColors.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        this.parent = parent;
        load();
    }

    private void load() {
        rows.clear();
        rows.add(new Row(Kind.MOUNTAIN, "", colorOr(Config.MOUNTAIN_RIVER_WATER_COLOR.get(), 0x2E9FC4)));
        rows.add(new Row(Kind.LOWLAND, "", colorOr(Config.LOWLAND_RIVER_WATER_COLOR.get(), 0x4279D2)));
        final java.util.Set<String> own = new java.util.HashSet<>();
        for (String entry : Config.BIOME_WATER_COLORS.get()) {
            final int eq = entry.lastIndexOf('=');
            if (eq <= 0) continue;
            final int color = Config.hexColor(entry.substring(eq + 1));
            if (color < 0) continue;
            final String name = entry.substring(0, eq).trim();
            rows.add(new Row(Kind.BIOME, name, color));
            own.add(name);
        }
        for (java.util.Map.Entry<net.minecraft.resources.ResourceLocation, Integer> b
                : com.AdamNew.dynamicwaters.client.RiverWaterColor.builtInColors().entrySet()) {
            final String name = b.getKey().toString();
            if (!own.contains(name)) rows.add(new Row(Kind.BUILT_IN, name, b.getValue()));
        }
    }

    private static boolean hasBuiltIn(String name) {
        final net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(name);
        return id != null && com.AdamNew.dynamicwaters.client.RiverWaterColor.builtInColors().containsKey(id);
    }

    private int find(Kind kind, String name) {
        for (int i = 0; i < rows.size(); i++) {
            final Row r = rows.get(i);
            if (r.kind() == kind && (kind != Kind.BIOME || r.name().equals(name))) return i;
        }
        return -1;
    }

    private static int colorOr(String hex, int fallback) {
        final int c = Config.hexColor(hex);
        return c >= 0 ? c : fallback;
    }

    static String hex(int color) {
        return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF);
    }

    private void save() {
        final List<String> biomes = new ArrayList<>();
        for (Row r : rows) {
            switch (r.kind()) {
                case MOUNTAIN -> Config.MOUNTAIN_RIVER_WATER_COLOR.set(hex(r.color()));
                case LOWLAND -> Config.LOWLAND_RIVER_WATER_COLOR.set(hex(r.color()));
                case BIOME -> biomes.add(r.name() + "=" + hex(r.color()));
                case BUILT_IN -> { }
            }
        }
        Config.BIOME_WATER_COLORS.set(List.copyOf(biomes));
        Config.MOUNTAIN_RIVER_WATER_COLOR.save();
    }

    void accept(int index, Row row) {
        final boolean inPlace = index >= 0 && index < rows.size() && rows.get(index).kind() != Kind.BUILT_IN;
        final int same = row.kind() == Kind.BIOME ? find(Kind.BIOME, row.name()) : -1;
        if (inPlace) {
            rows.set(index, row);
            if (same >= 0 && same != index) rows.remove(same);
        } else if (same >= 0) {
            rows.set(same, row);
        } else {
            rows.add(row);
        }
        save();
        load();
        selected = find(row.kind(), row.name());
        updateButtons();
    }

    @Override
    protected void init() {
        listW = Math.min(380, this.width - 40);
        listX = (this.width - listW) / 2;
        listY = 36;
        visible = Math.max(1, (this.height - listY - 60) / ROW);
        scroll = Mth.clamp(scroll, 0, maxScroll());
        final int cx = this.width / 2;
        editButton = addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.banks.edit"), b -> edit(selected))
                .bounds(cx - 154, this.height - 52, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.waterColors.add"), b -> edit(-1))
                .bounds(cx - 50, this.height - 52, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.waterColors.add.tooltip")))
                .build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.banks.delete"), b -> delete())
                .bounds(cx + 54, this.height - 52, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx - 75, this.height - 28, 150, 20).build());
        updateButtons();
    }

    private void updateButtons() {
        if (editButton != null) editButton.active = selected >= 0 && selected < rows.size();
        if (deleteButton != null) deleteButton.active = selected >= 0 && selected < rows.size() && rows.get(selected).kind() == Kind.BIOME;
    }

    private int maxScroll() {
        return Math.max(0, rows.size() - visible);
    }

    private void edit(int index) {
        Row row = index >= 0 && index < rows.size() ? rows.get(index) : new Row(Kind.BIOME, "", 0x3F76E4);
        if (row.kind() == Kind.BUILT_IN) row = new Row(Kind.BIOME, row.name(), row.color());
        this.minecraft.setScreen(new WaterColorEditScreen(this, index >= 0 && index < rows.size() ? index : -1, row));
    }

    private void delete() {
        if (selected < 0 || selected >= rows.size() || rows.get(selected).kind() != Kind.BIOME) return;
        final int index = selected;
        final String name = rows.get(index).name();
        this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                rows.remove(index);
                selected = -1;
                save();
                load();
                updateButtons();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("dynamicwaters.gui.waterColors.deleteConfirm", name),
                Component.translatable(hasBuiltIn(name) ? "dynamicwaters.gui.waterColors.deleteNoteBuiltIn"
                        : "dynamicwaters.gui.waterColors.deleteNote")));
    }

    private int at(double mx, double my) {
        if (mx < listX || mx >= listX + listW || my < listY || my >= listY + visible * ROW) return -1;
        final int i = scroll + (int) ((my - listY) / ROW);
        return i < rows.size() ? i : -1;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        com.AdamNew.dynamicwaters.client.gui.DWConfigScreen.tintWater(g, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("dynamicwaters.gui.waterColors.subtitle"), this.width / 2, 21, 0x9EFFFE);
        g.fill(listX - 2, listY - 2, listX + listW + 2, listY + visible * ROW + 2, 0x80000000);
        final boolean riversOn = Config.RIVER_WATER_COLOR.get();
        final boolean builtInOn = Config.BUILT_IN_WATER_COLORS.get();
        final int hover = at(mouseX, mouseY);
        for (int k = 0; k < visible && scroll + k < rows.size(); k++) {
            final int i = scroll + k;
            final Row r = rows.get(i);
            final int y = listY + k * ROW;
            if (i == selected) g.fill(listX, y, listX + listW, y + ROW, 0x60FFFFFF);
            else if (i == hover) g.fill(listX, y, listX + listW, y + ROW, 0x30FFFFFF);
            swatch(g, listX + 3, y + 3, 16, r.color());
            final boolean river = r.group() == 0;
            final boolean builtIn = r.kind() == Kind.BUILT_IN;
            final boolean off = river && !riversOn || builtIn && !builtInOn;
            final int text = off ? 0x808080 : builtIn ? 0xC8C8C8 : 0xFFFFFF;
            g.drawString(this.font, r.label(), listX + 24, y + 7, text);
            String note = "";
            if (river && !riversOn) note = Component.translatable("dynamicwaters.gui.waterColors.riversOff").getString();
            else if (builtIn) note = Component.translatable(builtInOn ? "dynamicwaters.gui.waterColors.builtIn"
                    : "dynamicwaters.gui.waterColors.builtInOff").getString();
            final String right = hex(r.color()) + (note.isEmpty() ? "" : "  " + note);
            g.drawString(this.font, right, listX + listW - this.font.width(right) - 4, y + 7, 0xA0A0A0);
            if (i + 1 < rows.size() && rows.get(i + 1).group() != r.group() && k + 1 < visible) {
                g.fill(listX, y + ROW - 1, listX + listW, y + ROW, 0x60FFFFFF);
            }
        }
        if (maxScroll() > 0) {
            final int barX = listX + listW + 3, total = visible * ROW;
            final int knob = Math.max(8, total * visible / (visible + maxScroll()));
            final int ky = listY + (total - knob) * scroll / maxScroll();
            g.fill(barX, listY, barX + 3, listY + total, 0x40FFFFFF);
            g.fill(barX, ky, barX + 3, ky + knob, 0xC0FFFFFF);
        }
    }

    static void swatch(GuiGraphics g, int x, int y, int size, int color) {
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        g.fill(x, y, x + size, y + size, 0xFF000000 | color);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        final int i = at(mx, my);
        if (i >= 0 && button == 0) {
            final long now = Util.getMillis();
            if (i == selected && now - lastClick < 300L) edit(i);
            selected = i;
            lastClick = now;
            updateButtons();
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
