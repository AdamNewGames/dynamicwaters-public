package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.Config;
import com.AdamNew.dynamicwaters.HydrologyManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    private static final boolean LAB_CLIENT_FLOW = Boolean.getBoolean("dynamicwaters.labClientFlow");

    @Inject(method = "tick", at = @At("TAIL"))
    private void dynamicwaters$pushMobsInRiver(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity)(Object)this;
        if (LAB_CLIENT_FLOW && entity instanceof Player && entity.level().isClientSide() && entity.tickCount % 20 == 0) {
            HydrologyManager.labClientFlow(entity);
        }

        if (entity.isInWater()) {
            Vec3 globalPos = com.AdamNew.dynamicwaters.SableCompat.getGlobalPos(entity);
            Vec3 flow = HydrologyManager.getRiverFlow(globalPos.x, (int)globalPos.y, globalPos.z, entity.level());
            if (!flow.equals(Vec3.ZERO)) {
                double mult = Config.getFlowMultiplierForEntity(entity);
                if (mult > 0.0) {
                    final boolean falling = HydrologyManager.inWaterfall(entity.level(), entity.blockPosition());
                    Vec3 push = HydrologyManager.pushOf(flow).scale(mult * 0.05);
                    if (falling) {
                        push = new Vec3(push.x * HydrologyManager.FALL_ON, -HydrologyManager.FALL_PULL,
                                push.z * HydrologyManager.FALL_ON);
                    } else if (!(entity instanceof Player)) {
                        push = push.add(HydrologyManager.paceMakeup(flow));
                    }

                    entity.setDeltaMovement(entity.getDeltaMovement().add(push));
                }
            }
        }
    }
}
