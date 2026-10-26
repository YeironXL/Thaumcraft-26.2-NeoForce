package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.VoidSiphonMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiVoidSiphon: the background panel with the output slot. */
public final class VoidSiphonScreen extends AbstractContainerScreen<VoidSiphonMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_void_siphon.png");
    public VoidSiphonScreen(VoidSiphonMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 166); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
