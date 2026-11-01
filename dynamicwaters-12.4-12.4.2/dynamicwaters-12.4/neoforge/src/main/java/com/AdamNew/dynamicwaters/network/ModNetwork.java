package com.AdamNew.dynamicwaters.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.AdamNew.dynamicwaters.HydrologyManager;

public class ModNetwork {
    private static boolean ownServer(IPayloadContext context) {
        return context.connection().isMemoryConnection();
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
            SyncSeedPacket.TYPE,
            SyncSeedPacket.STREAM_CODEC,
            (payload, context) -> {
                if (ownServer(context)) return;
                context.enqueueWork(() -> {
                    HydrologyManager.clearCaches();
                    HydrologyManager.setSeed(payload.seed());
                    HydrologyManager.setSeaLevel(payload.seaLevel());
                });
            }
        );

        registrar.playToClient(
            SyncWatershedsPacket.TYPE,
            SyncWatershedsPacket.STREAM_CODEC,
            (payload, context) -> {
                if (ownServer(context)) return;
                context.enqueueWork(() -> {
                    HydrologyManager.putRegion(new HydrologyManager.WatershedRegionData(
                            payload.paths(), payload.lakes(), payload.regionX(), payload.regionZ()));
                });
            }
        );

        registrar.playToClient(
            SyncVanillaFlowPacket.TYPE,
            SyncVanillaFlowPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() ->
                    com.AdamNew.dynamicwaters.client.VanillaFlowClientEvents.receive(payload))
        );

        registrar.playToClient(
            SyncMainRiversPacket.TYPE,
            SyncMainRiversPacket.STREAM_CODEC,
            (payload, context) -> {
                if (ownServer(context)) return;
                context.enqueueWork(() -> {
                    com.AdamNew.dynamicwaters.MainRivers.cacheRegion(payload.regionX(), payload.regionZ(), payload.edges());
                });
            }
        );

        registrar.playToClient(
            SyncFlowZonesPacket.TYPE,
            SyncFlowZonesPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() ->
                    com.AdamNew.dynamicwaters.client.FlowZoneClient.receive(payload))
        );
    }
}
