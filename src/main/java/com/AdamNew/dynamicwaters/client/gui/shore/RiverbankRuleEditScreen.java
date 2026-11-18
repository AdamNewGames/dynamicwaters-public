package com.AdamNew.dynamicwaters.client.gui.shore;

import com.AdamNew.dynamicwaters.shore.RiverBeach;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RiverbankRuleEditScreen extends com.AdamNew.dynamicwaters.client.gui.DWScreen {
    private static final int ROW = 20;
    private static final String[] MODES = {BankRuleDraft.AUTO, BankRuleDraft.NONE, "block"};

    private static final double MIN_WEIGHT_LOG = -1.0, MAX_WEIGHT_LOG = 3.0;

    private final RiverbankRulesScreen parent;
    private final BankRuleDraft draft;

    private final int index;
    private int selected;
    private int page;
    private boolean dirty;
    private Component error;

    private List<Holder.Reference<Biome>> usedIn = List.of();
    private final List<Holder.Reference<Biome>> biomes = RiverbankRulesScreen.worldBiomes();

    private int left, fullW, colW, panelX, panelW, contentY, listRows;

    RiverbankRuleEditScreen(RiverbankRulesScreen parent, BankRuleDraft draft, int index) {
        super(Component.translatable(draft.plantsRule ? "dynamicwaters.gui.bankRules.tab.plants" : "dynamicwaters.gui.bankRules.tab.leaves"));
        this.parent = parent;
        this.draft = draft;
        this.index = index;
        if (draft.plantsRule) draft.plants.sort((a, b) -> Double.compare(b.weight, a.weight));
    }

    private void measure() {
        final List<BankRuleDraft> rules = new ArrayList<>(parent.rules());
        int at = index;
        if (at >= 0 && at < rules.size()) rules.set(at, draft);
        else {
            rules.add(draft);
            at = rules.size() - 1;
        }
        final List<List<Holder.Reference<Biome>>> usage = RiverbankRulesScreen.usage(rules, biomes);
        usedIn = at < usage.size() ? usage.get(at) : List.of();
    }

    @Override
    protected void init() {
        measure();
        left = 8;
        fullW = this.width - 16;

        final int n = BankRuleDraft.LANDS.length + 1;
        final int perRow = fullW / n >= 56 ? n : (n + 1) / 2;
        final int bw = (fullW - (perRow - 1) * 2) / perRow;
        for (int k = 0; k < n; k++) {
            final String land = k == 0 ? null : BankRuleDraft.LANDS[k - 1];
            final boolean on = land == null ? draft.any : draft.kinds.contains(land);
            final Component name = Component.translatable("dynamicwaters.gui.bankRules.land." + (land == null ? "any" : land));
            final Component label = on ? Component.literal("✔ ").append(name).withStyle(ChatFormatting.GREEN)
                    : name.copy().withStyle(ChatFormatting.GRAY);
            final int row = k / perRow, col = k % perRow;
            addRenderableWidget(Button.builder(label, b -> {
                        if (land == null) draft.any = !draft.any;
                        else if (!draft.kinds.remove(land)) draft.kinds.add(land);
                        dirty = true;
                    })
                    .bounds(left + col * (bw + 2), 18 + row * 20, bw, 18)
                    .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.land."
                            + (land == null ? "any" : land) + ".desc")))
                    .build());
        }
        final int boxY = 18 + ((n + perRow - 1) / perRow) * 20 + 2;

        final EditBox box = new EditBox(this.font, left, boxY, fullW - 84, 16, Component.translatable("dynamicwaters.gui.bankRules.biomesBox"));
        box.setMaxLength(1024);
        box.setValue(String.join(", ", draft.biomes));
        box.setHint(Component.translatable("dynamicwaters.gui.bankRules.biomes.hint").withStyle(ChatFormatting.DARK_GRAY));
        box.setTooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.biomes.tooltip")));
        box.setResponder(s -> {
            draft.biomes.clear();
            for (String part : s.split("[\\s,]+")) {
                if (!part.isBlank()) draft.biomes.add(part.trim().toLowerCase(java.util.Locale.ROOT));
            }
            measure();
        });
        addRenderableWidget(box);
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.bankRules.addBiome"),
                        b -> this.minecraft.setScreen(new com.AdamNew.dynamicwaters.client.gui.water.BiomePickerScreen(this, id -> {
                            if (!draft.biomes.contains(id)) draft.biomes.add(id);
                        })))
                .bounds(left + fullW - 80, boxY - 2, 80, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.addBiome.tooltip")))
                .build());

        contentY = boxY + 24;
        colW = Math.min(230, (int) (this.width * 0.45));
        panelX = left + colW + 10;
        panelW = this.width - 8 - panelX;
        if (draft.plantsRule) plantControls();
        else leavesControls();

        final int by = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.banks.save"), b -> save())
                .bounds(this.width - 8 - 206, by, 102, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(this.width - 8 - 102, by, 102, 20).build());
    }

    private BankRuleDraft.Plant selectedPlant() {
        return selected >= 0 && selected < draft.plants.size() ? draft.plants.get(selected) : null;
    }

    private void plantControls() {
        final List<BankRuleDraft.Plant> list = draft.plants;
        final int addY = this.height - 52;
        listRows = Math.max(1, (addY - 2 - contentY - 12) / ROW);
        selected = Mth.clamp(selected, 0, Math.max(0, list.size() - 1));
        final int pages = Math.max(1, (list.size() + listRows - 1) / listRows);
        page = Mth.clamp(page, 0, pages - 1);
        for (int r = 0; r < listRows; r++) {
            final int i = page * listRows + r;
            if (i >= list.size()) break;
            final int y = listTop() + r * ROW + 2;
            final int bx = left + colW - 34;
            if (i > 0) {
                small("▲", bx, y, "dynamicwaters.gui.bankRules.plantUp", () -> {
                    Collections.swap(list, i, i - 1);
                    if (selected == i) selected = i - 1;
                    else if (selected == i - 1) selected = i;
                });
            }
            small("x", bx + 17, y, "dynamicwaters.gui.plants.remove", () -> {
                list.remove(i);
                if (selected >= i && selected > 0) selected--;
            });
        }
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.plants.add"), b -> pickPlant(null))
                .bounds(left, addY, 110, 20).build());
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page--;
                dirty = true;
            }).bounds(left + colW - 44, addY, 20, 20).build()).active = page > 0;
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page++;
                dirty = true;
            }).bounds(left + colW - 20, addY, 20, 20).build()).active = page < pages - 1;
        }

        final BankRuleDraft.Plant p = selectedPlant();
        if (p == null || panelW < 80) return;
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.plants.changeBlock"), b -> pickPlant(p))
                .bounds(panelX + panelW - 70, contentY + 12, 70, 18)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.changePlant.tooltip")))
                .build());
        final double pos = (Math.log10(Math.max(1.0E-3, p.weight)) - MIN_WEIGHT_LOG) / (MAX_WEIGHT_LOG - MIN_WEIGHT_LOG);
        addRenderableWidget(new WeightSlider(panelX, contentY + 34, panelW, pos, p));
    }

    private int listTop() {
        return contentY + 12;
    }

    private void pickPlant(BankRuleDraft.Plant change) {
        this.minecraft.setScreen(new BlockPickerScreen(this, id -> {
            if (change != null) {
                change.id = id;
                return;
            }
            draft.plants.add(new BankRuleDraft.Plant(id, Math.max(1.0, draft.total() * 0.05)));
            selected = draft.plants.size() - 1;
            page = selected / Math.max(1, listRows);
        }, true));
    }

    private int rowAt(double mx, double my) {
        if (!draft.plantsRule || mx < left || mx >= left + colW - 36 || my < listTop() || my >= listTop() + listRows * ROW) return -1;
        final int i = page * listRows + (int) ((my - listTop()) / ROW);
        return i >= 0 && i < draft.plants.size() ? i : -1;
    }

    private final class WeightSlider extends AbstractSliderButton {
        private final BankRuleDraft.Plant plant;

        WeightSlider(int x, int y, int w, double pos, BankRuleDraft.Plant plant) {
            super(x, y, w, 20, Component.empty(), Mth.clamp(pos, 0.0, 1.0));
            this.plant = plant;
            setTooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.weight.tooltip")));
            updateMessage();
        }

        private double weight() {
            final double raw = Math.pow(10.0, MIN_WEIGHT_LOG + (MAX_WEIGHT_LOG - MIN_WEIGHT_LOG) * this.value);
            return new java.math.BigDecimal(raw).round(new java.math.MathContext(2)).doubleValue();
        }

        @Override
        protected void updateMessage() {
            final double w = this.plant == null ? 0.0 : weight();
            final double total = this.plant == null ? 0.0 : draft.total() - Math.max(0.0, this.plant.weight) + w;
            setMessage(Component.translatable("dynamicwaters.gui.bankRules.weight", BankRuleDraft.number(w),
                    RiverbankRulesScreen.percent(total > 0 ? w / total : 0)));
        }

        @Override
        protected void applyValue() {
            plant.weight = weight();
        }
    }

    private String mode() {
        return BankRuleDraft.AUTO.equals(draft.leaves) || BankRuleDraft.NONE.equals(draft.leaves) ? draft.leaves : "block";
    }

    private void leavesControls() {
        addRenderableWidget(CycleButton.<String>builder(v -> Component.translatable("dynamicwaters.gui.bankRules.mode." + v))
                .withValues(MODES)
                .withInitialValue(mode())
                .withTooltip(v -> Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.mode." + v + ".desc")))
                .create(left, contentY + 12, colW, 20, Component.translatable("dynamicwaters.gui.bankRules.mode"), (b, v) -> {
                    if (v.equals("block")) {
                        if (BankRuleDraft.AUTO.equals(draft.leaves) || BankRuleDraft.NONE.equals(draft.leaves)) {
                            draft.leaves = "minecraft:oak_leaves";
                        }
                    } else {
                        draft.leaves = v;
                    }
                    dirty = true;
                }));
        if (mode().equals("block")) {
            addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.bankRules.pickLeaves"), b ->
                            this.minecraft.setScreen(new BlockPickerScreen(this, id -> draft.leaves = id,
                                    "dynamicwaters.gui.bankRules.pickLeaves", BlockSwatch::leaves)))
                    .bounds(left + 22, contentY + 36, colW - 22, 20).build());
        }
    }

    private void save() {
        if (draft.plantsRule && draft.total() <= 0.0) {
            error = Component.translatable("dynamicwaters.gui.bankRules.needPlant").withStyle(ChatFormatting.RED);
            return;
        }
        if (!draft.plantsRule && mode().equals("block") && !BlockSwatch.exists(draft.leaves)) {
            error = Component.translatable("dynamicwaters.gui.bankRules.badLeaves").withStyle(ChatFormatting.RED);
            return;
        }
        parent.put(index, draft);
        this.minecraft.setScreen(parent);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        com.AdamNew.dynamicwaters.client.gui.DWConfigScreen.tintWater(g, this.width, this.height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (dirty) {
            dirty = false;
            rebuildWidgets();
        }
        super.render(g, mouseX, mouseY, partialTick);
        final var font = this.font;
        final Component heading = this.title.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("  " + (index < 0
                        ? Component.translatable("dynamicwaters.gui.bankRules.newRule").getString()
                        : Component.translatable("dynamicwaters.gui.bankRules.rule", index + 1).getString()))
                        .withStyle(ChatFormatting.GRAY));
        g.drawCenteredString(font, heading, this.width / 2, 6, 0xFFFFFF);

        if (draft.plantsRule) renderPlants(g, mouseX, mouseY);
        else renderLeaves(g);

        final int wy = this.height - 64;
        final Component where;
        if (draft.landless()) {
            where = Component.translatable("dynamicwaters.gui.bankRules.nowhere.warn").withStyle(ChatFormatting.RED);
        } else if (biomes.isEmpty()) {
            where = Component.translatable("dynamicwaters.gui.bankRules.noWorld").withStyle(ChatFormatting.DARK_GRAY);
        } else if (usedIn.isEmpty()) {
            where = Component.translatable("dynamicwaters.gui.bankRules.unused.warn").withStyle(ChatFormatting.GOLD);
        } else {
            where = null;
        }
        if (error != null) g.drawCenteredString(font, error, this.width / 2, wy, 0xFF5555);
        else if (where != null) g.drawCenteredString(font, where, this.width / 2, wy, 0xFFFFFF);
    }

    private void renderPlants(GuiGraphics g, int mouseX, int mouseY) {
        final var font = this.font;
        g.drawString(font, Component.translatable("dynamicwaters.gui.bankRules.plants"), left, contentY, 0x9EFFFE);
        final List<BankRuleDraft.Plant> list = draft.plants;
        final double total = draft.total();
        final int hover = rowAt(mouseX, mouseY);
        for (int r = 0; r < listRows; r++) {
            final int i = page * listRows + r;
            if (i >= list.size()) break;
            final BankRuleDraft.Plant p = list.get(i);
            final int y = listTop() + r * ROW;
            if (i == selected) g.fill(left - 2, y, left + colW - 36, y + ROW - 1, 0x5038C8FF);
            else if (i == hover) g.fill(left - 2, y, left + colW - 36, y + ROW - 1, 0x30FFFFFF);
            BlockSwatch.icon(g, p.id, left, y + 2);
            final String share = RiverbankRulesScreen.percent(total > 0 ? Math.max(0.0, p.weight) / total : 0);
            final int sw = font.width(share);
            g.drawString(font, share, left + colW - 40 - sw, y + 6, 0x9EFFFE);
            g.drawString(font, RiverbankPalettesScreen.fit(font, BlockSwatch.name(p.id).getString(), colW - 40 - sw - 26), left + 20, y + 6,
                    BlockSwatch.exists(p.id) ? 0xFFFFFF : 0xFF5555);
        }
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("dynamicwaters.gui.bankRules.noPlants"), left, listTop() + 4, 0x909090);
        }

        if (panelW < 80) return;
        final BankRuleDraft.Plant p = selectedPlant();
        if (p != null) {
            g.drawString(font, RiverbankPalettesScreen.fit(font, BlockSwatch.name(p.id).getString(), panelW - 76), panelX, contentY + 12, 0xFFFFFF);
            g.drawString(font, RiverbankPalettesScreen.fit(font, p.id, panelW - 76), panelX, contentY + 22, 0x707070);
        }

        final int sy = contentY + 62;
        g.drawString(font, Component.translatable("dynamicwaters.gui.bankRules.sample"), panelX, sy, 0x9EFFFE);
        final int cols = Math.max(1, panelW / 17), rows = Math.max(0, Math.min(3, (this.height - 70 - sy - 12) / 34));
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int x = panelX + c * 17, y = sy + 12 + r * 34;
                BlockSwatch.draw(g, "minecraft:grass_block", x, y + 17, 16);
                final String id = pick(r * 131 + c, total);
                if (id != null) BlockSwatch.icon(g, id, x, y + 1);
            }
        }

        final int ly = sy + 12 + rows * 34 + 4;
        drawUsedIn(g, ly, false);
    }

    private String pick(int spot, double total) {
        if (total <= 0.0) return null;
        long h = spot * 0x9E3779B97F4A7C15L + 0x5B31L;
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        double at = ((h >>> 11) * 0x1.0p-53) * total;
        for (BankRuleDraft.Plant p : draft.plants) {
            at -= Math.max(0.0, p.weight);
            if (at < 0.0) return p.id;
        }
        return draft.plants.get(draft.plants.size() - 1).id;
    }

    private void renderLeaves(GuiGraphics g) {
        final var font = this.font;
        g.drawString(font, Component.translatable("dynamicwaters.gui.bankRules.leaves"), left, contentY, 0x9EFFFE);
        int ty = contentY + 38;
        if (mode().equals("block")) {
            BlockSwatch.icon(g, draft.leaves, left, contentY + 38);
            ty = contentY + 62;
            g.drawString(font, RiverbankPalettesScreen.fit(font, draft.leaves, colW), left, ty, BlockSwatch.exists(draft.leaves) ? 0x707070 : 0xFF5555);
            ty += 14;
        } else if (mode().equals(BankRuleDraft.NONE)) {
            com.AdamNew.dynamicwaters.client.gui.SafeItemIcon.render(g, new ItemStack(Items.BARRIER), left, contentY + 38, null);
            ty = contentY + 60;
        }
        for (FormattedCharSequence line : font.split(Component.translatable("dynamicwaters.gui.bankRules.mode." + mode() + ".desc"), colW)) {
            if (ty > this.height - 76) break;
            g.drawString(font, line, left, ty, 0x909090);
            ty += 10;
        }
        drawUsedIn(g, contentY, true);
    }

    private void drawUsedIn(GuiGraphics g, int y, boolean leaves) {
        if (biomes.isEmpty() || usedIn.isEmpty() || panelW < 80) return;
        final var font = this.font;
        g.drawString(font, Component.translatable("dynamicwaters.gui.bankRules.biomes", usedIn.size())
                .append(Component.literal(":")), panelX, y, 0x9EFFFE);
        final int rowH = leaves ? 17 : 10;
        final int fits = Math.max(0, (this.height - 70 - (y + 12)) / rowH);
        final boolean trees = RiverbankRulesScreen.knowsTrees();
        int shown = 0;
        for (Holder.Reference<Biome> b : usedIn) {
            if (shown >= fits - 1 && usedIn.size() > fits) break;
            final int ry = y + 12 + shown * rowH;
            final String name = b.key().location().toString().replace("minecraft:", "");
            if (leaves) {
                final String what;
                if (mode().equals("block")) what = draft.leaves;
                else if (mode().equals(BankRuleDraft.AUTO) && trees) {
                    final BlockState s = RiverBeach.autoLeaves(b);
                    what = s == null ? null : BlockSwatch.idOf(s.getBlock());
                } else what = null;
                if (what != null) BlockSwatch.icon(g, what, panelX, ry);
                final String tail = what != null ? "  " + BlockSwatch.name(what).getString()
                        : mode().equals(BankRuleDraft.AUTO) && !trees ? "  ?"
                        : "  " + Component.translatable("dynamicwaters.gui.bankRules.noBush").getString();
                g.drawString(font, RiverbankPalettesScreen.fit(font, name + tail, panelW - 20), panelX + 20, ry + 4,
                        what != null ? 0xFFFFFF : 0x808080);
            } else {
                g.drawString(font, RiverbankPalettesScreen.fit(font, name, panelW), panelX, ry, 0xC0C0C0);
            }
            shown++;
        }
        if (shown < usedIn.size()) {
            g.drawString(font, "+" + (usedIn.size() - shown), panelX, y + 12 + shown * rowH + (leaves ? 4 : 0), 0x808080);
        }
    }

    private void small(String label, int x, int y, String tipKey, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    action.run();
                    dirty = true;
                })
                .bounds(x, y, 16, 16)
                .tooltip(Tooltip.create(Component.translatable(tipKey)))
                .build());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        final int i = rowAt(mx, my);
        if (i >= 0 && button == 0) {
            selected = i;
            dirty = true;
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
