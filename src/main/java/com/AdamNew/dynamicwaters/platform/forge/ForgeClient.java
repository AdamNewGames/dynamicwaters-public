package com.AdamNew.dynamicwaters.platform.forge;

//? forge {
/*import com.AdamNew.dynamicwaters.DynamicWaters;
import com.AdamNew.dynamicwaters.client.ClientSetup;
import com.AdamNew.dynamicwaters.client.FlowZoneClient;
import com.AdamNew.dynamicwaters.client.IceFloeRenderer;
import com.AdamNew.dynamicwaters.client.MainMenuButtonHandler;
import com.AdamNew.dynamicwaters.client.VanillaFlowClientEvents;
import com.AdamNew.dynamicwaters.client.gui.DWConfigScreen;
import com.AdamNew.dynamicwaters.client.particle.RiverBubbleParticle;
import com.AdamNew.dynamicwaters.registry.ModEntities;
import com.AdamNew.dynamicwaters.registry.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ForgeClient {
    private ForgeClient() {
    }

    @Mod.EventBusSubscriber(modid = DynamicWaters.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            ClientSetup.clientSetup();
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new DWConfigScreen(parent)));
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

    @Mod.EventBusSubscriber(modid = DynamicWaters.MODID, value = Dist.CLIENT)
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
*///?}
