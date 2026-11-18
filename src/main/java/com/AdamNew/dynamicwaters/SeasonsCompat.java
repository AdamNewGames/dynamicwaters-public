package com.AdamNew.dynamicwaters;

import com.AdamNew.dynamicwaters.platform.Platform;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class SeasonsCompat {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final boolean LOADED = Platform.isModLoaded("sereneseasons");

    private static final int LAND_REACH = 24;

    private static volatile boolean ready, broken;
    private static Method seasonState, subSeason, tropical;

    private SeasonsCompat() {
    }

    private static boolean init() {
        if (!LOADED || broken) return false;
        if (ready) return true;
        synchronized (SeasonsCompat.class) {
            if (ready) return true;
            try {
                final Class<?> helper = Class.forName("sereneseasons.api.season.SeasonHelper");
                seasonState = helper.getMethod("getSeasonState", Level.class);
                tropical = helper.getMethod("usesTropicalSeasons", Holder.class);
                subSeason = Class.forName("sereneseasons.api.season.ISeasonState").getMethod("getSubSeason");
                ready = true;
            } catch (ReflectiveOperationException | LinkageError e) {
                broken = true;
                LOG.warn("Dynamic Waters: Serene Seasons' API not as expected, seasons ignored: {}", e.toString());
                return false;
            }
        }
        return true;
    }

    public static String subSeason(Level level) {
        if (!init()) return null;
        try {
            final Object state = seasonState.invoke(null, level);
            final Object sub = state == null ? null : subSeason.invoke(state);
            return sub instanceof Enum<?> e ? e.name() : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            LOG.warn("Dynamic Waters: Serene Seasons' season could not be read, seasons ignored: {}", e.toString());
            return null;
        }
    }

    public static boolean iceSeason(Level level) {
        if (!Config.SEASONAL_ICE_FLOES.get()) return false;
        final String sub = subSeason(level);
        return sub != null && sub.endsWith("_WINTER");
    }

    public static boolean iceSeasonAt(Level level, BlockPos pos) {
        if (!iceSeason(level)) return false;
        boolean land = false;
        for (int k = 0; k < 5; k++) {
            final BlockPos at = k == 0 ? pos : pos.offset(k == 1 ? LAND_REACH : k == 2 ? -LAND_REACH : 0, 0,
                    k == 3 ? LAND_REACH : k == 4 ? -LAND_REACH : 0);
            final Holder<Biome> biome = level.getBiome(at);
            if (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN)) continue;
            if (!wintry(biome)) return false;
            land = true;
        }
        return land || wintry(level.getBiome(pos));
    }

    private static boolean wintry(Holder<Biome> biome) {
        if (!biome.value().hasPrecipitation()) return false;
        try {
            return !(Boolean) tropical.invoke(null, biome);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return biome.value().getBaseTemperature() <= 0.8F;
        }
    }
}
