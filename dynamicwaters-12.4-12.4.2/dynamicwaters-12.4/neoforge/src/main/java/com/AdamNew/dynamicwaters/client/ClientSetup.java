package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.DynamicWaters;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = DynamicWaters.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        com.AdamNew.dynamicwaters.HydrologyManager.setDrawnLevelRivers(() -> {
            final net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null || level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
        });

        event.enqueueWork(() -> net.neoforged.fml.ModList.get().getModContainerById(DynamicWaters.MODID).ifPresent(container ->
                container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                        (mod, parent) -> new com.AdamNew.dynamicwaters.client.gui.DWConfigScreen(parent))));
    }
}
