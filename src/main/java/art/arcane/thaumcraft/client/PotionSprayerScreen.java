package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.menu.PotionSprayerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiPotionSprayer: charge column in the potion colour and one progress bar per recipe aspect. */
public final class PotionSprayerScreen extends AbstractContainerScreen<PotionSprayerMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_potion_sprayer.png");
    public PotionSprayerScreen(PotionSprayerMenu menu, Inventory inv, Component title) { super(menu, inv, title, 192, 233); }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        int k = leftPos, l = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k, l, 0, 0, imageWidth, imageHeight, 256, 256);
        var s = menu.sprayer;
        if (s == null) return;
        if (s.charges > 0) {
            int scroll = minecraft.player.tickCount % 256;
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 128, l + 36 + (8 - s.charges) * 9, 232, scroll, 8, s.charges * 9, 256, 256, 0xFF000000 | s.color);
        }
        int pos = 0;
        for (var e : s.recipe.entrySet()) {
            int x = k + 96 + 22 * (pos % 2), y = l + 46 + 16 * (pos / 2);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y - 14, 192, 56, 2, 14, 256, 256, 0xFF333333);
            int i1 = (int) (s.progress.getOrDefault(e.getKey(), 0) / (float) e.getValue() * 14.0f);
            if (i1 > 0) g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y - i1, 192, 56, 2, i1, 256, 256, 0xFF000000 | AspectRegistry.color(e.getKey()));
            ++pos;
        }
        pos = 0;
        for (var e : s.recipe.entrySet()) {
            int x = k + 79 + 22 * (pos % 2), y = l + 31 + 16 * (pos / 2);
            ThaumcraftTheme.drawTag(g, font, e.getKey(), x, y, e.getValue());
            ++pos;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 125, l + 28, 205, 28, 14, 88, 256, 256);
    }
}
