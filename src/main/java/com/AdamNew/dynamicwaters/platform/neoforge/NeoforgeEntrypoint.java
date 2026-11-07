package com.AdamNew.dynamicwaters.platform.neoforge;

//? neoforge {
import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.network.ModNetwork;
import com.AdamNew.dynamicwaters.registry.ModBlocks;
import com.AdamNew.dynamicwaters.registry.ModEntities;
import com.AdamNew.dynamicwaters.registry.ModFluids;
import com.AdamNew.dynamicwaters.registry.ModItems;
import com.AdamNew.dynamicwaters.registry.ModParticles;
import com.AdamNew.dynamicwaters.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@Mod(DynamicWaters.MODID)
public class NeoforgeEntrypoint {
    public NeoforgeEntrypoint(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModBlocks.BLOCKS.attach(modBus);
        ModFluids.FLUIDS.attach(modBus);
        ModItems.ITEMS.attach(modBus);
        ModParticles.PARTICLE_TYPES.attach(modBus);
        ModEntities.ENTITY_TYPES.attach(modBus);
        ModSounds.SOUND_EVENTS.attach(modBus);
        DynamicWaters.DENSITY_FUNCTIONS.attach(modBus);

        NeoForge.EVENT_BUS.register(NeoforgeEvents.class);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(NeoforgeEntrypoint::onConfigLoad);
        modBus.addListener(NeoforgeEntrypoint::addPackFinders);

        DynamicWaters.init();
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.SPEC) DynamicWaters.onConfigLoaded();
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        for (DynamicWaters.BuiltInPack pack : DynamicWaters.builtInPacks()) {
            event.addPackFinders(
                    ResourceLocation.fromNamespaceAndPath(DynamicWaters.MODID, "resourcepacks/" + pack.folder()),
                    PackType.SERVER_DATA,
                    Component.literal(pack.title()),
                    PackSource.BUILT_IN,
                    true,
                    Pack.Position.TOP);
        }
    }
}
//?}
