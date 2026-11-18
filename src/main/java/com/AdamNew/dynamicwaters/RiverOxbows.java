package com.AdamNew.dynamicwaters;

import com.AdamNew.dynamicwaters.river.RiverGraphEdge;
import com.AdamNew.dynamicwaters.terrain.CoarseHeightField;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RiverOxbows {
    static final double OXBOW_STEP = 80.0;
    static final double OXBOW_CHANCE = 0.35;
    static final double OXBOW_CLEAR = 24.0;
    static final double OXBOW_BEND = 0.12;
    static final double OXBOW_BEND_SPAN = 24.0;
    static final double OXBOW_GAP = 8.0;
    static final double OXBOW_SPAN = Math.toRadians(125.0);
    static final int OXBOW_DEPTH = 3;
    static final int OXBOW_MAX_RISE = 8;

    static final double BANK = OXBOW_MAX_RISE + 1.0;

    static final double OXBOW_MOUNTAIN_CLEAR = 4.0;

    public record Oxbow(double ox, double oz, double radius, double away, double halfWidth) {
        double reach() {
            return radius + halfWidth + BANK;
        }
    }

    private static final Oxbow[] NONE = new Oxbow[0];
    private static final Map<RiverGraphEdge, Oxbow[]> OXBOWS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final boolean LAB = Boolean.getBoolean("dynamicwaters.labOxbows");

    private RiverOxbows() {
    }

    static void carve(HydrologyManager.RiverCarving result, List<RiverGraphEdge> edges, double x, double z, int originalY,
            int seaLevel, ChunkGenerator generator, RandomState randomState) {
        double best = Double.MAX_VALUE;
        Oxbow at = null;
        for (RiverGraphEdge edge : edges) {
            for (Oxbow o : oxbowsOf(edge, seaLevel, generator, randomState)) {
                final double dx = x - o.ox(), dz = z - o.oz();
                if (dx * dx + dz * dz > (o.reach() + o.radius()) * (o.reach() + o.radius())) continue;
                final double d = ringDistance(o, x, z) - widthAt(o, x, z);
                if (d < best) {
                    best = d;
                    at = o;
                }
            }
        }
        if (at == null || best >= BANK) return;
        final int water = seaLevel - 1;
        if (best < 0.0) {
            final double hw = Math.max(0.5, widthAt(at, x, z));
            final double t = Mth.clamp(1.0 + best / hw, 0.0, 1.0);
            final int bed = water - Math.max(1, (int) Math.round(OXBOW_DEPTH * (1.0 - t * t)));
            result.modifies = true;
            result.oxbow = true;
            result.targetY = Math.min(result.targetY, Math.min(originalY, bed));
            result.waterLevel = water;
            result.skinLevel = Math.max(result.skinLevel, water);
            result.cavernRoofY = Math.max(result.cavernRoofY, HydrologyManager.skyY());
        } else {
            final int bank = water + 1 + (int) Math.floor(best);
            if (bank >= originalY) return;
            result.modifies = true;
            result.targetY = Math.min(result.targetY, bank);
            result.skinLevel = Math.max(result.skinLevel, water);
            result.cavernRoofY = Math.max(result.cavernRoofY, HydrologyManager.skyY());
            if (bank < originalY - 1) result.dressCut = true;
        }
    }

    private static double ringDistance(Oxbow o, double x, double z) {
        final double dx = x - o.ox(), dz = z - o.oz();
        final double a = Mth.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx) - o.away())) * Math.PI / 180.0;
        if (Math.abs(a) <= OXBOW_SPAN) return Math.abs(Math.hypot(dx, dz) - radiusAt(o, a));
        final double end = Math.copySign(OXBOW_SPAN, a);
        final double r = radiusAt(o, end);
        return Math.hypot(x - (o.ox() + r * Math.cos(o.away() + end)), z - (o.oz() + r * Math.sin(o.away() + end)));
    }

    private static double radiusAt(Oxbow o, double a) {
        final double phase = (Math.floorMod((long) Math.floor(o.ox()) * 31L + (long) Math.floor(o.oz()), 628L)) / 100.0;
        return o.radius() * (1.0 + WOBBLE * Math.sin(3.0 * a + phase) + 0.5 * WOBBLE * Math.sin(5.0 * a - phase));
    }

    static final double WOBBLE = 0.07;

    private static double widthAt(Oxbow o, double x, double z) {
        final double a = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(z - o.oz(), x - o.ox()) - o.away())) * Math.PI / 180.0);
        final double t = Mth.clamp(a / OXBOW_SPAN, 0.0, 1.0);
        return o.halfWidth() * (1.0 - 0.45 * t * t * t);
    }

    static Oxbow[] oxbowsOf(RiverGraphEdge edge, int seaLevel, ChunkGenerator generator, RandomState randomState) {
        if (edge.delta() || generator == null || randomState == null) return NONE;
        Oxbow[] known = OXBOWS.get(edge);
        if (known != null) return known;
        final double[] s = edge.fractal().segments;
        final int n = s.length / 2;
        if (n < 3) return NONE;
        final double[] along = new double[n];
        for (int i = 1; i < n; i++) along[i] = along[i - 1] + Math.hypot(s[2 * i] - s[2 * i - 2], s[2 * i + 1] - s[2 * i - 1]);
        final double total = along[n - 1];
        final long seed = HydrologyManager.worldSeed() ^ Math.round(edge.source().x()) * 0xBF58476D1CE4E5B9L
                ^ Math.round(edge.source().y()) * 0x94D049BB133111EBL;
        final List<Oxbow> found = new ArrayList<>();
        final double clearStart = edge.sourceEdge() ? OXBOW_CLEAR : 110.0;
        int k = 0;
        for (double at = clearStart; at <= total - OXBOW_CLEAR; at += OXBOW_STEP, k++) {
            if (draw(seed, 3 * k) >= OXBOW_CHANCE) continue;
            final double[] p = pointAt(s, along, at);
            final double[] a = pointAt(s, along, at - OXBOW_BEND_SPAN), b = pointAt(s, along, at + OXBOW_BEND_SPAN);
            final double ax = p[0] - a[0], az = p[1] - a[1], bx = b[0] - p[0], bz = b[1] - p[1];
            final double la = Math.hypot(ax, az), lb = Math.hypot(bx, bz);
            if (la < 1.0E-6 || lb < 1.0E-6) continue;
            final double turn = Math.atan2(ax * bz - az * bx, ax * bx + az * bz);
            if (Math.abs(turn) < OXBOW_BEND) {
                if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: oxbow candidate {} {} bend {}",
                        Math.round(p[0]), Math.round(p[1]), String.format(java.util.Locale.ROOT, "%.2f", turn));
                continue;
            }

            final double tx = (ax / la + bx / lb), tz = (az / la + bz / lb), tl = Math.hypot(tx, tz);
            if (tl < 1.0E-6) continue;
            final double nx = turn > 0.0 ? tz / tl : -tz / tl, nz = turn > 0.0 ? -tx / tl : tx / tl;
            final double radius = 11.0 + 7.0 * draw(seed, 3 * k + 1);
            final double halfWidth = 3.0 + 2.0 * draw(seed, 3 * k + 2);
            final double riverWater = HydrologyManager.normalRiverWaterRadius(edge.widthAt(p[0], p[1]));
            final double off = riverWater + OXBOW_GAP + halfWidth + radius * -Math.cos(OXBOW_SPAN);
            final Oxbow o = new Oxbow(p[0] + nx * off, p[1] + nz * off, radius, Math.atan2(nz, nx), halfWidth);
            if (fits(o, seaLevel, generator, randomState)) found.add(o);
        }
        known = found.isEmpty() ? NONE : found.toArray(new Oxbow[0]);
        if (OXBOWS.put(edge, known) == null && LAB) {
            for (Oxbow o : known) {
                com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: oxbow at {} {} radius {}",
                        Math.round(o.ox()), Math.round(o.oz()), Math.round(o.radius()));
            }
        }
        return known;
    }

    private static boolean fits(Oxbow o, int seaLevel, ChunkGenerator generator, RandomState randomState) {
        MainRivers.findNearest(o.ox(), o.oz(), generator, HydrologyManager.getDummyAccessor(), randomState);
        for (int i = 0; i <= 12; i++) {
            final double ang = o.away() - OXBOW_SPAN + 2.0 * OXBOW_SPAN * i / 12.0;
            for (int ring = -1; ring <= 1; ring++) {
                final double r = o.radius() + ring * (o.halfWidth() + BANK);
                final double x = o.ox() + r * Math.cos(ang), z = o.oz() + r * Math.sin(ang);
                final int ground = CoarseHeightField.surfaceAt(Mth.floor(x), Mth.floor(z), generator, randomState);
                if (ground < seaLevel - 1 || ground > seaLevel - 1 + OXBOW_MAX_RISE) {
                    if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: oxbow candidate {} {} ground {}",
                            Math.round(o.ox()), Math.round(o.oz()), ground);
                    return false;
                }

                if (HydrologyManager.mountainChannelNear(x, z, OXBOW_MOUNTAIN_CLEAR, generator, randomState)) {
                    if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: oxbow candidate {} {} mountain river",
                            Math.round(o.ox()), Math.round(o.oz()));
                    return false;
                }
                if (ring != 0) continue;
                for (RiverGraphEdge e : MainRivers.riversAt(x, z)) {
                    final double d = Math.sqrt(e.fractal().intersectDistance(x, z));
                    final double water = e.delta() ? e.deltaRadius(x, z) : HydrologyManager.normalRiverWaterRadius(e.widthAt(x, z));
                    if (d < water + OXBOW_GAP + o.halfWidth() - 1.0) {
                        if (LAB) com.mojang.logging.LogUtils.getLogger().info("Dynamic Waters lab: oxbow candidate {} {} river {}",
                                Math.round(o.ox()), Math.round(o.oz()), Math.round(d - water));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static double[] pointAt(double[] s, double[] along, double at) {
        final int n = along.length;
        if (at <= 0.0) return new double[] {s[0], s[1]};
        for (int i = 1; i < n; i++) {
            if (along[i] >= at) {
                final double seg = along[i] - along[i - 1];
                final double t = seg > 0.0 ? (at - along[i - 1]) / seg : 0.0;
                return new double[] {Mth.lerp(t, s[2 * i - 2], s[2 * i]), Mth.lerp(t, s[2 * i - 1], s[2 * i + 1])};
            }
        }
        return new double[] {s[2 * n - 2], s[2 * n - 1]};
    }

    private static double draw(long seed, int k) {
        long v = seed + k * 0xD1B54A32D192ED03L;
        v ^= v >>> 33;
        v *= 0xFF51AFD7ED558CCDL;
        v ^= v >>> 33;
        v *= 0xC4CEB9FE1A85EC53L;
        v ^= v >>> 33;
        return (v >>> 11) * 0x1.0p-53;
    }
}
