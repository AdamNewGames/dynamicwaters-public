package com.AdamNew.dynamicwaters;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

public final class RiverArches {
    static final double ARCH_CHANCE = 0.15;

    static final int ARCH_MIN_DEPTH = 12;

    static final int ARCH_MIN_CLEAR = 6;

    static final int ARCH_MIN_SPAN = 8, ARCH_MAX_SPAN = 30;

    static final int ARCH_THICK = 3;

    static final double ARCH_HALF = 1.5, ARCH_HALF_MORE = 1.5, ARCH_HALF_WALLS = 0.6;

    static final double ARCH_RISE_SHARE = 0.3, ARCH_RISE_MORE = 2.0;

    static final double ARCH_BOW = 1.2, ARCH_WAVER = 0.22;

    private static final int WALL_REACH = 24;

    private static final int RIM_REACH = 6;

    private static final int WALL_FOOT = 4;

    private static final int SCAN_OVER = 96;

    private static final double CHANCE = Double.parseDouble(System.getProperty("dynamicwaters.labArchChance",
            String.valueOf(ARCH_CHANCE)));

    private RiverArches() {
    }

    public static void place(WorldGenLevel level, ChunkAccess chunk) {
        if (!Config.RIVER_ARCHES.get()) return;
        final ServerLevel server = level.getLevel();
        if (!HydrologyManager.riversIn(server)) return;
        final ChunkPos cp = chunk.getPos();
        if (draw(cp, 0) >= CHANCE) return;
        final ChunkGenerator generator = server.getChunkSource().getGenerator();
        final RandomState randomState = server.getChunkSource().randomState();
        final int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ(), x1 = x0 + 15, z1 = z0 + 15;
        final double mx = x0 + 8.0, mz = z0 + 8.0;

        double best = Double.MAX_VALUE, cx = 0.0, cz = 0.0, ux = 0.0, uz = 0.0;
        WatershedPath mountain = null;
        for (WatershedPath p : HydrologyManager.mountainPathsIn(x0, z0, x1, z1, 0.0, generator, randomState)) {
            for (int i = 0; i + 1 < p.validNodeCount; i++) {
                final double ax = p.segmentsX[i], az = p.segmentsZ[i];
                final double dx = p.segmentsX[i + 1] - ax, dz = p.segmentsZ[i + 1] - az;
                final double len2 = dx * dx + dz * dz;
                if (len2 < 1.0E-6) continue;
                final double t = Mth.clamp(((mx - ax) * dx + (mz - az) * dz) / len2, 0.0, 1.0);
                final double px = ax + dx * t, pz = az + dz * t;
                final double d = (mx - px) * (mx - px) + (mz - pz) * (mz - pz);
                if (d >= best) continue;
                final double len = Math.sqrt(len2);
                best = d;
                cx = px;
                cz = pz;
                ux = dx / len;
                uz = dz / len;
                mountain = p;
            }
        }
        if (mountain != null && !HydrologyManager.carvedRiver(mountain, generator, randomState)) best = Double.MAX_VALUE;
        final com.AdamNew.dynamicwaters.river.RiverContext lowland = MainRivers.findNearest(mx, mz, generator,
                HydrologyManager.getDummyAccessor(), randomState);
        if (lowland != null) {
            final double[] on = new double[2];
            final double d = lowland.edge().fractal().nearestPoint(mx, mz, on);
            final net.minecraft.world.phys.Vec3 way = lowland.edge().fractal().calculateContinuousFlow(on[0], on[1]);
            final double wl = Math.hypot(way.x, way.z);
            if (d < best && wl > 1.0E-6) {
                best = d;
                cx = on[0];
                cz = on[1];
                ux = way.x / wl;
                uz = way.z / wl;
            }
        }
        if (best == Double.MAX_VALUE) return;
        if (Mth.floor(cx) < x0 || Mth.floor(cx) > x1 || Mth.floor(cz) < z0 || Mth.floor(cz) > z1) return;
        lab("tried", Mth.floor(cx), 0, Mth.floor(cz));
        arch(level, cp, cx, cz, ux, uz);
    }

    private static void arch(WorldGenLevel level, ChunkPos cp, double cx, double cz, double ux, double uz) {
        final int bx = Mth.floor(cx), bz = Mth.floor(cz);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

        final int water = ground(level, bx, bz, level.getMaxBuildHeight() - 1, level.getMinBuildHeight(), at);
        if (water == Integer.MIN_VALUE || !level.getFluidState(at.set(bx, water, bz)).is(net.minecraft.tags.FluidTags.WATER)) {
            lab("no water", bx, water, bz);
            return;
        }
        final double vx = -uz, vz = ux;

        final int[] rimA = rim(level, cp, cx, cz, vx, vz, water, at), rimB = rim(level, cp, cx, cz, -vx, -vz, water, at);
        if (rimA == null || rimB == null) {
            lab("no wall", bx, water, bz);
            return;
        }
        final int rimLow = Math.min(rimA[0], rimB[0]);
        if (rimLow - water < ARCH_MIN_DEPTH) {
            lab("shallow " + (rimLow - water), bx, water, bz);
            return;
        }
        final int thick = ARCH_THICK + (draw(cp, 1) < 0.5 ? 0 : 1);

        int open = water;
        while (open < rimLow && free(level.getBlockState(at.set(bx, open + 1, bz)))) open++;

        int top = Integer.MIN_VALUE;
        double spanA = 0.0, spanB = 0.0;
        for (int t = Math.min(rimLow, open); t - thick + 1 - (water + 1) >= ARCH_MIN_CLEAR; t--) {
            final int y = t - thick / 2;
            final double a = wall(level, cp, cx, cz, vx, vz, y, at), b = wall(level, cp, cx, cz, -vx, -vz, y, at);
            if (a < 0.0 || b < 0.0 || a + b > ARCH_MAX_SPAN || a + b < ARCH_MIN_SPAN) continue;
            top = t;
            spanA = a;
            spanB = b;
            break;
        }
        if (top == Integer.MIN_VALUE) {
            lab("too wide, depth " + (rimLow - water) + " open " + (open - water), bx, water, bz);
            return;
        }
        final int crown = top - thick + 1;

        final int rise = Math.max(0, Math.min((int) Math.round(ARCH_RISE_SHARE * Math.min(spanA, spanB)
                + ARCH_RISE_MORE * draw(cp, 2)), crown - (water + 1) - 3));
        final double half = ARCH_HALF + ARCH_HALF_MORE * draw(cp, 3);

        final double bow = (draw(cp, 4) * 2.0 - 1.0) * ARCH_BOW;
        final double p1 = draw(cp, 5) * Math.PI * 2.0, p2 = draw(cp, 6) * Math.PI * 2.0, p3 = draw(cp, 7) * Math.PI * 2.0;

        final LongArrayList cells = new LongArrayList();
        final LongOpenHashSet taken = new LongOpenHashSet();
        final Object2IntOpenHashMap<BlockState> rock = new Object2IntOpenHashMap<>();
        final Long2IntOpenHashMap columnTop = new Long2IntOpenHashMap();
        final int rows = (int) Math.ceil((half * (1.0 + ARCH_HALF_WALLS) * (1.0 + ARCH_WAVER) + ARCH_BOW) * 2.0);
        for (int r = -rows; r <= rows; r++) {
            final double a = r * 0.5;
            for (int side = -1; side <= 1; side += 2) {
                final double span = side > 0 ? spanA : spanB;
                final double sx = side * vx, sz = side * vz;
                for (int y = crown - rise - 1; y <= top; y++) {
                    final LongArrayList row = new LongArrayList();
                    boolean met = false;
                    for (double s = side > 0 ? 0.0 : 0.5; s <= WALL_REACH; s += 0.5) {
                        final int x = Mth.floor(cx + ux * a + sx * s), z = Mth.floor(cz + uz * a + sz * s);
                        if (!inRegion(cp, x, z)) break;
                        final BlockState state = level.getBlockState(at.set(x, y, z));
                        if (!state.getFluidState().isEmpty()) break;
                        if (!free(state)) {
                            met = true;
                            if (solidRock(state)) rock.addTo(state, 1);
                            break;
                        }
                        final double out = Math.min(1.0, s / span);
                        final double along = side * s;
                        final double width = half * (1.0 + ARCH_HALF_WALLS * out * out)
                                * (1.0 + ARCH_WAVER * (0.6 * Math.sin(along * 0.45 + p1) + 0.4 * Math.sin(along * 0.9 + p2)));
                        final double off = a - bow * (1.0 - out * out);
                        if (Math.abs(off) > width) continue;
                        final int bottom = Math.max(water + 3, crown - (int) Math.round(rise * out * out)
                                + (Math.sin(along * 0.6 + p3) > 0.55 ? 1 : 0));
                        final int over = top - (Math.sin(along * 0.5 + p2 + p3) > 0.5 ? 1 : 0);
                        if (y < bottom || y > over) continue;

                        final boolean edge = Math.abs(off) > width - 1.0;
                        if (edge && y == over && hash(x, y, z, 1) < 0.35) continue;
                        if (edge && y == bottom && hash(x, y, z, 2) < 0.35) continue;
                        row.add(BlockPos.asLong(x, y, z));
                    }

                    if (!met) {
                        if (r == 0 && y >= crown) {
                            lab("middle hangs at " + y, bx, water, bz);
                            return;
                        }
                        continue;
                    }
                    for (int k = 0; k < row.size(); k++) {
                        final long pos = row.getLong(k);
                        if (!taken.add(pos)) continue;
                        cells.add(pos);
                        final long column = BlockPos.asLong(BlockPos.getX(pos), 0, BlockPos.getZ(pos));
                        if (BlockPos.getY(pos) > columnTop.getOrDefault(column, Integer.MIN_VALUE)) {
                            columnTop.put(column, BlockPos.getY(pos));
                        }
                    }
                }
            }
        }
        if (cells.isEmpty()) return;

        BlockState stone = Blocks.STONE.defaultBlockState();
        int most = 0;
        for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<BlockState> e : rock.object2IntEntrySet()) {
            if (e.getIntValue() > most) {
                most = e.getIntValue();
                stone = e.getKey();
            }
        }
        final int[] lowRim = rimA[0] <= rimB[0] ? rimA : rimB;
        BlockState cover = null, under = null, snow = null;
        if (top >= rimLow - 1) {
            final BlockState rimTop = level.getBlockState(at.set(lowRim[1], lowRim[0], lowRim[2]));
            if (rimTop.is(BlockTags.DIRT) || rimTop.is(BlockTags.SAND)) {
                cover = rimTop;
                final BlockState below = level.getBlockState(at.set(lowRim[1], lowRim[0] - 1, lowRim[2]));
                under = below.is(BlockTags.DIRT) || below.is(BlockTags.SAND) ? below : null;
                final BlockState over = level.getBlockState(at.set(lowRim[1], lowRim[0] + 1, lowRim[2]));
                if (over.is(Blocks.SNOW)) snow = over;
            }
        }
        for (int k = 0; k < cells.size(); k++) {
            final long pos = cells.getLong(k);
            final int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
            final int columnY = columnTop.get(BlockPos.asLong(x, 0, z));
            BlockState state = stone;
            if (cover != null && y == columnY) state = cover;
            else if (under != null && y == columnY - 1) state = under;
            level.setBlock(at.set(x, y, z), state, 2);
            if (snow != null && y == columnY && level.getBlockState(at.set(x, y + 1, z)).isAir()) level.setBlock(at, snow, 2);
        }
        if (Boolean.getBoolean("dynamicwaters.labArches")) {
            com.mojang.logging.LogUtils.getLogger().info("[labArches] arch at {},{},{} water {} span {}+{} thick {} rise {} blocks {} rock {}",
                    bx, top, bz, water, Math.round(spanA), Math.round(spanB), thick, rise, cells.size(), stone.getBlock());
        }
    }

    private static int[] rim(WorldGenLevel level, ChunkPos cp, double cx, double cz, double dx, double dz, int water,
            BlockPos.MutableBlockPos at) {
        int[] best = null;
        int past = -1;
        for (int s = 1; s <= WALL_REACH + RIM_REACH; s++) {
            final int x = Mth.floor(cx + dx * s), z = Mth.floor(cz + dz * s);
            if (!inRegion(cp, x, z)) break;
            final int y = ground(level, x, z, water + SCAN_OVER, water, at);
            if (past < 0) {
                if (y == Integer.MIN_VALUE || y < water + WALL_FOOT) {
                    if (s >= WALL_REACH) return null;
                    continue;
                }
                past = s;
            }
            if (y != Integer.MIN_VALUE && (best == null || y > best[0])) best = new int[] { y, x, z };
            if (s - past >= RIM_REACH) break;
        }
        return best;
    }

    private static double wall(WorldGenLevel level, ChunkPos cp, double cx, double cz, double dx, double dz, int y,
            BlockPos.MutableBlockPos at) {
        for (double s = 1.0; s <= WALL_REACH; s += 0.5) {
            final int x = Mth.floor(cx + dx * s), z = Mth.floor(cz + dz * s);
            if (!inRegion(cp, x, z)) return -1.0;
            final BlockState state = level.getBlockState(at.set(x, y, z));
            if (!state.getFluidState().isEmpty()) return -1.0;
            if (free(state)) continue;
            for (int k = 1; k <= 2; k++) {
                final int bx = Mth.floor(cx + dx * (s + k)), bz = Mth.floor(cz + dz * (s + k));
                if (!inRegion(cp, bx, bz) || free(level.getBlockState(at.set(bx, y, bz)))) return -1.0;
            }
            return s;
        }
        return -1.0;
    }

    private static int ground(WorldGenLevel level, int x, int z, int hi, int lo, BlockPos.MutableBlockPos at) {
        for (int y = hi; y >= lo; y--) {
            final BlockState state = level.getBlockState(at.set(x, y, z));
            if (!state.getFluidState().isEmpty()) return y;
            if (free(state) || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) continue;
            return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean free(BlockState state) {
        return state.isAir() || state.getFluidState().isEmpty() && state.canBeReplaced();
    }

    private static boolean solidRock(BlockState state) {
        return state.isSolid() && !state.is(BlockTags.DIRT) && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS)
                && !(state.getBlock() instanceof FallingBlock) && state.getBlock() != Blocks.BEDROCK
                && state.getBlock().defaultBlockState().isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO);
    }

    private static void lab(String why, int x, int y, int z) {
        if (Boolean.getBoolean("dynamicwaters.labArches")) {
            com.mojang.logging.LogUtils.getLogger().info("[labArches] none at {},{},{}: {}", x, y, z, why);
        }
    }

    private static boolean inRegion(ChunkPos cp, int x, int z) {
        return Math.abs((x >> 4) - cp.x) <= 1 && Math.abs((z >> 4) - cp.z) <= 1;
    }

    private static double draw(ChunkPos cp, int k) {
        long v = cp.x * 0x9E3779B97F4A7C15L ^ cp.z * 0xC2B2AE3D27D4EB4FL ^ (k + 0x5A17L) * 0x165667B19E3779F9L
                ^ HydrologyManager.worldSeed();
        v ^= v >>> 33;
        v *= 0xFF51AFD7ED558CCDL;
        v ^= v >>> 33;
        v *= 0xC4CEB9FE1A85EC53L;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }

    private static double hash(int x, int y, int z, int k) {
        long v = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L ^ k * 0x27D4EB2F165667C5L;
        v ^= v >>> 33;
        v *= 0xFF51AFD7ED558CCDL;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }
}
