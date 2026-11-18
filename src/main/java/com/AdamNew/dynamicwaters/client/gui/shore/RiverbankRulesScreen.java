package com.AdamNew.dynamicwaters.client.gui.shore;

import com.AdamNew.dynamicwaters.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RiverbankRulesScreen extends com.AdamNew.dynamicwaters.client.gui.DWScreen {
    private final Screen parent;
    private boolean plantsTab;
    private RuleList list;
    private Button editButton, deleteButton, upButton, downButton;
    private int select = -1;
    private List<BankRuleDraft> rules = List.of();

    private List<Holder.Reference<Biome>> biomes = List.of();
    private List<List<Holder.Reference<Biome>>> usedIn = List.of();

    public RiverbankRulesScreen(Screen parent) {
        this(parent, true);
    }

    RiverbankRulesScreen(Screen parent, boolean plantsTab) {
        super(Component.translatable("dynamicwaters.gui.bankRules.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        this.parent = parent;
        this.plantsTab = plantsTab;
        this.biomes = worldBiomes();
        load();
    }

    private static ModConfigSpec.ConfigValue<List<? extends String>> value(boolean plants) {
        return plants ? Config.RIVERBANK_PLANT_MIXES : Config.RIVERBANK_BUSH_LEAVES;
    }

    private void load() {
        final List<BankRuleDraft> out = new ArrayList<>();
        for (String s : value(plantsTab).get()) out.add(BankRuleDraft.parse(s, plantsTab));
        rules = out;
        usedIn = usage(rules, biomes);
    }

    private void write() {
        final List<String> texts = new ArrayList<>();
        for (BankRuleDraft d : rules) texts.add(d.text());
        final ModConfigSpec.ConfigValue<List<? extends String>> v = value(plantsTab);
        v.set(texts);
        v.save();
        usedIn = usage(rules, biomes);
    }

    void put(int index, BankRuleDraft rule) {
        final List<BankRuleDraft> l = new ArrayList<>(rules);
        if (index >= 0 && index < l.size()) l.set(index, rule);
        else {
            l.add(rule);
            index = l.size() - 1;
        }
        rules = l;
        select = index;
        write();
    }

    List<BankRuleDraft> rules() {
        return rules;
    }

    private static List<String> defaults(boolean plants) {
        try {
            final Object raw = Config.SPEC.getSpec().get(value(plants).getPath());
            if (raw instanceof ModConfigSpec.ValueSpec vs && vs.getDefault() instanceof List<?> l) {
                final List<String> out = new ArrayList<>();
                for (Object o : l) out.add(String.valueOf(o));
                return out;
            }
        } catch (Throwable ignored) {
        }
        return List.of();
    }

    static List<Holder.Reference<Biome>> worldBiomes() {
        final Minecraft mc = Minecraft.getInstance();
        final RegistryAccess access = mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().registryAccess()
                : mc.level != null ? mc.level.registryAccess() : null;
        if (access == null) return List.of();
        try {
            return access.registryOrThrow(Registries.BIOME).holders()
                    .filter(h -> h.is(BiomeTags.IS_OVERWORLD))
                    .sorted(Comparator.comparing(h -> h.key().location().toString()))
                    .toList();
        } catch (Throwable t) {
            return List.of();
        }
    }

    static boolean knowsTrees() {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    static List<List<Holder.Reference<Biome>>> usage(List<BankRuleDraft> rules, List<Holder.Reference<Biome>> biomes) {
        final List<List<Holder.Reference<Biome>>> out = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) out.add(new ArrayList<>());
        for (Holder.Reference<Biome> b : biomes) {
            final String land = BankRuleDraft.landOf(b);
            for (int i = 0; i < rules.size(); i++) {
                if (rules.get(i).matches(b, land)) {
                    out.get(i).add(b);
                    break;
                }
            }
        }
        return out;
    }

    @Override
    protected void init() {
        final int cx = this.width / 2;

        final int tabW = Math.min(170, (this.width - 24) / 2);
        final Button plantsB = addRenderableWidget(Button.builder(tabLabel(true), b -> switchTo(true))
                .bounds(cx - tabW - 2, 32, tabW, 18)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.tab.plants.tooltip")))
                .build());
        final Button leavesB = addRenderableWidget(Button.builder(tabLabel(false), b -> switchTo(false))
                .bounds(cx + 2, 32, tabW, 18)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.tab.leaves.tooltip")))
                .build());
        plantsB.active = !plantsTab;
        leavesB.active = plantsTab;

        list = new RuleList(this.minecraft, this.width, this.height - 56 - 58, 56, 36);
        for (int i = 0; i < rules.size(); i++) {
            final RuleEntry e = new RuleEntry(i);
            list.add(e);
            if (i == select) list.setSelected(e);
        }
        addRenderableWidget(list);

        final int y1 = this.height - 52, y2 = this.height - 28;
        editButton = addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.banks.edit"), b -> edit())
                .bounds(cx - 154, y1, 74, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.bankRules.new"), b -> create())
                .bounds(cx - 78, y1, 74, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.new.tooltip")))
                .build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.banks.delete"), b -> delete())
                .bounds(cx - 2, y1, 74, 20).build());
        upButton = addRenderableWidget(Button.builder(Component.literal("▲"), b -> move(-1))
                .bounds(cx + 74, y1, 38, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.up")))
                .build());
        downButton = addRenderableWidget(Button.builder(Component.literal("▼"), b -> move(1))
                .bounds(cx + 116, y1, 38, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.down")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("dynamicwaters.gui.bankRules.reset"), b -> reset())
                .bounds(cx - 154, y2, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("dynamicwaters.gui.bankRules.reset.tooltip")))
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx + 4, y2, 150, 20).build());
        updateButtons();
    }

    private Component tabLabel(boolean plants) {
        return Component.translatable(plants ? "dynamicwaters.gui.bankRules.tab.plants" : "dynamicwaters.gui.bankRules.tab.leaves")
                .append(Component.literal(" " + value(plants).get().size()).withStyle(ChatFormatting.GRAY));
    }

    private void switchTo(boolean plants) {
        plantsTab = plants;
        select = -1;
        load();
        rebuildWidgets();
    }

    private int selectedIndex() {
        final RuleEntry e = list == null ? null : list.getSelected();
        return e == null ? -1 : e.index;
    }

    private void updateButtons() {
        final int i = selectedIndex();
        if (editButton != null) editButton.active = i >= 0;
        if (deleteButton != null) deleteButton.active = i >= 0;
        if (upButton != null) upButton.active = i > 0;
        if (downButton != null) downButton.active = i >= 0 && i < rules.size() - 1;
    }

    private void edit() {
        final int i = selectedIndex();
        if (i < 0) return;
        select = i;
        this.minecraft.setScreen(new RiverbankRuleEditScreen(this, rules.get(i).copy(), i));
    }

    private void create() {
        final BankRuleDraft fresh;
        if (!rules.isEmpty() && plantsTab) {
            fresh = rules.get(rules.size() - 1).copy();
            fresh.any = false;
            fresh.kinds.clear();
            fresh.biomes.clear();
        } else {
            fresh = new BankRuleDraft(plantsTab);
            if (plantsTab) fresh.plants.add(new BankRuleDraft.Plant("minecraft:tall_grass", 1.0));
        }
        this.minecraft.setScreen(new RiverbankRuleEditScreen(this, fresh, -1));
    }

    private void delete() {
        final int i = selectedIndex();
        if (i < 0) return;
        this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                final List<BankRuleDraft> l = new ArrayList<>(rules);
                l.remove(i);
                rules = l;
                select = Math.min(i, l.size() - 1);
                write();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("dynamicwaters.gui.bankRules.deleteConfirm", i + 1),
                Component.literal(rules.get(i).text()).withStyle(ChatFormatting.GRAY)));
    }

    private void move(int by) {
        final int i = selectedIndex(), j = i + by;
        if (i < 0 || j < 0 || j >= rules.size()) return;
        final List<BankRuleDraft> l = new ArrayList<>(rules);
        java.util.Collections.swap(l, i, j);
        rules = l;
        select = j;
        write();
        rebuildWidgets();
    }

    private void reset() {
        this.minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                final List<BankRuleDraft> l = new ArrayList<>();
                for (String s : defaults(plantsTab)) l.add(BankRuleDraft.parse(s, plantsTab));
                rules = l;
                select = -1;
                write();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("dynamicwaters.gui.bankRules.resetConfirm"),
                Component.translatable(plantsTab ? "dynamicwaters.gui.bankRules.tab.plants" : "dynamicwaters.gui.bankRules.tab.leaves")));
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
        g.drawCenteredString(this.font, Component.translatable("dynamicwaters.gui.bankRules.subtitle"), this.width / 2, 20, 0x9EFFFE);
        if (rules.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable(plantsTab ? "dynamicwaters.gui.bankRules.emptyPlants"
                    : "dynamicwaters.gui.bankRules.emptyLeaves"), this.width / 2, 66, 0xA0A0A0);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static String autoExample(List<Holder.Reference<Biome>> where) {
        if (!knowsTrees()) return null;
        for (Holder.Reference<Biome> b : where) {
            final var s = com.AdamNew.dynamicwaters.shore.RiverBeach.autoLeaves(b);
            if (s != null) return BlockSwatch.idOf(s.getBlock());
        }
        return null;
    }

    private final class RuleList extends ObjectSelectionList<RuleEntry> {
        RuleList(Minecraft mc, int width, int height, int y, int itemHeight) {
            //? if >=1.20.5 {
            super(mc, width, height, y, itemHeight);
            //?} else {
            /*super(mc, width, y + height, y, y + height, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
            *///?}
        }

        void add(RuleEntry e) {
            addEntry(e);
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, this.width - 40);
        }

        @Override
        public void setSelected(RuleEntry entry) {
            super.setSelected(entry);
            updateButtons();
        }
    }

    private final class RuleEntry extends ObjectSelectionList.Entry<RuleEntry> {
        final int index;
        private long lastClick;

        private final String example;

        RuleEntry(int index) {
            this.index = index;
            final BankRuleDraft d = rules.get(index);
            this.example = !d.plantsRule && BankRuleDraft.AUTO.equals(d.leaves) && index < usedIn.size()
                    ? autoExample(usedIn.get(index)) : null;
        }

        @Override
        public void render(GuiGraphics g, int i, int top, int left, int width, int height, int mouseX, int mouseY,
                boolean hovering, float partialTick) {
            final var font = RiverbankRulesScreen.this.font;
            final BankRuleDraft d = rules.get(index);
            g.drawString(font, (index + 1) + ".", left + 2, top + 4, 0x9EFFFE);
            final int ix = left + 18;

            int textX;
            if (d.plantsRule) {
                final List<BankRuleDraft.Plant> byWeight = new ArrayList<>(d.plants);
                byWeight.sort((a, b) -> Double.compare(b.weight, a.weight));
                final int n = Math.min(6, byWeight.size());
                for (int k = 0; k < n; k++) BlockSwatch.icon(g, byWeight.get(k).id, ix + k * 17, top + 2);
                textX = ix + Math.max(1, n) * 17 + 4;
            } else {
                if (BankRuleDraft.NONE.equals(d.leaves)) {
                    com.AdamNew.dynamicwaters.client.gui.SafeItemIcon.render(g, new ItemStack(Items.BARRIER), ix, top + 2, null);
                }
                else if (BankRuleDraft.AUTO.equals(d.leaves)) {
                    if (example != null) BlockSwatch.icon(g, example, ix, top + 2);
                    else com.AdamNew.dynamicwaters.client.gui.SafeItemIcon.render(g, new ItemStack(Items.OAK_SAPLING), ix, top + 2, null);
                    g.drawString(font, "A", ix + 11, top + 11, 0xFFFF55);
                } else BlockSwatch.icon(g, d.leaves, ix, top + 2);
                textX = ix + 21;
            }
            final int used = index < usedIn.size() ? usedIn.get(index).size() : -1;
            final Component count = biomes.isEmpty() ? Component.empty()
                    : used == 0 ? Component.translatable("dynamicwaters.gui.bankRules.unused").withStyle(ChatFormatting.GOLD)
                    : Component.translatable("dynamicwaters.gui.bankRules.biomes", used).withStyle(ChatFormatting.GREEN);
            final int cw = font.width(count);
            g.drawString(font, count, left + width - cw - 4, top + 4, 0xFFFFFF);
            final int tw = left + width - textX - cw - 10;
            g.drawString(font, RiverbankPalettesScreen.fit(font, d.landsText(), tw), textX, top + 4,
                    d.landless() ? 0xFF7070 : 0xFFFFFF);
            g.drawString(font, RiverbankPalettesScreen.fit(font, whatText(d, example), left + width - textX - 4), textX, top + 16, 0x909090);
            if (hovering && used > 0 && mouseX > left + width - cw - 6) {
                final List<Component> tip = new ArrayList<>();
                tip.add(Component.translatable("dynamicwaters.gui.bankRules.usedIn").withStyle(ChatFormatting.AQUA));
                final List<Holder.Reference<Biome>> where = usedIn.get(index);
                for (int k = 0; k < Math.min(18, where.size()); k++) {
                    tip.add(Component.literal(where.get(k).key().location().toString()).withStyle(ChatFormatting.GRAY));
                }
                if (where.size() > 18) tip.add(Component.literal("+" + (where.size() - 18)).withStyle(ChatFormatting.DARK_GRAY));
                RiverbankRulesScreen.this.setTooltipForNextRenderPass(tip.stream().map(Component::getVisualOrderText).toList());
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            final long now = Util.getMillis();
            if (RiverbankRulesScreen.this.list.getSelected() == this && now - lastClick < 300L) edit();
            lastClick = now;
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal((index + 1) + ". " + rules.get(index).landsText());
        }
    }

    static String whatText(BankRuleDraft d, String autoExample) {
        if (!d.plantsRule) {
            if (BankRuleDraft.NONE.equals(d.leaves)) return Component.translatable("dynamicwaters.gui.bankRules.leaves.none").getString();
            if (BankRuleDraft.AUTO.equals(d.leaves)) {
                final String auto = Component.translatable("dynamicwaters.gui.bankRules.leaves.auto").getString();
                return autoExample == null ? auto : auto + " - " + BlockSwatch.name(autoExample).getString() + ", ...";
            }
            return BlockSwatch.name(d.leaves).getString();
        }
        final double total = d.total();
        final List<BankRuleDraft.Plant> byWeight = new ArrayList<>(d.plants);
        byWeight.sort((a, b) -> Double.compare(b.weight, a.weight));
        final List<String> parts = new ArrayList<>();
        for (BankRuleDraft.Plant p : byWeight) {
            parts.add(BlockSwatch.name(p.id).getString() + " " + percent(total > 0 ? p.weight / total : 0));
        }
        return parts.isEmpty() ? Component.translatable("dynamicwaters.gui.bankRules.noPlants").getString() : String.join(" · ", parts);
    }

    static String percent(double share) {
        final double p = share * 100.0;
        if (p >= 10.0 || p == Math.rint(p)) return Math.round(p) + "%";
        return BankRuleDraft.number(Math.round(p * 10.0) / 10.0) + "%";
    }
}
