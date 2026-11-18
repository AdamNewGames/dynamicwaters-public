package com.AdamNew.dynamicwaters.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.lang.ref.WeakReference;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class RiverFoamParticle extends TextureSheetParticle {
    public enum Style {
        DRIFT(120, 80, 0.42F, 0.16F, 1.0F),

        RAPIDS(90, 60, 0.5F, 0.18F, 1.1F),

        FALL(70, 50, 0.6F, 0.2F, 0.9F);

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

    private static final double LIFT = 0.012D;

    private static final double DRIFT = 0.11D;

    private static final double TURN = 0.2D;

    private static final double SETTLE = 0.4D;

    private static final int MAX_FALL = 2;

    private static final int LIP_DROP = 2;

    private static final float FADE_IN = 0.15F;
    private static final float FADE_OUT = 0.5F;

    private static final double BANK_AHEAD = 0.15D;

    private static final int DISSOLVE_TICKS = 4;

    private static final double RANGE_SQ = 64.0D * 64.0D;

    private static volatile SpriteSet sprites;

    private static final float SPREAD = 0.5F;

    private static final float WHITE = 0.82F;

    private static final float STEEPEST = 1.0F;

    private static final float TILT = 0.3F;

    private static final int LIGHT_EVERY = 5;

    private static final int FLOW_TICKS = 5;

    private static final Long2ObjectOpenHashMap<Vec3> FLOWS = new Long2ObjectOpenHashMap<>();
    private static WeakReference<Level> flowsLevel = new WeakReference<>(null);
    private static long flowsSince = Long.MIN_VALUE;

    private final float startSize;
    private final float peakAlpha;
    private final float spin;

    private float slopeX;
    private float slopeZ;
    private float oSlopeX;
    private float oSlopeZ;
    private int light;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    private int dissolving = -1;

    public static void spawn(ClientLevel level, double x, double y, double z, double xd, double zd, float size,
            Style style) {
        final SpriteSet set = sprites;
        if (set != null && shown(level, x, y, z)) {
            net.minecraft.client.Minecraft.getInstance().particleEngine
                    .add(new RiverFoamParticle(level, x, y, z, xd, zd, size, style, set));
        }
    }

    static boolean shown(ClientLevel level, double x, double y, double z) {
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        final net.minecraft.client.ParticleStatus setting = minecraft.options.particles().get();
        if (setting == net.minecraft.client.ParticleStatus.MINIMAL
                || setting == net.minecraft.client.ParticleStatus.DECREASED && level.getRandom().nextBoolean()) {
            return false;
        }
        return minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) <= RANGE_SQ;
    }

    RiverFoamParticle(ClientLevel level, double x, double y, double z, double xd, double zd, float size,
            Style style, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd;
        this.yd = 0.0D;
        this.zd = zd;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.lifetime = style.life + this.random.nextInt(style.lifeSpread + 1);
        this.startSize = (0.11F + this.random.nextFloat() * 0.09F) * size * style.size;
        this.quadSize = this.startSize;
        this.peakAlpha = style.alpha + this.random.nextFloat() * style.alphaSpread;
        this.alpha = 0.0F;
        this.roll = this.oRoll = this.random.nextFloat() * (float) (Math.PI * 2.0D);
        this.spin = (this.random.nextFloat() - 0.5F) * 0.05F;
        final int water = BiomeColors.getAverageWaterColor(level, BlockPos.containing(x, y, z));
        this.rCol = Mth.lerp(WHITE, (water >> 16 & 255) / 255.0F, 1.0F);
        this.gCol = Mth.lerp(WHITE, (water >> 8 & 255) / 255.0F, 1.0F);
        this.bCol = Mth.lerp(WHITE, (water & 255) / 255.0F, 1.0F);
        this.light = super.getLightColor(0.0F);
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
        this.oSlopeX = this.slopeX;
        this.oSlopeZ = this.slopeZ;
        if (this.dissolving >= 0) {
            if (--this.dissolving < 0) {
                this.remove();
            }
            return;
        }
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        this.roll += this.spin;
        if (this.age % LIGHT_EVERY == 0) {
            this.light = super.getLightColor(0.0F);
        }

        FluidState water = this.waterUnder();
        if (water == null) {
            this.intoSpray(this.level.getBlockState(this.at).isAir() ? RiverMistParticle.Style.LIP
                    : RiverMistParticle.Style.BANK);
            return;
        }
        if (falling(water)) {
            if (this.fallBelow() >= LIP_DROP) {
                this.intoSpray(RiverMistParticle.Style.LIP);
                return;
            }
            this.at.set(this.probe);
            water = this.level.getFluidState(this.at);
            if (water.isEmpty()) {
                this.intoSpray(RiverMistParticle.Style.BANK);
                return;
            }
        }

        final Vec3 flow = this.flowAt(water);
        if (flow.x * flow.x + flow.z * flow.z > 1.0E-4D) {
            this.xd += (flow.x * DRIFT - this.xd) * TURN;
            this.zd += (flow.z * DRIFT - this.zd) * TURN;
        } else {
            this.xd *= this.friction;
            this.zd *= this.friction;
        }

        final double surface = this.at.getY() + water.getHeight(this.level, this.at);
        this.tiltTo(water, surface);
        final double nx = this.x + this.xd;
        final double nz = this.z + this.zd;
        if (this.bankAt(nx + Math.signum(this.xd) * BANK_AHEAD, this.z)
                || this.bankAt(nx, nz + Math.signum(this.zd) * BANK_AHEAD)) {
            this.intoSpray(RiverMistParticle.Style.BANK);
            return;
        }
        final double height = surface + LIFT + this.slopeX * (nx - (this.at.getX() + 0.5D))
                + this.slopeZ * (nz - (this.at.getZ() + 0.5D));
        this.setPos(nx, Mth.lerp(SETTLE, this.y, height), nz);
    }

    private int fallBelow() {
        this.probe.set(this.at);
        int fall = 0;
        while (fall < 8 && falling(this.level.getFluidState(this.probe))) {
            fall++;
            this.probe.move(Direction.DOWN);
        }
        return fall >= 8 || this.level.getBlockState(this.probe).isAir() ? 8 : fall;
    }

    private void intoSpray(RiverMistParticle.Style style) {
        this.dissolving = DISSOLVE_TICKS;
        if (!com.AdamNew.dynamicwaters.Config.ENABLE_RIVER_MIST.get()) {
            return;
        }
        final boolean lip = style == RiverMistParticle.Style.LIP;
        RiverMistParticle.spawn(this.level, this.x, this.y + 0.05D, this.z,
                this.xd * (lip ? 1.4D : 0.2D), lip ? -0.02D : 0.02D, this.zd * (lip ? 1.4D : 0.2D),
                0.8F + this.startSize * 2.0F, style);
    }

    private boolean bankAt(double px, double pz) {
        final int bx = Mth.floor(px);
        final int bz = Mth.floor(pz);
        if (bx == this.at.getX() && bz == this.at.getZ()) {
            return false;
        }
        this.probe.set(bx, this.at.getY(), bz);
        return this.level.getFluidState(this.probe).isEmpty() && !this.level.getBlockState(this.probe).isAir();
    }

    private FluidState waterUnder() {
        this.at.set(this.x, this.y - LIFT - 0.06D, this.z);
        FluidState water = this.level.getFluidState(this.at);

        for (int up = 0; up < 2 && !water.isEmpty(); up++) {
            final FluidState over = this.level.getFluidState(this.at.above());
            if (over.isEmpty() || falling(over)) {
                break;
            }
            this.at.move(Direction.UP);
            water = over;
        }
        for (int down = 0; water.isEmpty() && down < MAX_FALL && this.level.getBlockState(this.at).isAir(); down++) {
            this.at.move(Direction.DOWN);
            water = this.level.getFluidState(this.at);
        }
        return water.isEmpty() ? null : water;
    }

    private Vec3 flowAt(FluidState water) {
        final long now = this.level.getGameTime();
        final long key = this.at.asLong();
        synchronized (FLOWS) {
            if (flowsLevel.get() != this.level || now - flowsSince >= FLOW_TICKS || now < flowsSince) {
                FLOWS.clear();
                flowsLevel = new WeakReference<>(this.level);
                flowsSince = now;
            }
            final Vec3 known = FLOWS.get(key);
            if (known != null) {
                return known;
            }
        }
        final Vec3 flow = water.getFlow(this.level, this.at);
        synchronized (FLOWS) {
            FLOWS.put(key, flow);
        }
        return flow;
    }

    private static boolean falling(FluidState water) {
        return water.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)
                && water.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
    }

    private void tiltTo(FluidState water, double surface) {
        float toX = 0.0F;
        float toZ = 0.0F;

        if (!water.isSource()) {
            toX = (float) slope(this.surfaceBeside(-1, 0), surface, this.surfaceBeside(1, 0));
            toZ = (float) slope(this.surfaceBeside(0, -1), surface, this.surfaceBeside(0, 1));
            final float steep = Mth.sqrt(toX * toX + toZ * toZ);
            if (steep > STEEPEST) {
                toX *= STEEPEST / steep;
                toZ *= STEEPEST / steep;
            }
        }
        this.slopeX += (toX - this.slopeX) * TILT;
        this.slopeZ += (toZ - this.slopeZ) * TILT;
    }

    private double surfaceBeside(int dx, int dz) {
        final BlockPos.MutableBlockPos next = this.at.mutable().move(dx, 0, dz);
        FluidState water = this.level.getFluidState(next);
        if (water.isEmpty() && this.level.getBlockState(next).isAir()) {
            next.move(Direction.DOWN);
            water = this.level.getFluidState(next);
        }
        return water.isEmpty() ? Double.NaN : next.getY() + water.getHeight(this.level, next);
    }

    private static double slope(double before, double here, double after) {
        if (!Double.isNaN(before) && !Double.isNaN(after)) {
            return (after - before) * 0.5D;
        }
        if (!Double.isNaN(after)) {
            return after - here;
        }
        if (!Double.isNaN(before)) {
            return here - before;
        }
        return 0.0D;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return this.light;
    }

    @Override
    public void render(VertexConsumer out, Camera camera, float partialTick) {
        final float life = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float a = this.peakAlpha * ease(life / FADE_IN) * ease((1.0F - life) / FADE_OUT);
        if (this.dissolving >= 0) {
            a *= Mth.clamp((this.dissolving + 1.0F - partialTick) / (DISSOLVE_TICKS + 1.0F), 0.0F, 1.0F);
        }
        if (a < 0.01F) {
            return;
        }
        final Vec3 cam = camera.getPosition();
        final float px = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x);
        final float py = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y);
        final float pz = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z);
        final float size = this.startSize * (1.0F + SPREAD * life);

        final float sx = Mth.lerp(partialTick, this.oSlopeX, this.slopeX);
        final float sz = Mth.lerp(partialTick, this.oSlopeZ, this.slopeZ);
        final float nl = 1.0F / Mth.sqrt(sx * sx + 1.0F + sz * sz);
        final float nx = -sx * nl;
        final float ny = nl;
        final float nz = -sz * nl;

        final float turn = Mth.lerp(partialTick, this.oRoll, this.roll);
        float ux = Mth.cos(turn);
        float uy = 0.0F;
        float uz = Mth.sin(turn);
        final float along = ux * nx + uz * nz;
        ux -= along * nx;
        uy -= along * ny;
        uz -= along * nz;
        final float ul = size / Mth.sqrt(ux * ux + uy * uy + uz * uz);
        ux *= ul;
        uy *= ul;
        uz *= ul;
        final float wx = (uy * nz - uz * ny);
        final float wy = (uz * nx - ux * nz);
        final float wz = (ux * ny - uy * nx);

        final float u0 = this.getU0();
        final float u1 = this.getU1();
        final float v0 = this.getV0();
        final float v1 = this.getV1();
        final boolean fromBelow = px * nx + py * ny + pz * nz > 0.0F;
        if (!fromBelow) {
            this.corner(out, px - ux + wx, py - uy + wy, pz - uz + wz, u0, v1, a);
            this.corner(out, px + ux + wx, py + uy + wy, pz + uz + wz, u1, v1, a);
            this.corner(out, px + ux - wx, py + uy - wy, pz + uz - wz, u1, v0, a);
            this.corner(out, px - ux - wx, py - uy - wy, pz - uz - wz, u0, v0, a);
        } else {
            this.corner(out, px - ux - wx, py - uy - wy, pz - uz - wz, u0, v0, a);
            this.corner(out, px + ux - wx, py + uy - wy, pz + uz - wz, u1, v0, a);
            this.corner(out, px + ux + wx, py + uy + wy, pz + uz + wz, u1, v1, a);
            this.corner(out, px - ux + wx, py - uy + wy, pz - uz + wz, u0, v1, a);
        }
    }

    private static float ease(float t) {
        final float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    private void corner(VertexConsumer out, float x, float y, float z, float u, float v, float a) {
        //? if >=1.20.5 {
        out.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, a).setLight(this.light);
        //?} else {
        /*out.vertex(x, y, z).uv(u, v).color(this.rCol, this.gCol, this.bCol, a).uv2(this.light).endVertex();
        *///?}
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
            RiverFoamParticle.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new RiverFoamParticle(level, x, y, z, xSpeed, zSpeed, ySpeed > 0.0D ? (float) ySpeed : 1.0F,
                    Style.FALL, this.sprites);
        }
    }
}
