package com.AdamNew.dynamicwaters.shore;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import com.AdamNew.dynamicwaters.platform.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public final class RiverBeach {
    enum Land { FOREST, GRASSLAND, LUSH, COLD, DRY, DESERT, BADLANDS, BEACH }

    private static final int SPOT = 4;
    private static final int SALT = 0x5B31;

    private static final Mix[] EDGE = new Mix[Land.values().length], SHORE = new Mix[Land.values().length],
            BED = new Mix[Land.values().length], DEEP = new Mix[Land.values().length];

    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

    static {
        DEEP[Land.GRASSLAND.ordinal()] = new Mix(new Block[] {Blocks.GRAVEL, Blocks.CLAY}, new int[] {85, 15});
        DEEP[Land.BEACH.ordinal()] = new Mix(new Block[] {Blocks.SAND}, new int[] {1});
        set(Land.FOREST,
                new Mix(new Block[] {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT}, new int[] {50, 25, 25}),
                new Mix(new Block[] {Blocks.PODZOL, Blocks.COARSE_DIRT, Blocks.ROOTED_DIRT}, new int[] {45, 40, 15}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.STONE, Blocks.CLAY},
                        new int[] {55, 15, 10, 10, 10}));
        set(Land.GRASSLAND,
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.CLAY}, new int[] {70, 30}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.CLAY}, new int[] {70, 30}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.CLAY}, new int[] {80, 20}));
        set(Land.BEACH,
                new Mix(new Block[] {Blocks.SAND}, new int[] {1}),
                new Mix(new Block[] {Blocks.SAND}, new int[] {1}),
                new Mix(new Block[] {Blocks.SAND}, new int[] {1}));
        set(Land.LUSH,
                new Mix(new Block[] {Blocks.MOSSY_COBBLESTONE, Blocks.MUD}, new int[] {55, 45}),
                new Mix(new Block[] {Blocks.MUD, Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK}, new int[] {40, 35, 25}),
                new Mix(new Block[] {Blocks.CLAY, Blocks.GRAVEL, Blocks.MUD}, new int[] {35, 35, 30}));
        set(Land.COLD,
                new Mix(new Block[] {Blocks.COBBLESTONE, Blocks.GRAVEL}, new int[] {50, 50}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.STONE}, new int[] {70, 30}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.STONE}, new int[] {70, 30}));
        set(Land.DRY,
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.GRANITE}, new int[] {60, 40}),
                new Mix(new Block[] {Blocks.COARSE_DIRT, Blocks.GRAVEL}, new int[] {60, 40}),
                new Mix(new Block[] {Blocks.GRAVEL, Blocks.SAND}, new int[] {60, 40}));
        set(Land.DESERT,
                new Mix(new Block[] {Blocks.SANDSTONE, Blocks.GRAVEL}, new int[] {60, 40}),
                new Mix(new Block[] {Blocks.SAND, Blocks.SANDSTONE}, new int[] {70, 30}),
                new Mix(new Block[] {Blocks.SAND, Blocks.GRAVEL}, new int[] {60, 40}));
        set(Land.BADLANDS,
                new Mix(new Block[] {Blocks.RED_SANDSTONE, Blocks.TERRACOTTA}, new int[] {50, 50}),
                new Mix(new Block[] {Blocks.RED_SAND, Blocks.TERRACOTTA}, new int[] {70, 30}),
                new Mix(new Block[] {Blocks.RED_SAND, Blocks.GRAVEL}, new int[] {60, 40}));
    }

    private static void set(Land land, Mix edge, Mix shore, Mix bed) {
        EDGE[land.ordinal()] = edge;
        SHORE[land.ordinal()] = shore;
        BED[land.ordinal()] = bed;
    }

    private static final Mix WOOD_GROUND = new Mix(new Block[] {Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT,
            Blocks.ROOTED_DIRT}, new int[] {40, 25, 25, 10});

    private static final Mix TUFT_WET = new Mix(new Block[] {Blocks.MUD, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.GRASS_BLOCK},
            new int[] {35, 30, 15, 20});
    private static final Mix TUFT_DRY = new Mix(new Block[] {Blocks.GRASS_BLOCK, Blocks.COARSE_DIRT, Blocks.PODZOL},
            new int[] {65, 20, 15});
    private static final Mix PUDDLE_BED = new Mix(new Block[] {Blocks.MUD, Blocks.CLAY, Blocks.GRAVEL}, new int[] {45, 25, 30});

    private static final double[] BUSH = {0.10, 0.0, 0.14, 0.08, 0.04, 0.0, 0.0, 0.0};

    private static final double BUSH_OPEN = 0.05;
    private static final double[] PLANTS = {0.62, 0.66, 0.62, 0.43, 0.43, 0.06, 0.06, 0.0};
    private static final double[] DRIPLEAF = {0.18, 0.15, 0.3, 0.03, 0.05, 0.0, 0.0, 0.0};

    private static final double[] SUGAR_CANE = {0.0, 0.1, 0.08, 0.0, 0.04, 0.1, 0.0, 0.0};
    private static final int SUGAR_CANE_TALLEST = 3;

    private static final double MOUNTAIN_SHORE_SLOPE = 0.25;
    private static final int MOUNTAIN_BED_DEEPEST = 6;

    private static final double RAGGED_IN = 5.0, RAGGED_OUT = 4.0, RAGGED_STRETCH = 130.0, RAGGED_EDGE = 0.15;

    private static final double WET_LAND = 0.15, WET_EDGE = 0.45, WET_RIVER = 1.0;

    private static final double PATCH = 5.0;

    private static final double FLAT_FROM = 0.4, FLAT_MORE = 0.4, FLAT_NOISE = 12.0, FOOT_SLOPE = 0.6, FOOT_RUN = 12.0;

    private static final int TUFT_DEEPEST = 5;

    private static final int PUDDLE_DEEPEST = 8;
    private static final double RAGGED_BED_PAST = 4.0;
    private static final int TUFT_SIDE_DEEPEST = 3;

    private static final int SEAGRASS_DEEPEST = 3;
    private static final float COLDEST = -0.45F, HOTTEST = 0.55F, DRIEST = -0.35F;

    static final BlockState SHORT_GRASS =
            //? if >=1.20.5 {
            Blocks.SHORT_GRASS.defaultBlockState();
            //?} else {
            /*Blocks.GRASS.defaultBlockState();
            *///?}

    private RiverBeach() {
    }

    private record Mix(BlockState[] blocks, int[] weights, int total) {
        Mix(Block[] blocks, int[] weights) {
            this(states(blocks), weights, sum(weights));
        }

        BlockState pick(int x, int z, int salt) {
            return blocks[Patches.spot(weights, total, x, z, SPOT, salt)];
        }

        private static BlockState[] states(Block[] blocks) {
            final BlockState[] s = new BlockState[blocks.length];
            for (int i = 0; i < blocks.length; i++) s[i] = blocks[i].defaultBlockState();
            return s;
        }

        private static int sum(int[] w) {
            int t = 0;
            for (int v : w) t += v;
            return t;
        }
    }

    public static void ragged(HydrologyManager.RiverCarving c, double x, double z, int originalY, int seaLevel,
            RandomState random) {
        if (c.waterEdge == Double.MAX_VALUE) return;
        if (c.riverSplit || c.oxbow || c.isLake || c.pointBar || c.isWatershedWall || c.fordRock != Integer.MIN_VALUE) return;
        final int water = seaLevel - 1;
        if (originalY < water) return;
        if (c.cavernRoofY != -999 && c.cavernRoofY <= originalY + 1) return;
        final double edge = c.waterEdge;
        if (edge < -RAGGED_IN - FOOT_RUN || edge > RAGGED_OUT) return;
        if (edge < 0.0 && !c.isMainRiverWater) return;

        if (edge >= 0.0 && c.targetY > water + Math.floor(1.0 + 0.5 * edge)) return;

        final double s = HydrologyManager.riverNoise(x / RAGGED_STRETCH + 913.0, z / RAGGED_STRETCH - 377.0);
        final double t = Mth.clamp((s - Config.RAGGED_SHORES.get().from) / RAGGED_EDGE, 0.0, 1.0);
        final double strength = t * t * (3.0 - 2.0 * t);
        final double in = RAGGED_IN * strength, out = RAGGED_OUT * strength;
        if (strength <= 0.0 || edge < -in - FOOT_RUN || edge > out) return;
        if (random != null) {
            final Climate.TargetPoint p = random.sampler().sample(QuartPos.fromBlock(Mth.floor(x)), QuartPos.fromBlock(water),
                    QuartPos.fromBlock(Mth.floor(z)));
            final float temperature = Climate.unquantizeCoord(p.temperature()), humidity = Climate.unquantizeCoord(p.humidity());
            if (temperature < COLDEST || temperature > HOTTEST || humidity < DRIEST) return;
        }

        final double wet = edge >= 0.0 ? WET_LAND + (WET_EDGE - WET_LAND) * (1.0 - edge / out)
                : WET_EDGE + (WET_RIVER - WET_EDGE) * (-edge / in);
        final double patch = 0.5 + 0.5 * HydrologyManager.riverNoise(x / PATCH + 41.0, z / PATCH - 87.0)
                + 0.08 * (Patches.hash01(Mth.floor(x), SALT + 20, Mth.floor(z)) - 0.5);
        int foot = water - 1;
        if (edge < 0.0) {
            final double flat = in * (FLAT_FROM + FLAT_MORE
                    * (0.5 + 0.5 * HydrologyManager.riverNoise(x / FLAT_NOISE - 205.0, z / FLAT_NOISE + 63.0)));
            foot -= (int) Math.floor(Math.max(0.0, -edge - flat) * FOOT_SLOPE / strength);
        }
        if (edge < 0.0 && (patch < wet || foot < water - 1 || c.targetY < water - TUFT_DEEPEST)) {
            if (foot > c.targetY) {
                c.targetY = Math.min(foot, originalY);
                c.beach = HydrologyManager.BEACH_PUDDLE;
            } else if (-edge < in + RAGGED_BED_PAST) {
                c.beach = HydrologyManager.BEACH_PUDDLE;
            }
        } else if (patch < wet) {
            c.targetY = Math.min(c.targetY, water - 1);
            c.modifies = true;
            c.beach = HydrologyManager.BEACH_PUDDLE;
        } else {
            c.targetY = water;
            c.modifies = true;
            c.isMainRiverWater = false;
            c.beach = HydrologyManager.BEACH_TUFT;
        }
    }

    public static void dress(ChunkAccess chunk, HydrologyManager.RiverCarving[] carvings, BitSet[] protectedMap, int minH,
            int startX, int startZ, BiomeSource biomeSource, Climate.Sampler sampler) {
        if (!Config.RIVER_BEACHES.get()) return;
        final boolean plants = Config.RIVERBANK_PLANTS.get();
        final Map<Long, Holder<Biome>> landBiomes = new HashMap<>();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 256; i++) {
            final HydrologyManager.RiverCarving c = carvings[i];
            if (c == null || c.isLake || c.oxbow) continue;
            final int lx = i & 15, lz = i >> 4, x = startX + lx, z = startZ + lz, y = c.targetY, w = c.waterLevel;
            final BitSet bits = protectedMap[i];
            if (c.isMountainRiver) {
                if (c.beach == 0) mountain(chunk, c, bits, minH, i, x, z, at, biomeSource, sampler, landBiomes, plants);
                continue;
            }
            if (c.gravelBar && c.riverSplit) {
                gravelBar(chunk, bits, minH, lx, lz, x, y, z, w, at);
                continue;
            }
            if (c.beach == 0) continue;
            if (c.beach == HydrologyManager.BEACH_TOP) {
                rim(chunk, bits, minH, i, x, z, w, at, biomeSource, sampler, landBiomes, plants, c.riverSplit);
                continue;
            }
            if (c.beach == HydrologyManager.BEACH_TUFT || c.beach == HydrologyManager.BEACH_PUDDLE) {
                ragged(chunk, carvings, bits, minH, i, x, z, w, at, biomeSource, sampler, landBiomes, plants);
                continue;
            }
            if (c.beach != HydrologyManager.BEACH_BED && c.beach != HydrologyManager.BEACH_DEEP) {
                if (y != w || chunk.getBlockState(at.set(x, y + 1, z)).getFluidState().isSource()) continue;
            } else {
                if (y >= w || !chunk.getBlockState(at.set(x, y + 1, z)).getFluidState().isSource()) continue;
            }
            lay(chunk, bits, minH, lx, lz, x, y, z, w, c.beach, at, biomeSource, sampler, landBiomes, plants);
        }
    }

    private static void gravelBar(ChunkAccess chunk, BitSet bits, int minH, int lx, int lz, int x, int y, int z, int w,
            BlockPos.MutableBlockPos at) {
        if (y < w || y > w + 1 || !chunk.getBlockState(at.set(x, y + 1, z)).isAir()) return;
        if (guarded(bits, y, minH) || !ground(chunk.getBlockState(at.set(x, y, z)))) return;
        chunk.setBlockState(at, GRAVEL, false);
        for (int d = 1; d <= 2; d++) {
            if (guarded(bits, y - d, minH) || !ground(chunk.getBlockState(at.set(x, y - d, z)))
                    || !waterBeside(chunk, at, lx, lz, x, y - d, z)) return;
            chunk.setBlockState(at.set(x, y - d, z), GRAVEL, false);
        }
    }

    private static void lay(ChunkAccess chunk, BitSet bits, int minH, int lx, int lz, int x, int y, int z, int w,
            byte zone, BlockPos.MutableBlockPos at, BiomeSource biomeSource, Climate.Sampler sampler,
            Map<Long, Holder<Biome>> landBiomes, boolean plants) {
        if (guarded(bits, y, minH) || !ground(chunk.getBlockState(at.set(x, y, z)))) return;
        final Land land = kind(biomeAt(chunk, lx, lz, x, z, w, biomeSource, sampler, landBiomes), at.set(x, w, z));
        final boolean bed = zone == HydrologyManager.BEACH_BED || zone == HydrologyManager.BEACH_DEEP;
        final Mix mix = (zone == HydrologyManager.BEACH_DEEP ? DEEP : bed ? BED
                : zone == HydrologyManager.BEACH_EDGE ? EDGE : SHORE)[land.ordinal()];
        if (mix == null) return;
        final BlockState laid = mix.pick(x, z, SALT + zone);
        chunk.setBlockState(at.set(x, y, z), laid, false);
        if (!bed) {
            if (!guarded(bits, y - 1, minH) && ground(chunk.getBlockState(at.set(x, y - 1, z)))
                    && waterBeside(chunk, at, lx, lz, x, y - 1, z)) {
                chunk.setBlockState(at.set(x, y - 1, z), BED[land.ordinal()].pick(x, z, SALT + HydrologyManager.BEACH_BED), false);
            }
            if (plants && Patches.hash01(x, SALT + 15, z) < SUGAR_CANE[land.ordinal()]) {
                sugarCane(chunk, bits, minH, at, lx, lz, x, y, z, laid);
            }
        } else if (plants && y == w - 1 && Patches.hash01(x, SALT + 12, z) < DRIPLEAF[land.ordinal()]
                && !guarded(bits, w, minH) && !guarded(bits, w + 1, minH)) {
            dripleaf(chunk, bits, minH, at, x, w, z, true);
        }
    }

    private static void sugarCane(ChunkAccess chunk, BitSet bits, int minH, BlockPos.MutableBlockPos at, int lx, int lz,
            int x, int y, int z, BlockState ground) {
        if (!ground.is(BlockTags.DIRT) && !ground.is(Blocks.SAND) && !ground.is(Blocks.RED_SAND)) return;
        if (!waterBeside(chunk, at, lx, lz, x, y, z)) return;
        final int tall = 1 + (int) (Patches.hash01(x, SALT + 16, z) * SUGAR_CANE_TALLEST);
        for (int k = 1; k <= tall; k++) {
            if (guarded(bits, y + k, minH) || !chunk.getBlockState(at.set(x, y + k, z)).isAir()) return;
            chunk.setBlockState(at, Blocks.SUGAR_CANE.defaultBlockState(), false);
        }
    }

    private static void mountain(ChunkAccess chunk, HydrologyManager.RiverCarving c, BitSet bits, int minH, int i,
            int x, int z, BlockPos.MutableBlockPos at, BiomeSource biomeSource, Climate.Sampler sampler,
            Map<Long, Holder<Biome>> landBiomes, boolean plants) {
        final double e = c.mountainEdge;
        if (e == Double.MAX_VALUE || c.mountainEdgeSlope > MOUNTAIN_SHORE_SLOPE) return;
        final int lx = i & 15, lz = i >> 4;
        final int w = e < 0.0 ? c.waterLevel : c.mountainEdgeY;
        if (e < 0.0 && chunk.getBlockState(at.set(x, w, z)).getFluidState().isSource()) {
            int y = w - 1;
            while (y > w - MOUNTAIN_BED_DEEPEST && chunk.getBlockState(at.set(x, y, z)).getFluidState().isSource()) y--;
            if (chunk.getBlockState(at.set(x, y, z)).getFluidState().isSource()) return;
            lay(chunk, bits, minH, lx, lz, x, y, z, w, -e < HydrologyManager.beachBedIn() ? HydrologyManager.BEACH_BED
                    : HydrologyManager.BEACH_DEEP, at, biomeSource, sampler, landBiomes, plants);
            return;
        }

        final double shore = HydrologyManager.beachWidth(x, z);
        if (e < shore) {
            for (int y = w + 1; y >= w; y--) {
                if (chunk.getBlockState(at.set(x, y, z)).isAir()) continue;
                if (chunk.getBlockState(at.set(x, y + 1, z)).isAir()) {
                    lay(chunk, bits, minH, lx, lz, x, y, z, w, e < HydrologyManager.beachEdgeOut()
                            ? HydrologyManager.BEACH_EDGE : HydrologyManager.BEACH_SHORE, at, biomeSource, sampler, landBiomes, plants);
                    return;
                }
                break;
            }
        }
        if (e < shore + HydrologyManager.beachRim()) {
            rim(chunk, bits, minH, i, x, z, w, at, biomeSource, sampler, landBiomes, plants, false);
        }
    }

    private static void ragged(ChunkAccess chunk, HydrologyManager.RiverCarving[] carvings, BitSet bits, int minH, int i,
            int x, int z, int w, BlockPos.MutableBlockPos at, BiomeSource biomeSource, Climate.Sampler sampler,
            Map<Long, Holder<Biome>> landBiomes, boolean plants) {
        final HydrologyManager.RiverCarving c = carvings[i];
        if (c.beach == HydrologyManager.BEACH_TUFT) {
            if (guarded(bits, w, minH) || !ground(chunk.getBlockState(at.set(x, w, z)))
                    || chunk.getBlockState(at.set(x, w + 1, z)).getFluidState().isSource()) return;
            final boolean wet = besideWater(chunk, at, i, x, z, w, c);
            chunk.setBlockState(at.set(x, w, z), (wet ? TUFT_WET : TUFT_DRY).pick(x, z, SALT + 21), false);

            for (int y = w - 1; wet && y >= w - TUFT_SIDE_DEEPEST; y--) {
                if (guarded(bits, y, minH) || !ground(chunk.getBlockState(at.set(x, y, z)))
                        || !waterBeside(chunk, at, i & 15, i >> 4, x, y, z)) break;
                if (!chunk.getBlockState(at.set(x, y, z)).is(BlockTags.DIRT)) {
                    chunk.setBlockState(at, PUDDLE_BED.pick(x, z, SALT + 23), false);
                }
            }
            if (!plants || guarded(bits, w + 1, minH) || !chunk.getBlockState(at.set(x, w + 1, z)).isAir()) return;

            final double p = Patches.hash01(x, SALT + 22, z) / Math.max(1.0E-3, Config.RIVERBANK_PLANT_DENSITY.get());
            if (p < 0.38) plant(chunk, at, x, w, z, Blocks.TALL_GRASS);
            else if (p < 0.57) chunk.setBlockState(at.set(x, w + 1, z), SHORT_GRASS, false);
            else if (p < 0.6) plant(chunk, at, x, w, z, Blocks.FERN);
            else if (p < 0.66 && wet) dripleaf(chunk, bits, minH, at, x, w + 1, z, false);
            else if (p < 0.67) plant(chunk, at, x, w, z, Blocks.DANDELION);
        } else {
            final int y = c.targetY;
            if (y >= w || y < w - PUDDLE_DEEPEST || !chunk.getBlockState(at.set(x, y + 1, z)).getFluidState().isSource()) return;
            if (guarded(bits, y, minH) || !ground(chunk.getBlockState(at.set(x, y, z)))) return;
            chunk.setBlockState(at, PUDDLE_BED.pick(x, z, SALT + 23), false);
            if (!plants || guarded(bits, y + 1, minH) || !chunk.getBlockState(at.set(x, y + 1, z)).is(Blocks.WATER)) return;
            final double p = Patches.hash01(x, SALT + 24, z);
            if (p < 0.4 && y >= w - SEAGRASS_DEEPEST) chunk.setBlockState(at, Blocks.SEAGRASS.defaultBlockState(), false);
            else if (p < 0.55 && y == w - 1) dripleaf(chunk, bits, minH, at, x, w, z, true);
        }
    }

    private static boolean waterBeside(ChunkAccess chunk, BlockPos.MutableBlockPos at, int lx, int lz, int x, int y, int z) {
        if (lx > 0 && chunk.getBlockState(at.set(x - 1, y, z)).getFluidState().isSource()) return true;
        if (lx < 15 && chunk.getBlockState(at.set(x + 1, y, z)).getFluidState().isSource()) return true;
        if (lz > 0 && chunk.getBlockState(at.set(x, y, z - 1)).getFluidState().isSource()) return true;
        return lz < 15 && chunk.getBlockState(at.set(x, y, z + 1)).getFluidState().isSource();
    }

    private static boolean besideWater(ChunkAccess chunk, BlockPos.MutableBlockPos at, int i, int x, int z, int w,
            HydrologyManager.RiverCarving c) {
        final int lx = i & 15, lz = i >> 4;
        if ((lx == 0 || lx == 15 || lz == 0 || lz == 15) && c.waterEdge < 1.0) return true;
        if (lx > 0 && chunk.getBlockState(at.set(x - 1, w, z)).getFluidState().isSource()) return true;
        if (lx < 15 && chunk.getBlockState(at.set(x + 1, w, z)).getFluidState().isSource()) return true;
        if (lz > 0 && chunk.getBlockState(at.set(x, w, z - 1)).getFluidState().isSource()) return true;
        return lz < 15 && chunk.getBlockState(at.set(x, w, z + 1)).getFluidState().isSource();
    }

    private static void dripleaf(ChunkAccess chunk, BitSet bits, int minH, BlockPos.MutableBlockPos at, int x, int y, int z,
            boolean inWater) {
        if (!chunk.getBlockState(at.set(x, y + 1, z)).isAir()) return;
        if (chunk.getNoiseBiome(x >> 2, (y + 1) >> 2, z >> 2).value().coldEnoughToSnow(at)) return;
        final BlockState ground = chunk.getBlockState(at.set(x, y - 1, z));
        if (!ground.is(BlockTags.SMALL_DRIPLEAF_PLACEABLE) && !(inWater && ground.is(BlockTags.DIRT))) {
            if (guarded(bits, y - 1, minH) || !ground.isFaceSturdy(chunk, at, Direction.UP)) return;
            chunk.setBlockState(at, (inWater ? Blocks.CLAY : Blocks.MOSS_BLOCK).defaultBlockState(), false);
        }
        final Direction facing = Direction.from2DDataValue((int) (Patches.hash01(x, SALT + 13, z) * 4.0));
        final BlockState s = Blocks.SMALL_DRIPLEAF.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        chunk.setBlockState(at.set(x, y, z), s.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)
                .setValue(BlockStateProperties.WATERLOGGED, inWater), false);
        chunk.setBlockState(at.set(x, y + 1, z), s.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)
                .setValue(BlockStateProperties.WATERLOGGED, false), false);
    }

    private static void rim(ChunkAccess chunk, BitSet bits, int minH, int i, int x, int z, int w,
            BlockPos.MutableBlockPos at, BiomeSource biomeSource, Climate.Sampler sampler,
            Map<Long, Holder<Biome>> landBiomes, boolean plants, boolean island) {
        final int lx = i & 15, lz = i >> 4;
        int top = topOf(chunk, x, z, w, at);

        final boolean bare = top == Integer.MIN_VALUE && island;
        if (bare) top = groundTop(chunk, x, z, w, at);
        if (top == Integer.MIN_VALUE || guarded(bits, top, minH) || guarded(bits, top + 1, minH)) return;
        final Holder<Biome> biome = biomeAt(chunk, lx, lz, x, z, top, biomeSource, sampler, landBiomes);
        final Land land = kind(biome, at.set(x, top, z));
        if (bare) {
            if (land == Land.DESERT || land == Land.BADLANDS) return;
            chunk.setBlockState(at.set(x, top, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
        }
        if ((land == Land.FOREST || land == Land.COLD && wooded(biome))
                && chunk.getBlockState(at.set(x, top, z)).is(Blocks.GRASS_BLOCK)) {
            chunk.setBlockState(at, WOOD_GROUND.pick(x, z, SALT + 14), false);
        }
        if (!plants) return;
        final double u = Patches.hash01(x, SALT + 7, z);
        final BlockState leaves = u < Math.max(BUSH[land.ordinal()], BUSH_OPEN) ? bushLeaves(biome, land, u) : null;
        if (leaves != null) {
            chunk.setBlockState(at.set(x, top + 1, z), leaves, false);
            final double v = Patches.hash01(x, SALT + 8, z);
            if (v < 0.35 && chunk.getBlockState(at.set(x, top + 2, z)).isAir()) chunk.setBlockState(at, leaves, false);

            final int nx = v < 0.6 ? lx + 1 : lx, nz = v < 0.6 ? lz : lz + 1;
            if (nx < 16 && nz < 16 && Patches.hash01(x, SALT + 9, z) < 0.6) {
                final int wx = chunk.getPos().getMinBlockX() + nx, wz = chunk.getPos().getMinBlockZ() + nz;
                if (topOf(chunk, wx, wz, w, at) == top && chunk.getBlockState(at.set(wx, top + 1, wz)).isAir()) {
                    chunk.setBlockState(at, leaves, false);
                }
            }
            return;
        }
        if (Patches.hash01(x, SALT + 10, z) >= PLANTS[land.ordinal()] * Config.RIVERBANK_PLANT_DENSITY.get()) return;

        final BankRules.Plants mix = BankRules.plants(Config.RIVERBANK_PLANT_MIXES.get(), biome, land);
        if (mix == null) return;
        BlockState s = mix.pick(Patches.hash01(x, SALT + 11, z));

        if (Config.RIVERBANK_OWN_PLANTS.get()) {
            final BiomePlants.Own own = BiomePlants.of(biome);
            if (own != null && Patches.hash01(x, SALT + 15, z) < own.share()) {
                final BlockState mine = own.pick(Patches.hash01(x, SALT + 16, z));
                if (BiomePlants.survives(mine, chunk.getBlockState(at.set(x, top, z)))) s = mine;
            }
        }
        plant(chunk, at, x, top, z, s);
    }

    private static void plant(ChunkAccess chunk, BlockPos.MutableBlockPos at, int x, int top, int z, Block block) {
        plant(chunk, at, x, top, z, block == null ? SHORT_GRASS : block.defaultBlockState());
    }

    private static void plant(ChunkAccess chunk, BlockPos.MutableBlockPos at, int x, int top, int z, BlockState s) {
        final BlockState[] parts = TallPlant.parts(s);
        if (parts != null) {
            for (int k = 2; k <= parts.length; k++) {
                if (!chunk.getBlockState(at.set(x, top + k, z)).isAir()) return;
            }
            for (int k = 0; k < parts.length; k++) chunk.setBlockState(at.set(x, top + 1 + k, z), parts[k], false);
        } else {
            chunk.setBlockState(at.set(x, top + 1, z), s, false);
        }
    }

    private static int topOf(ChunkAccess chunk, int x, int z, int w, BlockPos.MutableBlockPos at) {
        for (int y = w + 6; y >= w; y--) {
            final BlockState s = chunk.getBlockState(at.set(x, y, z));
            if (s.isAir()) continue;
            if (!s.is(BlockTags.DIRT) || !chunk.getBlockState(at.set(x, y + 1, z)).isAir()) return Integer.MIN_VALUE;
            return y;
        }
        return Integer.MIN_VALUE;
    }

    private static int groundTop(ChunkAccess chunk, int x, int z, int w, BlockPos.MutableBlockPos at) {
        for (int y = w + 4; y >= w; y--) {
            final BlockState s = chunk.getBlockState(at.set(x, y, z));
            if (s.isAir()) continue;
            return ground(s) && chunk.getBlockState(at.set(x, y + 1, z)).isAir() ? y : Integer.MIN_VALUE;
        }
        return Integer.MIN_VALUE;
    }

    private static Holder<Biome> biomeAt(ChunkAccess chunk, int lx, int lz, int x, int z, int y, BiomeSource biomeSource,
            Climate.Sampler sampler, Map<Long, Holder<Biome>> landBiomes) {
        return ShoreSkin.landBiome(chunk.getNoiseBiome(lx >> 2, y >> 2, lz >> 2), x >> 2, y >> 2, z >> 2, biomeSource,
                sampler, landBiomes);
    }

    private static Land kind(Holder<Biome> land, BlockPos pos) {
        if (land.is(BiomeTags.IS_BEACH)) return Land.BEACH;
        if (land.is(ModTags.IS_BADLANDS)) return Land.BADLANDS;
        if (land.is(ModTags.IS_DESERT) || named(land, DESERT_NAMES)) return Land.DESERT;
        final Biome biome = land.value();
        if (biome.coldEnoughToSnow(pos) || land.is(ModTags.IS_SNOWY) || land.is(ModTags.IS_ICY)) return Land.COLD;
        if (land.is(ModTags.IS_JUNGLE) || land.is(ModTags.IS_SWAMP) || land.is(ModTags.IS_LUSH)
                || named(land, WET_NAMES)) return Land.LUSH;
        if (!biome.hasPrecipitation() || land.is(ModTags.IS_SAVANNA) || land.is(ModTags.IS_DRY_OVERWORLD)) return Land.DRY;
        return wooded(land) ? Land.FOREST : Land.GRASSLAND;
    }

    private static boolean wooded(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_FOREST) || biome.is(BiomeTags.IS_TAIGA) || biome.is(ModTags.IS_JUNGLE)) return true;
        final String name = name(biome);
        return name.contains("forest") || name.contains("wood") || name.contains("taiga") || name.contains("grove")
                || name.contains("jungle") || name.contains("rainforest") || name.contains("pine")
                || name.contains("conifer") || name.contains("boreal");
    }

    private static String name(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    private static final java.util.Set<String> WET_NAMES = java.util.Set.of("marsh", "marshes", "swamp", "swamps",
            "wetland", "wetlands", "bog", "bogs", "fen", "fens", "bayou", "mire", "mires", "quagmire");
    private static final java.util.Set<String> DESERT_NAMES = java.util.Set.of("desert", "deserts", "dune", "dunes");

    private static boolean named(Holder<Biome> biome, java.util.Set<String> words) {
        for (String w : name(biome).split("[_/]")) {
            if (words.contains(w)) return true;
        }
        return false;
    }

    private static BlockState bushLeaves(Holder<Biome> biome, Land land, double u) {
        final BankRules.Leaves rule = BankRules.leaves(Config.RIVERBANK_BUSH_LEAVES.get(), biome, land);
        if (rule == null) return null;
        final double bush = BUSH[land.ordinal()];
        if (!rule.auto()) return rule.state() != null && u < (bush > 0.0 ? bush : BUSH_OPEN) ? rule.state() : null;
        return u < bush ? autoLeaves(biome, land) : null;
    }

    private static BlockState autoLeaves(Holder<Biome> biome, Land land) {
        if (land == Land.DESERT || land == Land.BADLANDS || land == Land.GRASSLAND || land == Land.BEACH) return null;
        if ((land == Land.COLD || land == Land.DRY) && !wooded(biome)) return null;
        final BlockState own = BiomeTrees.leavesOf(biome);
        return own != null ? BankRules.kept(own) : namedLeaves(biome, land);
    }

    public static String landName(Holder<Biome> biome) {
        return kind(biome, new BlockPos(0, 64, 0)).name().toLowerCase(java.util.Locale.ROOT);
    }

    public static BlockState autoLeaves(Holder<Biome> biome) {
        return autoLeaves(biome, kind(biome, new BlockPos(0, 64, 0)));
    }

    private static BlockState namedLeaves(Holder<Biome> biome, Land land) {
        final String name = name(biome);
        final Block block;
        if (name.contains("cherry") || name.contains("sakura")) block = Blocks.CHERRY_LEAVES;
        else if (name.contains("mangrove")) block = Blocks.MANGROVE_LEAVES;
        else if (name.contains("dark") || name.contains("roofed")) block = Blocks.DARK_OAK_LEAVES;
        else if (name.contains("birch") || name.contains("aspen")) block = Blocks.BIRCH_LEAVES;
        else if (land == Land.LUSH && (biome.is(ModTags.IS_JUNGLE) || name.contains("jungle") || name.contains("tropic")
                || name.contains("rainforest"))) block = Blocks.JUNGLE_LEAVES;
        else if (biome.is(ModTags.IS_SAVANNA) || name.contains("savanna") || name.contains("acacia")) block = Blocks.ACACIA_LEAVES;
        else if (land == Land.COLD || biome.is(BiomeTags.IS_TAIGA) || name.contains("taiga") || name.contains("pine")
                || name.contains("spruce") || name.contains("conifer") || name.contains("boreal") || name.contains("fir"))
            block = Blocks.SPRUCE_LEAVES;
        else block = Blocks.OAK_LEAVES;
        return block.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static boolean ground(BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES);
    }

    private static boolean guarded(BitSet bits, int y, int minH) {
        return bits != null && y >= minH && bits.get(y - minH);
    }
}
