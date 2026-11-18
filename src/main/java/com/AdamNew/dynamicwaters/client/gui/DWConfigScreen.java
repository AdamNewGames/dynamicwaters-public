package com.AdamNew.dynamicwaters.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class DWConfigScreen extends DWScreen {
    private static final CubeMap CUBE_MAP = new CubeMap(
            ResourceLocation.fromNamespaceAndPath("dynamicwaters", "textures/gui/title/background/panorama"));
    private static final PanoramaRenderer PANORAMA = new PanoramaRenderer(CUBE_MAP);

    private static final int PANEL_MAX = 760;
    private static final int SIDE_W = 170;
    private static final int CONTROL_W = 150;
    private static final int LINE = 11;
    private static final int DESC_PAD = 8;
    private static final int SIDE_GAP = 4;

    private static final int ROOMY = 320;
    private int top, footer, gap, rowH, controlW;

    private static int lastCategory;

    private final Screen lastScreen;
    private final List<DWConfigCategories.Category> categories = DWConfigCategories.all();
    private float time;
    private String query = "";
    private EditBox search;
    private OptionList list;
    private int panelX, panelW, sideW, listX, listW, listTop, listBottom, descY, descH;

    private Object subject;
    private Object shownSubject;
    private List<FormattedCharSequence> descLines = List.of();
    private int descScroll;

    private double keepScroll;

    public DWConfigScreen(Screen lastScreen) {
        super(Component.translatable("dynamicwaters.gui.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        this.lastScreen = lastScreen;
        if (lastCategory < 0 || lastCategory >= categories.size()) lastCategory = 0;
    }

    private DWConfigCategories.Category category() {
        return categories.get(lastCategory);
    }

    @Override
    protected void init() {
        final double scroll = list != null ? list.getScrollAmount() : keepScroll;
        final boolean roomy = this.height >= ROOMY;
        top = roomy ? 34 : 30;
        footer = roomy ? 34 : 30;
        gap = roomy ? 10 : 8;
        rowH = roomy ? 30 : 27;
        panelW = Math.min(PANEL_MAX, this.width - (roomy ? 24 : 16));
        panelX = (this.width - panelW) / 2;
        sideW = Mth.clamp(panelW * 28 / 100, 104, SIDE_W);
        listX = panelX + sideW + gap;
        listW = panelW - sideW - gap;
        listTop = top;

        controlW = Mth.clamp((listW - 20) * 38 / 100, 96, CONTROL_W);
        final int bottom = this.height - footer;
        descH = Mth.clamp((bottom - top) * 3 / 10, 44, 120);
        listBottom = bottom - descH - gap;
        descY = listBottom + gap;
        shownSubject = null;

        final int searchW = Math.min(170, panelW / 3);
        search = new EditBox(this.font, panelX + panelW - searchW, top / 2 - 9, searchW, 18,
                Component.translatable("dynamicwaters.gui.search"));
        search.setHint(Component.translatable("dynamicwaters.gui.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(query);
        search.setResponder(s -> {
            if (!s.equals(query)) {
                query = s;
                fillList(0.0);
            }
        });
        addRenderableWidget(search);

        final int n = categories.size();
        final int sideGap = roomy ? SIDE_GAP : 3;
        final int bh = Mth.clamp((bottom - top - sideGap * (n - 1)) / n, 14, 22);
        for (int i = 0; i < n; i++) {
            addRenderableWidget(new CategoryButton(panelX, top + i * (bh + sideGap), sideW, bh, i));
        }

        list = new OptionList(this.minecraft, listW, listBottom - listTop, listTop, rowH);
        list.placeAt(listX);
        addRenderableWidget(list);
        fillList(scroll);

        boolean full = false;
        //? neoforge {
        full = true;
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.fullEditor"), btn -> openFullEditor(this.minecraft, this))
                .bounds(this.width / 2 - 154, this.height - footer / 2 - 10, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.fullEditor.tooltip")))
                .build());
        //?}
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
                .bounds(full ? this.width / 2 + 4 : this.width / 2 - 75, this.height - footer / 2 - 10, 150, 20)
                .build());
    }

    private void select(int index) {
        lastCategory = index;
        subject = categories.get(index);
        if (!query.isEmpty()) {
            query = "";
            search.setValue("");
        }
        fillList(0.0);
    }

    private void fillList(double scroll) {
        final List<Row> rows = new ArrayList<>();
        final String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            for (DWConfigCategories.Option o : category().options()) addRows(rows, o, null);
        } else {
            for (DWConfigCategories.Category c : categories) {
                final boolean all = c.title().getString().toLowerCase(Locale.ROOT).contains(q);
                for (DWConfigCategories.Option o : c.options()) {
                    if (all || matches(o, q)) addRows(rows, o, c);
                }
            }
        }
        list.setRows(rows);
        list.setScrollAmount(scroll);
    }

    private boolean matches(DWConfigCategories.Option o, String q) {
        return o.label().getString().toLowerCase(Locale.ROOT).contains(q) || o.key().toLowerCase(Locale.ROOT).contains(q)
                || description(o).getString().toLowerCase(Locale.ROOT).contains(q);
    }

    private void addRows(List<Row> rows, DWConfigCategories.Option o, DWConfigCategories.Category from) {
        final AbstractWidget control = controlFor(o);
        if (control == null) return;
        rows.add(new OptionRow(o, control, from));

        switch (o.key()) {
            case "riverbankPalettes" -> rows.add(new EditorRow("dynamicwaters.gui.banks",
                    () -> new com.AdamNew.dynamicwaters.client.gui.shore.RiverbankPalettesScreen(this), from));
            case "waterPlants" -> rows.add(new EditorRow("dynamicwaters.gui.plants",
                    () -> new com.AdamNew.dynamicwaters.client.gui.shore.WaterPlantSetsScreen(this), from));
            case "riverbankPlantDensity" -> rows.add(new EditorRow("dynamicwaters.gui.bankRules",
                    () -> new com.AdamNew.dynamicwaters.client.gui.shore.RiverbankRulesScreen(this), from));
            case "builtInWaterColors" -> rows.add(new EditorRow("dynamicwaters.gui.waterColors",
                    () -> new com.AdamNew.dynamicwaters.client.gui.water.WaterColorsScreen(this), from));
            default -> {
            }
        }
    }

    //? neoforge {
    static void openFullEditor(Minecraft mc, Screen parent) {
        net.neoforged.fml.ModList.get().getModContainerById("dynamicwaters").ifPresent(container ->
                mc.setScreen(new net.neoforged.neoforge.client.gui.ConfigurationScreen(container, parent,
                        (screen, type, modConfig, title) -> new DWConfigSectionScreen(screen, type, modConfig, title))));
    }
    //?}

    private static ModConfigSpec.ValueSpec specOf(ModConfigSpec.ConfigValue<?> value) {
        try {
            final Object raw = com.AdamNew.dynamicwaters.Config.SPEC.getSpec().get(value.getPath());
            return raw instanceof ModConfigSpec.ValueSpec vs ? vs : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean changed(ModConfigSpec.ConfigValue<?> value) {
        final ModConfigSpec.ValueSpec spec = specOf(value);
        if (spec == null || spec.getDefault() == null) return false;
        final Object d = spec.getDefault(), c = value.get();
        if (d instanceof Number a && c instanceof Number b) return Math.abs(a.doubleValue() - b.doubleValue()) > 1e-6;
        return !java.util.Objects.equals(d, c);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void resetOption(DWConfigCategories.Option option) {
        final ModConfigSpec.ValueSpec spec = specOf(option.value());
        if (spec == null || spec.getDefault() == null) return;
        ((ModConfigSpec.ConfigValue) option.value()).set(spec.getDefault());
        option.value().save();
        fillList(list.getScrollAmount());
    }

    private static String defaultText(Object def) {
        if (def instanceof Boolean b) return net.minecraft.client.resources.language.I18n.get(b ? "dynamicwaters.gui.on" : "dynamicwaters.gui.off");
        if (def instanceof Enum<?> e) return DWConfigCategories.enumStatus(e.getDeclaringClass().getSimpleName(), e).getString();
        if (def instanceof Double d) return String.format("%.1f", d);
        return String.valueOf(def);
    }

    private static Component description(DWConfigCategories.Option option) {
        final String tipKey = "dynamicwaters.configuration." + option.key() + ".tooltip";
        if (net.minecraft.client.resources.language.I18n.exists(tipKey)) return Component.translatable(tipKey);
        final ModConfigSpec.ValueSpec spec = specOf(option.value());
        if (spec != null && spec.getComment() != null && !spec.getComment().isBlank()) return Component.literal(spec.getComment());
        return Component.empty();
    }

    private Component describe(Object s) {
        final MutableComponent out = Component.empty();
        if (s instanceof DWConfigCategories.Option o) {
            out.append(o.label().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            final ModConfigSpec.ValueSpec spec = specOf(o.value());
            if (spec != null && spec.getDefault() != null) {
                out.append(Component.literal("   ")).append(Component.translatable("dynamicwaters.gui.defaultValue",
                        defaultText(spec.getDefault())).withStyle(ChatFormatting.GRAY));
            }
            out.append("\n").append(description(o).copy().withStyle(ChatFormatting.WHITE));
            if (o.worldgen()) {
                out.append("\n").append(Component.translatable("dynamicwaters.gui.worldgenNote").withStyle(ChatFormatting.YELLOW));
            }
        } else if (s instanceof EditorRow e) {
            out.append(Component.translatable(e.prefix + ".label").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            out.append("\n").append(Component.translatable(e.prefix + ".button.tooltip").withStyle(ChatFormatting.WHITE));
        } else if (s instanceof DWConfigCategories.Category c) {
            out.append(c.title().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            final Component status = c.status().get();
            if (!status.getString().isEmpty()) out.append(Component.literal("   ")).append(status);
            out.append("\n").append(c.description().copy().withStyle(ChatFormatting.WHITE));
        }
        return out;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private AbstractWidget controlFor(DWConfigCategories.Option option) {
        final ModConfigSpec.ConfigValue<?> value = option.value();
        final int w = CONTROL_W, h = 20;
        if (value instanceof ModConfigSpec.BooleanValue bool) {
            return CycleButton.onOffBuilder(bool.get())
                    .displayOnlyValue()
                    .create(0, 0, w, h, option.label(), (btn, v) -> {
                        bool.set(v);
                        bool.save();
                    });
        }
        if (value instanceof ModConfigSpec.EnumValue<?> en) {
            return enumControl((ModConfigSpec.EnumValue) en, option, w, h);
        }
        if (value instanceof ModConfigSpec.IntValue iv) {
            final ModConfigSpec.ValueSpec spec = specOf(iv);
            final ModConfigSpec.Range<Integer> range = spec != null ? spec.getRange() : null;
            final int min = range != null ? range.getMin() : 0;
            final int max = range != null ? range.getMax() : Math.max(1, iv.get() * 4);
            return new NumberSlider(w, h, min, max, iv.get(), 1.0, v -> {
                iv.set((int) Math.round(v));
                iv.save();
            });
        }
        if (value instanceof ModConfigSpec.DoubleValue dv) {
            final ModConfigSpec.ValueSpec spec = specOf(dv);
            final ModConfigSpec.Range<Double> range = spec != null ? spec.getRange() : null;
            final double min = range != null ? range.getMin() : 0.0;
            final double max = range != null ? range.getMax() : Math.max(1.0, dv.get() * 4.0);

            final double step = (max - min) <= 20.0 ? 0.1 : 1.0;
            return new NumberSlider(w, h, min, max, dv.get(), step, v -> {
                dv.set(v);
                dv.save();
            });
        }
        return null;
    }

    private <E extends Enum<E>> AbstractWidget enumControl(ModConfigSpec.EnumValue<E> ev, DWConfigCategories.Option option, int w, int h) {
        final E current = ev.get();
        final E[] values = current.getDeclaringClass().getEnumConstants();
        final String enumName = current.getDeclaringClass().getSimpleName();
        return CycleButton.<E>builder(v -> DWConfigCategories.enumStatus(enumName, v))
                .withValues(values)
                .withInitialValue(current)
                .displayOnlyValue()
                .create(0, 0, w, h, option.label(), (btn, v) -> {
                    ev.set(v);
                    ev.save();
                });
    }

    private static final class NumberSlider extends AbstractSliderButton {
        private final double min, max, step;
        private final java.util.function.DoubleConsumer onChange;

        NumberSlider(int w, int h, double min, double max, double initial, double step, java.util.function.DoubleConsumer onChange) {
            super(0, 0, w, h, Component.empty(), max > min ? (initial - min) / (max - min) : 0.0);
            this.min = min;
            this.max = max;
            this.step = step;
            this.onChange = onChange;
            updateMessage();
        }

        private double current() {
            final double raw = min + (max - min) * this.value;
            return Math.round(raw / step) * step;
        }

        @Override
        protected void updateMessage() {
            final double v = current();
            setMessage(Component.literal(step >= 1.0 ? String.format("%d", Math.round(v)) : String.format("%.1f", v)));
        }

        @Override
        protected void applyValue() {
            onChange.accept(current());
        }
    }

    private static String fit(Font font, String text, int width) {
        if (width <= 0) return "";
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("..."))).stripTrailing() + "...";
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
            //? if >=1.20.5 {
            PANORAMA.render(graphics, this.width, this.height, blur, partialTick);
            //?} else {
            /*PANORAMA.render(partialTick, blur);
            *///?}
        } else {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
            tintWater(graphics, this.width, this.height);
        }

        graphics.fill(panelX - 4, 0, panelX + panelW + 4, this.height, 0x66000000);
        graphics.fill(panelX - 5, 0, panelX - 4, this.height, 0x66999999);
        graphics.fill(panelX + panelW + 4, 0, panelX + panelW + 5, this.height, 0x66999999);
        graphics.fill(listX, descY, listX + listW, descY + descH, 0xB0000000);
        graphics.fill(listX, descY, listX + listW, descY + 1, 0x8055FFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        final Component where = query.isBlank() ? category().title()
                : Component.translatable("dynamicwaters.gui.searchResults", list.rowCount());
        final Component head = Component.empty().append(this.title)
                .append(Component.literal("  ›  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(where.copy().withStyle(ChatFormatting.GRAY));

        final int headRoom = panelW - Math.min(170, panelW / 3) - 8;
        if (this.font.width(head) <= headRoom) {
            graphics.drawString(this.font, head, panelX, top / 2 - 4, 0xFFFFFF);
        } else {
            graphics.drawString(this.font, fit(this.font, head.getString(), headRoom), panelX, top / 2 - 4, 0xFFFFFF);
        }
        if (list.rowCount() == 0) {
            graphics.drawCenteredString(this.font, Component.translatable("dynamicwaters.gui.noResults"),
                    listX + listW / 2, listTop + 12, 0xA0A0A0);
        }
        renderDescription(graphics);
    }

    private int descVisible() {
        return Math.max(1, (descH - 2 * DESC_PAD + 2) / LINE);
    }

    private void renderDescription(GuiGraphics graphics) {
        final Object s = subject != null ? subject : category();
        if (s != shownSubject) {
            shownSubject = s;
            descScroll = 0;
            descLines = this.font.split(describe(s), listW - 2 * DESC_PAD - 10);
        }
        final int visible = descVisible();
        descScroll = Mth.clamp(descScroll, 0, Math.max(0, descLines.size() - visible));
        for (int i = 0; i < visible && descScroll + i < descLines.size(); i++) {
            graphics.drawString(this.font, descLines.get(descScroll + i), listX + DESC_PAD, descY + DESC_PAD + i * LINE, 0xFFFFFF);
        }
        if (descScroll > 0) graphics.drawString(this.font, "▲", listX + listW - 11, descY + 5, 0x9EFFFE);
        if (descScroll + visible < descLines.size()) {
            graphics.drawString(this.font, "▼", listX + listW - 11, descY + descH - 13, 0x9EFFFE);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= descY && mouseY < descY + descH) {
            descScroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private static final String LAB_SHOT = System.getProperty("dynamicwaters.labConfigShot");
    private static boolean labOpened;
    private int labTicks;

    public static void labOpen(Screen from) {
        if (LAB_SHOT == null || labOpened) return;
        labOpened = true;
        Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new DWConfigScreen(from)));
    }

    @Override
    public void tick() {
        super.tick();
        if (LAB_SHOT == null || ++labTicks % 20 != 0) return;
        final int shot = labTicks / 20 - 1;
        try (com.mojang.blaze3d.platform.NativeImage img =
                net.minecraft.client.Screenshot.takeScreenshot(this.minecraft.getMainRenderTarget())) {
            img.writeToFile(java.nio.file.Path.of(LAB_SHOT, "config_" + shot + ".png"));
        } catch (Exception e) {
            com.mojang.logging.LogUtils.getLogger().warn("Dynamic Waters lab: config shot failed", e);
        }
        if (shot + 1 < categories.size()) select(shot + 1);
        else this.minecraft.stop();
    }

    private final class CategoryButton extends Button {
        private final int index;

        CategoryButton(int x, int y, int w, int h, int index) {
            super(x, y, w, h, categories.get(index).title(), btn -> select(index), DEFAULT_NARRATION);
            this.index = index;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            final DWConfigCategories.Category c = categories.get(index);
            final boolean chosen = query.isBlank() && index == lastCategory;
            final int x = getX(), y = getY(), w = getWidth(), h = getHeight();
            graphics.fill(x, y, x + w, y + h, chosen ? 0xC0184C6E : isHoveredOrFocused() ? 0x90305060 : 0x60000000);
            if (chosen) graphics.fill(x, y, x + 2, y + h, 0xFF55FFFF);
            int textX = x + 8;
            if (h >= 16) {
                SafeItemIcon.render(graphics, new ItemStack(c.icon()), x + 5, y + (h - 16) / 2, null);
                textX = x + 25;
            }
            final int colour = c.enabled().getAsBoolean() ? (chosen ? 0xFFFFFF : 0xE0E0E0) : 0x8A8A8A;
            final Font font = DWConfigScreen.this.font;
            final String title = c.title().getString();
            final int textW = x + w - 4 - textX;

            final List<net.minecraft.network.chat.FormattedText> parts = font.getSplitter()
                    .splitLines(title, textW, net.minecraft.network.chat.Style.EMPTY);
            if (parts.size() >= 2 && h >= 17) {
                final String first = parts.get(0).getString();
                final String rest = title.substring(Math.min(title.length(), first.length())).trim();
                final int y0 = y + (h - 17) / 2;
                graphics.drawString(font, first, textX, y0, colour);
                graphics.drawString(font, fit(font, rest, textW), textX, y0 + 9, colour);
            } else {
                graphics.drawString(font, fit(font, title, textW), textX, y + (h - 8) / 2, colour);
            }
            if (isHovered()) subject = c;
        }
    }

    private final class OptionList extends ContainerObjectSelectionList<Row> {
        OptionList(Minecraft mc, int width, int height, int y, int rowH) {
            //? if >=1.20.5 {
            super(mc, width, height, y, rowH);
            //?} else {
            /*super(mc, width, y + height, y, y + height, rowH);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            *///?}
        }

        void placeAt(int x) {
            //? if >=1.20.5 {
            setX(x);
            //?} else {
            /*setLeftPos(x);
            *///?}
        }

        void setRows(List<Row> rows) {
            replaceEntries(rows);
        }

        int rowCount() {
            return children().size();
        }

        @Override
        public int getRowWidth() {
            return this.width - 20;
        }

        @Override
        protected int getScrollbarPosition() {
            return getRowLeft() + getRowWidth() + 2;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
        protected final List<AbstractWidget> widgets = new ArrayList<>();

        static void back(GuiGraphics graphics, int top, int left, int width, int height, boolean hovering) {
            graphics.fill(left - 2, top - 1, left + width + 2, top + height + 1, hovering ? 0x50305A78 : 0x30000000);
            if (hovering) graphics.fill(left - 2, top - 1, left, top + height + 1, 0xC055FFFF);
        }

        static int controlY(int top, int height) {
            return top + (height - 20) / 2;
        }

        static int textY(int top, int height) {
            return top + (height - 8) / 2;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }

    private final class OptionRow extends Row {
        private final DWConfigCategories.Option option;
        private final AbstractWidget control;
        private final Button reset;

        private final DWConfigCategories.Category from;

        OptionRow(DWConfigCategories.Option option, AbstractWidget control, DWConfigCategories.Category from) {
            this.option = option;
            this.control = control;
            this.from = from;
            this.reset = Button.builder(Component.literal("↺"), b -> resetOption(option)).size(20, 20)
                    .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.reset"))).build();
            widgets.add(control);
            widgets.add(reset);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            if (hovering) subject = option;
            back(graphics, top, left, width, height, hovering);
            final Font font = DWConfigScreen.this.font;
            final boolean changed = changed(option.value());
            int textX = left + 6;
            if (from != null) {
                SafeItemIcon.render(graphics, new ItemStack(from.icon()), left + 4, top + (height - 16) / 2, null);
                textX += 22;
            }
            final int cw = Math.min(controlW, width / 2);
            final int cx = left + width - cw - 26;
            control.setWidth(cw);
            control.setX(cx);
            control.setY(controlY(top, height));
            control.render(graphics, mouseX, mouseY, partialTick);
            reset.active = changed;
            reset.setX(left + width - 22);
            reset.setY(controlY(top, height));
            reset.render(graphics, mouseX, mouseY, partialTick);
            final String label = (changed ? "* " : "") + option.label().getString();
            graphics.drawString(font, fit(font, label, cx - textX - 8), textX, textY(top, height), changed ? 0xFFE27A : 0xFFFFFF);
        }
    }

    private final class EditorRow extends Row {
        final String prefix;
        private final Button open;
        private final DWConfigCategories.Category from;

        EditorRow(String prefix, Supplier<Screen> editor, DWConfigCategories.Category from) {
            this.prefix = prefix;
            this.from = from;
            this.open = Button.builder(Component.translatable(prefix + ".button"), b -> {
                keepScroll = list.getScrollAmount();
                DWConfigScreen.this.minecraft.setScreen(editor.get());
            }).size(CONTROL_W, 20).build();
            widgets.add(open);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            if (hovering) subject = this;
            back(graphics, top, left, width, height, hovering);
            final Font font = DWConfigScreen.this.font;
            int textX = left + 14;
            if (from != null) {
                SafeItemIcon.render(graphics, new ItemStack(from.icon()), left + 4, top + (height - 16) / 2, null);
                textX += 22;
            }
            final int cw = Math.min(controlW, width / 2);
            final int cx = left + width - cw - 26;
            open.setWidth(cw);
            open.setX(cx);
            open.setY(controlY(top, height));
            open.render(graphics, mouseX, mouseY, partialTick);
            graphics.drawString(font, fit(font, "→ " + Component.translatable(prefix + ".label").getString(), cx - textX - 8),
                    textX, textY(top, height), 0x9EFFFE);
        }
    }
}
