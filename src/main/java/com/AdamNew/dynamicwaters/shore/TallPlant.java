package com.AdamNew.dynamicwaters.shore;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Collection;
import java.util.Optional;

final class TallPlant {
    private static final String[] NAMES = { "half", "part", "segment", "section" };
    private static final String[][] ORDERS = {
            { "lower", "upper" }, { "bottom", "top" }, { "lower", "middle", "upper" }, { "bottom", "middle", "top" } };

    private TallPlant() {
    }

    static BlockState[] parts(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            boolean named = false;
            for (String n : NAMES) named |= n.equals(p.getName());
            if (!named) continue;
            final BlockState[] out = parts(state, p);
            if (out != null) return out;
        }
        return null;
    }

    private static <T extends Comparable<T>> BlockState[] parts(BlockState state, Property<T> p) {
        final Collection<T> values = p.getPossibleValues();
        for (String[] order : ORDERS) {
            if (order.length != values.size()) continue;
            final BlockState[] out = new BlockState[order.length];
            boolean all = true;
            for (int i = 0; i < order.length && all; i++) {
                final Optional<T> v = p.getValue(order[i]);
                if (v.isPresent()) out[i] = state.setValue(p, v.get());
                else all = false;
            }
            if (all) return out;
        }
        return null;
    }
}
