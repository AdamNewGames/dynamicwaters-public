package com.AdamNew.dynamicwaters;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldShape {
    private static final String DATA_NAME = "dynamicwaters_world";

    public record Scale(double lowlandWidth, double lowlandDepth, double mountainWidth, double mountainDepth) {
        public static final Scale DEFAULT = new Scale(1.0, 1.0, 1.0, 1.0);
    }

    private WorldShape() {
    }

    public static Config.LowlandValley valley(ServerLevel overworld) {
        final Data data = data(overworld);
        if (data.valley == null) {
            data.valley = overworld.getGameTime() == 0L ? Config.LOWLAND_RIVER_VALLEY.get() : Config.LowlandValley.GORGE;
            data.setDirty();
        }
        return data.valley;
    }

    public static com.AdamNew.dynamicwaters.terrain.LowlandCut.Settings cut(ServerLevel overworld) {
        final Config.LowlandValley mode = valley(overworld);
        final Data data = data(overworld);
        if (data.cutBy == null) {
            final boolean fresh = overworld.getGameTime() == 0L;
            data.cutBy = fresh ? Config.LOWLAND_CUT_BY.get() : Config.LowlandCutBy.HEIGHT;
            data.valleyBelow = fresh ? Config.LOWLAND_VALLEY_BELOW.get() : 100.0;
            data.tunnelAbove = fresh ? Config.LOWLAND_TUNNEL_ABOVE.get() : 150.0;
            data.valleyBiomes = fresh ? java.util.List.copyOf(Config.LOWLAND_VALLEY_BIOMES.get()) : java.util.List.of();
            data.tunnelBiomes = fresh ? java.util.List.copyOf(Config.LOWLAND_TUNNEL_BIOMES.get()) : java.util.List.of();
            data.setDirty();
        }
        return new com.AdamNew.dynamicwaters.terrain.LowlandCut.Settings(mode, data.cutBy, data.valleyBelow,
                data.tunnelAbove, data.valleyBiomes, data.tunnelBiomes);
    }

    public static Scale scale(ServerLevel overworld) {
        final Data data = data(overworld);
        if (data.scale == null) {
            data.scale = overworld.getGameTime() == 0L
                    ? new Scale(Config.LOWLAND_RIVER_WIDTH.get(), Config.LOWLAND_RIVER_DEPTH.get(),
                            Config.MOUNTAIN_RIVER_WIDTH.get(), Config.MOUNTAIN_RIVER_DEPTH.get())
                    : Scale.DEFAULT;
            data.setDirty();
        }
        return data.scale;
    }

    private static Data data(ServerLevel level) {
        //? if >=1.20.5 {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), DATA_NAME);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(Data::load, Data::new, DATA_NAME);
        *///?}
    }

    public static final class Data extends SavedData {
        Config.LowlandValley valley;
        Config.LowlandCutBy cutBy;
        double valleyBelow, tunnelAbove;
        java.util.List<String> valleyBiomes = java.util.List.of(), tunnelBiomes = java.util.List.of();
        Scale scale;

        //? if >=1.20.5 {
        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
        //?} else {
        /*static Data load(CompoundTag tag) {
        *///?}
            final Data data = new Data();
            final String valley = tag.getString("LowlandRiverValley");
            for (Config.LowlandValley v : Config.LowlandValley.values()) {
                if (v.name().equals(valley)) data.valley = v;
            }
            if (tag.contains("LowlandRiverWidth")) {
                data.scale = new Scale(tag.getDouble("LowlandRiverWidth"), tag.getDouble("LowlandRiverDepth"),
                        tag.getDouble("MountainRiverWidth"), tag.getDouble("MountainRiverDepth"));
            }
            final String by = tag.getString("LowlandRiverCutBy");
            for (Config.LowlandCutBy b : Config.LowlandCutBy.values()) {
                if (b.name().equals(by)) data.cutBy = b;
            }
            if (data.cutBy != null) {
                data.valleyBelow = tag.getDouble("LowlandValleyBelow");
                data.tunnelAbove = tag.getDouble("LowlandTunnelAbove");
                data.valleyBiomes = splitList(tag.getString("LowlandValleyBiomes"));
                data.tunnelBiomes = splitList(tag.getString("LowlandTunnelBiomes"));
            }
            return data;
        }

        @Override
        //? if >=1.20.5 {
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        //?} else {
        /*public CompoundTag save(CompoundTag tag) {
        *///?}
            if (valley != null) tag.putString("LowlandRiverValley", valley.name());
            if (scale != null) {
                tag.putDouble("LowlandRiverWidth", scale.lowlandWidth());
                tag.putDouble("LowlandRiverDepth", scale.lowlandDepth());
                tag.putDouble("MountainRiverWidth", scale.mountainWidth());
                tag.putDouble("MountainRiverDepth", scale.mountainDepth());
            }
            if (cutBy != null) {
                tag.putString("LowlandRiverCutBy", cutBy.name());
                tag.putDouble("LowlandValleyBelow", valleyBelow);
                tag.putDouble("LowlandTunnelAbove", tunnelAbove);
                tag.putString("LowlandValleyBiomes", String.join(",", valleyBiomes));
                tag.putString("LowlandTunnelBiomes", String.join(",", tunnelBiomes));
            }
            return tag;
        }

        private static java.util.List<String> splitList(String saved) {
            if (saved.isEmpty()) return java.util.List.of();
            final java.util.List<String> out = new java.util.ArrayList<>();
            for (String s : saved.split(",")) {
                if (!s.isBlank()) out.add(s.trim());
            }
            return java.util.List.copyOf(out);
        }
    }
}
