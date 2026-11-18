package com.AdamNew.dynamicwaters.command;

import com.AdamNew.dynamicwaters.HydrologyManager;
import com.AdamNew.dynamicwaters.MainRivers;
import com.AdamNew.dynamicwaters.WaterfallCaves;
import com.AdamNew.dynamicwaters.WatershedPath;
import com.AdamNew.dynamicwaters.river.RiverGraphEdge;
import com.AdamNew.dynamicwaters.terrain.CoarseHeightField;
import com.AdamNew.dynamicwaters.terrain.WatershedLake;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

public final class RiverLocateCommand {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final int MOUNTAIN_REACH = 4;

    private static final int LOWLAND_REACH = 1;

    private static final int FALL_MIN = 6;

    private static final int LAKE_MIN_CELLS = 8;

    private static final double GORGE_MIN = 0.5;

    private enum Kind {
        WATERFALL, CAVE, LAKE, DELTA, GORGE;

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        boolean lowland() {
            return this == DELTA || this == GORGE;
        }
    }

    private RiverLocateCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> locate = Commands.literal("locate").requires(s -> s.hasPermission(2));
        for (Kind kind : Kind.values()) {
            locate.then(Commands.literal(kind.id()).executes(context -> run(context.getSource(), kind)));
        }
        dispatcher.register(Commands.literal("dynamicwaters").then(locate));
    }

    private static int run(CommandSourceStack source, Kind kind) {
        final ServerLevel level = source.getLevel();
        if (!HydrologyManager.riversIn(level)) {
            source.sendFailure(Component.translatable("dynamicwaters.locate.overworld"));
            return 0;
        }
        final Vec3 pos = source.getPosition();
        final ChunkGenerator generator = level.getChunkSource().getGenerator();
        final RandomState randomState = level.getChunkSource().randomState();
        final int seaLevel = level.getSeaLevel();
        final MinecraftServer server = source.getServer();
        final Component what = Component.translatable("dynamicwaters.locate.kind." + kind.id());
        source.sendSystemMessage(Component.translatable("dynamicwaters.locate.searching", what));
        CompletableFuture.supplyAsync(() -> kind.lowland()
                        ? findLowland(kind, pos.x, pos.z, generator, randomState, seaLevel)
                        : findMountain(kind, pos.x, pos.z, generator, randomState, seaLevel))
                .whenComplete((found, error) -> server.execute(() -> report(source, kind, what, pos, found, error)));
        return 1;
    }

    private static void report(CommandSourceStack source, Kind kind, Component what, Vec3 pos, int[] found, Throwable error) {
        if (error != null || found == null) {
            final int reach = kind.lowland() ? (2 * LOWLAND_REACH + 1) * MainRivers.REGION_SIZE / 2
                    : MOUNTAIN_REACH * HydrologyManager.WATERSHED_REGION_SIZE;
            LOG.info("Dynamic Waters: no {} within {} blocks of {} {}{}", kind.id(), reach, Mth.floor(pos.x), Mth.floor(pos.z),
                    error != null ? " (" + error + ")" : "");
            source.sendFailure(Component.translatable("dynamicwaters.locate.none", what, reach));
            return;
        }
        final int distance = (int) Math.round(Math.hypot(found[0] + 0.5 - pos.x, found[2] + 0.5 - pos.z));
        LOG.info("Dynamic Waters: nearest {} to {} {} at {} {} {}, {} blocks away", kind.id(), Mth.floor(pos.x),
                Mth.floor(pos.z), found[0], found[1], found[2], distance);
        final String tp = "/tp @s " + found[3] + " " + found[4] + " " + found[5];
        final Component coordinates = ComponentUtils.wrapInSquareBrackets(
                Component.translatable("chat.coordinates", found[0], found[1], found[2])).withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, tp))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        source.sendSuccess(() -> Component.translatable("dynamicwaters.locate.found", what, coordinates, distance), false);
    }

    private static int[] findMountain(Kind kind, double x, double z, ChunkGenerator generator, RandomState randomState,
            int seaLevel) {
        final int size = HydrologyManager.WATERSHED_REGION_SIZE;
        final int hx = Math.floorDiv(Mth.floor(x), size), hz = Math.floorDiv(Mth.floor(z), size);
        int[] best = null;
        double bestSq = Double.MAX_VALUE;
        for (int ring = 0; ring <= MOUNTAIN_REACH; ring++) {
            if (best != null && Math.sqrt(bestSq) <= (ring - 1) * (double) size) break;
            for (int rx = hx - ring; rx <= hx + ring; rx++) {
                for (int rz = hz - ring; rz <= hz + ring; rz++) {
                    if (Math.max(Math.abs(rx - hx), Math.abs(rz - hz)) != ring) continue;
                    final HydrologyManager.WatershedRegionData data = HydrologyManager.getWatershedRegion(rx, rz, generator,
                            HydrologyManager.getDummyAccessor(), randomState);
                    if (kind == Kind.LAKE) {
                        for (WatershedLake lake : data.lakes) {
                            final int[] at = nearestOfLake(lake, x, z, seaLevel);
                            if (at == null) continue;
                            final double d = sq(at[0] - x, at[2] - z);
                            if (d < bestSq) {
                                bestSq = d;
                                best = at;
                            }
                        }
                        continue;
                    }
                    for (WatershedPath p : data.allPaths) {
                        Boolean carved = null;
                        for (int f = 0; f + 1 < p.validNodeCount; f++) {
                            if (!WatershedPath.isFall(p.segmentsY, f) || p.segmentsY[f + 1] - p.segmentsY[f] < FALL_MIN) continue;
                            final double dx = p.segmentsX[f + 1] - p.segmentsX[f], dz = p.segmentsZ[f + 1] - p.segmentsZ[f];
                            final double len = Math.hypot(dx, dz);
                            if (len < 1.0E-6) continue;
                            final double fx = p.segmentsX[f] + WatershedPath.FALL_FACE_T * dx;
                            final double fz = p.segmentsZ[f] + WatershedPath.FALL_FACE_T * dz;
                            final double d = sq(fx - x, fz - z);
                            if (d >= bestSq) continue;
                            if (kind == Kind.CAVE && !WaterfallCaves.planned(p, f)) continue;
                            if (carved == null) carved = HydrologyManager.carvedRiver(p, generator, randomState);
                            if (!carved) break;
                            final double ux = dx / len, uz = dz / len;
                            final int low = p.segmentsY[f], high = p.segmentsY[f + 1];
                            bestSq = d;

                            final double back = kind == Kind.CAVE ? 2.0 : 8.0;
                            best = new int[] { Mth.floor(fx), kind == Kind.CAVE ? low + 1 : high, Mth.floor(fz),
                                    Mth.floor(fx - ux * back), low + 1, Mth.floor(fz - uz * back) };
                        }
                    }
                }
            }
        }
        return best;
    }

    private static int[] nearestOfLake(WatershedLake lake, double x, double z, int seaLevel) {
        if (!(lake.waterY > seaLevel - 1 || lake.dig > WatershedLake.SHAVE)) return null;
        final int cell = CoarseHeightField.CELL_SIZE;
        int cells = 0, bx = 0, bz = 0;
        double near = Double.MAX_VALUE;
        for (int j = 0; j < lake.cellsH; j++) {
            for (int i = 0; i < lake.cellsW; i++) {
                final int k = j * lake.cellsW + i;
                if ((lake.bits[k >>> 6] & (1L << (k & 63))) == 0) continue;
                cells++;
                final int cx = (lake.minCellX + i) * cell + cell / 2, cz = (lake.minCellZ + j) * cell + cell / 2;
                final double d = sq(cx - x, cz - z);
                if (d < near) {
                    near = d;
                    bx = cx;
                    bz = cz;
                }
            }
        }
        if (cells < LAKE_MIN_CELLS) return null;
        return new int[] { bx, lake.waterY, bz, bx, lake.waterY + 1, bz };
    }

    private static int[] findLowland(Kind kind, double x, double z, ChunkGenerator generator, RandomState randomState,
            int seaLevel) {
        final int size = MainRivers.REGION_SIZE;
        final int hx = Math.floorDiv(Mth.floor(x), size), hz = Math.floorDiv(Mth.floor(z), size);
        final double[] on = new double[2], ground = new double[1];
        int[] best = null;
        double bestSq = Double.MAX_VALUE;
        for (int rx = hx - LOWLAND_REACH; rx <= hx + LOWLAND_REACH; rx++) {
            for (int rz = hz - LOWLAND_REACH; rz <= hz + LOWLAND_REACH; rz++) {
                for (RiverGraphEdge edge : MainRivers.getRegion(rx, rz, generator, HydrologyManager.getDummyAccessor(), randomState)) {
                    if (kind == Kind.DELTA) {
                        if (!edge.delta()) continue;
                        final double d = edge.fractal().nearestPoint(x, z, on);
                        if (d >= bestSq) continue;
                        bestSq = d;
                        best = new int[] { Mth.floor(on[0]), seaLevel - 1, Mth.floor(on[1]),
                                Mth.floor(on[0]), seaLevel + 1, Mth.floor(on[1]) };
                        continue;
                    }
                    if (edge.delta() || edge.fractal().boxDistanceSq(x, z) >= bestSq) continue;
                    final double[] s = edge.fractal().segments;
                    for (int i = 0; i + 1 < s.length; i += 2) {
                        final double d = sq(s[i] - x, s[i + 1] - z);
                        if (d >= bestSq) continue;
                        if (HydrologyManager.lowlandGorgeAt(edge, s[i], s[i + 1], generator, randomState, ground) < GORGE_MIN) continue;
                        bestSq = d;
                        best = new int[] { Mth.floor(s[i]), seaLevel - 1, Mth.floor(s[i + 1]),
                                Mth.floor(s[i]), seaLevel + 1, Mth.floor(s[i + 1]) };
                    }
                }
            }
        }
        return best;
    }

    private static double sq(double dx, double dz) {
        return dx * dx + dz * dz;
    }
}
