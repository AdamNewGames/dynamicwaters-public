package com.AdamNew.dynamicwaters;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import com.AdamNew.dynamicwaters.river.RiverContext;
import com.AdamNew.dynamicwaters.river.RiverShapeNoise;

public record FractalRiverFunction(DensityFunction delegate) implements DensityFunction {
    public static final MapCodec<FractalRiverFunction> CODEC = DensityFunction.HOLDER_HELPER_CODEC
            .fieldOf("delegate").xmap(FractalRiverFunction::new, FractalRiverFunction::delegate);

    @Override
    public double compute(FunctionContext context) {
        return delegate.compute(context);
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        delegate.fillArray(array, provider);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new FractalRiverFunction(delegate.mapAll(visitor)));
    }

    @Override
    public double minValue() { return -1.0; }
    @Override
    public double maxValue() { return delegate.maxValue(); }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(CODEC);
    }
}
