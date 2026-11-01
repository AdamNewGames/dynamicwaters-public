package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.terrain.WiredRouter;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomState.class)
public class RandomStateMixin implements WiredRouter {
    @Shadow
    @Final
    private NoiseRouter router;

    @Unique
    private NoiseRouter dynamicwaters$wired;

    @Inject(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/level/levelgen/RandomState;router:Lnet/minecraft/world/level/levelgen/NoiseRouter;"))
    private void dynamicwaters$keepWiredRouter(NoiseGeneratorSettings settings, HolderGetter<NormalNoise.NoiseParameters> noises,
            long seed, CallbackInfo ci) {
        this.dynamicwaters$wired = this.router;
    }

    @Override
    public NoiseRouter dynamicwaters$wiredRouter() {
        return this.dynamicwaters$wired;
    }
}
