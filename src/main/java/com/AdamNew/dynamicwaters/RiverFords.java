package com.AdamNew.dynamicwaters;

import com.AdamNew.dynamicwaters.river.RiverGraphEdge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RiverFords {
    static final double FORD_STEP = 72.0;

    static final double FORD_CHANCE = 0.11;

    static final double FORD_CLEAR = 48.0;
    static final double HEAD_CLEAR = 110.0;

    static final double END_CLEAR = 28.0;

    static final double FORD_HALF = 2.5;

    static final double FORD_RISE = 2.5;
    static final double FORD_SOFT = 3.0;
    static final double FORD_REACH = FORD_HALF + FORD_SOFT * 0.5 + 9.0 * FORD_RISE;

    static double reach() {
        return FORD_REACH + 9.0 * FORD_RISE * (Math.max(1.0, HydrologyManager.riverScale().lowlandDepth()) - 1.0);
    }

    static final int SEA_DEPTH = 6;
    static final double SEA_CLEAR = 8.0;
    static final int SEA_RAYS = 16;

    static final double ROCK_MIN = 2.0;
    static final double ROCK_MAX = 3.0;
    static final double ROCK_LUMP = 0.12;
    static final double ROCK_SINK = 0.5;

    static final double ROCK_GAP = 2.0;
    static final double ROCK_GAP_MORE = 1.5;

    static final double ROCK_SKIP = 0.2;

    static final double ROCK_SWAY = 1.2;

    static final double LOG_CHANCE = 0.8;
    static final int LOG_MIN = 3;
    static final int LOG_MAX = 5;

    static final int LOG_GAP = 2;

    public record Rock(double along, double across, double radius, double sink, double lump) {
    }

    public record Snag(int x, int z, boolean alongX, int length) {
    }

    public record Ford(double x, double z, double tx, double tz, Rock[] rocks, Snag snag) {
    }

    private static final Ford[] NONE = new Ford[0];

    private static final boolean LAB = Boolean.getBoolean("dynamicwaters.labFords");
    private static final Map<RiverGraphEdge, Ford[]> FORDS = Collections.synchronizedMap(new WeakHashMap<>());

    private RiverFords() {
    }

    static Ford[] fordsOf(RiverGraphEdge edge) {
        if (edge.delta()) return NONE;
        Ford[] known = FORDS.get(edge);
        if (known != null) return known;
        final double[] s = edge.fractal().segments;
        final int n = s.length / 2;
        if (n < 2) return NONE;
        final double[] along = new double[n];
        for (int i = 1; i < n; i++) along[i] = along[i - 1] + Math.hypot(s[2 * i] - s[2 * i - 2], s[2 * i + 1] - s[2 * i - 1]);
        final double total = along[n - 1];
        final List<Ford> found = new ArrayList<>();
        final long seed = HydrologyManager.worldSeed() ^ Math.round(edge.source().x()) * 0x9E3779B97F4A7C15L
                ^ Math.round(edge.source().y()) * 0xC2B2AE3D27D4EB4FL;

        final double slopes = reach() + 1.5;
        final double confluence = Math.max(FORD_CLEAR, slopes), onward = Math.max(END_CLEAR, slopes);
        final double fromStart = !edge.sourceEdge() ? Math.max(HEAD_CLEAR, slopes) : inflows(edge) >= 2 ? confluence : onward;
        final RiverGraphEdge down = edge.drainEdge();
        final double fromEnd = down == null || inflows(down) >= 2 ? confluence : onward;
        int k = 0;
        for (double at = fromStart; at <= total - fromEnd; at += FORD_STEP, k++) {
            if (draw(seed, k) >= FORD_CHANCE) continue;
            final double[] p = pointAt(s, along, at), a = pointAt(s, along, at - 4.0), b = pointAt(s, along, at + 4.0);
            final double dx = b[0] - a[0], dz = b[1] - a[1], len = Math.hypot(dx, dz);
            if (len < 1.0E-6) continue;
            final double tx = dx / len, tz = dz / len;
            final long own = seed ^ (k + 1) * 0xBF58476D1CE4E5B9L;
            final double w = HydrologyManager.waterRadiusAt(edge, p[0], p[1]);
            if (nearSea(p[0], p[1], w)) {
                if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: ford by the sea at {} {}",
                        Math.round(p[0]), Math.round(p[1]));
                continue;
            }
            found.add(new Ford(p[0], p[1], tx, tz, rocks(own, w), snag(own, p[0], p[1], tx, tz, w)));
        }
        known = found.isEmpty() ? NONE : found.toArray(new Ford[0]);
        if (FORDS.put(edge, known) == null && LAB) {
            for (Ford f : known) {
                com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: ford at {} {} way {} {} rocks {} log {}",
                        Math.round(f.x()), Math.round(f.z()), String.format(java.util.Locale.ROOT, "%.2f", f.tx()),
                        String.format(java.util.Locale.ROOT, "%.2f", f.tz()), f.rocks().length,
                        f.snag() == null ? "-" : f.snag().x() + " " + f.snag().z());
            }
        }
        return known;
    }

    private static boolean nearSea(double x, double z, double w) {
        final net.minecraft.world.level.chunk.ChunkGenerator generator = HydrologyManager.getGlobalGenerator();
        final net.minecraft.world.level.levelgen.RandomState random = HydrologyManager.getGlobalRandomState();
        if (generator == null || random == null) return false;
        final int deep = HydrologyManager.getSeaLevel() - SEA_DEPTH;
        final double reach = reach() + w + SEA_CLEAR;
        for (int ring = 1; ring <= 2; ring++) {
            final double r = reach * ring * 0.5;
            for (int i = 0; i < SEA_RAYS; i++) {
                final double a = (i + 0.5 * ring) * Math.PI * 2.0 / SEA_RAYS;
                final int sx = Mth.floor(x + r * Math.cos(a)), sz = Mth.floor(z + r * Math.sin(a));
                if (com.AdamNew.dynamicwaters.terrain.CoarseHeightField.surfaceAt(sx, sz, generator, random) < deep) return true;
            }
        }
        return false;
    }

    private static Rock[] rocks(long seed, double w) {
        final List<Rock> rocks = new ArrayList<>();
        int j = 0;
        double radius = Mth.lerp(draw(seed, j++), ROCK_MIN, ROCK_MAX);
        double across = -w + 0.5 + draw(seed, j++);
        Rock first = null;
        while (across <= w - 0.5) {
            final Rock rock = new Rock((draw(seed, j++) * 2.0 - 1.0) * ROCK_SWAY, across, radius,
                    ROCK_SINK * (1.0 + draw(seed, j++)), draw(seed, j++) * Math.PI * 2.0);
            if (first == null) first = rock;
            if (draw(seed, j++) >= ROCK_SKIP) rocks.add(rock);
            final double next = Mth.lerp(draw(seed, j++), ROCK_MIN, ROCK_MAX);
            across += radius + ROCK_GAP + draw(seed, j++) * ROCK_GAP_MORE + next;
            radius = next;
        }
        if (rocks.isEmpty() && first != null) rocks.add(first);
        return rocks.toArray(new Rock[0]);
    }

    private static Snag snag(long seed, double fx, double fz, double tx, double tz, double w) {
        if (draw(seed, 100) >= LOG_CHANCE || w < 3.0) return null;
        final int length = LOG_MIN + (int) (draw(seed, 105) * (LOG_MAX - LOG_MIN + 1));
        final double side = draw(seed, 101) < 0.5 ? -1.0 : 1.0;
        final double along = side * (FORD_HALF + 4.0 + draw(seed, 102) * 2.0);
        final double across = (draw(seed, 103) * 2.0 - 1.0) * Math.max(0.0, w - 1.5 - length * 0.5);
        final double cx = fx + along * tx - across * tz, cz = fz + along * tz + across * tx;
        final boolean alongX = Math.abs(tx) < Math.abs(tz);
        final int mx = Mth.floor(cx), mz = Mth.floor(cz);
        final int mid = alongX ? mx : mz;
        final int chunkMin = Math.floorDiv(mid, 16) * 16;
        final int start = Mth.clamp(mid - length / 2, chunkMin, chunkMin + 16 - length);
        return alongX ? new Snag(start, mz, true, length) : new Snag(mx, start, false, length);
    }

    public static double shallow(RiverGraphEdge edge, double x, double z, double waterRadius, double[] rock) {
        rock[0] = Double.NEGATIVE_INFINITY;
        double best = Double.POSITIVE_INFINITY;
        for (Ford f : fordsOf(edge)) {
            final double rx = x - f.x(), rz = z - f.z();
            final double along = rx * f.tx() + rz * f.tz();
            final double across = -rx * f.tz() + rz * f.tx();
            if (Math.abs(across) > waterRadius + 1.0 || Math.abs(along) > reach()) continue;
            final double off = Math.max(0.0, Math.abs(along) - FORD_HALF);
            final double drop = off < FORD_SOFT ? off * off / (2.0 * FORD_SOFT * FORD_RISE) : (off - FORD_SOFT * 0.5) / FORD_RISE;
            if (drop < best) best = drop;
            if (Math.abs(along) > FORD_HALF + ROCK_SWAY + ROCK_MAX * (1.0 + ROCK_LUMP)) continue;
            for (Rock r : f.rocks()) {
                final double da = along - r.along(), dc = across - r.across();
                final double edge2 = r.radius() * (1.0 + ROCK_LUMP * Math.sin(3.0 * Math.atan2(dc, da) + r.lump()));
                final double d = Math.sqrt(da * da + dc * dc) / edge2;
                if (d > 1.0) continue;
                rock[0] = Math.max(rock[0], r.radius() * Math.sqrt(1.0 - d * d) - r.sink());
            }
        }
        return best;
    }

    public static void decorated(WorldGenLevel level, ChunkAccess chunk) {
        if (!Config.RIVER_FORDS.get() || !Config.dynamicNormalRivers() || !HydrologyManager.riversIn(level.getLevel())) return;
        final ChunkPos pos = chunk.getPos();
        final int water = level.getSeaLevel() - 1;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        final int minX = pos.getMinBlockX() - 16, maxX = pos.getMaxBlockX() + 16;
        final int minZ = pos.getMinBlockZ() - 16, maxZ = pos.getMaxBlockZ() + 16;
        final java.util.Set<RiverGraphEdge> edges = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        edges.addAll(MainRivers.riversAt(minX, minZ));
        edges.addAll(MainRivers.riversAt(maxX, minZ));
        edges.addAll(MainRivers.riversAt(minX, maxZ));
        edges.addAll(MainRivers.riversAt(maxX, maxZ));
        for (RiverGraphEdge edge : edges) {
            for (Ford f : fordsOf(edge)) {
                mend(level, f, water, minX, maxX, minZ, maxZ, at);
                final Snag s = f.snag();
                if (s == null || s.x() >> 4 != pos.x || s.z() >> 4 != pos.z) continue;
                sink(chunk, pos, s, water, at);
            }
        }
    }

    private static void mend(WorldGenLevel level, Ford f, int water, int minX, int maxX, int minZ, int maxZ,
            BlockPos.MutableBlockPos at) {
        final BlockPos.MutableBlockPos near = new BlockPos.MutableBlockPos();
        for (Rock r : f.rocks()) {
            final double cx = f.x() + r.along() * f.tx() - r.across() * f.tz();
            final double cz = f.z() + r.along() * f.tz() + r.across() * f.tx();
            final int reach = (int) Math.ceil(r.radius() * (1.0 + ROCK_LUMP)) + 1;
            final int x0 = Math.max(minX, Mth.floor(cx) - reach), x1 = Math.min(maxX, Mth.floor(cx) + reach);
            final int z0 = Math.max(minZ, Mth.floor(cz) - reach), z1 = Math.min(maxZ, Mth.floor(cz) + reach);
            if (x0 > x1 || z0 > z1) continue;
            for (int pass = 0; pass < 2; pass++) {
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        final int top = rockTop(f, r, x, z, water);
                        for (int y = top; y >= water; y--) {
                            if (!soft(level.getBlockState(at.set(x, y, z)))) continue;
                            final BlockState stone = stoneNext(level, x, y, z, minX, maxX, minZ, maxZ, near);
                            if (stone != null) level.setBlock(at, stone, 2);
                        }
                    }
                }
            }
        }
    }

    private static int rockTop(Ford f, Rock r, int x, int z, int water) {
        final double rx = x - f.x(), rz = z - f.z();
        final double da = rx * f.tx() + rz * f.tz() - r.along(), dc = -rx * f.tz() + rz * f.tx() - r.across();
        final double edge = r.radius() * (1.0 + ROCK_LUMP * Math.sin(3.0 * Math.atan2(dc, da) + r.lump()));
        final double d = Math.sqrt(da * da + dc * dc) / edge;
        if (d > 1.0) return Integer.MIN_VALUE;
        return (int) Math.round(water + r.radius() * Math.sqrt(1.0 - d * d) - r.sink());
    }

    private static BlockState stoneNext(WorldGenLevel level, int x, int y, int z, int minX, int maxX, int minZ, int maxZ,
            BlockPos.MutableBlockPos near) {
        final int[][] sides = {{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};
        for (int[] d : sides) {
            final int nx = x + d[0], nz = z + d[2];
            if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
            final BlockState state = level.getBlockState(near.set(nx, y + d[1], nz));
            if (stone(state)) return state;
        }
        return null;
    }

    private static boolean soft(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.DIRT) || state.is(net.minecraft.tags.BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY);
    }

    private static boolean stone(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.SANDSTONE) || state.is(net.minecraft.tags.BlockTags.TERRACOTTA);
    }

    private static void sink(ChunkAccess chunk, ChunkPos pos, Snag s, int water, BlockPos.MutableBlockPos at) {
        final int n = s.length();
        final int[] bed = new int[n];
        for (int k = 0; k < n; k++) {
            final int x = s.alongX() ? s.x() + k : s.x(), z = s.alongX() ? s.z() : s.z() + k;
            final BlockState top = chunk.getBlockState(at.set(x, water, z));
            if (!wet(top) && !top.is(Blocks.ICE)) return;
            int b = water - 1;
            while (b > chunk.getMinBuildHeight() && wet(chunk.getBlockState(at.set(x, b, z)))) b--;
            bed[k] = b;
        }
        final int[] sorted = bed.clone();
        java.util.Arrays.sort(sorted);
        final int y = sorted[n / 2] + 1;
        if (y > water - 1) return;
        for (int b : bed) if (b > y || b < y - 1 - LOG_GAP) return;
        final BlockState log = RiverLogs.wood(chunk, pos, y, at)
                .setValue(RotatedPillarBlock.AXIS, s.alongX() ? Direction.Axis.X : Direction.Axis.Z);
        for (int k = 0; k < n; k++) {
            final int x = s.alongX() ? s.x() + k : s.x(), z = s.alongX() ? s.z() : s.z() + k;

            for (int ky = bed[k] + 1; ky < water; ky++) {
                final BlockState here = chunk.getBlockState(at.set(x, ky, z));
                if (wet(here) && !here.is(Blocks.WATER)) chunk.setBlockState(at, Blocks.WATER.defaultBlockState(), false);
            }
            chunk.setBlockState(at.set(x, y, z), log, false);
        }
        if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: ford log at {} {} {} along {} length {}",
                s.x(), y, s.z(), s.alongX() ? "x" : "z", n);
    }

    private static boolean wet(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    private static int inflows(RiverGraphEdge edge) {
        int n = 0;
        for (RiverGraphEdge e : MainRivers.riversAt(edge.source().x(), edge.source().y())) {
            if (e.drainEdge() == edge) n++;
        }
        return n;
    }

    private static double[] pointAt(double[] s, double[] along, double at) {
        final int n = along.length;
        if (at <= 0.0) return new double[] {s[0], s[1]};
        for (int i = 1; i < n; i++) {
            if (along[i] >= at) {
                final double seg = along[i] - along[i - 1];
                final double t = seg > 0.0 ? (at - along[i - 1]) / seg : 0.0;
                return new double[] {Mth.lerp(t, s[2 * i - 2], s[2 * i]), Mth.lerp(t, s[2 * i - 1], s[2 * i + 1])};
            }
        }
        return new double[] {s[2 * n - 2], s[2 * n - 1]};
    }

    private static double draw(long seed, int k) {
        long v = seed + k * 0xD1B54A32D192ED03L;
        v ^= v >>> 33;
        v *= 0xFF51AFD7ED558CCDL;
        v ^= v >>> 33;
        v *= 0xC4CEB9FE1A85EC53L;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }
}
