package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.block.FlowOverrideBlock;
import com.AdamNew.dynamicwaters.block.RiverWaterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, DynamicWaters.MODID);

    public static final DeferredHolder<Block, Block> RIVER_WATER = BLOCKS.register("river_water",
            () -> new RiverWaterBlock((net.minecraft.world.level.material.FlowingFluid) ModFluids.RIVER_WATER.get(), BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));

    public static final DeferredHolder<Block, Block> FLOW_OVERRIDE = BLOCKS.register("flow_override",
            () -> new FlowOverrideBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AIR)
                    .noCollission()
                    .noLootTable()
                    .air()));
}
