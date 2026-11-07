package com.AdamNew.dynamicwaters.network;

import com.AdamNew.dynamicwaters.HydrologyManager;

public class ModNetwork {
    public static void receive(SyncSeedPacket payload, boolean ownServer) {
        if (ownServer) return;
        HydrologyManager.clearCaches();
        HydrologyManager.setSeed(payload.seed());
        HydrologyManager.setSeaLevel(payload.seaLevel());
    }

    public static void receive(SyncWatershedsPacket payload, boolean ownServer) {
        if (ownServer) return;
        HydrologyManager.putRegion(new HydrologyManager.WatershedRegionData(
                payload.paths(), payload.lakes(), payload.regionX(), payload.regionZ()));
    }

    public static void receive(SyncMainRiversPacket payload, boolean ownServer) {
        if (ownServer) return;
        com.AdamNew.dynamicwaters.MainRivers.cacheRegion(payload.regionX(), payload.regionZ(), payload.edges());
    }

    //? neoforge {
    private static boolean ownServer(net.neoforged.neoforge.network.handling.IPayloadContext context) {
        return context.connection().isMemoryConnection();
    }

    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        final net.neoforged.neoforge.network.registration.PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(SyncSeedPacket.TYPE, SyncSeedPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> receive(payload, ownServer(context))));

        registrar.playToClient(SyncWatershedsPacket.TYPE, SyncWatershedsPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> receive(payload, ownServer(context))));

        registrar.playToClient(SyncVanillaFlowPacket.TYPE, SyncVanillaFlowPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() ->
                    com.AdamNew.dynamicwaters.client.VanillaFlowClientEvents.receive(payload)));

        registrar.playToClient(SyncMainRiversPacket.TYPE, SyncMainRiversPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> receive(payload, ownServer(context))));

        registrar.playToClient(SyncFlowZonesPacket.TYPE, SyncFlowZonesPacket.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() ->
                    com.AdamNew.dynamicwaters.client.FlowZoneClient.receive(payload)));
    }
    //?} forge {
    /*public static final net.minecraftforge.network.simple.SimpleChannel CHANNEL = net.minecraftforge.network.NetworkRegistry.newSimpleChannel(
            new net.minecraft.resources.ResourceLocation("dynamicwaters", "main"), () -> "1", "1"::equals, "1"::equals);

    private static boolean ownServer(java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> context) {
        return context.get().getNetworkManager().isMemoryConnection();
    }

    private static <T extends Payload> void toClient(int id, Class<T> type, java.util.function.Function<net.minecraft.network.FriendlyByteBuf, T> read,
            java.util.function.BiConsumer<T, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context>> handle) {
        CHANNEL.messageBuilder(type, id, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Payload::write)
                .decoder(read)
                .consumerMainThread(handle)
                .add();
    }

    public static void register() {
        toClient(0, SyncSeedPacket.class, SyncSeedPacket::new, (payload, context) -> receive(payload, ownServer(context)));
        toClient(1, SyncWatershedsPacket.class, SyncWatershedsPacket::new, (payload, context) -> receive(payload, ownServer(context)));
        toClient(2, SyncVanillaFlowPacket.class, SyncVanillaFlowPacket::new, (payload, context) ->
                com.AdamNew.dynamicwaters.client.VanillaFlowClientEvents.receive(payload));
        toClient(3, SyncMainRiversPacket.class, SyncMainRiversPacket::new, (payload, context) -> receive(payload, ownServer(context)));
        toClient(4, SyncFlowZonesPacket.class, SyncFlowZonesPacket::new, (payload, context) ->
                com.AdamNew.dynamicwaters.client.FlowZoneClient.receive(payload));
    }
    *///?}
}
