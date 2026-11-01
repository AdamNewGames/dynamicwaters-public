package com.AdamNew.dynamicwaters.mixin;

import com.AdamNew.dynamicwaters.HydrologyManager;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BiomeSource.class)
public class BiomeSourceMixin {
    @WrapMethod(method = "findBiomeHorizontal(IIIIILjava/util/function/Predicate;Lnet/minecraft/util/RandomSource;ZLnet/minecraft/world/level/biome/Climate$Sampler;)Lcom/mojang/datafixers/util/Pair;")
    private Pair<BlockPos, Holder<Biome>> dynamicwaters$searchWithoutRivers(int x, int y, int z, int radius, int increment,
            Predicate<Holder<Biome>> biomePredicate, RandomSource random, boolean findClosest, Climate.Sampler sampler,
            Operation<Pair<BlockPos, Holder<Biome>>> original) {
        final boolean was = HydrologyManager.isGeneratingRivers();
        HydrologyManager.setGeneratingRivers(true);
        try {
            return original.call(x, y, z, radius, increment, biomePredicate, random, findClosest, sampler);
        } finally {
            HydrologyManager.setGeneratingRivers(was);
        }
    }
}
