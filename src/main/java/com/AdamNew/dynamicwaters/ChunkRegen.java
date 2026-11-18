package com.AdamNew.dynamicwaters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
//? if >=1.21 {
import net.minecraft.world.level.chunk.status.ChunkStatus;
//?} else {
/*import net.minecraft.world.level.chunk.ChunkStatus;
*///?}

public final class ChunkRegen {
    private static final int READ = 8;

    private static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
            | Block.UPDATE_MOVE_BY_PISTON;

    private ChunkRegen() {
    }

    public static long regenerate(ServerLevel level, ChunkPos center, int radius) {
        final int ring = radius + 2;
        final Map<Long, ProtoChunk> made = new HashMap<>();
        final List<ProtoChunk> all = new ArrayList<>(), decorated = new ArrayList<>(), inner = new ArrayList<>();
        final net.minecraft.core.Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        for (int dz = -ring; dz <= ring; dz++) {
            for (int dx = -ring; dx <= ring; dx++) {
                final ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);
                final ChunkAccess old = level.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_REFERENCES, true);
                final ProtoChunk proto = new ProtoChunk(pos, UpgradeData.EMPTY, level, biomes, null);
                proto.setAllStarts(old.getAllStarts());
                proto.setAllReferences(old.getAllReferences());
                //? if >=1.21 {
                proto.setPersistedStatus(ChunkStatus.STRUCTURE_REFERENCES);
                //?} else {
                /*proto.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
                *///?}
                made.put(pos.toLong(), proto);
                all.add(proto);
                final int out = Math.max(Math.abs(dx), Math.abs(dz));
                if (out <= radius + 1) decorated.add(proto);
                if (out <= radius) inner.add(proto);
            }
        }
        generate(level, center, ring, made, all, decorated);
        long changed = 0;
        final List<ChunkAccess> placed = new ArrayList<>();
        for (ProtoChunk proto : inner) {
            final LevelChunk chunk = level.getChunk(proto.getPos().x, proto.getPos().z);
            changed += place(level, proto, chunk);
            placed.add(chunk);
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(placed);
        return changed;
    }

    private static long place(ServerLevel level, ProtoChunk proto, LevelChunk chunk) {
        for (BlockEntity old : new ArrayList<>(chunk.getBlockEntities().values())) {
            if (old instanceof Container container) container.clearContent();
        }
        long changed = 0;
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        final int x0 = proto.getPos().getMinBlockX(), z0 = proto.getPos().getMinBlockZ();
        final LevelChunkSection[] from = proto.getSections();
        for (int s = 0; s < from.length; s++) {
            final LevelChunkSection section = from[s];
            final int y0 = SectionPos.sectionToBlockCoord(proto.getSectionYFromSectionIndex(s));
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        final BlockState now = section.getBlockState(x, y, z);
                        at.set(x0 + x, y0 + y, z0 + z);
                        if (chunk.getBlockState(at) == now) continue;
                        level.setBlock(at, now, SET_FLAGS);
                        changed++;
                    }
                }
            }
        }
        for (BlockPos pos : proto.getBlockEntitiesPos()) {
            final BlockEntity entity = proto.getBlockEntity(pos);
            if (entity != null) {
                level.setBlockEntity(entity);
            } else {
                final net.minecraft.nbt.CompoundTag tag = proto.getBlockEntityNbt(pos);
                if (tag != null) {
                    chunk.setBlockEntityNbt(tag);
                    chunk.getBlockEntity(pos);
                }
            }
        }

        final it.unimi.dsi.fastutil.shorts.ShortList[] post = proto.getPostProcessing();
        for (int s = 0; s < post.length; s++) {
            if (post[s] == null) continue;
            final int sectionY = proto.getSectionYFromSectionIndex(s);
            for (int k = 0; k < post[s].size(); k++) {
                final BlockPos pos = ProtoChunk.unpackOffsetCoordinates(post[s].getShort(k), sectionY, proto.getPos());
                final BlockState state = level.getBlockState(pos);
                final net.minecraft.world.level.material.FluidState fluid = state.getFluidState();
                if (!fluid.isEmpty()) fluid.tick(level, pos);
                if (!(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
                    final BlockState shaped = Block.updateFromNeighbourShapes(state, level, pos);
                    if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_INVISIBLE);
                }
            }
        }

        final net.minecraft.world.level.biome.Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
        final LevelChunkSection[] to = chunk.getSections();
        for (int s = 0; s < to.length && s < from.length; s++) {
            final LevelChunkSection source = from[s];
            to[s].fillBiomesFromNoise((qx, qy, qz, unused) -> source.getNoiseBiome(qx & 3, qy & 3, qz & 3), sampler,
                    QuartPos.fromSection(proto.getPos().x), QuartPos.fromSection(chunk.getSectionYFromSectionIndex(s)),
                    QuartPos.fromSection(proto.getPos().z));
        }
        chunk.setUnsaved(true);
        LivingCurrent.onChunkLoad(level, chunk);
        return changed;
    }

    //? if >=1.21 {
    private static void generate(ServerLevel level, ChunkPos center, int ring, Map<Long, ProtoChunk> made,
            List<ProtoChunk> all, List<ProtoChunk> decorated) {
        final net.minecraft.world.level.chunk.status.WorldGenContext context =
                ((com.AdamNew.dynamicwaters.mixin.ChunkMapAccessor) level.getChunkSource().chunkMap).dynamicwaters$worldGenContext();
        final net.minecraft.util.StaticCache2D<net.minecraft.server.level.GenerationChunkHolder> cache =
                net.minecraft.util.StaticCache2D.create(center.x, center.z, ring + READ, (x, z) -> {
                    final ProtoChunk proto = made.get(ChunkPos.asLong(x, z));
                    return proto != null ? new Holder(new ChunkPos(x, z), proto, ChunkStatus.FULL)
                            : holderOf(level, x, z);
                });
        for (ChunkStatus status : List.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS)) {
            final net.minecraft.world.level.chunk.status.ChunkStep step =
                    net.minecraft.world.level.chunk.status.ChunkPyramid.GENERATION_PYRAMID.getStepTo(status);
            for (ProtoChunk proto : all) step.apply(context, cache, proto).join();
        }
        final net.minecraft.world.level.chunk.status.ChunkStep features =
                net.minecraft.world.level.chunk.status.ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        for (ProtoChunk proto : decorated) features.apply(context, cache, proto).join();
    }

    private static Holder holderOf(ServerLevel level, int x, int z) {
        final ChunkAccess chunk = level.getChunk(x, z, ChunkStatus.STRUCTURE_STARTS, true);
        return new Holder(new ChunkPos(x, z), chunk, chunk.getPersistedStatus());
    }

    private static final class Holder extends net.minecraft.server.level.GenerationChunkHolder {
        Holder(ChunkPos pos, ChunkAccess chunk, ChunkStatus upTo) {
            super(pos);
            final java.util.concurrent.atomic.AtomicReferenceArray<CompletableFuture<net.minecraft.server.level.ChunkResult<ChunkAccess>>> futures =
                    ((com.AdamNew.dynamicwaters.mixin.GenerationChunkHolderAccessor) (Object) this).dynamicwaters$futures();
            final CompletableFuture<net.minecraft.server.level.ChunkResult<ChunkAccess>> done =
                    CompletableFuture.completedFuture(net.minecraft.server.level.ChunkResult.of(chunk));
            for (ChunkStatus status : ChunkStatus.getStatusList()) {
                if (status.isAfter(upTo)) break;
                futures.set(status.getIndex(), done);
            }
        }

        @Override
        public int getTicketLevel() {
            return 0;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }
    //?} else {
    /*    private static void generate(ServerLevel level, ChunkPos center, int ring, Map<Long, ProtoChunk> made,
            List<ProtoChunk> all, List<ProtoChunk> decorated) {
        final net.minecraft.server.level.ServerChunkCache source = level.getChunkSource();
        for (ChunkStatus status : List.of(ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS,
                ChunkStatus.FEATURES)) {
            final int range = status.getRange();
            for (ProtoChunk proto : status == ChunkStatus.FEATURES ? decorated : all) {
                final ChunkPos pos = proto.getPos();
                final List<ChunkAccess> around = new ArrayList<>((2 * range + 1) * (2 * range + 1));
                for (int dz = -range; dz <= range; dz++) {
                    for (int dx = -range; dx <= range; dx++) {
                        final ProtoChunk other = made.get(ChunkPos.asLong(pos.x + dx, pos.z + dz));
                        around.add(other != null ? other
                                : level.getChunk(pos.x + dx, pos.z + dz, ChunkStatus.STRUCTURE_STARTS, true));
                    }
                }
                status.generate(net.minecraft.Util.backgroundExecutor(), level, source.getGenerator(),
                        level.getServer().getStructureManager(), source.getLightEngine(),
                        c -> CompletableFuture.completedFuture(com.mojang.datafixers.util.Either.left(c)), around).join();
            }
        }
    }
    *///?}
}
