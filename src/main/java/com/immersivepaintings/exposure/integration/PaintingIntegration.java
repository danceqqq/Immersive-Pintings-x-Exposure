package com.immersivepaintings.exposure.integration;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import com.immersivepaintings.exposure.exposure.ExposurePhoto;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Интеграция для применения фотографий Exposure к картинам Immersive Paintings
 */
public class PaintingIntegration {
    
    private static final PaintingIntegration INSTANCE = new PaintingIntegration();
    
    public static PaintingIntegration getInstance() {
        return INSTANCE;
    }
    
    private PaintingIntegration() {
    }
    
    /**
     * Применяет фотографию Exposure к картине Immersive Paintings
     */
    public boolean applyPhotoToPainting(ExposurePhoto photo, Object paintingEditScreen) {
        if (!ImmersivePaintingsExposure.isIntegrationEnabled()) {
            ImmersivePaintingsExposure.LOGGER.warn("Cannot apply photo - integration not enabled");
            return false;
        }
        
        try {
            // Метод 1: Попытка через рефлексию найти метод установки текстуры
            if (applyViaReflection(photo, paintingEditScreen)) {
                return true;
            }
            
            // Метод 2: Копирование файла фотографии в директорию Immersive Paintings
            if (applyViaFileCopy(photo)) {
                return true;
            }
            
            // Метод 3: Через сохранение выбранной фотографии в статическое поле
            return applyViaStorage(photo, paintingEditScreen);
            
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to apply photo to painting", e);
            return false;
        }
    }
    
    private boolean applyViaReflection(ExposurePhoto photo, Object paintingEditScreen) {
        try {
            Class<?> screenClass = paintingEditScreen.getClass();
            
            // Поиск методов для установки текстуры/изображения
            Method[] methods = screenClass.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.contains("set") && (methodName.contains("image") || methodName.contains("texture") || methodName.contains("painting"))) ||
                    methodName.contains("apply") || methodName.contains("load")) {
                    
                    try {
                        method.setAccessible(true);
                        Class<?>[] paramTypes = method.getParameterTypes();
                        
                        // Попытка вызова с ResourceLocation
                        if (paramTypes.length == 1 && paramTypes[0] == ResourceLocation.class) {
                            ResourceLocation textureLoc = convertPhotoToTexture(photo);
                            method.invoke(paintingEditScreen, textureLoc);
                            ImmersivePaintingsExposure.LOGGER.info("Applied photo via method: {}", method.getName());
                            return true;
                        }
                        
                        // Попытка вызова с String (путь к файлу)
                        if (paramTypes.length == 1 && paramTypes[0] == String.class) {
                            String filePath = photo.getFilePath().toString();
                            method.invoke(paintingEditScreen, filePath);
                            ImmersivePaintingsExposure.LOGGER.info("Applied photo via method (String): {}", method.getName());
                            return true;
                        }
                        
                    } catch (Exception e) {
                        ImmersivePaintingsExposure.LOGGER.debug("Could not invoke method {}", method.getName(), e);
                    }
                }
            }
            
            // Поиск полей для установки текстуры
            Field[] fields = screenClass.getDeclaredFields();
            for (Field field : fields) {
                String fieldName = field.getName().toLowerCase();
                if (fieldName.contains("image") || fieldName.contains("texture") || 
                    fieldName.contains("painting") || fieldName.contains("selected")) {
                    
                    try {
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();
                        
                        if (fieldType == ResourceLocation.class) {
                            ResourceLocation textureLoc = convertPhotoToTexture(photo);
                            field.set(paintingEditScreen, textureLoc);
                            ImmersivePaintingsExposure.LOGGER.info("Applied photo via field: {}", field.getName());
                            return true;
                        }
                        
                        if (fieldType == String.class) {
                            String filePath = photo.getFilePath().toString();
                            field.set(paintingEditScreen, filePath);
                            ImmersivePaintingsExposure.LOGGER.info("Applied photo via field (String): {}", field.getName());
                            return true;
                        }
                        
                    } catch (Exception e) {
                        ImmersivePaintingsExposure.LOGGER.debug("Could not set field {}", field.getName(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.debug("Reflection method failed", e);
        }
        
        return false;
    }
    
    private boolean applyViaFileCopy(ExposurePhoto photo) {
        try {
            // Получаем путь к директории Immersive Paintings для пользовательских картин
            Path immersivePaintingsDir = getImmersivePaintingsUserDir();
            if (immersivePaintingsDir == null || !immersivePaintingsDir.toFile().exists()) {
                // Создаем директорию, если её нет
                if (immersivePaintingsDir != null) {
                    Files.createDirectories(immersivePaintingsDir);
                } else {
                    return false;
                }
            }
            
            // Копируем фотографию в директорию Immersive Paintings
            Path targetPath = immersivePaintingsDir.resolve(photo.getName());
            Files.copy(photo.getFilePath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            ImmersivePaintingsExposure.LOGGER.info("Copied Exposure photo to Immersive Paintings directory: {}", targetPath);
            return true;
            
        } catch (IOException e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to copy photo file", e);
            return false;
        }
    }
    
    private boolean applyViaStorage(ExposurePhoto photo, Object paintingEditScreen) {
        try {
            // Сохраняем выбранную фотографию в статическое хранилище
            SelectedPhotoStorage.setSelectedPhoto(photo);
            ImmersivePaintingsExposure.LOGGER.info("Stored selected photo for later use: {}", photo.getName());
            
            // Попытка уведомить GUI о новой выбранной фотографии через событие
            // Это может потребовать отправки пакета на сервер или обновления GUI
            return true;
            
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to store selected photo", e);
            return false;
        }
    }
    
    private ResourceLocation convertPhotoToTexture(ExposurePhoto photo) {
        // Создаем ResourceLocation для фотографии
        // Формат: immersivepaintings_exposure:exposure_photos/[filename]
        String texturePath = "exposure_photos/" + photo.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        return new ResourceLocation(
            ImmersivePaintingsExposure.MOD_ID,
            texturePath
        );
    }
    
    private Path getImmersivePaintingsUserDir() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.gameDirectory == null) {
                return null;
            }
            
            // Стандартный путь: .minecraft/config/immersive_paintings/user_paintings/
            Path gameDir = mc.gameDirectory.toPath();
            Path configDir = gameDir.resolve("config").resolve("immersive_paintings");
            Path userPaintingsDir = configDir.resolve("user_paintings");
            
            return userPaintingsDir;
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to get Immersive Paintings directory", e);
            return null;
        }
    }
    
    /**
     * Статическое хранилище для выбранной фотографии
     */
    public static class SelectedPhotoStorage {
        private static ExposurePhoto selectedPhoto = null;
        
        public static void setSelectedPhoto(ExposurePhoto photo) {
            selectedPhoto = photo;
        }
        
        public static ExposurePhoto getSelectedPhoto() {
            return selectedPhoto;
        }
        
        public static void clearSelectedPhoto() {
            selectedPhoto = null;
        }
    }
}

