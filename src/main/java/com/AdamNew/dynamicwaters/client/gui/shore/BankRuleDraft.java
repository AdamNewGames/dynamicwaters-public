package com.AdamNew.dynamicwaters.client.gui.shore;

import com.AdamNew.dynamicwaters.shore.RiverBeach;
import com.AdamNew.dynamicwaters.terrain.BiomeList;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class BankRuleDraft {
    static final String[] LANDS = {"forest", "grassland", "lush", "cold", "dry", "desert", "badlands", "beach"};
    static final String AUTO = "auto", NONE = "none";

    static final class Plant {
        String id;
        double weight;

        Plant(String id, double weight) {
            this.id = id;
            this.weight = weight;
        }
    }

    final boolean plantsRule;
    boolean any;
    final Set<String> kinds = new LinkedHashSet<>();

    final List<String> biomes = new ArrayList<>();
    final List<Plant> plants = new ArrayList<>();
    String leaves = AUTO;

    BankRuleDraft(boolean plantsRule) {
        this.plantsRule = plantsRule;
    }

    static BankRuleDraft parse(String entry, boolean plantsRule) {
        final BankRuleDraft d = new BankRuleDraft(plantsRule);
        final int arrow = entry.indexOf("->");
        final String lands = arrow < 0 ? entry : entry.substring(0, arrow);
        for (String part : lands.trim().split("[\\s,]+")) {
            if (part.isEmpty()) continue;
            if (part.equals("*")) d.any = true;
            else if (part.startsWith("@")) d.kinds.add(part.substring(1).toLowerCase(Locale.ROOT));
            else d.biomes.add(part);
        }
        final String what = arrow < 0 ? "" : entry.substring(arrow + 2).trim();
        if (plantsRule) {
            for (String item : what.split(",")) {
                final String[] words = item.trim().split("\\s+");
                if (words.length == 0 || words[0].isEmpty()) continue;
                double w = 1.0;
                if (words.length > 1) {
                    try {
                        w = Double.parseDouble(words[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                d.plants.add(new Plant(words[0].toLowerCase(Locale.ROOT), w));
            }
        } else {
            d.leaves = what.isEmpty() ? AUTO : what.toLowerCase(Locale.ROOT);
        }
        return d;
    }

    BankRuleDraft copy() {
        final BankRuleDraft d = new BankRuleDraft(plantsRule);
        d.any = any;
        d.kinds.addAll(kinds);
        d.biomes.addAll(biomes);
        for (Plant p : plants) d.plants.add(new Plant(p.id, p.weight));
        d.leaves = leaves;
        return d;
    }

    String text() {
        final List<String> lands = new ArrayList<>();
        if (any) lands.add("*");
        for (String k : LANDS) {
            if (kinds.contains(k)) lands.add("@" + k);
        }
        for (String k : kinds) {
            if (!List.of(LANDS).contains(k)) lands.add("@" + k);
        }
        lands.addAll(biomes);
        final StringBuilder sb = new StringBuilder(String.join(" ", lands)).append(" -> ");
        if (plantsRule) {
            for (int i = 0; i < plants.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(plants.get(i).id).append(' ').append(number(plants.get(i).weight));
            }
        } else {
            sb.append(leaves);
        }
        return sb.toString();
    }

    double total() {
        double t = 0.0;
        for (Plant p : plants) t += Math.max(0.0, p.weight);
        return t;
    }

    boolean landless() {
        return !any && kinds.isEmpty() && biomes.isEmpty();
    }

    boolean matches(Holder<Biome> biome, String land) {
        return any || kinds.contains(land) || !biomes.isEmpty() && BiomeList.of(biomes).test(biome);
    }

    String landsText() {
        final List<String> out = new ArrayList<>();
        if (any) out.add(Component.translatable("dynamicwaters.gui.bankRules.land.any").getString());
        for (String k : LANDS) {
            if (kinds.contains(k)) out.add(Component.translatable("dynamicwaters.gui.bankRules.land." + k).getString());
        }
        for (String b : biomes) out.add(b.replace("minecraft:", ""));
        return out.isEmpty() ? Component.translatable("dynamicwaters.gui.bankRules.nowhere").getString() : String.join(", ", out);
    }

    static String number(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1.0E9) return String.valueOf((long) v);
        return new java.math.BigDecimal(v).round(new java.math.MathContext(3)).stripTrailingZeros().toPlainString();
    }

    static String landOf(Holder<Biome> biome) {
        return RiverBeach.landName(biome);
    }
}
