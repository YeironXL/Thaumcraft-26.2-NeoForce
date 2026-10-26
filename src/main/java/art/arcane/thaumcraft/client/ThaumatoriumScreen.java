package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import art.arcane.thaumcraft.menu.ThaumatoriumMenu;
import art.arcane.thaumcraft.network.ThaumatoriumSelectPayload;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** TC6 GuiThaumatorium: pick up to N recipes for the catalyst in the slot; shows the cycling recipe's essentia progress. */
public final class ThaumatoriumScreen extends AbstractContainerScreen<ThaumatoriumMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_thaumatorium.png");
    private int index;
    private List<ThaumatoriumBlockEntity.Entry> hashList = new ArrayList<>();
    private long lastHLUpdate;

    public ThaumatoriumScreen(ThaumatoriumMenu menu, Inventory inv, Component title) { super(menu, inv, title, 175, 216); }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    private boolean isSelected(String id) { return menu.te != null && menu.te.recipeHash.contains(id); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        int k = leftPos, l = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k, l, 0, 0, imageWidth, imageHeight, 256, 256);
        var te = menu.te;
        if (te == null) return;
        long t = System.currentTimeMillis();
        if (t > lastHLUpdate) { hashList = te.generateRecipeHashlist(); lastHLUpdate = t + 500L; }
        if (!hashList.isEmpty()) {
            if (index > hashList.size() / 2) index = hashList.size() / 2;
            if (index < 0 || hashList.size() <= 6) index = 0;
            if (hashList.size() > 6) {
                if (index > 0) g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 82, l + 56, 176, 56, 8, 11, 256, 256);
                if (index < hashList.size() / 2.0f - 3.0f) g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 82, l + 93, 176, 93, 8, 11, 256, 256);
            }
        }
        // essentia of the recipe currently cycled through (one per second)
        if (!te.selected.isEmpty()) {
            var cr = te.selected.get((int) (t / 1000L % te.selected.size()));
            int count = 0, px = 0, py = 0;
            for (var e : new TreeMap<>(cr.aspects()).entrySet()) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 98 + 16 * px, l + 40 + 20 * py, 176, 4, 12, 3, 256, 256);
                int i1 = (int) (te.essentia.getOrDefault(e.getKey(), 0) / (float) e.getValue() * 12.0f);
                if (i1 > 0) g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 98 + 16 * px, l + 40 + 20 * py, 176, 0, i1, 3, 256, 256, 0xFF000000 | AspectRegistry.color(e.getKey()));
                ThaumcraftTheme.drawTag(g, font, e.getKey(), k + 96 + 16 * px, l + 24 + 20 * py, e.getValue());
                if (++px > 1) { px = 0; ++py; }
                if (++count >= 8) break;
            }
        }
        if (te.maxRecipes > 1) {
            String text = te.recipeHash.size() + "/" + te.maxRecipes;
            g.pose().pushMatrix();
            g.pose().translate(k + 64, l + 48);
            g.pose().scale(0.5f, 0.5f);
            g.text(font, text, -font.width(text) / 2, 0, 0xFFFFFFFF, false);
            g.pose().popMatrix();
        }
        // outputs
        int px = 0, py = 0, q = 0, idx = 0;
        for (var e : hashList) {
            if (q++ < index * 2) continue;
            int x = k + 48 + px * 16, y = l + 56 + py * 16;
            if (isSelected(e.id())) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, 176, 8, 16, 16, 256, 256, 0xB3FFFFFF);
                if (te.selected.size() > 1 && te.selected.get((int) (t / 1000L % te.selected.size())).id().equals(e.id()))
                    g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, 176, 8, 16, 16, 256, 256, 0xB3FFFFFF);
            }
            g.item(e.output(), x, y);
            g.itemDecorations(font, e.output(), x, y);
            if (++px > 1) { px = 0; ++py; }
            if (++idx >= 6) break;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractRenderState(g, mx, my, pt);
        int px = 0, py = 0, q = 0, idx = 0;
        for (var e : hashList) {
            if (q++ < index * 2) continue;
            int xx = mx - (leftPos + 48 + px * 16), yy = my - (topPos + 56 + py * 16);
            if (xx >= 0 && yy >= 0 && xx < 16 && yy < 16) { g.setTooltipForNextFrame(font, e.output(), mx, my); break; }
            if (++px > 1) { px = 0; ++py; }
            if (++idx >= 6) break;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent ev, boolean dbl) {
        int mx = (int) ev.x(), my = (int) ev.y();
        int px = 0, py = 0, q = 0, idx = 0;
        for (var e : hashList) {
            if (q++ < index * 2) continue;
            int x = mx - (leftPos + 48 + px * 16), y = my - (topPos + 56 + py * 16);
            if (x >= 0 && y >= 0 && x < 16 && y < 16) {
                ClientPacketDistributor.sendToServer(new ThaumatoriumSelectPayload(menu.te.getBlockPos(), e.id()));
                minecraft.player.playSound(ModSounds.get("hhon"), 0.3f, 1.0f);
                lastHLUpdate = 0L;
                return true;
            }
            if (++px > 1) { px = 0; ++py; }
            if (++idx >= 6) break;
        }
        if (hashList.size() > 6) {
            int x2 = mx - (leftPos + 82);
            if (index > 0 && x2 >= 0 && x2 < 8 && my - (topPos + 56) >= 0 && my - (topPos + 56) < 11) { --index; click(); return true; }
            if (index < hashList.size() / 2.0f - 3.0f && x2 >= 0 && x2 < 8 && my - (topPos + 93) >= 0 && my - (topPos + 93) < 11) { ++index; click(); return true; }
        }
        return super.mouseClicked(ev, dbl);
    }
    private void click() { minecraft.player.playSound(ModSounds.get("clack"), 0.4f, 1.0f); lastHLUpdate = 0L; }
}
