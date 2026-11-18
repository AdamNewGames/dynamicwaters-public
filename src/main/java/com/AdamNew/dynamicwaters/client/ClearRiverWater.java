package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;

public final class ClearRiverWater {
    private static final float EASE_MS = 600.0F;

    private static float clear;
    private static long last;
    private static boolean wasUnder;
    private static BlockPos askedAt;
    private static ClientLevel askedIn;
    private static boolean river;

    private ClearRiverWater() {
    }

    public static void apply(Camera camera, float farPlaneDistance) {
        final long now = Util.getMillis();
        final float step = last == 0L ? 0.0F : Math.min(250L, now - last) / EASE_MS;
        last = now;
        if (camera.getFluidInCamera() != FogType.WATER || !Config.CLEAR_RIVER_WATER.get()) {
            wasUnder = false;
            clear = 0.0F;
            return;
        }
        if (camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        final boolean inRiver = inRiver(level, camera.getBlockPosition());
        if (!wasUnder) {
            clear = inRiver ? 1.0F : 0.0F;
            wasUnder = true;
        } else {
            clear = inRiver ? Math.min(1.0F, clear + step) : Math.max(0.0F, clear - step);
        }
        if (clear <= 0.0F) return;
        final float end = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogEnd(Mth.lerp(clear, end, Math.max(end, farPlaneDistance)));
    }

    private static boolean inRiver(ClientLevel level, BlockPos pos) {
        if (level == askedIn && pos.equals(askedAt)) return river;
        askedIn = level;
        askedAt = pos.immutable();
        river = level.getBiome(pos).is(BiomeTags.IS_RIVER)
                || HydrologyManager.seenRiverFlow(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level).lengthSqr() > 1.0E-4D;
        return river;
    }
}
