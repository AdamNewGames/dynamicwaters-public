package com.AdamNew.dynamicwaters.platform;

import com.AdamNew.dynamicwaters.DynamicWaters;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class Registrar<T> {
    //? neoforge {
    private final net.neoforged.neoforge.registries.DeferredRegister<T> deferred;

    private Registrar(ResourceKey<? extends Registry<T>> registry) {
        this.deferred = net.neoforged.neoforge.registries.DeferredRegister.create(registry, DynamicWaters.MODID);
    }

    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> factory) {
        return deferred.register(name, factory);
    }

    public void attach(net.neoforged.bus.api.IEventBus modBus) {
        deferred.register(modBus);
    }
    //?} forge {
    /*private final net.minecraftforge.registries.DeferredRegister<T> deferred;

    private Registrar(ResourceKey<? extends Registry<T>> registry) {
        this.deferred = net.minecraftforge.registries.DeferredRegister.create(registry, DynamicWaters.MODID);
    }

    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> factory) {
        return deferred.register(name, factory);
    }

    public void attach(net.minecraftforge.eventbus.api.IEventBus modBus) {
        deferred.register(modBus);
    }
    *///?} fabric {
    /*private final Registry<T> registry;

    @SuppressWarnings("unchecked")
    private Registrar(ResourceKey<? extends Registry<T>> key) {
        this.registry = (Registry<T>) net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(key.location());
    }

    public <I extends T> Supplier<I> register(String name, Supplier<? extends I> factory) {
        final I value = factory.get();
        Registry.register(registry, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DynamicWaters.MODID, name), value);
        return () -> value;
    }

    public void attach() {
    }
    *///?}

    public static <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registry) {
        return new Registrar<>(registry);
    }
}
