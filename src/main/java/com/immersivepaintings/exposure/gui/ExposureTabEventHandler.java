package com.immersivepaintings.exposure.gui;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Обработчик событий для интеграции вкладки Exposure
 */
public class ExposureTabEventHandler {
    
    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (!ImmersivePaintingsExposure.isIntegrationEnabled()) {
            return;
        }
        
        Screen screen = event.getScreen();
        if (screen == null) {
            return;
        }
        
        String screenClassName = screen.getClass().getName();
        
        // Проверяем, является ли это GUI Immersive Paintings
        if (isImmersivePaintingsScreen(screenClassName)) {
            ImmersivePaintingsExposure.LOGGER.debug("Detected Immersive Paintings GUI: {}", screenClassName);
            
            try {
                addExposureTab(screen, event);
            } catch (Exception e) {
                ImmersivePaintingsExposure.LOGGER.error("Failed to add Exposure tab to GUI", e);
            }
        }
    }
    
    private boolean isImmersivePaintingsScreen(String className) {
        return className.contains("immersive_paintings") || 
               className.contains("immersivepaintings") ||
               (className.contains("PaintingEditScreen") || 
                className.contains("PaintingEditGui"));
    }
    
    private void addExposureTab(Screen screen, ScreenEvent.Init.Post event) {
        try {
            // Ищем стрелки страниц (pagination buttons) внизу экрана
            Button rightArrowButton = findPaginationButton(screen, true);
            
            if (rightArrowButton != null) {
                // Размещаем кнопку справа от правой стрелки
                int newX = rightArrowButton.getX() + rightArrowButton.getWidth() + 5;
                int newY = rightArrowButton.getY();
                int buttonWidth = 90;
                
                // Проверяем границы экрана
                if (newX + buttonWidth > screen.width) {
                    newX = screen.width - buttonWidth - 10;
                }
                
                Button exposureTabButton = Button.builder(
                    Component.translatable("gui.immersivepaintings_exposure.tab.exposure"),
                    (button) -> openExposureTab(screen)
                ).bounds(newX, newY, buttonWidth, 20).build();
                
                event.addListener(exposureTabButton);
                ImmersivePaintingsExposure.LOGGER.info("Successfully added Exposure tab button next to pagination at ({}, {})", newX, newY);
                return;
            }
            
            // Если не нашли стрелки, пробуем найти кнопки вкладок
            Field[] fields = screen.getClass().getDeclaredFields();
            java.util.List<Button> tabButtons = new java.util.ArrayList<>();
            
            // Также проверяем все виджеты экрана
            for (net.minecraft.client.gui.components.events.GuiEventListener listener : screen.children()) {
                if (listener instanceof Button button && isTabButton(button)) {
                    tabButtons.add(button);
                }
            }
            
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(screen);
                    
                    if (value instanceof Button button) {
                        // Проверяем, является ли это кнопкой вкладки
                        if (isTabButton(button) && !tabButtons.contains(button)) {
                            tabButtons.add(button);
                        }
                    } else if (value instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<Object> list = (java.util.List<Object>) value;
                        for (Object item : list) {
                            if (item instanceof Button button && isTabButton(button) && !tabButtons.contains(button)) {
                                tabButtons.add(button);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки доступа к полям
                }
            }
            
            if (!tabButtons.isEmpty()) {
                // Сортируем кнопки по X координате
                tabButtons.sort((a, b) -> Integer.compare(a.getX(), b.getX()));
                
                // Находим самую правую кнопку
                Button rightmostButton = tabButtons.get(tabButtons.size() - 1);
                int newX = rightmostButton.getX() + rightmostButton.getWidth() + 5;
                int newY = rightmostButton.getY();
                
                // Проверяем, не выходит ли за границы экрана
                int screenWidth = screen.width;
                int buttonWidth = 90; // Ширина кнопки "Exposure"
                if (newX + buttonWidth > screenWidth) {
                    // Если не помещается, размещаем на новой строке
                    newX = tabButtons.get(0).getX();
                    newY = rightmostButton.getY() + rightmostButton.getHeight() + 5;
                }
                
                // Создаем кнопку вкладки Exposure
                Button exposureTabButton = Button.builder(
                    Component.translatable("gui.immersivepaintings_exposure.tab.exposure"),
                    (button) -> openExposureTab(screen)
                ).bounds(newX, newY, buttonWidth, 20).build();
                
                event.addListener(exposureTabButton);
                ImmersivePaintingsExposure.LOGGER.info("Successfully added Exposure tab button at ({}, {})", newX, newY);
            } else {
                // Альтернативный метод - добавить кнопку в нижнюю часть экрана
                addExposureTabButtonAlternative(screen, event);
            }
            
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.warn("Could not find tab buttons via reflection, trying alternative method", e);
            addExposureTabButtonAlternative(screen, event);
        }
    }
    
    /**
     * Ищет кнопку пагинации (стрелку вправо или влево)
     */
    private Button findPaginationButton(Screen screen, boolean rightArrow) {
        // Ищем кнопки со стрелками внизу экрана
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : screen.children()) {
            if (listener instanceof Button button) {
                String buttonText = button.getMessage().getString();
                // Ищем стрелки: ">>", ">", "→" или кнопки пагинации
                if ((rightArrow && (buttonText.contains(">>") || buttonText.contains(">") || buttonText.contains("→"))) ||
                    (!rightArrow && (buttonText.contains("<<") || buttonText.contains("<") || buttonText.contains("←")))) {
                    // Проверяем, что кнопка находится внизу экрана (примерно в нижней трети)
                    if (button.getY() > screen.height * 0.6) {
                        return button;
                    }
                }
            }
        }
        
        // Также ищем по позиции - обычно стрелки находятся внизу по центру
        int bottomY = screen.height - 50;
        Button closestButton = null;
        int closestDistance = Integer.MAX_VALUE;
        
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : screen.children()) {
            if (listener instanceof Button button) {
                // Ищем кнопки внизу экрана
                if (button.getY() >= bottomY - 30 && button.getY() <= bottomY + 30) {
                    int distance = Math.abs(button.getY() - bottomY);
                    if (distance < closestDistance) {
                        // Для правой стрелки ищем самую правую кнопку
                        if (rightArrow) {
                            if (closestButton == null || button.getX() > closestButton.getX()) {
                                closestButton = button;
                                closestDistance = distance;
                            }
                        } else {
                            // Для левой стрелки ищем самую левую кнопку
                            if (closestButton == null || button.getX() < closestButton.getX()) {
                                closestButton = button;
                                closestDistance = distance;
                            }
                        }
                    }
                }
            }
        }
        
        return closestButton;
    }
    
    private boolean isTabButton(Button button) {
        // Проверяем текст кнопки на наличие типичных названий вкладок
        String buttonText = button.getMessage().getString().toLowerCase();
        return buttonText.contains("yours") || 
               buttonText.contains("datapack") ||
               buttonText.contains("player") ||
               buttonText.contains("new") ||
               buttonText.contains("frame");
    }
    
    private void addExposureTabButtonAlternative(Screen screen, ScreenEvent.Init.Post event) {
        // Добавляем кнопку вкладки Exposure в нижней части экрана справа
        // Пытаемся найти место рядом со стрелками пагинации
        int x = screen.width - 110; // Справа с отступом
        int y = screen.height - 50; // Внизу экрана
        
        // Проверяем, не пересекается ли с другими виджетами
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : screen.children()) {
            if (listener instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                if (widget.getX() < x + 100 && widget.getX() + widget.getWidth() > x &&
                    widget.getY() < y + 20 && widget.getY() + widget.getHeight() > y) {
                    // Если пересекается, размещаем левее
                    x = widget.getX() - 105;
                    if (x < 10) {
                        x = 10;
                        y = widget.getY() + widget.getHeight() + 5;
                    }
                }
            }
        }
        
        Button exposureTabButton = Button.builder(
            Component.translatable("gui.immersivepaintings_exposure.tab.exposure"),
            (button) -> openExposureTab(screen)
        ).bounds(x, y, 100, 20).build();
        
        event.addListener(exposureTabButton);
        ImmersivePaintingsExposure.LOGGER.info("Added Exposure tab button using alternative method at ({}, {})", x, y);
    }
    
    private void openExposureTab(Screen parentScreen) {
        openExposureTabScreen(parentScreen);
    }
    
    public static void openExposureTabScreen(Screen parentScreen) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        
        ExposureTabScreen exposureScreen = new ExposureTabScreen(
            parentScreen,
            mc.player,
            photo -> {
                // Обработка выбора фотографии
                ImmersivePaintingsExposure.LOGGER.info("Selected Exposure photo: {}", photo.getName());
                applyPhotoToPaintingStatic(photo, parentScreen);
                mc.setScreen(parentScreen);
            }
        );
        
        mc.setScreen(exposureScreen);
    }
    
    private static void applyPhotoToPaintingStatic(com.immersivepaintings.exposure.exposure.ExposurePhoto photo, Screen parentScreen) {
        com.immersivepaintings.exposure.integration.PaintingIntegration integration = 
            com.immersivepaintings.exposure.integration.PaintingIntegration.getInstance();
        
        boolean success = integration.applyPhotoToPainting(photo, parentScreen);
        
        if (success) {
            ImmersivePaintingsExposure.LOGGER.info("Successfully applied Exposure photo to painting: {}", photo.getName());
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "gui.immersivepaintings_exposure.photo_selected",
                    photo.getName()
                ),
                false
            );
        } else {
            ImmersivePaintingsExposure.LOGGER.warn("Failed to apply Exposure photo to painting: {}", photo.getName());
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "gui.immersivepaintings_exposure.error.applying"
                ),
                true
            );
        }
    }
    
    private void applyPhotoToPainting(com.immersivepaintings.exposure.exposure.ExposurePhoto photo, Screen parentScreen) {
        com.immersivepaintings.exposure.integration.PaintingIntegration integration = 
            com.immersivepaintings.exposure.integration.PaintingIntegration.getInstance();
        
        boolean success = integration.applyPhotoToPainting(photo, parentScreen);
        
        if (success) {
            ImmersivePaintingsExposure.LOGGER.info("Successfully applied Exposure photo to painting: {}", photo.getName());
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "gui.immersivepaintings_exposure.photo_selected",
                    photo.getName()
                ),
                false
            );
        } else {
            ImmersivePaintingsExposure.LOGGER.warn("Failed to apply Exposure photo to painting: {}", photo.getName());
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                    "gui.immersivepaintings_exposure.error.applying"
                ),
                true
            );
        }
    }
}
