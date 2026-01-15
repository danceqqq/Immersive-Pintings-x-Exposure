package com.immersivepaintings.exposure.exposure;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Менеджер для работы с фотографиями из мода Exposure
 */
public class ExposurePhotoManager {
    
    private static final ExposurePhotoManager INSTANCE = new ExposurePhotoManager();
    
    public static ExposurePhotoManager getInstance() {
        return INSTANCE;
    }
    
    private ExposurePhotoManager() {
    }
    
    /**
     * Получает список всех доступных фотографий Exposure для текущего игрока
     */
    public List<ExposurePhoto> getAvailablePhotos(Player player) {
        List<ExposurePhoto> photos = new ArrayList<>();
        
        if (!ImmersivePaintingsExposure.isExposureLoaded()) {
            return photos;
        }
        
        try {
            // Попытка получить фотографии через рефлексию API Exposure
            photos = getPhotosViaReflection(player);
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to get Exposure photos", e);
        }
        
        return photos;
    }
    
    private List<ExposurePhoto> getPhotosViaReflection(Player player) throws Exception {
        List<ExposurePhoto> photos = new ArrayList<>();
        
        // Попытка найти классы Exposure через рефлексию
        try {
            Class<?> exposurePhotoClass = Class.forName("com.mortuusars.exposure.item.PhotographItem");
            Class<?> exposureManagerClass = Class.forName("com.mortuusars.exposure.ExposureClient");
            
            // Получаем путь к фотографиям
            Path photosPath = getExposurePhotosPath(player);
            
            if (photosPath != null && photosPath.toFile().exists()) {
                File[] photoFiles = photosPath.toFile().listFiles((dir, name) -> 
                    name.toLowerCase().endsWith(".png") || 
                    name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".jpeg"));
                
                if (photoFiles != null) {
                    for (File photoFile : photoFiles) {
                        ExposurePhoto photo = new ExposurePhoto(
                            photoFile.getName(),
                            photoFile.toPath(),
                            new ResourceLocation("exposure", "photos/" + photoFile.getName())
                        );
                        photos.add(photo);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            ImmersivePaintingsExposure.LOGGER.warn("Could not find Exposure classes, trying alternative method");
            // Альтернативный способ - прямое чтение из директории
            photos = getPhotosFromDirectory(player);
        }
        
        return photos;
    }
    
    private List<ExposurePhoto> getPhotosFromDirectory(Player player) {
        List<ExposurePhoto> photos = new ArrayList<>();
        
        Path photosPath = getExposurePhotosPath(player);
        if (photosPath == null) {
            ImmersivePaintingsExposure.LOGGER.warn("Exposure photos path is null");
            return photos;
        }
        
        File photosDir = photosPath.toFile();
        if (!photosDir.exists()) {
            ImmersivePaintingsExposure.LOGGER.warn("Exposure photos directory does not exist: {}", photosPath);
            return photos;
        }
        
        if (!photosDir.isDirectory()) {
            ImmersivePaintingsExposure.LOGGER.warn("Exposure photos path is not a directory: {}", photosPath);
            return photos;
        }
        
        // Фильтр для поиска фотографий: [player_name]_[id].png или просто *.png
        String playerName = player.getName().getString().toLowerCase();
        File[] photoFiles = photosDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            // Проверяем расширение
            boolean validExtension = lowerName.endsWith(".png") || 
                                     lowerName.endsWith(".jpg") ||
                                     lowerName.endsWith(".jpeg") ||
                                     lowerName.endsWith(".webp");
            
            if (!validExtension) {
                return false;
            }
            
            // Если имя файла начинается с имени игрока, это фотография этого игрока
            // Формат: [player_name]_[id].png или [player_name]_[id]_[suffix].png
            return lowerName.startsWith(playerName + "_") || 
                   lowerName.startsWith(playerName + ".") ||
                   true; // Также показываем все фотографии в папке мира
        });
        
        if (photoFiles != null && photoFiles.length > 0) {
            ImmersivePaintingsExposure.LOGGER.info("Found {} Exposure photos in: {}", photoFiles.length, photosPath);
            for (File photoFile : photoFiles) {
                if (photoFile.isFile()) {
                    ExposurePhoto photo = new ExposurePhoto(
                        photoFile.getName(),
                        photoFile.toPath(),
                        new ResourceLocation("exposure", "photos/" + photoFile.getName())
                    );
                    photos.add(photo);
                    ImmersivePaintingsExposure.LOGGER.debug("Added photo: {}", photoFile.getName());
                }
            }
        } else {
            ImmersivePaintingsExposure.LOGGER.warn("No photo files found in: {}", photosPath);
        }
        
        return photos;
    }
    
    /**
     * Получает путь к директории с фотографиями Exposure
     * Формат пути: [gameDir]/exposures/[world_name]/[player_name]_[id].png
     */
    private Path getExposurePhotosPath(Player player) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return null;
            }
            
            Path gameDir = mc.gameDirectory.toPath();
            String playerName = player.getName().getString();
            
            // Получаем название текущего мира
            String worldName = getCurrentWorldName(mc);
            if (worldName == null || worldName.isEmpty()) {
                worldName = "New World"; // Значение по умолчанию
            }
            
            // Основной путь: [gameDir]/exposures/[world_name]/
            Path exposuresDir = gameDir.resolve("exposures").resolve(worldName);
            
            if (exposuresDir.toFile().exists()) {
                ImmersivePaintingsExposure.LOGGER.debug("Found Exposure photos at: {} (world: {}, player: {})", 
                    exposuresDir, worldName, playerName);
                return exposuresDir;
            }
            
            // Альтернативные пути для проверки
            
            // Вариант 1: exposures/[world_name]/ (основной)
            ImmersivePaintingsExposure.LOGGER.debug("Checking path: {}", exposuresDir);
            
            // Вариант 2: exposures/ (без названия мира, все фотографии)
            Path exposuresRoot = gameDir.resolve("exposures");
            if (exposuresRoot.toFile().exists()) {
                ImmersivePaintingsExposure.LOGGER.debug("Found exposures root directory: {}", exposuresRoot);
                // Проверяем, есть ли подпапки с мирами
                File[] worldDirs = exposuresRoot.toFile().listFiles(File::isDirectory);
                if (worldDirs != null && worldDirs.length > 0) {
                    // Ищем папку с текущим миром
                    for (File worldDir : worldDirs) {
                        if (worldDir.getName().equals(worldName)) {
                            ImmersivePaintingsExposure.LOGGER.debug("Found world directory: {}", worldDir.toPath());
                            return worldDir.toPath();
                        }
                    }
                    // Если не нашли, возвращаем первую найденную папку мира
                    ImmersivePaintingsExposure.LOGGER.debug("World '{}' not found, using first available: {}", 
                        worldName, worldDirs[0].toPath());
                    return worldDirs[0].toPath();
                }
            }
            
            // Вариант 3: Старый формат exposure/photos/[uuid]/
            UUID playerUUID = player.getUUID();
            Path oldFormatPath = gameDir.resolve("exposure").resolve("photos").resolve(playerUUID.toString());
            if (oldFormatPath.toFile().exists()) {
                ImmersivePaintingsExposure.LOGGER.debug("Found Exposure photos in old format at: {}", oldFormatPath);
                return oldFormatPath;
            }
            
            // Вариант 4: Попытка через рефлексию получить путь из Exposure API
            try {
                Class<?> exposureClient = Class.forName("com.mortuusars.exposure.ExposureClient");
                java.lang.reflect.Method getPhotosDir = exposureClient.getDeclaredMethod("getPhotosDirectory");
                if (getPhotosDir != null) {
                    getPhotosDir.setAccessible(true);
                    Object photosDir = getPhotosDir.invoke(null);
                    if (photosDir instanceof Path) {
                        Path exposurePath = ((Path) photosDir).resolve(worldName);
                        if (exposurePath.toFile().exists()) {
                            ImmersivePaintingsExposure.LOGGER.debug("Found Exposure photos via API at: {}", exposurePath);
                            return exposurePath;
                        }
                    }
                }
            } catch (Exception e) {
                ImmersivePaintingsExposure.LOGGER.debug("Could not get path via Exposure API", e);
            }
            
            // Логируем информацию для отладки
            ImmersivePaintingsExposure.LOGGER.warn("Exposure photos directory not found. Checked paths:");
            ImmersivePaintingsExposure.LOGGER.warn("  - {}", exposuresDir);
            ImmersivePaintingsExposure.LOGGER.warn("  - {}", exposuresRoot);
            ImmersivePaintingsExposure.LOGGER.warn("  - {}", oldFormatPath);
            ImmersivePaintingsExposure.LOGGER.warn("Game directory: {}", gameDir);
            ImmersivePaintingsExposure.LOGGER.warn("World name: {}", worldName);
            ImmersivePaintingsExposure.LOGGER.warn("Player name: {}", playerName);
            
            // Возвращаем ожидаемый путь (даже если не существует, для создания)
            return exposuresDir;
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.error("Failed to get Exposure photos path", e);
            return null;
        }
    }
    
    /**
     * Получает название текущего мира
     */
    private String getCurrentWorldName(Minecraft mc) {
        try {
            // Попытка получить через уровень
            if (mc.level != null) {
                // Получаем имя уровня/мира
                if (mc.level.dimension() != null) {
                    // Попытка получить имя из сохранения
                    if (mc.getSingleplayerServer() != null) {
                        String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
                        if (levelName != null && !levelName.isEmpty()) {
                            return levelName;
                        }
                    }
                    
                    // Альтернативный способ - через путь к сохранению
                    if (mc.level.getServer() != null) {
                        try {
                            java.lang.reflect.Field levelStorageField = mc.level.getServer().getClass()
                                .getDeclaredField("storageSource");
                            if (levelStorageField != null) {
                                levelStorageField.setAccessible(true);
                                Object storageSource = levelStorageField.get(mc.level.getServer());
                                if (storageSource != null) {
                                    java.lang.reflect.Method getLevelName = storageSource.getClass()
                                        .getDeclaredMethod("getLevelName");
                                    if (getLevelName != null) {
                                        getLevelName.setAccessible(true);
                                        Object levelName = getLevelName.invoke(storageSource);
                                        if (levelName instanceof String) {
                                            return (String) levelName;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            ImmersivePaintingsExposure.LOGGER.debug("Could not get level name via reflection", e);
                        }
                    }
                }
            }
            
            // Если не удалось получить, пробуем через путь к сохранению
            if (mc.gameDirectory != null) {
                Path savesDir = mc.gameDirectory.toPath().resolve("saves");
                if (savesDir.toFile().exists()) {
                    // Если есть только один мир, используем его
                    File[] worlds = savesDir.toFile().listFiles(File::isDirectory);
                    if (worlds != null && worlds.length == 1) {
                        return worlds[0].getName();
                    }
                }
            }
            
        } catch (Exception e) {
            ImmersivePaintingsExposure.LOGGER.debug("Error getting world name", e);
        }
        
        return null;
    }
    
    /**
     * Конвертирует фотографию Exposure в формат, подходящий для Immersive Paintings
     */
    public ResourceLocation convertToPaintingTexture(ExposurePhoto photo) {
        // Здесь должна быть логика конвертации фотографии в текстуру для картины
        // Пока возвращаем оригинальный ResourceLocation
        return photo.getTextureLocation();
    }
}

