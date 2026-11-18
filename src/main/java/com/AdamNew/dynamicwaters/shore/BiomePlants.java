package com.AdamNew.dynamicwaters.shore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BiomePlants {
    public record Own(BlockState[] states, double[] upTo, double share) {
        BlockState pick(double p) {
            final double at = p * upTo[upTo.length - 1];
            for (int k = 0; k < upTo.length; k++) {
                if (at < upTo[k]) return states[k];
            }
            return states[states.length - 1];
        }
    }

    private static final Map<Biome, Optional<Own>> OWN = Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static final Map<Long, Boolean> SURVIVES = new ConcurrentHashMap<>();

    private BiomePlants() {
    }

    public static Own of(Holder<Biome> biome) {
        final Biome b = biome.value();
        Optional<Own> known = OWN.get(b);
        if (known == null) {
            known = Optional.ofNullable(find(b));
            OWN.put(b, known);
        }
        return known.orElse(null);
    }

    private static Own find(Biome biome) {
        final Map<Block, Double> shares = BiomeTrees.weigh(biome, BiomePlants::groundPlant,
                GenerationStep.Decoration.VEGETAL_DECORATION.ordinal(), true);
        double all = 0.0, own = 0.0;
        final List<Map.Entry<Block, Double>> mods = new ArrayList<>();
        for (Map.Entry<Block, Double> e : shares.entrySet()) {
            if (onWater(e.getKey().defaultBlockState())) continue;
            all += e.getValue();
            if (!"minecraft".equals(BuiltInRegistries.BLOCK.getKey(e.getKey()).getNamespace())) {
                own += e.getValue();
                mods.add(e);
            }
        }
        if (mods.isEmpty() || !(all > 0.0)) return null;

        mods.sort(Map.Entry.comparingByKey(java.util.Comparator.comparing(k -> BuiltInRegistries.BLOCK.getKey(k).toString())));
        final BlockState[] states = new BlockState[mods.size()];
        final double[] upTo = new double[mods.size()];
        double sum = 0.0;
        for (int k = 0; k < states.length; k++) {
            states[k] = mods.get(k).getKey().defaultBlockState();
            sum += mods.get(k).getValue();
            upTo[k] = sum;
        }
        return new Own(states, upTo, own / all);
    }

    private static boolean groundPlant(BlockState s) {
        final Block b = s.getBlock();
        return b instanceof BushBlock && !(b instanceof SaplingBlock) && !(b instanceof CropBlock)
                && !(b instanceof MushroomBlock) && !(b instanceof WaterlilyBlock) && s.getFluidState().isEmpty();
    }

    private static boolean onWater(BlockState plant) {
        return survives(plant, Blocks.WATER.defaultBlockState());
    }

    public static boolean survives(BlockState plant, BlockState ground) {
        final BlockState base = bottom(plant);
        final long key = ((long) Block.getId(base) << 32) | (Block.getId(ground) & 0xFFFFFFFFL);
        Boolean known = SURVIVES.get(key);
        if (known == null) {
            boolean ok;
            try {
                ok = base.canSurvive(standIn(ground), BlockPos.ZERO);
            } catch (Throwable t) {
                ok = false;
            }
            known = ok;
            SURVIVES.put(key, known);
        }
        return known;
    }

    private static BlockState bottom(BlockState plant) {
        final BlockState[] parts = TallPlant.parts(plant);
        return parts != null ? parts[0] : plant;
    }

    private static LevelReader standIn(BlockState ground) {
        final FluidState groundFluid = ground.getFluidState();
        final InvocationHandler h = (proxy, m, args) -> {
            if (m.getDeclaringClass() == Object.class) {
                if (m.getName().equals("hashCode")) return System.identityHashCode(proxy);
                if (m.getName().equals("equals")) return proxy == args[0];
                return "BiomePlants.standIn";
            }
            final Class<?> r = m.getReturnType();
            if (args != null && args.length == 1 && args[0] instanceof BlockPos p) {
                final boolean under = p.getX() == 0 && p.getZ() == 0 && p.getY() == -1;
                if (r == BlockState.class) return under ? ground : Blocks.AIR.defaultBlockState();
                if (r == FluidState.class) return under ? groundFluid : Fluids.EMPTY.defaultFluidState();
            }
            if (m.isDefault()) return InvocationHandler.invokeDefault(proxy, m, args);
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == long.class) return 0L;
            if (r == float.class) return 0.0F;
            if (r == double.class) return 0.0;
            return null;
        };
        return (LevelReader) Proxy.newProxyInstance(LevelReader.class.getClassLoader(), new Class<?>[] { LevelReader.class }, h);
    }
}
