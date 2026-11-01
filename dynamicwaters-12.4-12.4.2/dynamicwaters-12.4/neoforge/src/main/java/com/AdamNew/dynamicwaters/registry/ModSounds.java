package com.AdamNew.dynamicwaters.registry;

import com.AdamNew.dynamicwaters.DynamicWaters;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, DynamicWaters.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RIVER_FLOW = registerSoundEvent("river_flow");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundEvent(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DynamicWaters.MODID, name)));
    }
}
