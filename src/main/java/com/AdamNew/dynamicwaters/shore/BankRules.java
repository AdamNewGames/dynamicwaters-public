package com.AdamNew.dynamicwaters.shore;

import com.AdamNew.dynamicwaters.terrain.BiomeList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BankRules {
    private record Lands(BiomeList biomes, boolean[] kinds, boolean any) {
        boolean test(Holder<Biome> biome, RiverBeach.Land land) {
            return any || kinds[land.ordinal()] || biomes.test(biome);
        }

        static Lands of(String text) {
            final List<String> ids = new ArrayList<>();
            final boolean[] kinds = new boolean[RiverBeach.Land.values().length];
            boolean any = false;
            for (String part : text.trim().split("[\\s,]+")) {
                if (part.isEmpty()) continue;
                if (part.equals("*")) {
                    any = true;
                } else if (part.startsWith("@")) {
                    for (RiverBeach.Land l : RiverBeach.Land.values()) {
                        if (l.name().equalsIgnoreCase(part.substring(1))) kinds[l.ordinal()] = true;
                    }
                } else {
                    ids.add(part);
                }
            }
            return new Lands(BiomeList.of(ids), kinds, any);
        }
    }

    record Plants(Lands lands, BlockState[] states, double[] upTo) {
        BlockState pick(double p) {
            final double at = p * upTo[upTo.length - 1];
            for (int k = 0; k < upTo.length; k++) {
                if (at < upTo[k]) return states[k];
            }
            return states[states.length - 1];
        }
    }

    record Leaves(Lands lands, boolean auto, BlockState state) {
    }

    private record Read<T>(List<? extends String> from, List<T> rules) {
    }

    private static volatile Read<Plants> plants = new Read<>(null, List.of());
    private static volatile Read<Leaves> leaves = new Read<>(null, List.of());

    private BankRules() {
    }

    static Plants plants(List<? extends String> config, Holder<Biome> biome, RiverBeach.Land land) {
        Read<Plants> read = plants;
        if (read.from() != config) plants = read = new Read<>(config, plantRules(config));
        for (Plants rule : read.rules()) {
            if (rule.lands().test(biome, land)) return rule;
        }
        return null;
    }

    static Leaves leaves(List<? extends String> config, Holder<Biome> biome, RiverBeach.Land land) {
        Read<Leaves> read = leaves;
        if (read.from() != config) leaves = read = new Read<>(config, leavesRules(config));
        for (Leaves rule : read.rules()) {
            if (rule.lands().test(biome, land)) return rule;
        }
        return null;
    }

    private static List<Plants> plantRules(List<? extends String> config) {
        final List<Plants> rules = new ArrayList<>();
        for (String entry : config) {
            final int arrow = entry.indexOf("->");
            if (arrow < 0) continue;
            final List<BlockState> states = new ArrayList<>();
            final List<Double> upTo = new ArrayList<>();
            double sum = 0.0;
            for (String item : entry.substring(arrow + 2).split(",")) {
                final String[] words = item.trim().split("\\s+");
                if (words.length == 0 || words[0].isEmpty()) continue;
                final BlockState s = plant(words[0].toLowerCase(Locale.ROOT));
                double weight = 1.0;
                if (words.length > 1) {
                    try {
                        weight = Double.parseDouble(words[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (s == null || !(weight > 0.0)) continue;
                sum += weight;
                states.add(s);
                upTo.add(sum);
            }
            if (states.isEmpty()) continue;
            final double[] sums = new double[upTo.size()];
            for (int k = 0; k < sums.length; k++) sums[k] = upTo.get(k);
            rules.add(new Plants(Lands.of(entry.substring(0, arrow)), states.toArray(new BlockState[0]), sums));
        }
        return rules;
    }

    private static List<Leaves> leavesRules(List<? extends String> config) {
        final List<Leaves> rules = new ArrayList<>();
        for (String entry : config) {
            final int arrow = entry.indexOf("->");
            if (arrow < 0) continue;
            final String what = entry.substring(arrow + 2).trim().toLowerCase(Locale.ROOT);
            final Lands lands = Lands.of(entry.substring(0, arrow));
            if (what.equals("auto")) {
                rules.add(new Leaves(lands, true, null));
            } else if (what.equals("none")) {
                rules.add(new Leaves(lands, false, null));
            } else {
                final Block block = block(what);
                if (block != null) rules.add(new Leaves(lands, false, kept(block.defaultBlockState())));
            }
        }
        return rules;
    }

    static BlockState kept(BlockState leaves) {
        return leaves.hasProperty(BlockStateProperties.PERSISTENT)
                ? leaves.setValue(BlockStateProperties.PERSISTENT, true) : leaves;
    }

    private static BlockState plant(String id) {
        if (id.equals("minecraft:short_grass") || id.equals("minecraft:grass") || id.equals("short_grass")
                || id.equals("grass")) {
            return RiverBeach.SHORT_GRASS;
        }
        final Block block = block(id);
        return block == null ? null : block.defaultBlockState();
    }

    private static Block block(String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : BuiltInRegistries.BLOCK.getOptional(key).orElse(null);
    }
}
