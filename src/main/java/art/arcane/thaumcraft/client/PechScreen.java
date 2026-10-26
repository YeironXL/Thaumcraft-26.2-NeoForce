package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.PechMenu;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiPech: the 175x232 trade screen; the dice (67,24) lights up and rolls the trade when a valued item is offered. */
public final class PechScreen extends AbstractContainerScreen<PechMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_pech.png");
    public PechScreen(PechMenu menu, Inventory inv, Component title) { super(menu, inv, title, 175, 232); }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (menu.canRoll()) g.blit(RenderPipelines.GUI_TEXTURED, TEX, leftPos + 67, topPos + 24, 176, 0, 25, 25, 256, 256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        double x = event.x() - (leftPos + 67), y = event.y() - (topPos + 24);
        if (x >= 0 && y >= 0 && x < 25 && y < 25 && menu.canRoll()) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            var e = minecraft.getCameraEntity();
            if (e != null) e.playSound(ModSounds.get("pech_dice"), 0.5f, 0.95f + e.level().getRandom().nextFloat() * 0.1f);
            return true;
        }
        return super.mouseClicked(event, dbl);
    }
}
