package com.AdamNew.dynamicwaters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.material.FluidState;

public final class WaterfallCaves {
    static final int CAVE_MIN_FALL = 7;

    static final double CAVE_CHANCE = 0.45;

    static final double CAVE_DEPTH = 3.0, CAVE_DEPTH_MORE = 1.5;

    static final double CAVE_WIDTH_SHARE = 0.75, CAVE_HALF_MIN = 1.6, CAVE_HALF_MAX = 5.0;

    static final double CAVE_RIVER_MIN = 2.2;

    private static final int REACH = 12;

    private WaterfallCaves() {
    }

    public static void carve(WorldGenLevel level, ChunkAccess chunk) {
        if (!Config.WATERFALL_CAVES.get()) return;
        final ServerLevel server = level.getLevel();
        if (!HydrologyManager.riversIn(server)) return;
        final ChunkGenerator generator = server.getChunkSource().getGenerator();
        final RandomState randomState = server.getChunkSource().randomState();
        final int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ(), x1 = x0 + 15, z1 = z0 + 15;
        for (WatershedPath p : HydrologyManager.mountainPathsIn(x0, z0, x1, z1, REACH, generator, randomState)) {
            Boolean carved = null;
            for (int f = 0; f + 1 < p.validNodeCount; f++) {
                if (!WatershedPath.isFall(p.segmentsY, f) || p.segmentsY[f + 1] - p.segmentsY[f] < CAVE_MIN_FALL) continue;
                final double dx = p.segmentsX[f + 1] - p.segmentsX[f], dz = p.segmentsZ[f + 1] - p.segmentsZ[f];
                final double len = Math.hypot(dx, dz);
                if (len < 1.0E-6) continue;
                final double fx = p.segmentsX[f] + WatershedPath.FALL_FACE_T * dx, fz = p.segmentsZ[f] + WatershedPath.FALL_FACE_T * dz;
                if (fx < x0 - REACH || fx > x1 + REACH || fz < z0 - REACH || fz > z1 + REACH) continue;
                if (draw(p, f, 0) >= CAVE_CHANCE) continue;
                if (carved == null) carved = HydrologyManager.carvedRiver(p, generator, randomState);
                if (!carved) break;
                final double halfRiver = HydrologyManager.mountainWidthAt(p, fx, fz);
                if (halfRiver < CAVE_RIVER_MIN) continue;
                cave(level, x0, z0, p, f, fx, fz, dx / len, dz / len, halfRiver);
            }
        }
    }

    public static boolean planned(WatershedPath p, int f) {
        if (!Config.WATERFALL_CAVES.get() || f < 0 || f + 1 >= p.validNodeCount || !WatershedPath.isFall(p.segmentsY, f)
                || p.segmentsY[f + 1] - p.segmentsY[f] < CAVE_MIN_FALL || draw(p, f, 0) >= CAVE_CHANCE) {
            return false;
        }
        final double fx = p.segmentsX[f] + WatershedPath.FALL_FACE_T * (p.segmentsX[f + 1] - p.segmentsX[f]);
        final double fz = p.segmentsZ[f] + WatershedPath.FALL_FACE_T * (p.segmentsZ[f + 1] - p.segmentsZ[f]);
        return HydrologyManager.mountainWidthAt(p, fx, fz) >= CAVE_RIVER_MIN;
    }

    private static void cave(WorldGenLevel level, int x0, int z0, WatershedPath p, int f, double fx, double fz,
            double ux, double uz, double halfRiver) {
        final int low = surface(level, fx - 1.5 * ux, fz - 1.5 * uz, p.segmentsY[f]);
        final int high = surface(level, fx + 2.0 * ux, fz + 2.0 * uz, p.segmentsY[f + 1]);
        if (low == Integer.MIN_VALUE || high == Integer.MIN_VALUE || high - low < CAVE_MIN_FALL) return;
        final Shape shape = new Shape(fx, fz, ux, uz, CAVE_DEPTH + CAVE_DEPTH_MORE * draw(p, f, 1),
                Mth.clamp(CAVE_WIDTH_SHARE * halfRiver, CAVE_HALF_MIN, CAVE_HALF_MAX), high - low - 4);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                final int x = x0 + lx, z = z0 + lz;
                final int h = shape.height(x, z);
                if (h == 0) continue;

                final BlockState floor = level.getBlockState(at.set(x, low, z));
                if (floor.isAir() || !floor.getFluidState().isEmpty()) continue;

                int top = low;
                for (int y = low + 1; y <= low + h; y++) {
                    if (!level.getFluidState(at.set(x, y, z)).isEmpty() || wetBeside(level, x, y, z, at)) break;
                    top = y;
                }

                final BlockState roof = level.getBlockState(at.set(x, top + 1, z));
                if (top - low < 2 || roof.isAir() || !roof.getFluidState().isEmpty()) continue;

                if (roof.getBlock() instanceof net.minecraft.world.level.block.FallingBlock) {
                    level.setBlock(at, Blocks.STONE.defaultBlockState(), 2);
                }
                for (int y = low + 1; y <= top; y++) {
                    if (!level.getBlockState(at.set(x, y, z)).isAir()) level.setBlock(at, Blocks.CAVE_AIR.defaultBlockState(), 2);
                }
                dress(level, shape, x, z, low, top, at);
            }
        }
    }

    private record Shape(double fx, double fz, double ux, double uz, double depth, double half, int most) {
        int height(int x, int z) {
            final double ox = x + 0.5 - fx, oz = z + 0.5 - fz;
            final double a = ox * ux + oz * uz, b = Math.abs(oz * ux - ox * uz);
            if (a <= -0.6 || a > depth) return 0;
            final double back = Math.max(0.0, a) / (depth + 0.5);
            final double reach = half * Math.sqrt(Math.max(0.0, 1.0 - back * back));
            if (b > reach) return 0;
            final double across = b / half;
            final int h = Math.min((int) Math.floor(2.0 + 2.2 * (1.0 - across * across) * (1.0 - 0.35 * back)), most);
            return h < 2 ? 0 : h;
        }
    }

    private static void dress(WorldGenLevel level, Shape shape, int x, int z, int low, int top, BlockPos.MutableBlockPos at) {
        if (hash(x, low, z, 1) < 0.4 && level.getBlockState(at.set(x, low + 1, z)).isAir()) {
            level.setBlock(at, Blocks.MOSS_CARPET.defaultBlockState(), 2);
        }
        if (hash(x, top, z, 2) < 0.08 && top - low >= 3 && level.getBlockState(at.set(x, top, z)).isAir()) {
            level.setBlock(at, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                    .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP), 2);
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            final int y = low + 2;
            if (y > top || hash(x, y, z, 3 + side.get2DDataValue()) >= 0.07) continue;
            if (shape.height(x + side.getStepX(), z + side.getStepZ()) != 0) continue;
            final BlockPos wall = new BlockPos(x + side.getStepX(), y, z + side.getStepZ());
            final BlockState w = level.getBlockState(wall);
            if (w.isAir() || !w.getFluidState().isEmpty() || !w.isFaceSturdy(level, wall, side.getOpposite())) continue;
            if (!level.getBlockState(at.set(x, y, z)).isAir()) continue;
            level.setBlock(at, Blocks.GLOW_LICHEN.defaultBlockState().setValue(MultifaceBlock.getFaceProperty(side), true), 2);
            break;
        }
    }

    private static boolean wetBeside(WorldGenLevel level, int x, int y, int z, BlockPos.MutableBlockPos at) {
        for (Direction d : Direction.values()) {
            if (d == Direction.UP) continue;
            if (!level.getFluidState(at.set(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ())).isEmpty()) return true;
        }
        return false;
    }

    private static int surface(WorldGenLevel level, double x, double z, int hint) {
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final int bx = Mth.floor(x), bz = Mth.floor(z);
        for (int y = hint + 6; y >= hint - 10; y--) {
            final FluidState water = level.getFluidState(at.set(bx, y, bz));
            if (water.is(FluidTags.WATER) && water.isSource() && !level.getFluidState(at.set(bx, y + 1, bz)).is(FluidTags.WATER)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static double draw(WatershedPath p, int f, int k) {
        long v = Math.round(p.segmentsX[0]) * 0x9E3779B97F4A7C15L ^ Math.round(p.segmentsZ[0]) * 0xC2B2AE3D27D4EB4FL
                ^ (f * 31L + k) * 0x165667B19E3779F9L ^ HydrologyManager.worldSeed();
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
