package com.immersivepaintings.exposure.gui;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер для загрузки и кэширования текстур фотографий
 */
public class PhotoTextureManager {
    
    private static final PhotoTextureManager INSTANCE = new PhotoTextureManager();
    private final Map<String, ResourceLocation> textureCache = new HashMap<>();
    private final Map<String, Long> textureTimestamps = new HashMap<>();
    
    public static PhotoTextureManager getInstance() {
        return INSTANCE;
    }
    
    private PhotoTextureManager() {
    }
    
    /**
     * Загружает текстуру из файла фотографии
     */
    public ResourceLocation getOrLoadTexture(Path photoPath, String photoName) {
        String cacheKey = photoPath.toString();
        
        // Проверяем кэш
        if (textureCache.containsKey(cacheKey)) {
            // Проверяем, не изменился ли файл
            File file = photoPath.toFile();
            if (file.exists()) {
                Long cachedTime = textureTimestamps.get(cacheKey);
                if (cachedTime != null && cachedTime == file.lastModified()) {
                    return textureCache.get(cacheKey);
                }
            }
        }
        
        try {
            // Загружаем изображение
            ResourceLocation texture = loadTextureFromFile(photoPath, photoName);
            if (texture != null) {
                textureCache.put(cacheKey, texture);
                File file = photoPath.toFile();
                if (file.exists()) {
                    textureTimestamps.put(cacheKey, file.lastModified());
                }
                return texture;
            }
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to load texture for photo: {}", photoName, e);
        }
        
        return null;
    }
    
    private ResourceLocation loadTextureFromFile(Path photoPath, String photoName) throws IOException {
        File file = photoPath.toFile();
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            // Загружаем изображение через NativeImage
            NativeImage image = NativeImage.read(fis);
            
            // Масштабируем до миниатюры (если нужно)
            int maxSize = 128; // Максимальный размер миниатюры
            if (image.getWidth() > maxSize || image.getHeight() > maxSize) {
                NativeImage scaled = scaleImage(image, maxSize);
                image.close();
                image = scaled;
            }
            
            // Создаем динамическую текстуру
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            ResourceLocation textureLocation = new ResourceLocation(
                ImmersivePaintingsExposure.MOD_ID,
                "exposure_photo_" + photoName.hashCode()
            );
            
            // Регистрируем текстуру
            Minecraft.getInstance().getTextureManager().register(textureLocation, dynamicTexture);
            
            return textureLocation;
        }
    }
    
    /**
     * Масштабирует изображение с сохранением пропорций
     */
    private NativeImage scaleImage(NativeImage source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        
        // Вычисляем новые размеры с сохранением пропорций
        int newWidth, newHeight;
        if (width > height) {
            newWidth = maxSize;
            newHeight = (int) ((double) height * maxSize / width);
        } else {
            newHeight = maxSize;
            newWidth = (int) ((double) width * maxSize / height);
        }
        
        NativeImage scaled = new NativeImage(newWidth, newHeight, false);
        
        // Простое масштабирование (ближайший сосед)
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                int srcX = x * width / newWidth;
                int srcY = y * height / newHeight;
                int color = source.getPixelRGBA(srcX, srcY);
                scaled.setPixelRGBA(x, y, color);
            }
        }
        
        return scaled;
    }
    
    /**
     * Очищает кэш текстур
     */
    public void clearCache() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getTextureManager() != null) {
            for (ResourceLocation texture : textureCache.values()) {
                mc.getTextureManager().release(texture);
            }
        }
        textureCache.clear();
        textureTimestamps.clear();
    }
}

