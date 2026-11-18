package com.AdamNew.dynamicwaters.command;

import com.AdamNew.dynamicwaters.ChunkRegen;
import com.AdamNew.dynamicwaters.shore.RiverbankPalettes;
import com.AdamNew.dynamicwaters.shore.WaterPlantSets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.ChunkPos;

public final class ReloadCommand {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final int MOST = 16;
    private static final long CONFIRM_MILLIS = 30_000L;

    private record Asked(String who, int radius, long until) {
    }

    private static Asked asked;

    private ReloadCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dynamicwaters").then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> reload(context.getSource()))
                .then(Commands.argument("radiusInChunks", IntegerArgumentType.integer(1, MOST))
                        .executes(context -> regenerate(context.getSource(),
                                IntegerArgumentType.getInteger(context, "radiusInChunks"))))));
    }

    private static int reload(CommandSourceStack source) {
        reloadData();
        source.sendSuccess(() -> Component.translatable("dynamicwaters.reload.done"), true);
        return 1;
    }

    private static void reloadData() {
        RiverbankPalettes.reload();
        WaterPlantSets.reload();
    }

    private static int regenerate(CommandSourceStack source, int radius) {
        final int side = 2 * radius + 1, chunks = side * side;
        final long now = System.currentTimeMillis();
        final Asked before = asked;
        if (before == null || !before.who().equals(source.getTextName()) || before.radius() != radius || now > before.until()) {
            asked = new Asked(source.getTextName(), radius, now + CONFIRM_MILLIS);
            final Component confirm = Component.translatable("dynamicwaters.reload.confirm").withStyle(style -> style
                    .withColor(ChatFormatting.RED).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dynamicwaters reload " + radius))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("dynamicwaters.reload.confirm.hover"))));
            source.sendSystemMessage(Component.translatable("dynamicwaters.reload.warning", side, radius, chunks)
                    .withStyle(ChatFormatting.GOLD).append(" ").append(confirm));
            return 0;
        }
        asked = null;
        reloadData();
        final ChunkPos center = new ChunkPos(BlockPos.containing(source.getPosition()));
        final long t0 = System.nanoTime();
        final long changed;
        try {
            changed = ChunkRegen.regenerate(source.getLevel(), center, radius);
        } catch (RuntimeException e) {
            LOG.error("Dynamic Waters: regenerating {} chunks round {} failed", chunks, center, e);
            source.sendFailure(Component.translatable("dynamicwaters.reload.failed", e.toString()));
            return 0;
        }
        final String seconds = String.format(java.util.Locale.ROOT, "%.1f", (System.nanoTime() - t0) / 1.0e9);
        LOG.info("Dynamic Waters: regenerated {} chunks round {}: {} blocks changed in {} s", chunks, center, changed, seconds);
        source.sendSuccess(() -> Component.translatable("dynamicwaters.reload.regenerated", side, changed, seconds), true);
        return 1;
    }
}
