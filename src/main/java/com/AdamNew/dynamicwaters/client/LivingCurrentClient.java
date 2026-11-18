package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.LivingCurrent;
import com.AdamNew.dynamicwaters.network.SyncLivingCurrentPacket;
import net.minecraft.client.Minecraft;

public final class LivingCurrentClient {
    private LivingCurrentClient() {
    }

    public static void receive(SyncLivingCurrentPacket packet) {
        LivingCurrent.putClient(packet.chunkX(), packet.chunkZ(), packet.field());
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.levelRenderer == null) return;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        if (packet.field() != null) {
            for (int i = 0; i < 256; i++) {
                if (packet.field().depth()[i] <= 0) continue;
                low = Math.min(low, packet.field().top()[i] - packet.field().depth()[i]);
                high = Math.max(high, packet.field().top()[i]);
            }
        }
        if (low > high) {
            low = mc.level.getMinBuildHeight();
            high = mc.level.getMaxBuildHeight() - 1;
        }
        for (int sy = low >> 4; sy <= high >> 4; sy++) {
            mc.levelRenderer.setSectionDirty(packet.chunkX(), sy, packet.chunkZ());
        }
    }
}
