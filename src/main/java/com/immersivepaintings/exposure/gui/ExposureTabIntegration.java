package com.immersivepaintings.exposure.gui;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Интеграция для добавления вкладки Exposure в GUI Immersive Paintings
 */
public class ExposureTabIntegration {
    
    private static boolean integrationRegistered = false;
    private static final List<ITabIntegrationHandler> handlers = new ArrayList<>();
    
    public static void register() {
        if (integrationRegistered) {
            return;
        }
        
        try {
            // Регистрация через события
            MinecraftForge.EVENT_BUS.register(new ExposureTabEventHandler());
            
            // Попытка регистрации через рефлексию
            registerTabViaReflection();
            
            integrationRegistered = true;
            ImmersivePaintingsExposure.LOGGER.info("Exposure tab integration registered successfully");
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to register Exposure tab integration", e);
        }
    }
    
    private static void registerTabViaReflection() {
        try {
            // Попытка найти класс GUI Immersive Paintings
            Class<?>[] classesToTry = {
                Class.forName("com.lukekorth.immersive_paintings.client.gui.PaintingEditScreen"),
                Class.forName("immersive_paintings.client.gui.PaintingEditScreen"),
                Class.forName("com.lukekorth.immersive_paintings.client.screen.PaintingEditScreen")
            };
            
            for (Class<?> guiClass : classesToTry) {
                try {
                    ImmersivePaintingsExposure.LOGGER.info("Found Immersive Paintings GUI class: {}", guiClass.getName());
                    // Попытка найти метод для добавления вкладок
                    findAndRegisterTabMethod(guiClass);
                    break;
                } catch (Exception ignored) {
                    // Продолжаем попытки
                }
            }
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.warn("Could not find Immersive Paintings GUI class directly, using event-based approach only");
        }
    }
    
    private static void findAndRegisterTabMethod(Class<?> guiClass) {
        try {
            // Поиск методов, которые могут использоваться для регистрации вкладок
            Method[] methods = guiClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if (methodName.contains("tab") || methodName.contains("register") || methodName.contains("add")) {
                    ImmersivePaintingsExposure.LOGGER.debug("Found potential tab registration method: {}", method.getName());
                }
            }
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.debug("Could not find tab registration methods", e);
        }
    }
    
    public static void addTabHandler(ITabIntegrationHandler handler) {
        handlers.add(handler);
    }
    
    public interface ITabIntegrationHandler {
        void onScreenInit(ScreenEvent.Init.Post event);
    }
}
