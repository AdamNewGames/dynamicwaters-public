package com.AdamNew.dynamicwaters.client;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import com.AdamNew.dynamicwaters.client.particle.RiverFoamParticle;
import com.AdamNew.dynamicwaters.client.particle.RiverMistParticle;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.ref.WeakReference;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class RiverFoam {
    private static final int DRIFT_REACH = 48;

    private static final int DRIFT_PICKS = 26;

    private static final int SPRAY_FALL = 2;

    private static final int DRY_FALL = 3;

    private static final int MOST_SPOTS = 192;

    private static final int CHECK_EVERY = 20;

    private static final int THROW_EVERY = 5;

    private static final int SPRAY_EVERY = 3;

    private static final int TALLEST = 24;

    private static final double LIFT = 0.012D;

    private static final double RAPIDS = 2.4D;

    private static final double RIVER = 0.01D;

    private static final Long2ObjectOpenHashMap<Spot> SPOTS = new Long2ObjectOpenHashMap<>();
    private static WeakReference<Level> spotsLevel = new WeakReference<>(null);

    private RiverFoam() {
    }

    private static final class Spot {
        int fall;
        boolean dry;
        long nextCheck;
        final int phase;

        Spot(int phase) {
            this.phase = phase;
        }
    }

    private static boolean falling(FluidState water) {
        return water.hasProperty(FlowingFluid.FALLING) && water.getValue(FlowingFluid.FALLING);
    }

    public static void noticeWater(Level level, BlockPos pos, FluidState water) {
        if (!water.is(FluidTags.WATER) || !Config.ENABLE_RIVER_FOAM.get() && !Config.ENABLE_RIVER_MIST.get()) {
            return;
        }
        if (falling(water)) {
            final BlockPos below = pos.below();
            final FluidState under = level.getFluidState(below);
            if (under.is(FluidTags.WATER) && !falling(under)) {
                keep(level, below);
            } else if (under.isEmpty() && !level.getBlockState(below).isAir()) {
                keep(level, pos);
            }
        } else if (falling(level.getFluidState(pos.above()))) {
            keep(level, pos);
        }
    }

    private static void keep(Level level, BlockPos foot) {
        if (spotsLevel.get() != level) {
            SPOTS.clear();
            spotsLevel = new WeakReference<>(level);
        }
        if (SPOTS.size() < MOST_SPOTS) {
            SPOTS.computeIfAbsent(foot.asLong(), k -> new Spot(level.getRandom().nextInt(THROW_EVERY)));
        }
    }

    public static void tick(ClientLevel level, int px, int py, int pz) {
        if (spotsLevel.get() != level) {
            SPOTS.clear();
            spotsLevel = new WeakReference<>(level);
        }
        final boolean foam = Config.ENABLE_RIVER_FOAM.get();
        final boolean mist = Config.ENABLE_RIVER_MIST.get();
        if (!foam && !mist) {
            SPOTS.clear();
            return;
        }
        final RandomSource random = level.getRandom();
        pickRivers(level, px, pz, foam, mist, random);
        if (SPOTS.isEmpty()) {
            return;
        }
        final long now = level.getGameTime();
        final double reachSq = (double) DRIFT_REACH * DRIFT_REACH;
        final BlockPos.MutableBlockPos foot = new BlockPos.MutableBlockPos();
        final ObjectIterator<Long2ObjectMap.Entry<Spot>> it = SPOTS.long2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            final Long2ObjectMap.Entry<Spot> entry = it.next();
            final Spot spot = entry.getValue();
            foot.set(entry.getLongKey());
            final double dx = foot.getX() + 0.5D - px;
            final double dy = foot.getY() + 0.5D - py;
            final double dz = foot.getZ() + 0.5D - pz;
            final double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > reachSq) {
                it.remove();
                continue;
            }
            if (now >= spot.nextCheck) {
                check(level, foot, spot);
                spot.nextCheck = now + CHECK_EVERY;
                if (spot.fall == 0) {
                    it.remove();
                    continue;
                }
            }
            final boolean far = distSq > reachSq * 0.25D;
            if (foam && !spot.dry && (now + spot.phase) % THROW_EVERY == 0) {
                throwFoam(level, foot, spot.fall, far, random);
            }
            if (mist && spot.fall >= SPRAY_FALL && (now + spot.phase) % SPRAY_EVERY == 0) {
                throwSpray(level, foot, spot.fall, spot.dry, far, random);
            }
        }
    }

    private static void pickRivers(ClientLevel level, int px, int pz, boolean foam, boolean mist,
            RandomSource random) {
        final int picks = Mth.clamp(DRIFT_PICKS * 50 / Math.max(1, Config.RIVER_PARTICLE_DENSITY.get()), 2, 400);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < picks; i++) {
            final double r = DRIFT_REACH * Math.sqrt(random.nextDouble());
            final double turn = random.nextDouble() * Math.PI * 2.0D;
            final int x = px + Mth.floor(Math.cos(turn) * r);
            final int z = pz + Mth.floor(Math.sin(turn) * r);
            at.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
            final FluidState water = level.getFluidState(at);
            if (!water.is(FluidTags.WATER)) {
                continue;
            }
            if (falling(water)) {
                for (int down = 0; down < TALLEST && falling(level.getFluidState(at)); down++) {
                    at.move(Direction.DOWN);
                }
                final FluidState under = level.getFluidState(at);
                if (under.is(FluidTags.WATER) && !falling(under)) {
                    keep(level, at);
                } else if (under.isEmpty() && !level.getBlockState(at).isAir()) {
                    keep(level, at.above());
                }
                continue;
            }
            final Vec3 flow = HydrologyManager.seenRiverFlow(x + 0.5D, at.getY(), z + 0.5D, level);
            final double speedSq = flow.lengthSqr();
            if (speedSq <= RIVER) {
                continue;
            }
            if (againstBank(level, at, water, flow, speedSq, mist, random)) {
                continue;
            }
            if (!foam || random.nextDouble() > Math.min(1.0D, 0.25D + 0.75D * Math.sqrt(speedSq))) {
                continue;
            }
            if (speedSq > RAPIDS) {
                onSurface(level, at, water, flow, 0.85F + random.nextFloat() * 0.35F, RiverFoamParticle.Style.RAPIDS,
                        random);
            } else {
                onSurface(level, at, water, flow, 0.75F + random.nextFloat() * 0.5F, RiverFoamParticle.Style.DRIFT,
                        random);
            }
        }
    }

    private static boolean againstBank(ClientLevel level, BlockPos pos, FluidState water, Vec3 flow, double speedSq,
            boolean mist, RandomSource random) {
        final Direction into = Direction.getNearest(flow.x, 0.0D, flow.z);
        final BlockPos bank = pos.relative(into);
        if (!level.getFluidState(bank).isEmpty() || !level.getBlockState(bank).isFaceSturdy(level, bank,
                into.getOpposite())) {
            return false;
        }
        if (!mist) {
            return true;
        }
        final double surface = pos.getY() + water.getHeight(level, pos);
        final double speed = Math.sqrt(speedSq);
        final int puffs = speedSq > RAPIDS ? 2 : 1;
        for (int i = 0; i < puffs; i++) {
            final double across = random.nextDouble() * 0.8D - 0.4D;
            final double x = pos.getX() + 0.5D + into.getStepX() * 0.25D + into.getStepZ() * across;
            final double z = pos.getZ() + 0.5D + into.getStepZ() * 0.25D + into.getStepX() * across;
            RiverMistParticle.spawn(level, x, surface + 0.05D, z, -into.getStepX() * 0.015D * speed,
                    0.015D + 0.012D * speed, -into.getStepZ() * 0.015D * speed, 0.9F + 0.2F * (float) speed,
                    RiverMistParticle.Style.BANK);
        }
        return true;
    }

    private static void check(Level level, BlockPos foot, Spot spot) {
        final FluidState here = level.getFluidState(foot);
        final BlockPos.MutableBlockPos up = foot.mutable();
        int fall = 0;
        if (here.is(FluidTags.WATER) && !falling(here)) {
            spot.dry = false;
            up.move(Direction.UP);
        } else if (falling(here) && level.getFluidState(foot.below()).isEmpty()
                && !level.getBlockState(foot.below()).isAir()) {
            spot.dry = true;
        } else {
            spot.fall = 0;
            return;
        }
        while (fall < TALLEST && falling(level.getFluidState(up))) {
            fall++;
            up.move(Direction.UP);
        }
        spot.fall = spot.dry && fall < DRY_FALL ? 0 : fall;
    }

    private static void throwSpray(ClientLevel level, BlockPos foot, int fall, boolean dry, boolean far,
            RandomSource random) {
        final double strength = 1.0D + Math.min(fall, 20) * 0.04D;
        final float size = Math.min(2.0F, 0.8F + fall * 0.06F);
        final double base;
        if (dry) {
            base = foot.getY() + 0.1D;
        } else {
            base = foot.getY() + level.getFluidState(foot).getHeight(level, foot);
        }
        final int billows = far ? 1 : fall >= 8 ? 2 : 1;
        for (int i = 0; i < billows; i++) {
            puff(level, foot, base, 0.2D, 0.5D, strength, size, RiverMistParticle.Style.FALL, random);
        }
        final int fine = far ? 0 : fall >= 6 ? 2 : 1;
        for (int i = 0; i < fine; i++) {
            puff(level, foot, base, 0.1D, 0.5D, strength * 1.4D, size, RiverMistParticle.Style.FALL_FINE, random);
        }
    }

    private static void puff(ClientLevel level, BlockPos foot, double base, double near, double spread,
            double strength, float size, RiverMistParticle.Style style, RandomSource random) {
        final double turn = random.nextDouble() * Math.PI * 2.0D;
        final double cos = Math.cos(turn);
        final double sin = Math.sin(turn);
        final double r = near + random.nextDouble() * spread;
        final double out = (0.03D + random.nextDouble() * 0.04D) * strength;
        final double up = (0.02D + random.nextDouble() * 0.03D) * strength;
        RiverMistParticle.spawn(level, foot.getX() + 0.5D + cos * r, base, foot.getZ() + 0.5D + sin * r,
                cos * out, up, sin * out, size * (0.85F + random.nextFloat() * 0.3F), style);
    }

    private static void throwFoam(ClientLevel level, BlockPos foot, int fall, boolean far, RandomSource random) {
        int count = Math.min(4, 1 + fall / 5);
        if (far) {
            count = Math.max(1, count / 2);
        }
        final float size = Math.min(1.4F, 0.85F + fall * 0.03F);
        final double push = 0.04D + Math.min(fall, 16) * 0.005D;
        final BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            final Direction way = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            side.setWithOffset(foot, way);
            final FluidState water = level.getFluidState(side);
            if (water.isEmpty() || !level.getFluidState(side.above()).isEmpty()) {
                continue;
            }
            final double across = random.nextDouble() * 0.9D - 0.45D;
            final double x = foot.getX() + 0.5D + way.getStepX() * 0.6D + way.getStepZ() * across;
            final double z = foot.getZ() + 0.5D + way.getStepZ() * 0.6D + way.getStepX() * across;
            final double y = side.getY() + water.getHeight(level, side) + LIFT;
            final double speed = push * (0.7D + random.nextDouble() * 0.8D);
            RiverFoamParticle.spawn(level, x, y, z, way.getStepX() * speed, way.getStepZ() * speed,
                    size * (0.85F + random.nextFloat() * 0.3F), RiverFoamParticle.Style.FALL);
        }
    }

    private static void onSurface(ClientLevel level, BlockPos pos, FluidState water, Vec3 flow, float size,
            RiverFoamParticle.Style style, RandomSource random) {
        final double y = pos.getY() + water.getHeight(level, pos) + LIFT;
        RiverFoamParticle.spawn(level, pos.getX() + random.nextDouble(), y, pos.getZ() + random.nextDouble(),
                flow.x * 0.08D, flow.z * 0.08D, size, style);
    }
}
