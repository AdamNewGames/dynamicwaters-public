package com.AdamNew.dynamicwaters.terrain;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record BiomeList(Set<ResourceLocation> ids, List<TagKey<Biome>> tags, List<String> prefixes) {
    public static final BiomeList NONE = new BiomeList(Set.of(), List.of(), List.of());

    public static BiomeList of(List<? extends String> entries) {
        final Set<ResourceLocation> ids = new HashSet<>();
        final List<TagKey<Biome>> tags = new ArrayList<>();
        final List<String> prefixes = new ArrayList<>();
        for (String raw : entries) {
            final String e = raw.trim();
            if (e.isEmpty()) continue;
            if (e.endsWith("*")) {
                prefixes.add(e.substring(0, e.length() - 1));
            } else if (e.startsWith("#")) {
                final ResourceLocation id = ResourceLocation.tryParse(e.substring(1));
                if (id != null) tags.add(TagKey.create(Registries.BIOME, id));
            } else {
                final ResourceLocation id = ResourceLocation.tryParse(e);
                if (id != null) ids.add(id);
            }
        }
        return new BiomeList(ids, tags, prefixes);
    }

    public boolean test(Holder<Biome> biome) {
        final ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key != null) {
            if (ids.contains(key.location())) return true;
            final String name = key.location().toString();
            for (String p : prefixes) {
                if (name.startsWith(p)) return true;
            }
        }
        for (TagKey<Biome> tag : tags) {
            if (biome.is(tag)) return true;
        }
        return false;
    }
}
