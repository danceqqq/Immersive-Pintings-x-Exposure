package com.immersivepaintings.exposure.exposure;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * Представляет фотографию из мода Exposure
 */
public class ExposurePhoto {
    private final String name;
    private final Path filePath;
    private final ResourceLocation textureLocation;
    
    public ExposurePhoto(String name, Path filePath, ResourceLocation textureLocation) {
        this.name = name;
        this.filePath = filePath;
        this.textureLocation = textureLocation;
    }
    
    public String getName() {
        return name;
    }
    
    public Path getFilePath() {
        return filePath;
    }
    
    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }
    
    @Override
    public String toString() {
        return "ExposurePhoto{name='" + name + "'}";
    }
}

