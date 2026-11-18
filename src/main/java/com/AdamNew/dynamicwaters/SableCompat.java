package com.AdamNew.dynamicwaters;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableCompat {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private SableCompat() {}

    private static volatile boolean checked;
    private static Object helper;
    private static MethodHandle containingEntity, containingPos, project;

    private static synchronized void init() {
        if (checked) return;
        try {
            final Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable");
            final Class<?> companion = Class.forName("dev.ryanhcode.sable.ActiveSableCompanion");
            final Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            containingEntity = LOOKUP.findVirtual(companion, "getContaining", MethodType.methodType(subLevel, Entity.class));
            containingPos = LOOKUP.findVirtual(companion, "getContaining",
                    MethodType.methodType(subLevel, Level.class, Position.class));
            project = LOOKUP.findVirtual(companion, "projectOutOfSubLevel",
                    MethodType.methodType(Vec3.class, Level.class, Position.class));
            helper = LOOKUP.findStaticGetter(sable, "HELPER", companion).invoke();
        } catch (ClassNotFoundException e) {
            helper = null;
        } catch (Throwable t) {
            helper = null;
            LOG.warn("Dynamic Waters: Sable's sub-levels could not be read, the current inside ships is off: {}", t.toString());
        }
        checked = true;
    }

    public static Vec3 getGlobalPos(Entity entity) {
        if (!checked) init();
        if (helper != null) {
            try {
                if (containingEntity.invoke(helper, entity) != null) {
                    return (Vec3) project.invoke(helper, entity.level(), (Position) entity.position());
                }
            } catch (Throwable ignored) {
            }
        }
        return ValkyrienCompat.worldPos(entity);
    }

    public static BlockPos getGlobalBlockPos(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        if (!checked) init();
        if (helper != null && level instanceof Level l) {
            try {
                final Position local = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (containingPos.invoke(helper, l, local) != null) {
                    final Vec3 global = (Vec3) project.invoke(helper, l, local);
                    return BlockPos.containing(global.x, global.y, global.z);
                }
            } catch (Throwable ignored) {
            }
        }
        return ValkyrienCompat.worldPos(level, pos);
    }

    private static volatile boolean physicsChecked;
    private static boolean physicsWarned;
    private static Class<?> serverSubLevel;
    private static MethodHandle getContainer, allSubLevels, logicalPose, posePosition, poseOrientation;
    private static MethodHandle massTracker, mass, handleOf, linearImpulse, latestVelocity;

    private static synchronized void initPhysics() {
        if (physicsChecked) return;
        try {
            final Class<?> container = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            final Class<?> serverContainer = Class.forName("dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer");
            final Class<?> pose = Class.forName("dev.ryanhcode.sable.companion.math.Pose3d");
            final Class<?> massData = Class.forName("dev.ryanhcode.sable.api.physics.mass.MassData");
            final Class<?> handle = Class.forName("dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle");
            final Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            getContainer = LOOKUP.findStatic(container, "getContainer", MethodType.methodType(serverContainer, ServerLevel.class));
            allSubLevels = LOOKUP.findVirtual(serverContainer, "getAllSubLevels", MethodType.methodType(List.class));
            logicalPose = LOOKUP.findVirtual(subLevel, "logicalPose", MethodType.methodType(pose));
            posePosition = LOOKUP.findVirtual(pose, "position", MethodType.methodType(Vector3d.class));
            poseOrientation = LOOKUP.findVirtual(pose, "orientation", MethodType.methodType(org.joml.Quaterniond.class));
            massTracker = LOOKUP.findVirtual(subLevel, "getMassTracker", MethodType.methodType(massData));
            mass = LOOKUP.findVirtual(massData, "getMass", MethodType.methodType(double.class));
            handleOf = LOOKUP.findStatic(handle, "of", MethodType.methodType(handle, subLevel));
            linearImpulse = LOOKUP.findVirtual(handle, "applyLinearImpulse", MethodType.methodType(void.class, Vector3dc.class));
            latestVelocity = LOOKUP.findGetter(subLevel, "latestLinearVelocity", Vector3d.class);
            serverSubLevel = subLevel;
            LOG.info("Dynamic Waters: Sable found, the rivers' current carries its ships.");
        } catch (ClassNotFoundException e) {
            serverSubLevel = null;
        } catch (Throwable t) {
            serverSubLevel = null;
            LOG.warn("Dynamic Waters: Sable's physics could not be read, ships feel no current: {}", t.toString());
        }
        physicsChecked = true;
    }

    public static void tickPhysicsShips(ServerLevel level) {
        if (!physicsChecked) initPhysics();
        if (serverSubLevel == null) return;
        try {
            final Object container = getContainer.invoke(level);
            if (container == null) return;
            final List<?> subLevels = (List<?>) allSubLevels.invoke(container);
            if (subLevels == null) return;
            for (Object ship : subLevels) {
                if (!serverSubLevel.isInstance(ship)) continue;
                final Object pose = logicalPose.invoke(ship);
                final Vector3d centre = (Vector3d) posePosition.invoke(pose);
                final Vec3 flow = HydrologyManager.getRiverFlow(centre.x, (int) centre.y, centre.z, level);
                if (flow.lengthSqr() <= 0.01) continue;

                final double mult = Config.SABLE_SHIP_SPEED.get();

                final Vector3d target = new Vector3d(flow.x * mult * 2.0, 0.0, flow.z * mult * 2.0);
                final Vector3d current = (Vector3d) latestVelocity.invoke(ship);
                final Vector3d globalDelta = new Vector3d(target.x - current.x, 0.0, target.z - current.z);
                final Quaterniondc orientation = (Quaterniondc) poseOrientation.invoke(pose);
                final Vector3d localDelta = new Vector3d();
                orientation.transformInverse(globalDelta, localDelta);
                final double shipMass = (double) mass.invoke(massTracker.invoke(ship));

                localDelta.mul(shipMass * 0.03);

                final Object handle = handleOf.invoke(ship);
                if (handle != null) linearImpulse.invoke(handle, (Vector3dc) localDelta);
            }
        } catch (Throwable t) {
            if (!physicsWarned) {
                physicsWarned = true;
                LOG.warn("Dynamic Waters: pushing Sable ships failed (logged once): {}", t.toString());
            }
        }
    }
}
