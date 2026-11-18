package com.AdamNew.dynamicwaters;

import com.AdamNew.dynamicwaters.platform.ModTags;
import com.AdamNew.dynamicwaters.terrain.WatershedStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

public final class OpenedGroundBiomes {
    private static final int CUT = 6;

    private static final int UNDER = 8;

    private static final boolean OFF = Boolean.getBoolean("dynamicwaters.labNoOpenedGround");

    private OpenedGroundBiomes() {
    }

    public static void repaint(ChunkAccess chunk, int[] was, Climate.Sampler sampler) {
        if (was == null || OFF) return;
        final ChunkPos pos = chunk.getPos();
        final int x0 = pos.getMinBlockX(), z0 = pos.getMinBlockZ();
        final int qx0 = QuartPos.fromBlock(x0), qz0 = QuartPos.fromBlock(z0);
        final int minY = chunk.getMinBuildHeight();
        final int topQuart = QuartPos.fromBlock(chunk.getMaxBuildHeight() - 1);
        @SuppressWarnings("unchecked")
        final Holder<Biome>[] surface = new Holder[16];
        final int[] from = new int[16], to = new int[16];
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE, columns = 0;
        for (int i = 0; i < 16; i++) {
            final int bx = (i & 3) * 4, bz = (i >> 2) * 4;
            int newLow = Integer.MAX_VALUE, oldHigh = Integer.MIN_VALUE;
            boolean cut = false;
            for (int dz = 0; dz < 4; dz++) {
                for (int dx = 0; dx < 4; dx++) {
                    final int old = was[bx + dx + (bz + dz) * 16];
                    final int now = top(chunk, x0 + bx + dx, z0 + bz + dz, old, minY, at);
                    if (old - now >= CUT) cut = true;
                    newLow = Math.min(newLow, now);
                    oldHigh = Math.max(oldHigh, old);
                }
            }
            if (!cut) continue;
            final int qx = qx0 + (i & 3), qz = qz0 + (i >> 2);
            Holder<Biome> over = null;
            for (int qy = QuartPos.fromBlock(oldHigh); qy <= topQuart; qy++) {
                final Holder<Biome> b = chunk.getNoiseBiome(qx, qy, qz);
                if (!b.is(ModTags.IS_UNDERGROUND)) {
                    over = b;
                    break;
                }
            }
            if (over == null) continue;
            surface[i] = over;
            from[i] = QuartPos.fromBlock(Math.max(minY, newLow - UNDER));
            to[i] = QuartPos.fromBlock(oldHigh);
            lowest = Math.min(lowest, from[i]);
            highest = Math.max(highest, to[i]);
            columns++;
        }
        if (columns == 0) return;
        final int s0 = Math.max(chunk.getMinSection(), QuartPos.toSection(lowest));
        final int s1 = Math.min(chunk.getMaxSection() - 1, QuartPos.toSection(highest));
        int changed = 0;
        for (int s = s0; s <= s1; s++) {
            final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(s));
            final int sq = QuartPos.fromSection(s);
            boolean any = false;
            for (int i = 0; i < 16 && !any; i++) {
                if (surface[i] == null) continue;
                for (int qy = Math.max(from[i], sq); qy <= Math.min(to[i], sq + 3); qy++) {
                    if (section.getNoiseBiome(i & 3, qy & 3, i >> 2).is(ModTags.IS_UNDERGROUND)) {
                        any = true;
                        break;
                    }
                }
            }
            if (!any) continue;
            section.fillBiomesFromNoise((qx, qy, qz, unused) -> {
                final int lx = qx & 3, lz = qz & 3, i = lx + lz * 4;
                final Holder<Biome> own = section.getNoiseBiome(lx, qy & 3, lz);
                return surface[i] != null && qy >= from[i] && qy <= to[i] && own.is(ModTags.IS_UNDERGROUND) ? surface[i] : own;
            }, sampler, qx0, sq, qz0);
            changed++;
        }
        if (changed > 0) WatershedStats.openedGroundChunks.increment();
    }

    private static int top(ChunkAccess chunk, int x, int z, int from, int minY, BlockPos.MutableBlockPos at) {
        for (int y = from; y > minY; y--) {
            if (!chunk.getBlockState(at.set(x, y, z)).isAir()) return y;
        }
        return minY;
    }
}
