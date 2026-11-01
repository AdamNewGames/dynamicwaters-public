package com.AdamNew.dynamicwaters.client.gui;

import com.AdamNew.dynamicwaters.client.gui.shore.RiverbankPalettesScreen;
import com.AdamNew.dynamicwaters.client.gui.shore.WaterPlantSetsScreen;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DWConfigSectionScreen extends ConfigurationScreen.ConfigurationSectionScreen {
    private static final String BANKS_KEY = "riverbankPalettes";

    public DWConfigSectionScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    private DWConfigSectionScreen(Context parentContext, Screen parent, Map<String, Object> valueSpecs, String key,
            Set<? extends UnmodifiableConfig.Entry> entrySet, Component title) {
        super(parentContext, parent, valueSpecs, key, entrySet, title);
    }

    @Override
    protected Element createSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        final Element original = super.createSection(key, subconfig, subsection);
        if (original == null || original.widget() == null) return original;

        final AbstractWidget old = original.widget();
        final Button button = Button.builder(old.getMessage(), b -> this.minecraft.setScreen(this.sectionCache.computeIfAbsent(key,
                        k -> new DWConfigSectionScreen(this.context, this, subconfig.valueMap(), key, subsection.entrySet(),
                                Component.translatable(getTranslationKey(key))).rebuild())))
                .tooltip(original.tooltip() != null ? Tooltip.create(original.tooltip()) : null)
                .width(old.getWidth())
                .build();
        return new Element(original.name(), original.tooltip(), button, false);
    }

    @Override
    protected Collection<? extends Element> createSyntheticValues() {
        final boolean holdsBanks = this.context.entries().stream().anyMatch(e -> BANKS_KEY.equals(e.getKey()));
        if (!holdsBanks) return super.createSyntheticValues();
        final Component tip = Component.translatable("dynamicwaters.gui.banks.button.tooltip");
        final Button edit = Button.builder(Component.translatable("dynamicwaters.gui.banks.button"),
                        b -> this.minecraft.setScreen(new RiverbankPalettesScreen(this)))
                .tooltip(Tooltip.create(tip))
                .width(Button.DEFAULT_WIDTH)
                .build();
        final Component plantsTip = Component.translatable("dynamicwaters.gui.plants.button.tooltip");
        final Button plants = Button.builder(Component.translatable("dynamicwaters.gui.plants.button"),
                        b -> this.minecraft.setScreen(new WaterPlantSetsScreen(this)))
                .tooltip(Tooltip.create(plantsTip))
                .width(Button.DEFAULT_WIDTH)
                .build();
        return List.of(new Element(Component.translatable("dynamicwaters.gui.banks.label"), tip, edit, false),
                new Element(Component.translatable("dynamicwaters.gui.plants.label"), plantsTip, plants, false));
    }
}
