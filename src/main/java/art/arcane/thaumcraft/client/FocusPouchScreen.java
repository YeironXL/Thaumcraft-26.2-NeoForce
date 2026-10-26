package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.FocusPouchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiFocusPouch. */
public final class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_focuspouch.png");
    public FocusPouchScreen(FocusPouchMenu menu, Inventory inv, Component title) { super(menu, inv, title, 175, 232); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, 8 + menu.lockedHotbarSlot() * 18, 209, 240, 0, 16, 16, 256, 256);
    }
}
