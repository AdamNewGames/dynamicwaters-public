package com.AdamNew.dynamicwaters;

import com.AdamNew.dynamicwaters.block.FlowOverrideBlock;
import com.AdamNew.dynamicwaters.network.SyncFlowZonesPacket;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FlowZones {
    public static final int STILL = -1;

    public static final int DEPTH = 24;

    public static final int MAX_SIDE = 512;

    public static final int MAX_ZONES = 1024;

    private static final String DATA_NAME = "dynamicwaters_flow_zones";
    private static final Vec3[] DIRECTIONS = new Vec3[16];

    static {
        for (int i = 0; i < DIRECTIONS.length; i++) DIRECTIONS[i] = FlowOverrideBlock.directionToVec3(i);
    }

    private static final Map<ResourceKey<Level>, Index> SERVER = new ConcurrentHashMap<>();

    private static volatile Index client = Index.EMPTY;

    private FlowZones() {
    }

    public record Zone(int id, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int direction) {
        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
        }

        public Vec3 flow() {
            return direction == STILL ? Vec3.ZERO : DIRECTIONS[direction];
        }

        public int sizeX() {
            return maxX - minX + 1;
        }

        public int sizeZ() {
            return maxZ - minZ + 1;
        }
    }

    private static final class Index {
        static final Index EMPTY = new Index(List.of());

        final List<Zone> zones;
        private final Long2ObjectOpenHashMap<Zone[]> byChunk = new Long2ObjectOpenHashMap<>();

        Index(List<Zone> zones) {
            this.zones = List.copyOf(zones);
            final Map<Long, List<Zone>> lists = new HashMap<>();
            for (int i = this.zones.size() - 1; i >= 0; i--) {
                final Zone zone = this.zones.get(i);
                for (int cx = zone.minX() >> 4; cx <= zone.maxX() >> 4; cx++) {
                    for (int cz = zone.minZ() >> 4; cz <= zone.maxZ() >> 4; cz++) {
                        lists.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new ArrayList<>()).add(zone);
                    }
                }
            }
            lists.forEach((key, here) -> byChunk.put(key.longValue(), here.toArray(new Zone[0])));
        }

        Zone at(int x, int y, int z) {
            final Zone[] here = byChunk.get(ChunkPos.asLong(x >> 4, z >> 4));
            if (here == null) return null;
            for (Zone zone : here) {
                if (zone.contains(x, y, z)) return zone;
            }
            return null;
        }
    }

    public static Vec3 flowAt(BlockGetter level, double x, int y, double z) {
        final Index index = level instanceof ServerLevel server ? SERVER.getOrDefault(server.dimension(), Index.EMPTY) : client;
        if (index.zones.isEmpty()) return null;
        final Zone zone = index.at(Mth.floor(x), y, Mth.floor(z));
        return zone != null ? zone.flow() : null;
    }

    public static Component describe(int direction) {
        return direction == STILL ? Component.translatable("dynamicwaters.flow_zone.still")
                : Component.translatable("dynamicwaters.flow_zone.runs", directionName(direction));
    }

    public static Component directionName(int direction) {
        return Component.translatable("dynamicwaters.direction." + direction);
    }

    public static int direction(int stored) {
        return stored >= 0 && stored < DIRECTIONS.length ? stored : STILL;
    }

    public static List<Zone> zones(ServerLevel level) {
        return List.copyOf(data(level).zones);
    }

    public static Zone add(ServerLevel level, BlockPos a, BlockPos b, int direction) {
        final Data data = data(level);
        if (data.zones.size() >= MAX_ZONES) return null;
        final Zone zone = new Zone(data.nextId++,
                Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()) - DEPTH, Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()) + 1, Math.max(a.getZ(), b.getZ()),
                direction(direction));
        data.zones.add(zone);
        changed(level, data);
        return zone;
    }

    public static Zone removeAt(ServerLevel level, BlockPos pos) {
        final Data data = data(level);
        final int y = pos.getY();
        for (int i = data.zones.size() - 1; i >= 0; i--) {
            final Zone zone = data.zones.get(i);
            if (y <= zone.maxY() + 3 && zone.contains(pos.getX(), Math.min(y, zone.maxY()), pos.getZ())) {
                data.zones.remove(i);
                changed(level, data);
                return zone;
            }
        }
        return null;
    }

    public static Zone remove(ServerLevel level, int id) {
        final Data data = data(level);
        for (int i = 0; i < data.zones.size(); i++) {
            if (data.zones.get(i).id() == id) {
                final Zone zone = data.zones.remove(i);
                changed(level, data);
                return zone;
            }
        }
        return null;
    }

    public static int clear(ServerLevel level) {
        final Data data = data(level);
        final int n = data.zones.size();
        if (n > 0) {
            data.zones.clear();
            changed(level, data);
        }
        return n;
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncFlowZonesPacket(zones(player.serverLevel())));
    }

    private static void changed(ServerLevel level, Data data) {
        data.setDirty();
        index(level, data);
        PacketDistributor.sendToPlayersInDimension(level, new SyncFlowZonesPacket(data.zones));
    }

    private static void index(ServerLevel level, Data data) {
        SERVER.put(level.dimension(), data.zones.isEmpty() ? Index.EMPTY : new Index(data.zones));
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load), DATA_NAME);
    }

    public static final class Data extends SavedData {
        final List<Zone> zones = new ArrayList<>();
        int nextId = 1;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            final Data data = new Data();
            data.nextId = Math.max(1, tag.getInt("NextId"));
            final ListTag list = tag.getList("Zones", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final CompoundTag z = list.getCompound(i);
                final int[] box = z.getIntArray("Box");
                if (box.length != 6) continue;
                data.zones.add(new Zone(z.getInt("Id"), box[0], box[1], box[2], box[3], box[4], box[5],
                        direction(z.getInt("Direction"))));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("NextId", nextId);
            final ListTag list = new ListTag();
            for (Zone zone : zones) {
                final CompoundTag z = new CompoundTag();
                z.putInt("Id", zone.id());
                z.putIntArray("Box", new int[] {zone.minX(), zone.minY(), zone.minZ(), zone.maxX(), zone.maxY(), zone.maxZ()});
                z.putInt("Direction", zone.direction());
                list.add(z);
            }
            tag.put("Zones", list);
            return tag;
        }
    }

    public static List<Zone> clientZones() {
        return client.zones;
    }

    public static void setClient(List<Zone> zones) {
        client = zones.isEmpty() ? Index.EMPTY : new Index(zones);
    }

    public static void clearClient() {
        client = Index.EMPTY;
    }

    @EventBusSubscriber(modid = DynamicWaters.MODID)
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) index(level, data(level));
        }

        @SubscribeEvent
        public static void onLevelUnload(LevelEvent.Unload event) {
            if (event.getLevel() instanceof ServerLevel level) SERVER.remove(level.dimension());
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            SERVER.clear();
        }

        @SubscribeEvent
        public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) syncTo(player);
        }

        @SubscribeEvent
        public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) syncTo(player);
        }

        @SubscribeEvent
        public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) syncTo(player);
        }
    }
}
