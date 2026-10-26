package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.DeconstructionTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Deconstruction table GUI: item slot and a progress bar in the TC6 stone-panel style. */
public final class DeconstructionTableScreen extends AbstractContainerScreen<DeconstructionTableMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_deconstruction.png");
    public DeconstructionTableScreen(DeconstructionTableMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 166); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        int w = (int) (26 * menu.progress());
        if (w > 0) g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos + 88, topPos + 37, 176, 0, w, 9, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractRenderState(g, mx, my, t);
        int x = mx - (leftPos + 87), y = my - (topPos + 36);
        if (x >= 0 && y >= 0 && x < 28 && y < 11)
            g.setComponentTooltipForNextFrame(font, java.util.List.of(Component.translatable("tc.deconstruction.progress", (int) (menu.progress() * 100))), mx, my);
    }
}
