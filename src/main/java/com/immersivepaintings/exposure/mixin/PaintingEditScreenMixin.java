package com.immersivepaintings.exposure.mixin;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import com.immersivepaintings.exposure.gui.ExposureTabEventHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Mixin для интеграции с GUI Immersive Paintings
 * Добавляет вкладку Exposure в интерфейс редактирования картин
 * 
 * Примечание: Mixin будет применен только если классы Immersive Paintings найдены во время выполнения.
 * Если классы не найдены, используется fallback через события (ExposureTabEventHandler).
 */
// Mixin закомментирован, так как классы Immersive Paintings недоступны во время компиляции
// Раскомментируйте и настройте targets после добавления мода в classpath
/*
@Mixin(targets = {
    "com.lukekorth.immersive_paintings.client.gui.PaintingEditScreen",
    "immersive_paintings.client.gui.PaintingEditScreen",
    "com.lukekorth.immersive_paintings.client.screen.PaintingEditScreen"
}, remap = false)
public abstract class PaintingEditScreenMixin extends Screen {
    
    protected PaintingEditScreenMixin(Component title) {
        super(title);
    }
    
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        if (!ImmersivePaintingsExposure.isIntegrationEnabled()) {
            return;
        }
        
        try {
            ImmersivePaintingsExposure.LOGGER.debug("Mixin: Adding Exposure tab to Immersive Paintings GUI");
            addExposureTabButton();
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to add Exposure tab via Mixin", e);
            // Fallback to event-based approach
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.Init.Post(this, this.children(), false));
        }
    }
    
    private void addExposureTabButton() {
        try {
            // Попытка найти существующие кнопки вкладок
            List<Button> tabButtons = findTabButtons();
            
            if (!tabButtons.isEmpty()) {
                Button lastButton = tabButtons.get(tabButtons.size() - 1);
                int newX = lastButton.getX() + lastButton.getWidth() + 5;
                int newY = lastButton.getY();
                
                Button exposureButton = Button.builder(
                    Component.translatable("gui.immersivepaintings_exposure.tab.exposure"),
                    (button) -> ExposureTabEventHandler.openExposureTabScreen(this)
                ).bounds(newX, newY, 80, 20).build();
                
                this.addRenderableWidget(exposureButton);
                ImmersivePaintingsExposure.LOGGER.info("Successfully added Exposure tab button via Mixin");
            } else {
                // Альтернативный метод - добавить в верхний левый угол
                Button exposureButton = Button.builder(
                    Component.translatable("gui.immersivepaintings_exposure.tab.exposure"),
                    (button) -> ExposureTabEventHandler.openExposureTabScreen(this)
                ).bounds(10, 10, 100, 20).build();
                
                this.addRenderableWidget(exposureButton);
                ImmersivePaintingsExposure.LOGGER.info("Added Exposure tab button via Mixin (alternative position)");
            }
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.warn("Could not add Exposure tab button via Mixin, using fallback", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private List<Button> findTabButtons() {
        List<Button> tabButtons = new java.util.ArrayList<>();
        
        try {
            // Поиск полей с кнопками
            Field[] fields = this.getClass().getSuperclass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(this);
                
                if (value instanceof Button button && isTabButton(button)) {
                    tabButtons.add(button);
                } else if (value instanceof List) {
                    for (Object item : (List<?>) value) {
                        if (item instanceof Button button && isTabButton(button)) {
                            tabButtons.add(button);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.debug("Could not find tab buttons via reflection", e);
        }
        
        return tabButtons;
    }
    
    private boolean isTabButton(Button button) {
        String buttonText = button.getMessage().getString().toLowerCase();
        return buttonText.contains("yours") || 
               buttonText.contains("datapack") ||
               buttonText.contains("player") ||
               buttonText.contains("new") ||
               buttonText.contains("frame");
    }
}
*/
