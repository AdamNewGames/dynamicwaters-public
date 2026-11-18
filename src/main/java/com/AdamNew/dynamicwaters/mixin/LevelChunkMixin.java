package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.LivingCurrent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void dynamicwaters$livingCurrent(BlockPos pos, BlockState state, boolean moving,
            CallbackInfoReturnable<BlockState> cir) {
        final BlockState before = cir.getReturnValue();
        if (before == null || before == state) return;
        if (((LevelChunk) (Object) this).getLevel() instanceof ServerLevel level) {
            LivingCurrent.blockChanged(level, pos, before, state);
        }
    }
}
