package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.HandMirrorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiHandMirror: the looking glass, with the hand mirror's hotbar slot covered. */
public final class HandMirrorScreen extends AbstractContainerScreen<HandMirrorMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_handmirror.png");
    public HandMirrorScreen(HandMirrorMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 166); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}
    /** The red cross over the mirror's own hotbar slot is drawn above the item (TC6 zLevel 300). */
    @Override protected void extractSlots(GuiGraphicsExtractor g, int mx, int my) {
        super.extractSlots(g, mx, my);
        g.nextStratum();
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, 8 + menu.currentItem * 18, 142, 240, 0, 16, 16, 256, 256);
    }
}
