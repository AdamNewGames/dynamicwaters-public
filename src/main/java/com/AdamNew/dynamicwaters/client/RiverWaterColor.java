package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

public final class RiverWaterColor implements ColorResolver {
    private static final float SHIFT = 0.6F;
    private static final float MOUNTAIN_ABOVE = 8.0F;

    private record Settings(boolean rivers, int mountain, int lowland, Map<ResourceLocation, Integer> biomes,
            List<Map.Entry<TagKey<Biome>, Integer>> tags, Map<ResourceLocation, Integer> builtIn) {
    }

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final String BUILT_IN_RESOURCE = "/dynamicwaters/water_colors.json";
    private static volatile Map<ResourceLocation, Integer> builtIn;

    private static volatile Settings settings;
    private static Object settingsRead;

    private static final Map<Biome, int[]> BIOMES = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final int MUD = 0x6E5A3C;
    private static final float MUD_RAIN = 0.3F, MUD_STORM = 0.5F;
    private static final int MUD_CLEARS = 1200;

    private static volatile int mud;

    private static int clearing;

    private final ColorResolver base;

    public RiverWaterColor(ColorResolver base) {
        this.base = base;
    }

    @Override
    public int getColor(Biome biome, double x, double z) {
        final int own = this.base.getColor(biome, x, z);
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return own;
        Settings s = settings;
        if (s == null) s = settings = read();
        if (s == null) return own;
        final int[] is = biome(level, biome, s);
        final int colour;
        if (is[1] >= 0) {
            colour = (own & 0xFF000000) | is[1];
        } else if (!s.rivers() || is[0] == 0) {
            colour = own;
        } else {
            final int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z)) - 1;
            final float up = smooth((top - (level.getSeaLevel() - 1)) / MOUNTAIN_ABOVE);
            colour = mix(own, mix(s.lowland(), s.mountain(), up), SHIFT);
        }

        final int m = mud;
        if (m == 0 || is[0] == 0 || is[2] == 0) return colour;
        return mix(colour, MUD, m == 2 ? MUD_STORM : MUD_RAIN);
    }

    private static void weather() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final boolean on;
        try {
            on = Config.MUDDY_RAIN_RIVERS.get();
        } catch (IllegalStateException notLoadedYet) {
            return;
        }
        final int want = level == null || !on ? 0 : level.isThundering() ? 2 : level.isRaining() ? 1 : 0;
        if (want == mud) {
            clearing = 0;
            return;
        }
        if (want < mud && on && level != null && ++clearing < MUD_CLEARS) return;
        clearing = 0;
        mud = want;
        if (level != null) minecraft.levelRenderer.allChanged();
    }

    public static void tick() {
        weather();
        final Object now = settingsNow();
        if (now == null || now.equals(settingsRead)) return;
        final boolean first = settingsRead == null;
        settingsRead = now;
        settings = read();
        BIOMES.clear();
        if (!first && Minecraft.getInstance().level != null) Minecraft.getInstance().levelRenderer.allChanged();
    }

    private static Object settingsNow() {
        try {
            return List.of(Config.RIVER_WATER_COLOR.get(), Config.MOUNTAIN_RIVER_WATER_COLOR.get(),
                    Config.LOWLAND_RIVER_WATER_COLOR.get(), List.copyOf(Config.BIOME_WATER_COLORS.get()),
                    Config.BUILT_IN_WATER_COLORS.get());
        } catch (IllegalStateException notLoadedYet) {
            return null;
        }
    }

    private static Settings read() {
        try {
            final int mountain = Config.hexColor(Config.MOUNTAIN_RIVER_WATER_COLOR.get());
            final int lowland = Config.hexColor(Config.LOWLAND_RIVER_WATER_COLOR.get());
            final Map<ResourceLocation, Integer> biomes = new HashMap<>();
            final List<Map.Entry<TagKey<Biome>, Integer>> tags = new ArrayList<>();
            for (String entry : Config.BIOME_WATER_COLORS.get()) {
                final int eq = entry.lastIndexOf('=');
                if (eq <= 0) continue;
                final String name = entry.substring(0, eq).trim();
                final int colour = Config.hexColor(entry.substring(eq + 1));
                if (colour < 0) continue;
                if (name.startsWith("#")) {
                    final ResourceLocation id = ResourceLocation.tryParse(name.substring(1));
                    if (id != null) tags.add(Map.entry(TagKey.create(Registries.BIOME, id), colour));
                } else {
                    final ResourceLocation id = ResourceLocation.tryParse(name);
                    if (id != null) biomes.put(id, colour);
                }
            }
            return new Settings(Config.RIVER_WATER_COLOR.get(), mountain >= 0 ? mountain : 0x2E9FC4,
                    lowland >= 0 ? lowland : 0x4279D2, Map.copyOf(biomes), List.copyOf(tags),
                    Config.BUILT_IN_WATER_COLORS.get() ? builtInColors() : Map.of());
        } catch (IllegalStateException notLoadedYet) {
            return null;
        }
    }

    public static Map<ResourceLocation, Integer> builtInColors() {
        Map<ResourceLocation, Integer> known = builtIn;
        if (known != null) return known;
        final Map<ResourceLocation, Integer> found = new java.util.LinkedHashMap<>();
        try (java.io.InputStream in = RiverWaterColor.class.getResourceAsStream(BUILT_IN_RESOURCE)) {
            if (in == null) {
                LOG.warn("Dynamic Waters: built-in water colours {} are missing from the jar", BUILT_IN_RESOURCE);
            } else {
                final com.google.gson.JsonObject mods = com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> mod : mods.entrySet()) {
                    if (mod.getKey().startsWith("_") || !mod.getValue().isJsonObject()
                            || !com.AdamNew.dynamicwaters.platform.Platform.isModLoaded(mod.getKey())) continue;
                    for (Map.Entry<String, com.google.gson.JsonElement> b : mod.getValue().getAsJsonObject().entrySet()) {
                        final ResourceLocation id = ResourceLocation.tryParse(mod.getKey() + ":" + b.getKey());
                        final int colour = b.getValue().isJsonPrimitive() ? Config.hexColor(b.getValue().getAsString()) : -1;
                        if (id != null && colour >= 0) found.put(id, colour);
                    }
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            LOG.warn("Dynamic Waters: built-in water colours could not be read: {}", e.toString());
        }
        known = java.util.Collections.unmodifiableMap(found);
        builtIn = known;
        return known;
    }

    private static int[] biome(ClientLevel level, Biome biome, Settings s) {
        final int[] known = BIOMES.get(biome);
        if (known != null) return known;
        int river = 0, own = -1;
        try {
            final Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).wrapAsHolder(biome);
            river = holder.is(BiomeTags.IS_RIVER) ? 1 : 0;
            final Integer named = holder.unwrapKey().map(k -> s.biomes().get(k.location())).orElse(null);
            if (named != null) {
                own = named;
            } else {
                for (Map.Entry<TagKey<Biome>, Integer> tag : s.tags()) {
                    if (holder.is(tag.getKey())) {
                        own = tag.getValue();
                        break;
                    }
                }
                if (own < 0) own = holder.unwrapKey().map(k -> s.builtIn().get(k.location())).orElse(-1);
            }
        } catch (RuntimeException notThere) {
        }

        final int rains = biome.hasPrecipitation() && biome.getBaseTemperature() >= 0.15F ? 1 : 0;
        final int[] is = {river, own, rains};
        BIOMES.put(biome, is);
        return is;
    }

    private static int mix(int a, int b, float t) {
        final int r = Math.round(Mth.lerp(t, a >> 16 & 255, b >> 16 & 255));
        final int g = Math.round(Mth.lerp(t, a >> 8 & 255, b >> 8 & 255));
        final int bl = Math.round(Mth.lerp(t, a & 255, b & 255));
        return (a & 0xFF000000) | r << 16 | g << 8 | bl;
    }

    private static float smooth(float t) {
        final float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }
}
