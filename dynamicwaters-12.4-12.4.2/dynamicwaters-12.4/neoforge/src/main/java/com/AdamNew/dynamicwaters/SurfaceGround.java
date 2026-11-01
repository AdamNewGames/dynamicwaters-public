package com.AdamNew.dynamicwaters;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SurfaceGround {
    private static final MethodHandle NEW_CONTEXT, UPDATE_XZ, UPDATE_Y, APPLY_RULE, TRY_APPLY;

    static {
        MethodHandle newContext = null, updateXZ = null, updateY = null, applyRule = null, tryApply = null;
        try {
            final Class<?> context = Class.forName("net.minecraft.world.level.levelgen.SurfaceRules$Context");
            final Class<?> rule = Class.forName("net.minecraft.world.level.levelgen.SurfaceRules$SurfaceRule");
            final Class<?> source = Class.forName("net.minecraft.world.level.levelgen.SurfaceRules$RuleSource");
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            final Constructor<?> make = context.getDeclaredConstructor(SurfaceSystem.class, RandomState.class,
                    ChunkAccess.class, NoiseChunk.class, Function.class, Registry.class, WorldGenerationContext.class);
            make.setAccessible(true);
            newContext = lookup.unreflectConstructor(make).asType(MethodType.methodType(Object.class,
                    SurfaceSystem.class, RandomState.class, ChunkAccess.class, NoiseChunk.class, Function.class,
                    Registry.class, WorldGenerationContext.class));
            final Method xz = context.getDeclaredMethod("updateXZ", int.class, int.class);
            xz.setAccessible(true);
            updateXZ = lookup.unreflect(xz).asType(MethodType.methodType(void.class, Object.class, int.class, int.class));
            final Method y = context.getDeclaredMethod("updateY", int.class, int.class, int.class, int.class, int.class,
                    int.class);
            y.setAccessible(true);
            updateY = lookup.unreflect(y).asType(MethodType.methodType(void.class, Object.class, int.class, int.class,
                    int.class, int.class, int.class, int.class));
            applyRule = lookup.unreflect(Function.class.getMethod("apply", Object.class))
                    .asType(MethodType.methodType(Object.class, Object.class, Object.class));
            final Method apply = rule.getMethod("tryApply", int.class, int.class, int.class);
            apply.setAccessible(true);
            tryApply = lookup.unreflect(apply)
                    .asType(MethodType.methodType(BlockState.class, Object.class, int.class, int.class, int.class));
            if (!Function.class.isAssignableFrom(source)) throw new IllegalStateException("RuleSource is no Function");
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().warn(
                    "[Dynamic Waters] Surface rules out of reach ({}); river cuts are dressed in grass.", t.toString());
            newContext = null;
        }
        NEW_CONTEXT = newContext;
        UPDATE_XZ = updateXZ;
        UPDATE_Y = updateY;
        APPLY_RULE = applyRule;
        TRY_APPLY = tryApply;
    }

    private final NoiseBasedChunkGenerator generator;
    private final WorldGenRegion level;
    private final ChunkAccess chunk;
    private final RandomState random;
    private final Supplier<NoiseChunk> noise;

    private Object context, rule;
    private boolean failed = NEW_CONTEXT == null;

    public SurfaceGround(NoiseBasedChunkGenerator generator, WorldGenRegion level, ChunkAccess chunk, RandomState random,
            Supplier<NoiseChunk> noise) {
        this.generator = generator;
        this.level = level;
        this.chunk = chunk;
        this.random = random;
        this.noise = noise;
    }

    public BlockState top(int x, int y, int z) {
        if (failed) return null;
        try {
            if (rule == null) {
                final Function<BlockPos, Holder<Biome>> biomes = level.getBiomeManager()::getBiome;
                context = NEW_CONTEXT.invokeExact(random.surfaceSystem(), random, chunk, noise.get(), (Function) biomes,
                        (Registry) level.registryAccess().registryOrThrow(Registries.BIOME),
                        new WorldGenerationContext(generator, level));
                rule = APPLY_RULE.invokeExact((Object) generator.generatorSettings().value().surfaceRule(), context);
            }
            UPDATE_XZ.invokeExact(context, x, z);
            UPDATE_Y.invokeExact(context, 1, 1, Integer.MIN_VALUE, x, y, z);
            final BlockState laid = (BlockState) TRY_APPLY.invokeExact(rule, x, y, z);
            return laid != null ? laid : generator.generatorSettings().value().defaultBlock();
        } catch (Throwable t) {
            failed = true;
            com.mojang.logging.LogUtils.getLogger().warn(
                    "[Dynamic Waters] Surface rule failed ({}); river cuts here are dressed in grass.", t.toString());
            return null;
        }
    }

    public boolean rock(BlockState state) {
        return state == generator.generatorSettings().value().defaultBlock() || state.is(Blocks.DEEPSLATE);
    }
}
