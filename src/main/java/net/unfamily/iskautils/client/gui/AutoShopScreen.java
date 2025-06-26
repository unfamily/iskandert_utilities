package net.unfamily.iskautils.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.unfamily.iskautils.IskaUtils;

public class AutoShopScreen extends AbstractContainerScreen<AutoShopMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(IskaUtils.MOD_ID, "textures/gui/backgrounds/auto_shop.png");
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 160;

    public AutoShopScreen(AutoShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, GUI_WIDTH, GUI_HEIGHT);
    }
    
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Non chiamiamo super.renderLabels() per evitare titolo duplicato e scritta inventory
        
        // Titolo centrato in alto
        Component title = Component.translatable("block.iska_utils.auto_shop");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 8, 0x404040, false);
        
        // Testo accanto alla slot selezionata (slot 0) - spostato un po' più in alto
        Component selectText = Component.translatable("gui.iska_utils.auto_shop.select_item");
        guiGraphics.drawString(this.font, selectText, 75, 27, 0x404040, false);
        
        // Testo accanto alla slot encapsulated (slot 1) - spostato un po' più in alto
        Component encapsulatedText = Component.translatable("gui.iska_utils.auto_shop.encapsulated_item");
        guiGraphics.drawString(this.font, encapsulatedText, 75, 52, 0x404040, false);
    }
    
    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
} 