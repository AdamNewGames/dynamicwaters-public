package com.AdamNew.dynamicwaters.mixin.client;

import com.AdamNew.dynamicwaters.client.RiverFoam;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelFoamMixin {
    @Inject(method = "animateTick", at = @At("TAIL"))
    private void dynamicwaters$throwRiverFoam(int x, int y, int z, CallbackInfo ci) {
        RiverFoam.tick((ClientLevel) (Object) this, x, y, z);
        com.AdamNew.dynamicwaters.client.RiverWaterColor.tick();
    }
}
