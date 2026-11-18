package com.AdamNew.dynamicwaters.terrain;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class LowlandCut {
    public record Settings(Config.LowlandValley mode, Config.LowlandCutBy by, double valleyBelow, double tunnelAbove,
            List<String> valleyBiomes, List<String> tunnelBiomes) {
        public static final Settings GORGE = new Settings(Config.LowlandValley.GORGE, Config.LowlandCutBy.HEIGHT,
                100.0, 150.0, List.of(), List.of());

        public boolean valleys() {
            return mode == Config.LowlandValley.VALLEY || mode == Config.LowlandValley.VALLEY_GORGE
                    || mode == Config.LowlandValley.VALLEY_GORGE_TUNNEL;
        }

        public boolean tunnels() {
            return mode == Config.LowlandValley.GORGE_TUNNEL || mode == Config.LowlandValley.VALLEY_GORGE_TUNNEL;
        }

        public boolean mixed() {
            return mode != Config.LowlandValley.GORGE && mode != Config.LowlandValley.VALLEY;
        }
    }

    private static final double CUT_RAMP = 16.0;

    private static final int CUT_GROUND_GRID = 32;
    private static final double CUT_GROUND_RADIUS = 64.0;
    private static final int CUT_GROUND_STEP = 16;

    private static final int BIOME_GRID = 32;
    private static final int GRID_CACHE_LIMIT = 200_000;

    private static final double TUNNEL_SWITCH = 0.5;
    private static final byte GORGE = 0, VALLEY = 1, TUNNEL = 2;

    private static volatile Settings SETTINGS = Settings.GORGE;
    private static volatile BiomeList VALLEY_MATCH = BiomeList.NONE, TUNNEL_MATCH = BiomeList.NONE;
    private static final ConcurrentHashMap<Long, Byte> GRID = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Float> GROUND = new ConcurrentHashMap<>();

    private LowlandCut() {
    }

    public static void clearCaches() {
        GRID.clear();
        GROUND.clear();
    }

    public static boolean configure(Settings settings) {
        if (settings.equals(SETTINGS)) return false;
        SETTINGS = settings;
        VALLEY_MATCH = BiomeList.of(settings.valleyBiomes());
        TUNNEL_MATCH = BiomeList.of(settings.tunnelBiomes());
        GRID.clear();
        return true;
    }

    public static Settings settings() {
        return SETTINGS;
    }

    public static boolean anyValley() {
        return SETTINGS.valleys();
    }

    public static boolean anyTunnel() {
        return SETTINGS.tunnels();
    }

    public static boolean allValley() {
        return SETTINGS.mode() == Config.LowlandValley.VALLEY;
    }

    public static void shares(double x, double z, double riverGround, ChunkGenerator generator, RandomState randomState,
            double[] out) {
        final Settings s = SETTINGS;
        out[0] = 0.0;
        out[1] = 0.0;
        switch (s.mode()) {
            case GORGE -> {
                return;
            }
            case VALLEY -> {
                out[0] = 1.0;
                return;
            }
            default -> {
            }
        }
        if (s.by() == Config.LowlandCutBy.HEIGHT) {
            final double ground = generator != null && randomState != null ? cutGround(x, z, generator, randomState)
                    : riverGround;
            if (s.valleys()) out[0] = 1.0 - smooth((ground - s.valleyBelow()) / CUT_RAMP + 0.5);
            if (s.tunnels()) out[1] = smooth((ground - s.tunnelAbove()) / CUT_RAMP + 0.5);
        } else if (generator != null && randomState != null) {
            final double fx = x / BIOME_GRID, fz = z / BIOME_GRID;
            final int i = Mth.floor(fx), j = Mth.floor(fz);
            final double tx = fx - i, tz = fz - j;
            double v = 0.0, t = 0.0;
            for (int k = 0; k < 4; k++) {
                final int gi = i + (k & 1), gj = j + (k >> 1);
                final double w = ((k & 1) == 0 ? 1.0 - tx : tx) * ((k >> 1) == 0 ? 1.0 - tz : tz);
                final byte kind = kindAt(gi, gj, generator, randomState);
                if (kind == VALLEY) v += w;
                else if (kind == TUNNEL) t += w;
            }
            if (s.valleys()) out[0] = smooth(v);
            if (s.tunnels()) out[1] = smooth(t);
        }
        final double sum = out[0] + out[1];
        if (sum > 1.0) {
            out[0] /= sum;
            out[1] /= sum;
        }

        if (out[1] >= TUNNEL_SWITCH) {
            out[0] = 0.0;
            out[1] = 1.0;
        } else if (out[1] > 0.0) {
            out[0] = out[0] / (1.0 - out[1]);
            out[1] = 0.0;
        }
    }

    private static double smooth(double t) {
        final double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    private static double cutGround(double x, double z, ChunkGenerator generator, RandomState randomState) {
        final double fx = x / CUT_GROUND_GRID, fz = z / CUT_GROUND_GRID;
        final int i = Mth.floor(fx), j = Mth.floor(fz);
        final double tx = fx - i, tz = fz - j;
        final double g00 = gridGround(i, j, generator, randomState), g10 = gridGround(i + 1, j, generator, randomState);
        final double g01 = gridGround(i, j + 1, generator, randomState), g11 = gridGround(i + 1, j + 1, generator, randomState);
        return Mth.lerp(tz, Mth.lerp(tx, g00, g10), Mth.lerp(tx, g01, g11));
    }

    private static float gridGround(int gi, int gj, ChunkGenerator generator, RandomState randomState) {
        final long key = ((long) gi << 32) ^ (gj & 0xFFFFFFFFL);
        final Float known = GROUND.get(key);
        if (known != null) return known;
        final int cx = gi * CUT_GROUND_GRID, cz = gj * CUT_GROUND_GRID;
        final int r = (int) CUT_GROUND_RADIUS;
        final int sea = HydrologyManager.getSeaLevel();
        double sum = 0.0;
        int n = 0;
        for (int dz = -r; dz <= r; dz += CUT_GROUND_STEP) {
            for (int dx = -r; dx <= r; dx += CUT_GROUND_STEP) {
                if (dx * dx + dz * dz > r * r) continue;
                final int ground = CoarseHeightField.sample(Math.floorDiv(cx + dx, CoarseHeightField.CELL_SIZE),
                        Math.floorDiv(cz + dz, CoarseHeightField.CELL_SIZE), generator, randomState);
                sum += Math.max(ground, sea);
                n++;
            }
        }
        final float g = (float) (sum / Math.max(1, n));
        if (GROUND.size() >= GRID_CACHE_LIMIT) GROUND.clear();
        GROUND.put(key, g);
        return g;
    }

    private static byte kindAt(int gi, int gj, ChunkGenerator generator, RandomState randomState) {
        final long key = ((long) gi << 32) ^ (gj & 0xFFFFFFFFL);
        final Byte known = GRID.get(key);
        if (known != null) return known;
        final int x = gi * BIOME_GRID, z = gj * BIOME_GRID;
        final int ground = CoarseHeightField.sample(Math.floorDiv(x, CoarseHeightField.CELL_SIZE),
                Math.floorDiv(z, CoarseHeightField.CELL_SIZE), generator, randomState);

        final boolean was = HydrologyManager.isGeneratingRivers();
        HydrologyManager.setGeneratingRivers(true);
        byte kind;
        try {
            final Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x),
                    QuartPos.fromBlock(Math.max(ground, HydrologyManager.getSeaLevel())), QuartPos.fromBlock(z),
                    randomState.sampler());
            kind = VALLEY_MATCH.test(biome) ? VALLEY : TUNNEL_MATCH.test(biome) ? TUNNEL : GORGE;
        } finally {
            HydrologyManager.setGeneratingRivers(was);
        }
        if (GRID.size() >= GRID_CACHE_LIMIT) GRID.clear();
        GRID.put(key, kind);
        return kind;
    }
}
