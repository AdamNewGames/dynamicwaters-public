package com.AdamNew.dynamicwaters.platform.neoforge;

//? neoforge {
import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.client.ClientSetup;
import com.AdamNew.dynamicwaters.client.FlowZoneClient;
import com.AdamNew.dynamicwaters.client.IceFloeRenderer;
import com.AdamNew.dynamicwaters.client.MainMenuButtonHandler;
import com.AdamNew.dynamicwaters.client.VanillaFlowClientEvents;
import com.AdamNew.dynamicwaters.client.gui.DWConfigScreen;
import com.AdamNew.dynamicwaters.client.particle.RiverBubbleParticle;
import com.AdamNew.dynamicwaters.registry.ModEntities;
import com.AdamNew.dynamicwaters.registry.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class NeoforgeClient {
    private NeoforgeClient() {
    }

    @EventBusSubscriber(modid = DynamicWaters.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ClientSetup.clientSetup();

            event.enqueueWork(() -> ModList.get().getModContainerById(DynamicWaters.MODID).ifPresent(container ->
                    container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new DWConfigScreen(parent))));
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.ICE_FLOE.get(), IceFloeRenderer::new);
        }

        @SubscribeEvent
        public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.RIVER_BUBBLE.get(), RiverBubbleParticle.Provider::new);
        }
    }

    @EventBusSubscriber(modid = DynamicWaters.MODID, value = Dist.CLIENT)
    public static final class GameBus {
        private GameBus() {
        }

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            VanillaFlowClientEvents.onLoggingOut();
            FlowZoneClient.onLoggingOut();
        }

        @SubscribeEvent
        public static void onClone(ClientPlayerNetworkEvent.Clone event) {
            FlowZoneClient.onClone();
        }

        @SubscribeEvent
        public static void onRenderLevelStage(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
                FlowZoneClient.render(event.getPoseStack().last(), event.getCamera().getPosition());
            }
        }

        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event) {
            MainMenuButtonHandler.onScreenInit(event.getScreen(), event.getListenersList(), event::addListener);
        }
    }
}
//?}
