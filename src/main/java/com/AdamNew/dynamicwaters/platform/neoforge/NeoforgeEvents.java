package com.AdamNew.dynamicwaters.platform.neoforge;

//? neoforge {
import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.FlowZones;
import com.AdamNew.dynamicwaters.SableCompat;
import com.AdamNew.dynamicwaters.events.IceFloeSpawner;
import com.AdamNew.dynamicwaters.events.SpawningEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class NeoforgeEvents {
    private NeoforgeEvents() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        DynamicWaters.onServerAboutToStart();
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        DynamicWaters.onServerStarting();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        DynamicWaters.onServerStarted(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        DynamicWaters.onServerStopped();
        FlowZones.onServerStopped();
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DynamicWaters.onLevelLoad(level);
            FlowZones.onLevelLoad(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) FlowZones.onLevelUnload(level);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SableCompat.tickPhysicsShips(level);
            IceFloeSpawner.onLevelTick(level);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        SpawningEvents.onChunkLoad(event.getLevel(), event.getChunk());
    }

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        DynamicWaters.ModEvents.onChunkWatch(event.getPlayer(), event.getLevel(), event.getPos().x, event.getPos().z);
    }

    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        DynamicWaters.onChunkSent(event.getPlayer(), event.getLevel(), event.getChunk());
    }

    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        final var riverSpawns = SpawningEvents.riverSpawns(event.getLevel(), event.getPos(), event.getMobCategory());
        if (riverSpawns == null) return;

        for (var data : java.util.List.copyOf(event.getSpawnerDataList())) event.removeSpawnerData(data);
        for (var data : riverSpawns.unwrap()) event.addSpawnerData(data);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        DynamicWaters.onRegisterCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        DynamicWaters.ModEvents.onPlayerLogin(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) FlowZones.syncTo(player);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        DynamicWaters.ModEvents.onPlayerRespawn(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) FlowZones.syncTo(player);
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        DynamicWaters.ModEvents.onPlayerChangeDimension(event.getEntity());
        if (event.getEntity() instanceof ServerPlayer player) FlowZones.syncTo(player);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DynamicWaters.ModEvents.onPlayerLogout(event.getEntity());
    }
}
//?}
