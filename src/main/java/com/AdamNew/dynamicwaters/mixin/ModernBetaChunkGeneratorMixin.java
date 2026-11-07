package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mod.bluestaggo.modernerbeta.level.chunk.ModernBetaChunkGenerator")
public class ModernBetaChunkGeneratorMixin {
    private static final ThreadLocal<int[][]> ORIGINAL_HEIGHT_CACHE = ThreadLocal.withInitial(() -> new int[16][16]);

    @Inject(method = {"buildSurface", "m_214194_"}, at = @At("HEAD"), remap = false)
    private void dynamicwaters$carveAndFillRiver(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk, CallbackInfo ci) {
        if (level.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }

        HydrologyManager.setSeed(level.getLevel().getSeed());
        HydrologyManager.setSeaLevel(((ChunkGenerator)(Object)this).getSeaLevel());

        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        int seaLevel = ((ChunkGenerator)(Object)this).getSeaLevel();

        final int[] preCarve = new int[256];
        HydrologyManager.rememberPreCarveHeights(chunkPos.toLong(), preCarve);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                preCarve[x + z * 16] = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            }
        }
        com.AdamNew.dynamicwaters.LakeHollows.setChunkGround(startX, startZ, preCarve);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                double worldX = startX + x;
                double worldZ = startZ + z;

                int originalY = preCarve[x + z * 16];

                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = chunk.getNoiseBiome(x >> 2, originalY >> 2, z >> 2);
                if (HydrologyManager.isOceanBiome(biomeHolder) && originalY < seaLevel - 4) {
                    continue;
                }

                HydrologyManager.RiverCarving carving = HydrologyManager.calculateRiverCarving(worldX, worldZ, originalY, seaLevel, ((net.minecraft.world.level.chunk.ChunkGenerator)(Object)this), chunk, random);

                if (carving.gorgeSeal) {
                    HydrologyManager.gorgeSeal(chunk, startX + x, startZ + z, seaLevel, originalY, null, chunk.getMinBuildHeight());
                }

                if (carving.modifies) {
                    int targetY = carving.targetY;
                    int cavernRoofY = carving.cavernRoofY;

                    int carveTopY = Math.min(originalY, cavernRoofY);

                    if (originalY >= 90) {
                        for (int y = seaLevel; y <= originalY; y++) {
                            BlockState currentState = chunk.getBlockState(new BlockPos(startX + x, y, startZ + z));
                            if (currentState.isAir()) {
                                chunk.setBlockState(new BlockPos(startX + x, y, startZ + z), Blocks.STONE.defaultBlockState(), false);
                            }
                        }
                    }

                    chunk.setBlockState(new BlockPos(startX + x, targetY, startZ + z), Blocks.STONE.defaultBlockState(), false);
                    chunk.setBlockState(new BlockPos(startX + x, targetY - 1, startZ + z), Blocks.STONE.defaultBlockState(), false);

                    if (carving.isWatershedWall && targetY <= originalY) {
                        chunk.setBlockState(new BlockPos(startX + x, targetY, startZ + z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
                        chunk.setBlockState(new BlockPos(startX + x, targetY - 1, startZ + z), Blocks.DIRT.defaultBlockState(), false);
                        chunk.setBlockState(new BlockPos(startX + x, targetY - 2, startZ + z), Blocks.DIRT.defaultBlockState(), false);
                    }

                    if (targetY < carveTopY) {
                        for (int y = carveTopY; y > targetY; y--) {
                            chunk.setBlockState(new BlockPos(startX + x, y, startZ + z), Blocks.AIR.defaultBlockState(), false);
                        }
                    }

                    if (targetY > originalY + 4 && !carving.isWatershed) {
                        continue;
                    } else if (targetY > originalY && !carving.isWatershed) {
                        targetY = originalY;
                    }

                    if (targetY <= carving.waterLevel && !carving.isWatershedWall) {
                        BlockState riverWater = Blocks.WATER.defaultBlockState();
                        for (int y = carving.waterLevel; y >= targetY + 1; y--) {
                            chunk.setBlockState(new BlockPos(startX + x, y, startZ + z), riverWater, false);
                        }

                        if (targetY < originalY) {
                            chunk.setBlockState(new BlockPos(startX + x, targetY, startZ + z), Blocks.STONE.defaultBlockState(), false);
                            int hash = (int)(startX + x) * 31337 + (int)(startZ + z) * 42069;
                            hash ^= (hash >> 16);
                            hash ^= (hash << 5);
                            int hashAbs = Math.abs(hash % 100);
                            if (targetY < seaLevel - 2) {
                                if (hashAbs < 3 && targetY < seaLevel - 3) {
                                    chunk.setBlockState(new BlockPos(startX + x, targetY + 1, startZ + z), Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), false);
                                    chunk.setBlockState(new BlockPos(startX + x, targetY + 2, startZ + z), Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), false);
                                } else if (hashAbs < 10) {
                                    chunk.setBlockState(new BlockPos(startX + x, targetY + 1, startZ + z), Blocks.SEAGRASS.defaultBlockState(), false);
                                }
                            }
                        }
                    }

                    if ((carving.gorgeWall || carving.dressCut) && targetY >= carving.waterLevel && targetY < originalY) {
                        HydrologyManager.gorgeDress(chunk, startX + x, startZ + z, targetY, carving.waterLevel,
                                carving.gorgeSteep, null, chunk.getMinBuildHeight(), null, originalY);
                    }
                }
            }
        }
        com.AdamNew.dynamicwaters.LakeHollows.clearChunkGround();
    }

    @Inject(method = {"applyCarvers", "m_213679_"}, at = @At("HEAD"), remap = false)
    private void dynamicwaters$saveHeightsBeforeCarvers(WorldGenRegion level, long seed, RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving step, CallbackInfo ci) {
        if (level.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }
        if (step == net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR || step == net.minecraft.world.level.levelgen.GenerationStep.Carving.LIQUID) {
            int[][] heights = ORIGINAL_HEIGHT_CACHE.get();

            final int[] pre = HydrologyManager.takePreCarveHeights(chunk.getPos().toLong());
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    heights[x][z] = pre != null ? pre[x + z * 16] : chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
                }
            }
        }
    }

    @Inject(method = {"applyCarvers", "m_213679_"}, at = @At("RETURN"), remap = false)
    private void dynamicwaters$repairRiverAfterCarvers(WorldGenRegion level, long seed, RandomState randomState, net.minecraft.world.level.biome.BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving step, CallbackInfo ci) {
        if (level.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return;
        }

        HydrologyManager.setSeed(level.getLevel().getSeed());

        if (step == net.minecraft.world.level.levelgen.GenerationStep.Carving.AIR) {
            ChunkPos chunkPos = chunk.getPos();
            int startX = chunkPos.getMinBlockX();
            int startZ = chunkPos.getMinBlockZ();
            int seaLevel = ((ChunkGenerator)(Object)this).getSeaLevel();

            final int[][] preHeights = ORIGINAL_HEIGHT_CACHE.get();
            final int[] repairGround = new int[256];
            for (int i = 0; i < 256; i++) repairGround[i] = preHeights[i & 15][i >> 4];
            com.AdamNew.dynamicwaters.LakeHollows.setChunkGround(startX, startZ, repairGround);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    double worldX = startX + x;
                    double worldZ = startZ + z;

                    boolean inRiver = false;
                    net.minecraft.world.level.chunk.ChunkGenerator generator = ((net.minecraft.world.level.chunk.ChunkGenerator)(Object)this);
                    HydrologyManager.WatershedContext mountainCtx = HydrologyManager.evaluateWatershedContext(worldX, worldZ, generator, chunk, randomState, false);
                    if (mountainCtx.bestPath != null && mountainCtx.distance < mountainCtx.width + 12.0) {
                        inRiver = true;
                    } else if (HydrologyManager.isInOrBesideLake(worldX, worldZ)) {
                        inRiver = true;
                    } else {
                        com.AdamNew.dynamicwaters.river.RiverContext ctx = com.AdamNew.dynamicwaters.MainRivers.findNearest(worldX, worldZ, HydrologyManager.getGlobalGenerator(), HydrologyManager.getDummyAccessor(), HydrologyManager.getGlobalRandomState());
                        if (ctx != null && ctx.distSq() < ctx.widthSq()) {
                            inRiver = true;
                        }
                    }
                    if (!inRiver) continue;

                    int originalY = ORIGINAL_HEIGHT_CACHE.get()[x][z];
                    net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = chunk.getNoiseBiome(x >> 2, originalY >> 2, z >> 2);
                    if (HydrologyManager.isOceanBiome(biomeHolder) && originalY < seaLevel - 4) {
                        continue;
                    }
                    HydrologyManager.RiverCarving carving = HydrologyManager.calculateRiverCarving(worldX, worldZ, originalY, seaLevel, ((net.minecraft.world.level.chunk.ChunkGenerator)(Object)this), chunk, randomState);

                    if (carving.isPillar) {
                        for (int y = originalY; y >= seaLevel - 15; y--) {
                            BlockState s = chunk.getBlockState(new BlockPos(startX + x, y, startZ + z));
                            if (s.isAir() || s.is(Blocks.WATER)) {
                                chunk.setBlockState(new BlockPos(startX + x, y, startZ + z), Blocks.STONE.defaultBlockState(), false);
                            }
                        }
                        continue;
                    }

                    int targetY = (carving.modifies && carving.targetY <= originalY) ? carving.targetY : originalY;

                    int sealedFloorY = targetY;
                    for (int scan = targetY; scan >= targetY - 14; scan--) {
                        BlockState s = chunk.getBlockState(new BlockPos(startX + x, scan, startZ + z));
                        if (s.isAir() || s.is(Blocks.WATER)) {
                            chunk.setBlockState(new BlockPos(startX + x, scan, startZ + z), Blocks.STONE.defaultBlockState(), false);
                            sealedFloorY = scan;
                        } else {
                            break;
                        }
                    }

                    BlockState repairState;
                    if (carving.isWatershedWall) {
                        if (originalY > carving.waterLevel + 2) {
                            repairState = Blocks.STONE.defaultBlockState();
                        } else {
                            repairState = Blocks.DIRT.defaultBlockState();
                        }
                    } else {
                        repairState = Blocks.WATER.defaultBlockState();
                    }

                    int repairBottomY = Math.min(sealedFloorY, carving.waterLevel - 8);
                    if (carving.isWatershedWall) {
                        repairBottomY = Math.min(repairBottomY, originalY);
                    }

                    int repairTopY = carving.isWatershedWall ? Math.max(carving.waterLevel, carving.wallTopY) : carving.waterLevel;

                    for (int y = repairTopY; y > repairBottomY; y--) {
                        BlockState currentState = chunk.getBlockState(new BlockPos(startX + x, y, startZ + z));
                        if (currentState.isAir()) {
                            BlockState stateToPlace = repairState;
                            if (carving.isWatershedWall && y == repairTopY && originalY <= repairTopY + 2) {
                                stateToPlace = Blocks.GRASS_BLOCK.defaultBlockState();
                            }
                            chunk.setBlockState(new BlockPos(startX + x, y, startZ + z), stateToPlace, false);
                            if (!carving.isWatershedWall && stateToPlace.is(Blocks.WATER)) {
                                chunk.markPosForPostprocessing(new BlockPos(startX + x, y, startZ + z));
                            }
                        }
                    }
                }
            }
            com.AdamNew.dynamicwaters.LakeHollows.clearChunkGround();
        }
    }
}
