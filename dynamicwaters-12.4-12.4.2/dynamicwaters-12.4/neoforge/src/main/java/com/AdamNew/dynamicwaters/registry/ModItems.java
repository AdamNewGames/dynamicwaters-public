package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.item.FlowWandItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DynamicWaters.MODID);

    public static final DeferredHolder<Item, Item> FLOW_WAND = ITEMS.register("flow_wand",
            () -> new FlowWandItem(new Item.Properties().stacksTo(1)));
}
