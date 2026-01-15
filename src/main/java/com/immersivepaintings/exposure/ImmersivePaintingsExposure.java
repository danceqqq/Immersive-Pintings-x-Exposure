package com.immersivepaintings.exposure;

import com.immersivepaintings.exposure.integration.ModIntegration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ImmersivePaintingsExposure.MOD_ID)
public class ImmersivePaintingsExposure {
    public static final String MOD_ID = "immersivepaintings_exposure";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    private static boolean immersivePaintingsLoaded = false;
    private static boolean exposureLoaded = false;
    
    public ImmersivePaintingsExposure() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::commonSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        // Проверяем наличие зависимых модов
        checkModDependencies();
    }
    
    private void checkModDependencies() {
        immersivePaintingsLoaded = ModList.get().isLoaded("immersive_paintings");
        exposureLoaded = ModList.get().isLoaded("exposure");
        
        if (immersivePaintingsLoaded && exposureLoaded) {
            LOGGER.info("Both ImmersivePaintings and Exposure are loaded. Integration enabled.");
        } else {
            LOGGER.warn("Required mods are missing! ImmersivePaintings: {}, Exposure: {}", 
                       immersivePaintingsLoaded, exposureLoaded);
            LOGGER.warn("Integration features will be disabled.");
        }
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (isIntegrationEnabled()) {
                LOGGER.info("Initializing ImmersivePaintings × Exposure integration...");
                ModIntegration.init();
            } else {
                LOGGER.warn("Cannot initialize integration - required mods are missing!");
            }
        });
    }
    
    public static boolean isIntegrationEnabled() {
        return immersivePaintingsLoaded && exposureLoaded;
    }
    
    public static boolean isImmersivePaintingsLoaded() {
        return immersivePaintingsLoaded;
    }
    
    public static boolean isExposureLoaded() {
        return exposureLoaded;
    }
}

