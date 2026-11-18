package com.AdamNew.dynamicwaters.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class RiverMistParticle extends TextureSheetParticle {
    public enum Style {
        FALL(40, 35, 0.15F, 0.1F, 1.0F),

        FALL_FINE(25, 20, 0.36F, 0.14F, 0.55F),

        BANK(45, 40, 0.12F, 0.06F, 1.3F),

        LIP(35, 30, 0.13F, 0.07F, 1.15F);

        final int life;
        final int lifeSpread;
        final float alpha;
        final float alphaSpread;
        final float size;

        Style(int life, int lifeSpread, float alpha, float alphaSpread, float size) {
            this.life = life;
            this.lifeSpread = lifeSpread;
            this.alpha = alpha;
            this.alphaSpread = alphaSpread;
            this.size = size;
        }
    }

    private static final double DRAG = 0.9D;

    private static final double RISE = 0.0016D;

    private static final double STRAY = 0.0025D;

    private static final float SWELL = 2.0F;

    private static final float FADE_IN = 0.15F;
    private static final float FADE_OUT = 0.55F;

    private static final float WHITE = 0.75F;

    private static volatile SpriteSet sprites;

    private final float startSize;
    private final float peakAlpha;
    private final float spin;

    public static void spawn(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            float size, Style style) {
        final SpriteSet set = sprites;
        if (set != null && RiverFoamParticle.shown(level, x, y, z)) {
            net.minecraft.client.Minecraft.getInstance().particleEngine
                    .add(new RiverMistParticle(level, x, y, z, xd, yd, zd, size, style, set));
        }
    }

    RiverMistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, float size,
            Style style, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.hasPhysics = true;
        this.gravity = 0.0F;
        this.lifetime = style.life + this.random.nextInt(style.lifeSpread + 1);
        this.startSize = (0.5F + this.random.nextFloat() * 0.3F) * size * style.size;
        this.quadSize = this.startSize;
        this.peakAlpha = style.alpha + this.random.nextFloat() * style.alphaSpread;
        this.alpha = 0.0F;
        this.roll = this.oRoll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.spin = (this.random.nextFloat() - 0.5F) * 0.06F;
        final int water = BiomeColors.getAverageWaterColor(level, BlockPos.containing(x, y, z));
        this.rCol = Mth.lerp(WHITE, (water >> 16 & 255) / 255.0F, 1.0F);
        this.gCol = Mth.lerp(WHITE, (water >> 8 & 255) / 255.0F, 1.0F);
        this.bCol = Mth.lerp(WHITE, (water & 255) / 255.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.roll += this.spin;
        this.xd = (this.xd + (this.random.nextDouble() - 0.5D) * 2.0D * STRAY) * DRAG;
        this.yd = (this.yd + RISE) * DRAG;
        this.zd = (this.zd + (this.random.nextDouble() - 0.5D) * 2.0D * STRAY) * DRAG;
        this.move(this.xd, this.yd, this.zd);
    }

    @Override
    public float getQuadSize(float partialTick) {
        final float life = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        return this.startSize * (1.0F + SWELL * life);
    }

    @Override
    public void render(VertexConsumer out, Camera camera, float partialTick) {
        final float life = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        this.alpha = this.peakAlpha * ease(life / FADE_IN) * ease((1.0F - life) / FADE_OUT);
        if (this.alpha >= 0.01F) {
            super.render(out, camera, partialTick);
        }
    }

    private static float ease(float t) {
        final float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
            RiverMistParticle.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new RiverMistParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, 1.0F, Style.FALL, this.sprites);
        }
    }
}
