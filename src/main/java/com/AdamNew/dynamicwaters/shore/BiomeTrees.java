package com.AdamNew.dynamicwaters.shore;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class BiomeTrees {
    private static final int DEEPEST = 10;
    private static final int VISITS = 4000;
    private static final int PROVIDER_SAMPLES = 32;
    private static final int COUNT_SAMPLES = 200;
    private static final Map<Biome, Optional<BlockState>> LEAVES =
            Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private BiomeTrees() {
    }

    public static BlockState leavesOf(Holder<Biome> biome) {
        final Biome b = biome.value();
        Optional<BlockState> known = LEAVES.get(b);
        if (known == null) {
            known = Optional.ofNullable(find(b));
            LEAVES.put(b, known);
        }
        return known.orElse(null);
    }

    private static final class Walk {
        final Predicate<BlockState> wanted;
        final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Block, Double> found = new HashMap<>();
        double weight = 1.0;
        int left = VISITS;

        Walk(Predicate<BlockState> wanted) {
            this.wanted = wanted;
        }
    }

    private static boolean leaves(BlockState state) {
        return state.is(BlockTags.LEAVES) || state.getBlock() instanceof LeavesBlock;
    }

    static Map<Block, Double> weigh(Biome biome, Predicate<BlockState> wanted, int onlyStep, boolean tries) {
        final Map<Block, Double> shares = new HashMap<>();
        try {
            final java.util.List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
            for (int s = 0; s < steps.size(); s++) {
                if (onlyStep >= 0 && s != onlyStep) continue;
                for (Holder<PlacedFeature> placed : steps.get(s)) {
                    final Walk walk = new Walk(wanted);
                    walk(placed, walk, 0);
                    double all = 0.0;
                    for (double v : walk.found.values()) all += v;
                    if (all <= 0.0) continue;
                    final double often = often(placed) * (tries ? tries(placed) : 1.0);
                    for (Map.Entry<Block, Double> e : walk.found.entrySet()) {
                        shares.merge(e.getKey(), e.getValue() / all * often, Double::sum);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return shares;
    }

    private static double tries(Holder<PlacedFeature> placed) {
        try {
            if (placed.value().feature().value().config() instanceof RandomPatchConfiguration patch) {
                return Math.max(1, patch.tries());
            }
        } catch (Throwable ignored) {
        }
        return 1.0;
    }

    private static BlockState find(Biome biome) {
        final Map<Block, Double> shares = weigh(biome, BiomeTrees::leaves, -1, false);
        Block best = null;
        double most = 0.0;
        String bestId = "";
        for (Map.Entry<Block, Double> e : shares.entrySet()) {
            final double v = e.getValue();
            final String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(e.getKey()));

            if (v > most + 1.0E-9 || Math.abs(v - most) <= 1.0E-9 && id.compareTo(bestId) < 0) {
                most = v;
                best = e.getKey();
                bestId = id;
            }
        }
        return best == null ? null : best.defaultBlockState();
    }

    private static double often(Holder<PlacedFeature> placed) {
        double n = 1.0;
        try {
            for (PlacementModifier m : placed.value().placement()) {
                final JsonElement json = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, m).result().orElse(null);
                if (!(json instanceof JsonObject o) || !o.has("type")) continue;
                final String type = o.get("type").getAsString();
                if ((type.equals("minecraft:count") || type.equals("minecraft:count_on_every_layer")) && o.has("count")) {
                    n *= mean(IntProvider.CODEC.parse(JsonOps.INSTANCE, o.get("count")).result().orElse(null));
                } else if (type.equals("minecraft:rarity_filter") && o.has("chance")) {
                    n /= Math.max(1, o.get("chance").getAsInt());
                } else if (type.equals("minecraft:noise_threshold_count") && o.has("below_noise") && o.has("above_noise")) {
                    n *= 0.5 * (o.get("below_noise").getAsInt() + o.get("above_noise").getAsInt());
                }
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    private static double mean(IntProvider count) {
        if (count == null) return 1.0;
        final RandomSource random = RandomSource.create(0x5EEDL);
        double sum = 0.0;
        for (int k = 0; k < COUNT_SAMPLES; k++) sum += count.sample(random);
        return sum / COUNT_SAMPLES;
    }

    private static void walk(Object o, Walk w, int depth) {
        if (o == null || depth > DEEPEST || w.left-- <= 0) return;
        if (o instanceof BlockState state) {
            if (w.wanted.test(state)) w.found.merge(state.getBlock(), w.weight, Double::sum);
            return;
        }
        if (o instanceof Block || o instanceof Number || o instanceof CharSequence || o instanceof Boolean
                || o instanceof Enum<?> || o instanceof Class<?>) {
            return;
        }

        if (o instanceof Holder<?> holder) {
            if (holder.isBound()) walk(holder.value(), w, depth);
            return;
        }
        if (o instanceof PlacedFeature placed) {
            walk(placed.feature(), w, depth);
            return;
        }
        if (o instanceof ConfiguredFeature<?, ?> configured) {
            walk(configured.config(), w, depth);
            return;
        }
        if (o instanceof WeightedPlacedFeature weighted) {
            walk(weighted.feature, w, depth);
            return;
        }
        if (o instanceof BlockStateProvider provider) {
            final RandomSource random = RandomSource.create(0x5EEDL);
            for (int k = 0; k < PROVIDER_SAMPLES; k++) {
                try {
                    walk(provider.getState(random, new BlockPos(k * 97 - 1500, 64, k * 61 - 900)), w, depth + 1);
                } catch (Throwable ignored) {
                    break;
                }
            }
            return;
        }

        if (o instanceof RandomFeatureConfiguration random) {
            final double outer = w.weight;
            double rest = 1.0;
            for (WeightedPlacedFeature f : random.features) {
                w.weight = outer * rest * f.chance;
                walk(f.feature, w, depth + 1);
                rest *= 1.0 - f.chance;
            }
            w.weight = outer * rest;
            walk(random.defaultFeature, w, depth + 1);
            w.weight = outer;
            return;
        }
        if (!w.seen.add(o)) return;
        if (o instanceof HolderSet<?> set) {
            for (Holder<?> h : set) walk(h, w, depth + 1);
            return;
        }
        if (o instanceof Optional<?> optional) {
            optional.ifPresent(v -> walk(v, w, depth + 1));
            return;
        }
        if (o instanceof Iterable<?> iterable) {
            for (Object v : iterable) walk(v, w, depth + 1);
            return;
        }
        if (o instanceof Map<?, ?> map) {
            for (Object v : map.values()) walk(v, w, depth + 1);
            return;
        }
        if (o instanceof Object[] array) {
            for (Object v : array) walk(v, w, depth + 1);
            return;
        }
        if (o.getClass().isArray()) return;

        final Class<?> type = o.getClass();
        if (type.isRecord()) {
            for (RecordComponent c : type.getRecordComponents()) {
                try {
                    walk(c.getAccessor().invoke(o), w, depth + 1);
                } catch (Throwable ignored) {
                }
            }
            return;
        }
        for (Class<?> t = type; t != null && t != Object.class; t = t.getSuperclass()) {
            for (Field f : t.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                try {
                    if (!f.trySetAccessible()) continue;
                    walk(f.get(o), w, depth + 1);
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
