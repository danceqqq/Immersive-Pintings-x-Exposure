package com.immersivepaintings.exposure.gui;

import com.immersivepaintings.exposure.ImmersivePaintingsExposure;
import com.immersivepaintings.exposure.exposure.ExposurePhoto;
import com.immersivepaintings.exposure.exposure.ExposurePhotoManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран вкладки Exposure для выбора фотографий
 */
public class ExposureTabScreen extends Screen {
    
    private static final ResourceLocation EXPOSURE_ICON = new ResourceLocation(
        ImmersivePaintingsExposure.MOD_ID, "textures/gui/exposure_icon.png");
    
    private final Screen parentScreen;
    private final Player player;
    private List<ExposurePhoto> availablePhotos;
    private PhotoListWidget photoList;
    private int scrollOffset = 0;
    
    // Callback для выбора фотографии
    private PhotoSelectionCallback selectionCallback;
    
    public interface PhotoSelectionCallback {
        void onPhotoSelected(ExposurePhoto photo);
    }
    
    public ExposureTabScreen(Screen parentScreen, Player player, PhotoSelectionCallback callback) {
        super(Component.translatable("gui.immersivepaintings_exposure.tab.exposure"));
        this.parentScreen = parentScreen;
        this.player = player;
        this.selectionCallback = callback;
        this.availablePhotos = new ArrayList<>();
    }
    
    @Override
    protected void init() {
        super.init();
        
        // Загружаем доступные фотографии
        loadAvailablePhotos();
        
        // Создаем список фотографий
        int listWidth = this.width - 40;
        int listHeight = this.height - 100;
        int listX = 20;
        int listY = 60;
        
        this.photoList = new PhotoListWidget(
            this.minecraft, 
            listWidth, 
            listHeight, 
            listY, 
            listY + listHeight,
            40,
            listX,
            this.availablePhotos,
            this
        );
        
        this.addWidget(this.photoList);
        
        // Кнопка "Назад"
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.back"),
            (button) -> this.minecraft.setScreen(this.parentScreen)
        ).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
        
        // Кнопка "Обновить"
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.immersivepaintings_exposure.refresh"),
            (button) -> {
                loadAvailablePhotos();
                this.photoList.updatePhotos(this.availablePhotos);
            }
        ).bounds(this.width / 2 + 120, this.height - 30, 80, 20).build());
    }
    
    private void loadAvailablePhotos() {
        ExposurePhotoManager manager = ExposurePhotoManager.getInstance();
        this.availablePhotos = manager.getAvailablePhotos(this.player);
        ImmersivePaintingsExposure.LOGGER.debug("Loaded {} Exposure photos", this.availablePhotos.size());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Рендерим фон
        this.renderBackground(guiGraphics);
        
        // Заголовок
        guiGraphics.drawString(
            this.font,
            Component.translatable("gui.immersivepaintings_exposure.tab.exposure.title"),
            this.width / 2 - this.font.width(Component.translatable("gui.immersivepaintings_exposure.tab.exposure.title")) / 2,
            20,
            0xFFFFFF,
            false
        );
        
        // Рендерим список фотографий
        if (this.photoList != null) {
            this.photoList.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Информация о количестве фотографий
        String photoCountText = Component.translatable(
            "gui.immersivepaintings_exposure.photo_count",
            this.availablePhotos.size()
        ).getString();
        guiGraphics.drawString(
            this.font,
            photoCountText,
            20,
            this.height - 50,
            0xCCCCCC,
            false
        );
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.photoList != null && this.photoList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.photoList != null && this.photoList.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    public void onPhotoSelected(ExposurePhoto photo) {
        if (this.selectionCallback != null) {
            this.selectionCallback.onPhotoSelected(photo);
        }
    }
}

