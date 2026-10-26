package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity;
import art.arcane.thaumcraft.entity.construct.TurretCrossbowAdvancedEntity;
import art.arcane.thaumcraft.entity.construct.TurretMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Blocks;

/** TC6 GuiTurretBasic / GuiTurretAdvanced / GuiArcaneBore. */
public final class TurretScreen extends AbstractContainerScreen<TurretMenu> {
    private final Identifier tex;
    public TurretScreen(TurretMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 175, 232);
        tex = Identifier.fromNamespaceAndPath("thaumcraft", menu.construct instanceof ArcaneBoreEntity ? "textures/gui/gui_arcanebore.png"
                : menu.construct instanceof TurretCrossbowAdvancedEntity ? "textures/gui/gui_turret_advanced.png" : "textures/gui/gui_turret_basic.png");
    }
    private void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) { g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, 256, 256); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        int k = leftPos, l = topPos;
        blit(g, k, l, 0, 0, imageWidth, imageHeight);
        var c = menu.construct;
        int h = (int) (39.0f * (c.getHealth() / c.getMaxHealth()));
        blit(g, k + (c instanceof TurretCrossbowAdvancedEntity ? 30 : 68), l + 59, 192, 48, h, 6);
        if (c instanceof TurretCrossbowAdvancedEntity tt) {
            boolean[] v = {tt.getTargetAnimal(), tt.getTargetMob(), tt.getTargetPlayer(), tt.getTargetFriendly()};
            for (int i = 0; i < 4; i++) {
                int y = l + 13 + i * 14;
                blit(g, k + 90, y, 192, 16, 8, 8);
                if (v[i]) blit(g, k + 90, y, 192, 24, 8, 8);
                g.text(font, Component.translatable("button.turretfocus." + (i + 1)), k + 102, y, 0xFFFFFFFF, true);
            }
        }
        if (c instanceof ArcaneBoreEntity b) {
            var held = b.getMainHandItem();
            if (!held.isEmpty() && held.isDamageableItem() && held.getDamageValue() + 1 >= held.getMaxDamage()) blit(g, k + 80, l + 29, 240, 0, 16, 16);
            g.pose().pushMatrix(); g.pose().translate(k + 124, l + 18); g.pose().scale(0.5f, 0.5f);
            g.text(font, Component.literal("Width: " + (1 + b.getDigRadius() * 2)), 0, 0, 0xFFFFFFFF, true);
            g.text(font, Component.literal("Depth: " + b.getDigDepth()), 64, 0, 0xFFFFFFFF, true);
            g.text(font, Component.literal("Speed: +" + b.getDigSpeed(Blocks.STONE.defaultBlockState())), 0, 10, 0xFFFFFFFF, true);
            int base = 0, refining = b.getRefining(), fortune = b.getFortune();
            if (b.hasSilkTouch() || refining > 0 || fortune > 0) g.text(font, Component.literal("Other properties:"), 0, 24, 0xFFFFFFFF, true);
            if (refining > 0) { g.text(font, Component.literal("Refining " + refining), 4, 34 + base, 0xFFC0C0C0, true); base += 9; }
            if (fortune > 0) { g.text(font, Component.literal("Fortune " + fortune), 4, 34 + base, 0xFFEEC44A, true); base += 9; }
            if (b.hasSilkTouch()) g.text(font, Component.literal("Silk Touch"), 4, 34 + base, 0xFF8080FF, true);
            g.pose().popMatrix();
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (menu.construct instanceof TurretCrossbowAdvancedEntity) for (int i = 0; i < 4; i++) {
            int x = leftPos + 90, y = topPos + 13 + i * 14;
            if (e.x() >= x && e.y() >= y && e.x() < x + 8 && e.y() < y + 8) { Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, i + 1); return true; }
        }
        return super.mouseClicked(e, dbl);
    }
}
