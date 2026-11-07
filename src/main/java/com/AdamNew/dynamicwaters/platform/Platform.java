package com.AdamNew.dynamicwaters.platform;

import com.AdamNew.dynamicwaters.network.Payload;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Platform {
    private Platform() {
    }

    public static boolean isModLoaded(String modId) {
        //? neoforge {
        return net.neoforged.fml.ModList.get().isLoaded(modId);
        //?} forge {
        /*return net.minecraftforge.fml.ModList.get().isLoaded(modId);
        *///?} fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
        *///?}
    }

    public static Path configDir() {
        //? neoforge {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        //?} forge {
        /*return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        *///?} fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        *///?}
    }

    public static boolean isClient() {
        //? neoforge {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient();
        //?} forge {
        /*return net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient();
        *///?} fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
        *///?}
    }

    public static String modVersion(String modId) {
        //? neoforge {
        return net.neoforged.fml.ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
        //?} forge {
        /*return net.minecraftforge.fml.ModList.get().getModContainerById(modId)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("absent");
        *///?} fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("absent");
        *///?}
    }

    public static boolean isOwnPlayer(ServerPlayer player) {
        //? neoforge {
        return player.connection.getConnection().isMemoryConnection();
        //?} else {
        /*return player.connection.connection.isMemoryConnection();
        *///?}
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload) {
        //? neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload);
        //?} forge {
        /*com.AdamNew.dynamicwaters.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), payload);
        *///?} fabric {
        /*net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        *///?}
    }

    public static void sendToLevel(ServerLevel level, Payload payload) {
        //? neoforge {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);
        //?} forge {
        /*com.AdamNew.dynamicwaters.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.DIMENSION.with(level::dimension), payload);
        *///?} fabric {
        /*for (ServerPlayer player : level.players()) sendToPlayer(player, payload);
        *///?}
    }
}
