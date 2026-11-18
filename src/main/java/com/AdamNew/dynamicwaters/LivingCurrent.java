package com.AdamNew.dynamicwaters;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class LivingCurrent {
    private static final int HALO = 32;

    private static final int GROUP_REACH = 4;

    private static final int MOST_GROUP_CHUNKS = 16;

    private static final int LOOK_UP = 6;
    private static final int LOOK_FAR = 96;

    private static final int ROOF = 12;

    private static final int MOST_DEPTH = 8;

    private static final int REACH_DOWN = 16;

    private static final int SETTLE_TICKS = 20;

    private static final double LEAK_REACH = 64.0;

    private static final int MOST_SWEEPS = 8000;
    private static final double SETTLED = 1.0E-5;

    private static final int SPREAD = 4;

    private static final double MOST_CHANGE = 3.0;
    private static final float LEAST_CHANGE = 0.01F;

    private static final double MOST_RATIO = 2.5;

    private static final double FLOWED = 0.05;

    private static final double STILL = 0.12;
    private static final String DATA_NAME = "dynamicwaters_living_current";

    private static final byte OPEN = 0, WATER = 1, SOLID = 2;

    public record Field(float[] scale, float[] turn, float[] addX, float[] addZ, short[] top, byte[] depth) {
    }

    private static volatile Map<Long, Field> fields = Map.of();

    private static final java.util.concurrent.ConcurrentHashMap<Long, Field> CLIENT = new java.util.concurrent.ConcurrentHashMap<>();

    private static Data data;
    private static final LongOpenHashSet DIRTY = new LongOpenHashSet();
    private static long lastChange = Long.MIN_VALUE;

    private static final List<Win> WINS = new ArrayList<>();
    private static final AtomicInteger WORKING = new AtomicInteger();
    private static final ConcurrentLinkedQueue<Round> DONE = new ConcurrentLinkedQueue<>();
    private static boolean wasOff;
    private static int generation;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        final Thread t = new Thread(r, "Dynamic Waters living current");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });

    private LivingCurrent() {
    }

    private record Win(LongOpenHashSet group, Map<Long, Field> chunks) {
    }

    private record Round(int generation, List<Win> fresh, List<Win> stale) {
    }

    public static Vec3 apply(@Nullable Level level, double x, int y, double z, Vec3 planned) {
        final Map<Long, Field> f = level == null || level.isClientSide() ? CLIENT : fields;
        if (f.isEmpty()) return planned;
        if (level != null && level.dimension() != Level.OVERWORLD) return planned;
        final int bx = Mth.floor(x), bz = Mth.floor(z);
        final Field field = f.get(ChunkPos.asLong(bx >> 4, bz >> 4));
        if (field == null) return planned;
        final int i = (bx & 15) + ((bz & 15) << 4);
        if (field.depth()[i] <= 0 || y > field.top()[i] || y < field.top()[i] - REACH_DOWN) return planned;
        final double turn = field.turn()[i], scale = field.scale()[i];
        final double cos = Math.cos(turn) * scale, sin = Math.sin(turn) * scale;
        final double fx = planned.x * cos - planned.z * sin + field.addX()[i];
        final double fz = planned.x * sin + planned.z * cos + field.addZ()[i];

        if (fx * fx + fz * fz < STILL * STILL && Math.abs(planned.y) < 1.0E-6) return Vec3.ZERO;
        return new Vec3(fx, planned.y, fz);
    }

    public static void putClient(int chunkX, int chunkZ, @Nullable Field field) {
        final long key = ChunkPos.asLong(chunkX, chunkZ);
        if (field == null) {
            CLIENT.remove(key);
        } else {
            CLIENT.put(key, field);
        }
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static void sendWithChunk(net.minecraft.server.level.ServerPlayer player, ServerLevel level, LevelChunk chunk) {
        if (level.dimension() != Level.OVERWORLD) return;
        final Field f = fields.get(chunk.getPos().toLong());
        if (f != null) {
            com.AdamNew.dynamicwaters.platform.Platform.sendToPlayer(player,
                    new com.AdamNew.dynamicwaters.network.SyncLivingCurrentPacket(chunk.getPos().x, chunk.getPos().z, f));
        }
    }

    private static void sendChanged(ServerLevel level, Map<Long, Field> before) {
        final Map<Long, Field> now = fields;
        final LongOpenHashSet keys = new LongOpenHashSet();
        keys.addAll(before.keySet());
        keys.addAll(now.keySet());
        for (long key : keys) {
            final Field f = now.get(key);
            if (f == before.get(key)) continue;
            final int cx = ChunkPos.getX(key), cz = ChunkPos.getZ(key);
            final com.AdamNew.dynamicwaters.network.SyncLivingCurrentPacket packet =
                    new com.AdamNew.dynamicwaters.network.SyncLivingCurrentPacket(cx, cz, f);
            for (net.minecraft.server.level.ServerPlayer p : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(cx, cz), false)) {
                com.AdamNew.dynamicwaters.platform.Platform.sendToPlayer(p, packet);
            }
        }
    }

    private static byte kind(BlockState s) {
        if (s.blocksMotion() && !s.is(BlockTags.ICE)) return SOLID;
        return s.getFluidState().is(FluidTags.WATER) ? WATER : OPEN;
    }

    public static void blockChanged(ServerLevel level, BlockPos pos, BlockState before, BlockState after) {
        if (data == null || level.dimension() != Level.OVERWORLD || !level.getServer().isSameThread()) return;
        final byte was = kind(before), is = kind(after);
        if ((was == SOLID) == (is == SOLID)) return;
        if (!Config.LIVING_CURRENT.get() || !byRiver(level, pos, before)) return;
        final long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        final long key = pos.asLong();
        Long2ByteOpenHashMap blocks = data.changes.get(chunk);
        if (blocks != null && blocks.containsKey(key)) {
            if ((blocks.get(key) == SOLID) == (is == SOLID)) {
                blocks.remove(key);
                if (blocks.isEmpty()) data.changes.remove(chunk);
            }
        } else {
            if (blocks == null) data.changes.put(chunk, blocks = new Long2ByteOpenHashMap());
            blocks.put(key, was);
        }
        data.setDirty();
        DIRTY.add(chunk);
        lastChange = level.getGameTime();
    }

    private static boolean byRiver(ServerLevel level, BlockPos pos, BlockState before) {
        if (before.getFluidState().is(FluidTags.WATER) && running(level, pos)) return true;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int asked = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    at.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    if (!level.getFluidState(at).is(FluidTags.WATER)) continue;
                    if (running(level, at)) return true;
                    if (++asked >= 4) return false;
                }
            }
        }
        return false;
    }

    private static boolean running(ServerLevel level, BlockPos pos) {
        return HydrologyManager.plannedRiverFlow(pos.getX(), pos.getY(), pos.getZ(), level).lengthSqr() > 1.0E-4;
    }

    public static void onLevelLoad(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        generation++;
        data = data(level);
        WINS.clear();
        fields = Map.of();
        DIRTY.clear();
        DIRTY.addAll(data.changes.keySet());
        lastChange = Long.MIN_VALUE;
    }

    public static void onLevelUnload(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) return;
        forget();
    }

    public static void forget() {
        generation++;
        data = null;
        WINS.clear();
        DIRTY.clear();
        fields = Map.of();
    }

    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (data == null || level.dimension() != Level.OVERWORLD) return;
        final long key = chunk.getPos().toLong();
        if (!data.changes.containsKey(key)) return;
        for (Win w : WINS) {
            if (w.group().contains(key)) return;
        }
        DIRTY.add(key);
    }

    public static void tick(ServerLevel level) {
        if (data == null || level.dimension() != Level.OVERWORLD) return;
        Round done;
        while ((done = DONE.poll()) != null) {
            if (done.generation() != generation) continue;
            WINS.removeAll(done.stale());
            WINS.addAll(done.fresh());
            publish(level);
        }
        if (!Config.LIVING_CURRENT.get()) {
            if (!wasOff) {
                wasOff = true;
                WINS.clear();
                publish(level);
            }
            return;
        }
        if (wasOff) {
            wasOff = false;
            DIRTY.addAll(data.changes.keySet());
        }
        if (DIRTY.isEmpty() || WORKING.get() > 0 || level.getGameTime() - lastChange < SETTLE_TICKS) return;
        startRound(level);
    }

    private static void publish(ServerLevel level) {
        final Map<Long, Field> before = fields;
        fields = sum();
        sendChanged(level, before);
    }

    private static Map<Long, Field> sum() {
        if (WINS.isEmpty()) return Map.of();
        final Map<Long, Field> sum = new HashMap<>();
        for (Win w : WINS) {
            for (Map.Entry<Long, Field> e : w.chunks().entrySet()) {
                final Field had = sum.get(e.getKey());
                if (had == null) {
                    sum.put(e.getKey(), e.getValue());
                } else {
                    final Field f = e.getValue();
                    final float[] scale = had.scale().clone(), turn = had.turn().clone();
                    final float[] addX = had.addX().clone(), addZ = had.addZ().clone();
                    final short[] top = had.top().clone();
                    final byte[] depth = had.depth().clone();
                    for (int i = 0; i < 256; i++) {
                        scale[i] *= f.scale()[i];
                        turn[i] += f.turn()[i];
                        addX[i] += f.addX()[i];
                        addZ[i] += f.addZ()[i];
                        if (depth[i] <= 0) {
                            top[i] = f.top()[i];
                            depth[i] = f.depth()[i];
                        }
                    }
                    sum.put(e.getKey(), new Field(scale, turn, addX, addZ, top, depth));
                }
            }
        }
        return Map.copyOf(sum);
    }

    private static void startRound(ServerLevel level) {
        final LongOpenHashSet dirty = new LongOpenHashSet(DIRTY);
        final List<LongOpenHashSet> groups = groups(data.changes.keySet());
        final List<Snapshot> snapshots = new ArrayList<>();
        final LongOpenHashSet handled = new LongOpenHashSet();
        final List<Win> stale = new ArrayList<>();
        for (LongOpenHashSet group : groups) {
            if (!near(group, dirty)) continue;
            final long t0 = System.nanoTime();
            final Snapshot s = snapshot(level, group);
            if (System.getProperty("dynamicwaters.labCurrentDump") != null) {
                com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: living current snapshot {} ms",
                        (System.nanoTime() - t0) / 1_000_000);
            }
            if (s == null) continue;
            snapshots.add(s);
            handled.addAll(group);
        }

        for (long d : dirty) {
            boolean waiting = false;
            for (LongOpenHashSet group : groups) {
                if (!handled.contains(group.iterator().nextLong()) && near(group, d)) waiting = true;
            }
            if (!waiting) DIRTY.remove(d);
        }
        for (Win w : WINS) {
            if (near(w.group(), dirty)) stale.add(w);
        }
        if (snapshots.isEmpty()) {
            if (!stale.isEmpty()) {
                WINS.removeAll(stale);
                publish(level);
            }

            lastChange = level.getGameTime();
            return;
        }
        final int gen = generation;
        WORKING.incrementAndGet();
        WORKER.execute(() -> {
            try {
                final List<Win> fresh = new ArrayList<>();
                for (Snapshot s : snapshots) {
                    final Win w = solve(s);
                    if (w != null) fresh.add(w);
                }
                DONE.add(new Round(gen, fresh, stale));
            } catch (Throwable t) {
                com.mojang.logging.LogUtils.getLogger().warn("Dynamic Waters: the living current could not be worked out", t);
                DONE.add(new Round(gen, List.of(), stale));
            } finally {
                WORKING.decrementAndGet();
            }
        });
    }

    private static List<LongOpenHashSet> groups(it.unimi.dsi.fastutil.longs.LongSet changed) {
        final List<LongOpenHashSet> groups = new ArrayList<>();
        final LongOpenHashSet left = new LongOpenHashSet(changed);
        while (!left.isEmpty()) {
            final long first = left.iterator().nextLong();
            left.remove(first);
            final int bucketX = Math.floorDiv(ChunkPos.getX(first), MOST_GROUP_CHUNKS);
            final int bucketZ = Math.floorDiv(ChunkPos.getZ(first), MOST_GROUP_CHUNKS);
            final LongOpenHashSet group = new LongOpenHashSet();
            final LongArrayList todo = new LongArrayList();
            group.add(first);
            todo.add(first);
            while (!todo.isEmpty()) {
                final long c = todo.removeLong(todo.size() - 1);
                final int cx = ChunkPos.getX(c), cz = ChunkPos.getZ(c);
                final LongArrayList found = new LongArrayList();
                for (long o : left) {
                    final int ox = ChunkPos.getX(o), oz = ChunkPos.getZ(o);
                    if (Math.max(Math.abs(ox - cx), Math.abs(oz - cz)) > GROUP_REACH) continue;
                    if (Math.floorDiv(ox, MOST_GROUP_CHUNKS) != bucketX
                            || Math.floorDiv(oz, MOST_GROUP_CHUNKS) != bucketZ) continue;
                    found.add(o);
                }
                for (int i = 0; i < found.size(); i++) {
                    final long o = found.getLong(i);
                    left.remove(o);
                    group.add(o);
                    todo.add(o);
                }
            }
            groups.add(group);
        }
        return groups;
    }

    private static boolean near(LongOpenHashSet group, LongOpenHashSet chunks) {
        for (long c : chunks) {
            if (near(group, c)) return true;
        }
        return false;
    }

    private static boolean near(LongOpenHashSet group, long chunk) {
        final int x = ChunkPos.getX(chunk), z = ChunkPos.getZ(chunk);
        for (long g : group) {
            if (Math.max(Math.abs(ChunkPos.getX(g) - x), Math.abs(ChunkPos.getZ(g) - z)) <= GROUP_REACH) return true;
        }
        return false;
    }

    private record Snapshot(LongOpenHashSet group, int x0, int z0, int w, int h, byte[] depthNow, byte[] depthWas,
            short[] topNow, float[] planX, float[] planZ) {
    }

    private static @Nullable Snapshot snapshot(ServerLevel level, LongOpenHashSet group) {
        int cx0 = Integer.MAX_VALUE, cz0 = Integer.MAX_VALUE, cx1 = Integer.MIN_VALUE, cz1 = Integer.MIN_VALUE;
        for (long c : group) {
            cx0 = Math.min(cx0, ChunkPos.getX(c));
            cz0 = Math.min(cz0, ChunkPos.getZ(c));
            cx1 = Math.max(cx1, ChunkPos.getX(c));
            cz1 = Math.max(cz1, ChunkPos.getZ(c));
        }
        final int x0 = (cx0 << 4) - HALO, z0 = (cz0 << 4) - HALO;
        final int w = ((cx1 - cx0 + 1) << 4) + 2 * HALO, h = ((cz1 - cz0 + 1) << 4) + 2 * HALO;
        for (int cx = x0 >> 4; cx <= (x0 + w - 1) >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= (z0 + h - 1) >> 4; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) return null;
            }
        }

        final Long2ByteOpenHashMap was = new Long2ByteOpenHashMap();
        final it.unimi.dsi.fastutil.ints.IntArrayList ys = new it.unimi.dsi.fastutil.ints.IntArrayList();
        for (int cx = (x0 >> 4) - 1; cx <= ((x0 + w - 1) >> 4) + 1; cx++) {
            for (int cz = (z0 >> 4) - 1; cz <= ((z0 + h - 1) >> 4) + 1; cz++) {
                final Long2ByteOpenHashMap blocks = data.changes.get(ChunkPos.asLong(cx, cz));
                if (blocks == null) continue;
                for (Long2ByteMap.Entry e : blocks.long2ByteEntrySet()) {
                    was.put(e.getLongKey(), e.getByteValue());
                    if (group.contains(ChunkPos.asLong(cx, cz))) ys.add(BlockPos.getY(e.getLongKey()));
                }
            }
        }
        if (ys.isEmpty()) return null;
        ys.sort(null);
        final int yRef = ys.getInt(ys.size() / 2);

        final byte[] depthNow = new byte[w * h], depthWas = new byte[w * h];
        final short[] topNow = new short[w * h];
        final float[] planX = new float[w * h], planZ = new float[w * h];
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                final int x = x0 + i, z = z0 + j, c = i + j * w;

                final int standing = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                final int from = Math.min(yRef + LOOK_FAR, Math.max(yRef + LOOK_UP, standing));

                final int topWas = waterTop(level, at, x, z, from, yRef - LOOK_FAR, was, true);
                depthWas[c] = depthAt(level, at, x, topWas, z, was, true);

                final int floor = topWas != Integer.MIN_VALUE ? topWas : yRef - 4;
                final int top = waterTop(level, at, x, z, Math.max(from, topWas), floor, was, false);
                depthNow[c] = depthAt(level, at, x, top, z, was, false);
                topNow[c] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, top));

                if (depthWas[c] > 0) {
                    final Vec3 p = HydrologyManager.plannedRiverFlow(x + 0.5, topWas, z + 0.5, level);
                    planX[c] = (float) p.x;
                    planZ[c] = (float) p.z;
                }
            }
        }
        return new Snapshot(group, x0, z0, w, h, depthNow, depthWas, topNow, planX, planZ);
    }

    private static int waterTop(ServerLevel level, BlockPos.MutableBlockPos at, int x, int z, int from, int lowest,
            Long2ByteOpenHashMap was, boolean before) {
        int solid = 0;
        for (int y = from; y >= lowest; y--) {
            final byte k = kindAt(level, at.set(x, y, z), was, before);
            if (k == WATER) return y;
            if (k == SOLID && ++solid > ROOF) return Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private static byte kindAt(ServerLevel level, BlockPos at, Long2ByteOpenHashMap was, boolean before) {
        if (before) {
            final long key = at.asLong();
            if (was.containsKey(key)) return was.get(key);
        }
        return kind(level.getBlockState(at));
    }

    private static byte depthAt(ServerLevel level, BlockPos.MutableBlockPos at, int x, int top, int z,
            Long2ByteOpenHashMap was, boolean before) {
        if (top == Integer.MIN_VALUE) return 0;
        int d = 0;
        while (d < MOST_DEPTH && kindAt(level, at.set(x, top - d, z), was, before) == WATER) d++;
        return (byte) d;
    }

    private static @Nullable Win solve(Snapshot s) {
        final int w = s.w(), h = s.h(), n = w * h;
        final byte[] depthWas = s.depthWas(), depthNow = s.depthNow();
        boolean changed = false;
        for (int c = 0; c < n && !changed; c++) changed = depthWas[c] != depthNow[c];
        if (!changed) return null;
        final double[] laid = headLaid(w, h, depthWas, s.planX(), s.planZ());
        final double[] was = head(w, h, depthWas, depthWas, laid);
        final double[] now = head(w, h, depthNow, depthWas, laid);
        labDump(s, was, now);

        final float[] sc = new float[n], tu = new float[n], ax = new float[n], az = new float[n];

        final boolean[] slack = new boolean[n];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                final int c = i + j * w;
                sc[c] = 1.0F;
                if (depthNow[c] <= 0) continue;
                final double nowX = -slope(now, depthNow, w, h, i, j, 1, 0);
                final double nowZ = -slope(now, depthNow, w, h, i, j, 0, 1);
                final double wasX = -slope(was, depthWas, w, h, i, j, 1, 0);
                final double wasZ = -slope(was, depthWas, w, h, i, j, 0, 1);
                final double edge = Math.min(Math.min(i, w - 1 - i), Math.min(j, h - 1 - j)) / (double) HALO;
                final double t = Mth.clamp(edge, 0.0, 1.0);
                final double fade = t * t * (3.0 - 2.0 * t);
                final double nowSize = Math.hypot(nowX, nowZ), wasSize = Math.hypot(wasX, wasZ);
                double addX = 0.0, addZ = 0.0;
                if (depthWas[c] > 0 && wasSize >= FLOWED) {
                    sc[c] = (float) (1.0 + fade * (Math.min(MOST_RATIO, nowSize / wasSize) - 1.0));
                    if (nowSize >= FLOWED) {
                        tu[c] = (float) (fade * Mth.wrapDegrees(Math.toDegrees(Math.atan2(nowZ, nowX) - Math.atan2(wasZ, wasX)))
                                * Math.PI / 180.0);
                    }
                } else if (depthWas[c] <= 0) {
                    sc[c] = (float) (1.0 - fade);
                    addX = fade * nowX;
                    addZ = fade * nowZ;
                } else {
                    slack[c] = Math.hypot(s.planX()[c], s.planZ()[c]) >= FLOWED;
                    if (Math.hypot(nowX - wasX, nowZ - wasZ) >= FLOWED) {
                        addX = fade * (nowX - wasX);
                        addZ = fade * (nowZ - wasZ);
                    }
                }
                final double size = Math.hypot(addX, addZ);
                if (size > MOST_CHANGE) {
                    addX *= MOST_CHANGE / size;
                    addZ *= MOST_CHANGE / size;
                }
                ax[c] = (float) addX;
                az[c] = (float) addZ;
            }
        }
        spread(w, h, depthNow, slack, sc);

        final Map<Long, Field> chunks = new HashMap<>();
        final int cx0 = s.x0() >> 4, cz0 = s.z0() >> 4;
        final int cx1 = (s.x0() + w - 1) >> 4, cz1 = (s.z0() + h - 1) >> 4;
        for (int cz = cz0; cz <= cz1; cz++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                final float[] scales = new float[256], turns = new float[256];
                final float[] addX = new float[256], addZ = new float[256];
                final short[] top = new short[256];
                final byte[] depth = new byte[256];
                boolean any = false;
                for (int k = 0; k < 256; k++) {
                    scales[k] = 1.0F;
                    final int i = (cx << 4) + (k & 15) - s.x0(), j = (cz << 4) + (k >> 4) - s.z0();
                    if (i < 0 || j < 0 || i >= w || j >= h) continue;
                    final int c = i + j * w;
                    if (depthNow[c] <= 0) continue;
                    if (Math.abs(sc[c] - 1.0F) < LEAST_CHANGE && Math.abs(tu[c]) < LEAST_CHANGE
                            && Math.abs(ax[c]) < LEAST_CHANGE && Math.abs(az[c]) < LEAST_CHANGE) continue;
                    scales[k] = sc[c];
                    turns[k] = tu[c];
                    addX[k] = ax[c];
                    addZ[k] = az[c];
                    top[k] = s.topNow()[c];
                    depth[k] = depthNow[c];
                    any = true;
                }
                if (any) chunks.put(ChunkPos.asLong(cx, cz), new Field(scales, turns, addX, addZ, top, depth));
            }
        }
        return new Win(s.group(), chunks);
    }

    private static void spread(int w, int h, byte[] depthNow, boolean[] slack, float[] sc) {
        final boolean[] waits = slack.clone();
        final float[] next = new float[w * h];
        for (int pass = 0; pass < SPREAD; pass++) {
            boolean any = false;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    final int c = i + j * w;
                    next[c] = Float.NaN;
                    if (!waits[c]) continue;
                    float sum = 0.0F;
                    int count = 0;
                    for (int d = 0; d < 4; d++) {
                        final int ni = i + (d == 0 ? 1 : d == 1 ? -1 : 0), nj = j + (d == 2 ? 1 : d == 3 ? -1 : 0);
                        if (ni < 0 || nj < 0 || ni >= w || nj >= h) continue;
                        final int nc = ni + nj * w;
                        if (depthNow[nc] <= 0 || waits[nc]) continue;
                        sum += sc[nc];
                        count++;
                    }
                    if (count > 0) next[c] = sum / count;
                }
            }
            for (int c = 0; c < w * h; c++) {
                if (Float.isNaN(next[c])) continue;
                sc[c] = next[c];
                waits[c] = false;
                any = true;
            }
            if (!any) break;
        }
    }

    private static void labDump(Snapshot s, double[] was, double[] now) {
        final String dir = System.getProperty("dynamicwaters.labCurrentDump");
        if (dir == null) return;
        final StringBuilder sb = new StringBuilder();
        sb.append(s.x0()).append(' ').append(s.z0()).append(' ').append(s.w()).append(' ').append(s.h()).append('\n');
        for (int c = 0; c < s.w() * s.h(); c++) {
            if (s.depthWas()[c] <= 0 && s.depthNow()[c] <= 0) continue;
            sb.append(String.format(java.util.Locale.ROOT, "%d %d %d %d %d %.5f %.5f%n", s.x0() + c % s.w(), s.z0() + c / s.w(),
                    s.depthWas()[c], s.depthNow()[c], -1, was[c], now[c]));
        }
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(dir));
            java.nio.file.Files.writeString(java.nio.file.Path.of(dir, "group_" + s.x0() + "_" + s.z0() + ".txt"), sb);
        } catch (java.io.IOException ignored) {
        }
    }

    private static final int[] SIDE_I = {1, -1, 0, 0}, SIDE_J = {0, 0, 1, -1};

    private static double[] headLaid(int w, int h, byte[] depth, float[] planX, float[] planZ) {
        final double[] head = new double[w * h];
        final double omega = Math.min(1.97, 2.0 / (1.0 + Math.sin(Math.PI / Math.max(w, h))));
        for (int sweep = 0; sweep < MOST_SWEEPS; sweep++) {
            double most = 0.0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    final int c = i + j * w;
                    final int k = depth[c];
                    if (k <= 0) continue;
                    double sum = 0.0, weight = 0.0;
                    for (int d = 0; d < 4; d++) {
                        final int ni = i + SIDE_I[d], nj = j + SIDE_J[d];
                        if (ni < 0 || nj < 0 || ni >= w || nj >= h) continue;
                        final int nc = ni + nj * w;
                        if (depth[nc] <= 0) continue;
                        final int f = Math.min(k, depth[nc]);

                        final double run = 0.5 * ((planX[c] + planX[nc]) * SIDE_I[d] + (planZ[c] + planZ[nc]) * SIDE_J[d]);
                        sum += f * (head[nc] + run);
                        weight += f;
                    }
                    if (weight <= 0.0) continue;
                    final double next = head[c] + omega * (sum / weight - head[c]);
                    most = Math.max(most, Math.abs(next - head[c]));
                    head[c] = next;
                }
            }
            if (most < SETTLED) break;
        }
        return head;
    }

    private static double[] head(int w, int h, byte[] depth, byte[] depthWas, double[] laid) {
        final int n = w * h;
        final double[] head = new double[n], draws = new double[n];
        final boolean[] fixed = new boolean[n], set = new boolean[n];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                final int c = i + j * w;
                if (depthWas[c] <= 0) continue;
                head[c] = laid[c];
                set[c] = true;
                fixed[c] = depth[c] > 0 && (i == 0 || j == 0 || i == w - 1 || j == h - 1);
                draws[c] = depthWas[c] / (LEAK_REACH * LEAK_REACH);
            }
        }

        for (boolean any = true; any; ) {
            any = false;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    final int c = i + j * w;
                    if (set[c] || depth[c] <= 0) continue;
                    for (int d = 0; d < 4 && !set[c]; d++) {
                        final int ni = i + SIDE_I[d], nj = j + SIDE_J[d];
                        if (ni < 0 || nj < 0 || ni >= w || nj >= h || !set[ni + nj * w] || depth[ni + nj * w] <= 0) continue;
                        head[c] = head[ni + nj * w];
                        set[c] = true;
                        any = true;
                    }
                }
            }
        }
        final double omega = Math.min(1.97, 2.0 / (1.0 + Math.sin(Math.PI / Math.max(w, h))));
        for (int sweep = 0; sweep < MOST_SWEEPS; sweep++) {
            double most = 0.0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    final int c = i + j * w;
                    final int k = depth[c];
                    if (k <= 0 || fixed[c]) continue;
                    double sum = draws[c] * laid[c], weight = draws[c];
                    for (int d = 0; d < 4; d++) {
                        final int ni = i + SIDE_I[d], nj = j + SIDE_J[d];
                        if (ni < 0 || nj < 0 || ni >= w || nj >= h) continue;
                        final int nc = ni + nj * w;
                        if (depth[nc] <= 0) continue;
                        final int f = Math.min(k, depth[nc]);
                        sum += f * head[nc];
                        weight += f;
                    }
                    if (weight <= 0.0) continue;
                    final double next = head[c] + omega * (sum / weight - head[c]);
                    most = Math.max(most, Math.abs(next - head[c]));
                    head[c] = next;
                }
            }
            if (most < SETTLED) break;
        }
        return head;
    }

    private static double slope(double[] phi, byte[] depth, int w, int h, int i, int j, int di, int dj) {
        final int c = i + j * w;
        if (depth[c] <= 0) return 0.0;
        final int ia = i + di, ja = j + dj, ib = i - di, jb = j - dj;
        final boolean a = ia >= 0 && ja >= 0 && ia < w && ja < h && depth[ia + ja * w] > 0;
        final boolean b = ib >= 0 && jb >= 0 && ib < w && jb < h && depth[ib + jb * w] > 0;
        if (a && b) return (phi[ia + ja * w] - phi[ib + jb * w]) * 0.5;
        if (a) return phi[ia + ja * w] - phi[c];
        if (b) return phi[c] - phi[ib + jb * w];
        return 0.0;
    }

    private static Data data(ServerLevel level) {
        //? if >=1.20.5 {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), DATA_NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(Data::load, Data::new, DATA_NAME);
        *///?}
    }

    public static final class Data extends SavedData {
        final Long2ObjectOpenHashMap<Long2ByteOpenHashMap> changes = new Long2ObjectOpenHashMap<>();

        //? if >=1.20.5 {
        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
        //?} else {
        /*static Data load(CompoundTag tag) {
        *///?}
            final Data data = new Data();
            final ListTag list = tag.getList("Chunks", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                final CompoundTag c = list.getCompound(i);
                final long[] blocks = c.getLongArray("Blocks");
                final byte[] kinds = c.getByteArray("Was");
                if (blocks.length != kinds.length || blocks.length == 0) continue;
                final Long2ByteOpenHashMap m = new Long2ByteOpenHashMap();
                for (int k = 0; k < blocks.length; k++) m.put(blocks[k], kinds[k]);
                data.changes.put(c.getLong("Chunk"), m);
            }
            return data;
        }

        @Override
        //? if >=1.20.5 {
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        //?} else {
        /*public CompoundTag save(CompoundTag tag) {
        *///?}
            final ListTag list = new ListTag();
            for (Long2ObjectMap.Entry<Long2ByteOpenHashMap> e : changes.long2ObjectEntrySet()) {
                final CompoundTag c = new CompoundTag();
                c.putLong("Chunk", e.getLongKey());
                final long[] blocks = new long[e.getValue().size()];
                final byte[] kinds = new byte[blocks.length];
                int k = 0;
                for (Long2ByteMap.Entry b : e.getValue().long2ByteEntrySet()) {
                    blocks[k] = b.getLongKey();
                    kinds[k++] = b.getByteValue();
                }
                c.putLongArray("Blocks", blocks);
                c.putByteArray("Was", kinds);
                list.add(c);
            }
            tag.put("Chunks", list);
            return tag;
        }
    }
}
