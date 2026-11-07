package com.AdamNew.dynamicwaters.platform.forge;

//? forge {
/*import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.network.ModNetwork;
import com.AdamNew.dynamicwaters.registry.ModBlocks;
import com.AdamNew.dynamicwaters.registry.ModEntities;
import com.AdamNew.dynamicwaters.registry.ModFluids;
import com.AdamNew.dynamicwaters.registry.ModItems;
import com.AdamNew.dynamicwaters.registry.ModParticles;
import com.AdamNew.dynamicwaters.registry.ModSounds;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.PathPackResources;

@Mod(DynamicWaters.MODID)
public class ForgeEntrypoint {
    public ForgeEntrypoint() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.attach(modBus);
        ModFluids.FLUIDS.attach(modBus);
        ModItems.ITEMS.attach(modBus);
        ModParticles.PARTICLE_TYPES.attach(modBus);
        ModEntities.ENTITY_TYPES.attach(modBus);
        ModSounds.SOUND_EVENTS.attach(modBus);
        DynamicWaters.DENSITY_FUNCTIONS.attach(modBus);

        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
        modBus.addListener(ForgeEntrypoint::onCommonSetup);
        modBus.addListener(ForgeEntrypoint::onConfigLoad);
        modBus.addListener(ForgeEntrypoint::addPackFinders);

        DynamicWaters.init();
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.SPEC) DynamicWaters.onConfigLoaded();
    }

    private static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        for (DynamicWaters.BuiltInPack builtIn : DynamicWaters.builtInPacks()) {
            final Path root = ModList.get().getModFileById(DynamicWaters.MODID).getFile().findResource("resourcepacks/" + builtIn.folder());
            final String id = DynamicWaters.MODID + ":" + builtIn.folder();
            final Pack pack = Pack.readMetaAndCreate(id, Component.literal(builtIn.title()), true,
                    name -> new PathPackResources(name, true, root),
                    PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
            if (pack != null) event.addRepositorySource(packs -> packs.accept(pack));
        }
    }
}
*///?}
