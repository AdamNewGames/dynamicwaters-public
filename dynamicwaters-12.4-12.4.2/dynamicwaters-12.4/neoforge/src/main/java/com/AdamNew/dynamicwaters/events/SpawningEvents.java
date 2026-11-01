package com.AdamNew.dynamicwaters.events;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = DynamicWaters.MODID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.GAME)
public class SpawningEvents {
    @SubscribeEvent
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        LevelAccessor level = event.getLevel();
        if (level == null) return;

        if (!level.getFluidState(event.getPos()).is(net.minecraft.tags.FluidTags.WATER)) return;

        net.minecraft.world.level.Level mcLevel = level instanceof net.minecraft.world.level.Level ? (net.minecraft.world.level.Level) level : null;
        Vec3 flow = HydrologyManager.getRiverFlow(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), mcLevel);
        if (!flow.equals(Vec3.ZERO)) {
            try {
                Holder<Biome> riverBiome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.RIVER);

                var riverSpawns = riverBiome.value().getMobSettings().getMobs(event.getMobCategory());

                var list = event.getSpawnerDataList();
                list.clear();

                for (var spawnData : riverSpawns.unwrap()) {
                    list.add(spawnData);
                }
            } catch (Exception e) {
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof net.minecraft.world.level.Level mcLevel
                && HydrologyManager.riversIn(mcLevel)) {
            net.minecraft.world.level.chunk.ChunkGenerator generator = null;
            net.minecraft.world.level.levelgen.RandomState randomState = null;

            if (mcLevel instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                generator = serverLevel.getChunkSource().getGenerator();
                randomState = serverLevel.getChunkSource().randomState();
            } else {
                generator = HydrologyManager.getGlobalGenerator();
                randomState = HydrologyManager.getGlobalRandomState();
            }

            if (generator != null && randomState != null) {
                int blockX = event.getChunk().getPos().getMinBlockX() + 8;
                int blockZ = event.getChunk().getPos().getMinBlockZ() + 8;
                com.AdamNew.dynamicwaters.MainRivers.prefetchAround(blockX, blockZ, generator, mcLevel, randomState);
            }
        }
    }
}
