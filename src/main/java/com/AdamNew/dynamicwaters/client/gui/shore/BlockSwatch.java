package com.AdamNew.dynamicwaters.client.gui.shore;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BlockSwatch {
    private record Face(TextureAtlasSprite sprite, int tint) {
    }

    private static final Map<String, List<Face>> FACES = new HashMap<>();
    private static List<Block> dressable;
    private static List<Block> plantable;

    private static final String[] WATER_PLANTS = {
            "minecraft:seagrass", "minecraft:tall_seagrass", "minecraft:kelp", "minecraft:sea_pickle",
            "minecraft:lily_pad", "minecraft:sugar_cane", "minecraft:frogspawn",
            "minecraft:small_dripleaf", "minecraft:big_dripleaf",
            "minecraft:tube_coral", "minecraft:brain_coral", "minecraft:bubble_coral", "minecraft:fire_coral", "minecraft:horn_coral",
            "minecraft:tube_coral_fan", "minecraft:brain_coral_fan", "minecraft:bubble_coral_fan", "minecraft:fire_coral_fan",
            "minecraft:horn_coral_fan",
    };

    private BlockSwatch() {
    }

    static Block block(String id) {
        if (RiverbankPaletteDraft.KEEP.equals(id)) return null;
        final ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }

    static boolean exists(String id) {
        return RiverbankPaletteDraft.KEEP.equals(id) || block(id) != null;
    }

    static boolean loose(String id) {
        return block(id) instanceof FallingBlock;
    }

    static Component name(String id) {
        if (RiverbankPaletteDraft.KEEP.equals(id)) return Component.translatable("dynamicwaters.gui.banks.keep");
        final Block b = block(id);
        return b == null ? Component.literal(id).withStyle(ChatFormatting.RED) : b.getName();
    }

    static void draw(GuiGraphics g, String id, int x, int y, int size) {
        final List<Face> faces = faces(id);
        if (faces.isEmpty()) {
            g.fill(x, y, x + size, y + size, 0xFFB000B0);
            return;
        }
        for (Face f : faces) {
            if (f.tint() == -1) {
                g.blit(x, y, 0, size, size, f.sprite());
            } else {
                g.blit(x, y, 0, size, size, f.sprite(),
                        ((f.tint() >> 16) & 0xFF) / 255f, ((f.tint() >> 8) & 0xFF) / 255f, (f.tint() & 0xFF) / 255f, 1f);
            }
        }
    }

    private static List<Face> faces(String id) {
        final List<Face> cached = FACES.get(id);
        if (cached != null) return cached;
        final List<Face> out = new ArrayList<>();
        final Block b = block(RiverbankPaletteDraft.KEEP.equals(id) ? "minecraft:grass_block" : id);
        if (b != null) {
            try {
                final BlockState state = b.defaultBlockState();
                final Minecraft mc = Minecraft.getInstance();
                final BakedModel model = mc.getBlockRenderer().getBlockModel(state);
                //? neoforge {
                final net.neoforged.neoforge.client.model.data.ModelData data = net.neoforged.neoforge.client.model.data.ModelData.EMPTY;
                List<BakedQuad> quads = model.getQuads(state, Direction.NORTH, RandomSource.create(42L), data, null);
                if (quads.isEmpty()) quads = model.getQuads(state, null, RandomSource.create(42L), data, null);
                //?} else {
                /*List<BakedQuad> quads = model.getQuads(state, Direction.NORTH, RandomSource.create(42L));
                if (quads.isEmpty()) quads = model.getQuads(state, null, RandomSource.create(42L));
                *///?}
                for (BakedQuad q : quads) {
                    final int tint = q.isTinted() ? mc.getBlockColors().getColor(state, null, null, q.getTintIndex()) : -1;
                    out.add(new Face(q.getSprite(), tint));
                    if (out.size() >= 3) break;
                }
                //? neoforge {
                if (out.isEmpty()) out.add(new Face(model.getParticleIcon(data), -1));
                //?} else {
                /*if (out.isEmpty()) out.add(new Face(model.getParticleIcon(), -1));
                *///?}
            } catch (Throwable t) {
                out.clear();
            }
        }
        FACES.put(id, out);
        return out;
    }

    static List<Block> dressable() {
        if (dressable != null) return dressable;
        final List<Block> out = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            final Item item = b.asItem();
            if (item == Items.AIR || item instanceof GameMasterBlockItem) continue;
            final BlockState s = b.defaultBlockState();
            try {
                if (s.hasBlockEntity() || !s.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) continue;
            } catch (Throwable t) {
                continue;
            }
            out.add(b);
        }
        out.sort(Comparator.comparing(b -> b.getName().getString().toLowerCase()));
        dressable = out;
        return out;
    }

    static List<Block> plantable() {
        if (plantable != null) return plantable;
        final List<Block> out = new ArrayList<>();
        for (String id : WATER_PLANTS) {
            final Block b = block(id);
            if (b != null) out.add(b);
        }
        final List<Block> rest = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            if (out.contains(b)) continue;
            final Item item = b.asItem();
            if (item == Items.AIR || item instanceof GameMasterBlockItem) continue;
            try {
                if (b.defaultBlockState().hasBlockEntity()) continue;
            } catch (Throwable t) {
                continue;
            }
            rest.add(b);
        }
        rest.sort(Comparator.comparing(b -> b.getName().getString().toLowerCase()));
        out.addAll(rest);
        plantable = out;
        return out;
    }

    static String idOf(Block b) {
        return BuiltInRegistries.BLOCK.getKey(b).toString();
    }
}
