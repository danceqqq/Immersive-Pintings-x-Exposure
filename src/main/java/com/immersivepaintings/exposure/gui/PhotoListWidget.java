package com.immersivepaintings.exposure.gui;

import com.immersivepaintings.exposure.exposure.ExposurePhoto;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Виджет списка фотографий с возможностью прокрутки
 */
public class PhotoListWidget extends AbstractWidget {
    
    private final Minecraft minecraft;
    private final int itemHeight;
    private final int itemWidth;
    private List<ExposurePhoto> photos;
    private int scrollAmount = 0;
    private ExposurePhoto selectedPhoto = null;
    private final ExposureTabScreen parentScreen;
    
    // Параметры сетки
    private static final int PHOTOS_PER_ROW = 4;
    private static final int PHOTO_PADDING = 10;
    
    public PhotoListWidget(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight, int x, List<ExposurePhoto> photos, ExposureTabScreen parentScreen) {
        super(x, y0, width, height - y0, net.minecraft.network.chat.Component.empty());
        this.minecraft = minecraft;
        this.itemHeight = itemHeight;
        this.itemWidth = (width - (PHOTOS_PER_ROW + 1) * PHOTO_PADDING) / PHOTOS_PER_ROW;
        this.photos = new ArrayList<>(photos);
        this.parentScreen = parentScreen;
    }
    
    public void updatePhotos(List<ExposurePhoto> newPhotos) {
        this.photos = new ArrayList<>(newPhotos);
        this.scrollAmount = 0;
        this.selectedPhoto = null;
    }
    
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.photos.isEmpty()) {
            String noPhotosText = net.minecraft.network.chat.Component.translatable(
                "gui.immersivepaintings_exposure.no_photos"
            ).getString();
            guiGraphics.drawString(
                this.minecraft.font,
                noPhotosText,
                this.getX() + this.width / 2 - this.minecraft.font.width(noPhotosText) / 2,
                this.getY() + this.height / 2,
                0xCCCCCC,
                false
            );
            return;
        }
        
        int rows = (this.photos.size() + PHOTOS_PER_ROW - 1) / PHOTOS_PER_ROW;
        int maxVisibleRows = this.height / (this.itemHeight + PHOTO_PADDING);
        int maxScroll = Math.max(0, rows - maxVisibleRows);
        
        this.scrollAmount = Math.max(0, Math.min(this.scrollAmount, maxScroll));
        
        int startRow = this.scrollAmount;
        int endRow = Math.min(startRow + maxVisibleRows, rows);
        
        for (int row = startRow; row < endRow; row++) {
            for (int col = 0; col < PHOTOS_PER_ROW; col++) {
                int index = row * PHOTOS_PER_ROW + col;
                if (index >= this.photos.size()) {
                    break;
                }
                
                ExposurePhoto photo = this.photos.get(index);
                int photoX = this.getX() + col * (this.itemWidth + PHOTO_PADDING) + PHOTO_PADDING;
                int photoY = this.getY() + (row - startRow) * (this.itemHeight + PHOTO_PADDING) + PHOTO_PADDING;
                
                renderPhoto(guiGraphics, photo, photoX, photoY, mouseX, mouseY);
            }
        }
    }
    
    private void renderPhoto(GuiGraphics guiGraphics, ExposurePhoto photo, int x, int y, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= x && mouseX < x + this.itemWidth &&
                           mouseY >= y && mouseY < y + this.itemHeight;
        boolean isSelected = photo.equals(this.selectedPhoto);
        
        // Фон фотографии
        int bgColor = isSelected ? 0xFF00FF00 : (isHovered ? 0xFF666666 : 0xFF333333);
        guiGraphics.fill(x - 2, y - 2, x + this.itemWidth + 2, y + this.itemHeight + 2, bgColor);
        
        // Попытка загрузить и отобразить текстуру фотографии
        try {
            ResourceLocation textureLoc = PhotoTextureManager.getInstance()
                .getOrLoadTexture(photo.getFilePath(), photo.getName());
            
            if (textureLoc != null) {
                // Отображаем текстуру фотографии
                RenderSystem.setShaderTexture(0, textureLoc);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                
                // Вычисляем размеры для отображения с сохранением пропорций
                int imageWidth = this.itemWidth - 4;
                int imageHeight = this.itemHeight - 4;
                
                guiGraphics.blit(
                    textureLoc,
                    x + 2,
                    y + 2,
                    0,
                    0,
                    imageWidth,
                    imageHeight,
                    imageWidth,
                    imageHeight
                );
                
                RenderSystem.disableBlend();
            } else {
                // Если текстура не загружена, отображаем имя фотографии
                String displayName = photo.getName();
                if (displayName.length() > 15) {
                    displayName = displayName.substring(0, 15) + "...";
                }
                guiGraphics.drawString(
                    this.minecraft.font,
                    displayName,
                    x + 4,
                    y + this.itemHeight / 2 - 4,
                    0xFFFFFF,
                    false
                );
            }
        } catch (Exception e) {
            // Если не удалось загрузить, отображаем имя фотографии
            String displayName = photo.getName();
            if (displayName.length() > 15) {
                displayName = displayName.substring(0, 15) + "...";
            }
            guiGraphics.drawString(
                this.minecraft.font,
                displayName,
                x + 4,
                y + this.itemHeight / 2 - 4,
                0xCCCCCC,
                false
            );
        }
        
        // Подсказка при наведении
        if (isHovered) {
            guiGraphics.renderTooltip(
                this.minecraft.font,
                net.minecraft.network.chat.Component.literal(photo.getName()),
                mouseX,
                mouseY
            );
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        if (button == 0) { // Левая кнопка мыши
            int rows = (this.photos.size() + PHOTOS_PER_ROW - 1) / PHOTOS_PER_ROW;
            int maxVisibleRows = this.height / (this.itemHeight + PHOTO_PADDING);
            int startRow = this.scrollAmount;
            
            int relativeY = (int) (mouseY - this.getY());
            int row = (relativeY / (this.itemHeight + PHOTO_PADDING)) + startRow;
            int col = (int) ((mouseX - this.getX() - PHOTO_PADDING) / (this.itemWidth + PHOTO_PADDING));
            
            if (row >= 0 && row < rows && col >= 0 && col < PHOTOS_PER_ROW) {
                int index = row * PHOTOS_PER_ROW + col;
                if (index >= 0 && index < this.photos.size()) {
                    this.selectedPhoto = this.photos.get(index);
                    
                    // Двойной клик для выбора
                    if (this.parentScreen != null) {
                        this.parentScreen.onPhotoSelected(this.selectedPhoto);
                    }
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.isMouseOver(mouseX, mouseY)) {
            int rows = (this.photos.size() + PHOTOS_PER_ROW - 1) / PHOTOS_PER_ROW;
            int maxVisibleRows = this.height / (this.itemHeight + PHOTO_PADDING);
            int maxScroll = Math.max(0, rows - maxVisibleRows);
            
            this.scrollAmount = (int) Math.max(0, Math.min(this.scrollAmount - delta, maxScroll));
            return true;
        }
        return false;
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, 
            net.minecraft.network.chat.Component.translatable("gui.immersivepaintings_exposure.photo_list"));
    }
    
    public ExposurePhoto getSelectedPhoto() {
        return this.selectedPhoto;
    }
}

