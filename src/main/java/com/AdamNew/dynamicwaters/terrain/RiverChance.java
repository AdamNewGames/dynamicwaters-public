package com.AdamNew.dynamicwaters.terrain;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class RiverChance {
    private static final int GRID = 64;
    private static final int PATCH = 192;
    private static final int CACHE_LIMIT = 200_000;

    private record Rule(BiomeList biomes, float chance) {
    }

    private record Rules(List<? extends String> from, List<Rule> rules) {
    }

    private static volatile Rules RULES = new Rules(null, List.of());
    private static final ConcurrentHashMap<Long, Float> GRID_CHANCE = new ConcurrentHashMap<>();

    private RiverChance() {
    }

    private static List<Rule> rules() {
        final List<? extends String> config = Config.RIVER_BIOME_CHANCE.get();
        final Rules known = RULES;
        if (known.from() == config) return known.rules();
        final List<Rule> out = new ArrayList<>();
        for (String entry : config) {
            final String[] parts = entry.trim().split("\\s+");
            if (parts.length < 2) continue;
            final float chance;
            try {
                chance = Mth.clamp(Float.parseFloat(parts[parts.length - 1]), 0.0f, 1.0f);
            } catch (NumberFormatException e) {
                continue;
            }
            out.add(new Rule(BiomeList.of(List.of(parts).subList(0, parts.length - 1)), chance));
        }
        RULES = new Rules(config, List.copyOf(out));
        GRID_CHANCE.clear();
        return out;
    }

    public static boolean any() {
        return !rules().isEmpty();
    }

    public static double at(double x, double z, ChunkGenerator generator, RandomState randomState) {
        final List<Rule> rules = rules();
        if (rules.isEmpty() || generator == null || randomState == null) return 1.0;
        final double fx = x / GRID, fz = z / GRID;
        final int i = Mth.floor(fx), j = Mth.floor(fz);
        final double tx = fx - i, tz = fz - j;
        return Mth.lerp(tz, Mth.lerp(tx, gridChance(i, j, rules, generator, randomState), gridChance(i + 1, j, rules, generator, randomState)),
                Mth.lerp(tx, gridChance(i, j + 1, rules, generator, randomState), gridChance(i + 1, j + 1, rules, generator, randomState)));
    }

    public static boolean lowlandAllows(double x, double z, long seed, ChunkGenerator generator, RandomState randomState) {
        final double chance = at(x, z, generator, randomState);
        if (chance >= 1.0) return true;
        if (chance <= 0.0) return false;
        final int px = Mth.floor(x / PATCH), pz = Mth.floor(z / PATCH);
        long h = seed ^ 0x5851F42D4C957F2DL ^ (px * 0x9E3779B97F4A7C15L) ^ (pz * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 31)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 29)) * 0x94D049BB133111EBL;
        h ^= h >>> 32;
        return ((h >>> 40) * 0x1.0p-24) < chance;
    }

    private static float gridChance(int gi, int gj, List<Rule> rules, ChunkGenerator generator, RandomState randomState) {
        final long key = ((long) gi << 32) ^ (gj & 0xFFFFFFFFL);
        final Float known = GRID_CHANCE.get(key);
        if (known != null) return known;
        final int x = gi * GRID, z = gj * GRID;
        final int ground = CoarseHeightField.sample(Math.floorDiv(x, CoarseHeightField.CELL_SIZE),
                Math.floorDiv(z, CoarseHeightField.CELL_SIZE), generator, randomState);

        final boolean was = HydrologyManager.isGeneratingRivers();
        HydrologyManager.setGeneratingRivers(true);
        float chance = 1.0f;
        try {
            final Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(Math.max(ground, HydrologyManager.getSeaLevel())), QuartPos.fromBlock(z),
                    randomState.sampler());
            for (Rule r : rules) {
                if (r.biomes().test(biome)) {
                    chance = r.chance();
                    break;
                }
            }
        } finally {
            HydrologyManager.setGeneratingRivers(was);
        }
        if (GRID_CHANCE.size() >= CACHE_LIMIT) GRID_CHANCE.clear();
        GRID_CHANCE.put(key, chance);
        return chance;
    }

    public static void clearCaches() {
        GRID_CHANCE.clear();
    }
}
