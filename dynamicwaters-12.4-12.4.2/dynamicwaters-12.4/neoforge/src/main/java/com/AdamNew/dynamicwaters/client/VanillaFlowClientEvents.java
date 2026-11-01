package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.HydrologyManager;
import com.AdamNew.dynamicwaters.VanillaRiverFlow;
import com.AdamNew.dynamicwaters.network.SyncVanillaFlowPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = DynamicWaters.MODID, value = Dist.CLIENT)
public final class VanillaFlowClientEvents {
    private VanillaFlowClientEvents() {
    }

    public static void receive(SyncVanillaFlowPacket packet) {
        VanillaRiverFlow.putClientChunk(packet.chunkX(), packet.chunkZ(), packet.angles());
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        final int sea = HydrologyManager.getSeaLevel();
        for (int sy = (sea - 8) >> 4; sy <= (sea + 2) >> 4; sy++) {
            mc.levelRenderer.setSectionDirty(packet.chunkX(), sy, packet.chunkZ());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VanillaRiverFlow.clearClient();
    }
}
