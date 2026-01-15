package com.immersivepaintings.exposure.integration;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import com.immersivepaintings.exposure.gui.ExposureTabIntegration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ImmersivePaintingsExposure.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModIntegration {
    
    private ModIntegration() {
        // Утилитный класс
    }
    
    public static void init() {
        ImmersivePaintingsExposure.LOGGER.info("Mod integration initialized");
    }
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ImmersivePaintingsExposure.isIntegrationEnabled()) {
            event.enqueueWork(() -> {
                ImmersivePaintingsExposure.LOGGER.info("Registering Exposure tab integration...");
                ExposureTabIntegration.register();
            });
        }
    }
}

