package com.AdamNew.dynamicwaters.client.gui.water;

import com.AdamNew.dynamicwaters.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntConsumer;

final class WaterColorEditScreen extends com.AdamNew.dynamicwaters.client.gui.DWScreen {
    private final WaterColorsScreen parent;
    private final int index;
    private final WaterColorsScreen.Kind kind;
    private String name;
    private int color;
    private EditBox nameBox, hexBox;
    private Channel red, green, blue;
    private Button saveButton;

    private boolean syncing;

    WaterColorEditScreen(WaterColorsScreen parent, int index, WaterColorsScreen.Row row) {
        super(Component.translatable("dynamicwaters.gui.waterColors.editTitle"));
        this.parent = parent;
        this.index = index;
        this.kind = row.kind();
        this.name = row.name();
        this.color = row.color();
    }

    @Override
    protected void init() {
        final int cx = this.width / 2, left = cx - 100;
        int y = 44;
        if (kind == WaterColorsScreen.Kind.BIOME) {
            nameBox = new EditBox(this.font, left, y, 140, 20, Component.translatable("dynamicwaters.gui.waterColors.name"));
            nameBox.setMaxLength(128);
            nameBox.setHint(Component.literal("minecraft:river / #minecraft:is_river").withStyle(ChatFormatting.DARK_GRAY));
            nameBox.setValue(name);
            nameBox.setResponder(v -> {
                name = v.trim();
                nameBox.setTextColor(nameOk() ? 0xE0E0E0 : 0xFF5555);
                updateSave();
            });
            addRenderableWidget(nameBox);
            addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.waterColors.pick"),
                            b -> this.minecraft.setScreen(new BiomePickerScreen(this, id -> name = id)))
                    .bounds(left + 144, y, 56, 20).build());
            y += 34;
        }
        hexBox = new EditBox(this.font, left, y, 70, 20, Component.translatable("dynamicwaters.gui.waterColors.hex"));
        hexBox.setMaxLength(7);
        hexBox.setValue(WaterColorsScreen.hex(color));
        hexBox.setResponder(v -> {
            final int c = Config.hexColor(v);
            hexBox.setTextColor(c >= 0 ? 0xE0E0E0 : 0xFF5555);
            if (c >= 0 && !syncing) {
                color = c;
                syncing = true;
                red.show(c >> 16 & 255);
                green.show(c >> 8 & 255);
                blue.show(c & 255);
                syncing = false;
            }
            updateSave();
        });
        addRenderableWidget(hexBox);
        y += 26;
        red = addRenderableWidget(new Channel(left, y, "red", color >> 16 & 255, v -> setChannel(16, v)));
        green = addRenderableWidget(new Channel(left, y + 24, "green", color >> 8 & 255, v -> setChannel(8, v)));
        blue = addRenderableWidget(new Channel(left, y + 48, "blue", color & 255, v -> setChannel(0, v)));
        if (nameBox != null) nameBox.setTextColor(nameOk() ? 0xE0E0E0 : 0xFF5555);
        saveButton = addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.waterColors.save"), b -> save())
                .bounds(cx - 102, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(cx + 2, this.height - 28, 100, 20).build());
        updateSave();
    }

    private void setChannel(int shift, int value) {
        if (syncing) return;
        color = color & ~(255 << shift) | (value & 255) << shift;
        syncing = true;
        hexBox.setValue(WaterColorsScreen.hex(color));
        syncing = false;
        updateSave();
    }

    private boolean nameOk() {
        if (kind != WaterColorsScreen.Kind.BIOME) return true;
        final String n = name.startsWith("#") ? name.substring(1) : name;
        return !n.isEmpty() && n.contains(":") && ResourceLocation.tryParse(n) != null;
    }

    private void updateSave() {
        if (saveButton != null) saveButton.active = nameOk() && hexBox != null && Config.hexColor(hexBox.getValue()) >= 0;
    }

    private void save() {
        if (!nameOk()) return;
        parent.accept(index, new WaterColorsScreen.Row(kind, kind == WaterColorsScreen.Kind.BIOME ? name : "", color));
        this.minecraft.setScreen(parent);
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
        final Component what = kind == WaterColorsScreen.Kind.BIOME ? Component.translatable("dynamicwaters.gui.waterColors.name")
                : new WaterColorsScreen.Row(kind, "", color).label();
        g.drawCenteredString(this.font, what, this.width / 2, 24, 0x9EFFFE);

        final int left = this.width / 2 - 100;
        final int hexY = hexBox.getY();
        WaterColorsScreen.swatch(g, left + 78, hexY + 1, 18, color);
        g.drawString(this.font, Component.translatable("dynamicwaters.gui.waterColors.preview"), left + 102, hexY + 6, 0xC0C0C0);
        if (kind == WaterColorsScreen.Kind.BIOME) {
            final int own = BiomePickerScreen.ownColor(name);
            if (own >= 0) {
                final int y = blue.getY() + 30;
                WaterColorsScreen.swatch(g, left, y, 14, own);
                g.drawString(this.font, Component.translatable("dynamicwaters.gui.waterColors.own", WaterColorsScreen.hex(own)),
                        left + 20, y + 3, 0xA0A0A0);
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static final class Channel extends AbstractSliderButton {
        private final String key;
        private final IntConsumer onChange;

        Channel(int x, int y, String key, int value, IntConsumer onChange) {
            super(x, y, 200, 20, Component.empty(), value / 255.0);
            this.key = key;
            this.onChange = onChange;
            updateMessage();
        }

        int channel() {
            return (int) Math.round(this.value * 255.0);
        }

        void show(int v) {
            this.value = v / 255.0;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("dynamicwaters.gui.waterColors." + key, channel()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(channel());
        }
    }
}
