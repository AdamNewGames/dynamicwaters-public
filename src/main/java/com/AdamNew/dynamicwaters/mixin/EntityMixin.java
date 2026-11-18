package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.EntityPhysicsContext;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z", at = @At("HEAD"))
    private void dynamicwaters$captureContextHead(TagKey<Fluid> fluidTag, double motionScale, CallbackInfoReturnable<Boolean> cir) {
        EntityPhysicsContext.set((Entity)(Object)this);
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z", at = @At("RETURN"))
    private void dynamicwaters$captureContextReturn(TagKey<Fluid> fluidTag, double motionScale, CallbackInfoReturnable<Boolean> cir) {
        EntityPhysicsContext.clear();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dynamicwaters$pushVehiclesAndPhysics(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (!entity.isInWater()) return;

        boolean isBoat = entity instanceof net.minecraft.world.entity.vehicle.Boat;
        boolean isItem = entity instanceof net.minecraft.world.entity.item.ItemEntity;
        String className = entity.getClass().getName().toLowerCase();
        boolean isPhysicsObj = className.contains("physics") ||
                               className.contains("ship") ||
                               className.contains("valkyrien") ||
                               className.contains("eureka") ||
                               className.contains("vs2") ||
                               className.contains("sable") ||
                               className.contains("aeronautics") ||
                               className.contains("contraption");
        if (!isBoat && !isItem && !isPhysicsObj) return;

        if (isPhysicsObj ? entity.level().isClientSide() : !entity.isControlledByLocalInstance()) return;

        net.minecraft.world.phys.Vec3 globalPos = com.AdamNew.dynamicwaters.SableCompat.getGlobalPos(entity);
        net.minecraft.world.phys.Vec3 flow = com.AdamNew.dynamicwaters.HydrologyManager.getRiverFlow(
            globalPos.x, (int)globalPos.y, globalPos.z, entity.level());
        if (flow.equals(net.minecraft.world.phys.Vec3.ZERO)) return;
        double mult = com.AdamNew.dynamicwaters.Config.getFlowMultiplierForEntity(entity);
        if (mult <= 0.0) return;

        net.minecraft.world.phys.Vec3 push = isItem ? net.minecraft.world.phys.Vec3.ZERO
                : com.AdamNew.dynamicwaters.HydrologyManager.pushOf(flow).scale(mult * 0.05);
        if (!isPhysicsObj) push = push.add(com.AdamNew.dynamicwaters.HydrologyManager.paceMakeup(flow));

        entity.setDeltaMovement(entity.getDeltaMovement().add(push));
    }
}
