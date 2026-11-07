package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.entity.IceFloe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;

public class IceFloeRenderer extends EntityRenderer<IceFloe> {
    private static final float TWO_PLATES_FROM = 0.5f;

    private final BlockRenderDispatcher blocks;

    public IceFloeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blocks = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(IceFloe floe, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        final float size = floe.size(), high = IceFloe.thickness(size);
        final long shape = it.unimi.dsi.fastutil.HashCommon.mix(floe.getUUID().getLeastSignificantBits());
        final float stretch = 1.0f + 0.3f * share(shape, 0);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-floe.getYRot()));
        plate(pose, buffers, light, size * stretch, high, size / stretch, 0.0f, 0.0f, 0.0f, 0.0f);
        if (size >= TWO_PLATES_FROM) {
            final float side = size * (0.5f + 0.3f * share(shape, 1));
            final float low = high * 0.75f;
            final float angle = share(shape, 2) * (float) (Math.PI * 2.0);
            final float out = size * 0.45f;
            plate(pose, buffers, light, side, low, side * 0.8f, (float) Math.cos(angle) * out, high - low,
                    (float) Math.sin(angle) * out, 15.0f + 50.0f * share(shape, 3));
        }
        pose.popPose();
        super.render(floe, yaw, partialTick, pose, buffers, light);
    }

    private static float share(long shape, int slot) {
        return ((shape >>> (slot * 8)) & 0xFF) / 255.0f;
    }

    private void plate(PoseStack pose, MultiBufferSource buffers, int light, float wide, float high, float deep, float x, float y,
            float z, float turn) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(turn));
        pose.scale(wide, high, deep);
        pose.translate(-0.5, 0.0, -0.5);
        blocks.renderSingleBlock(Blocks.ICE.defaultBlockState(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(IceFloe floe) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
