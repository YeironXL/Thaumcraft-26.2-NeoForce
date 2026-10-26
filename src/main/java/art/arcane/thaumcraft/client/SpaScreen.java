package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.menu.SpaMenu;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** TC6 GuiSpa: bath-salts slot, mix toggle and the tank gauge with its fluid. */
public final class SpaScreen extends AbstractContainerScreen<SpaMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_spa.png");
    public SpaScreen(SpaMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 166); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        int k = leftPos, l = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k, l, 0, 0, imageWidth, imageHeight, 256, 256);
        var spa = menu.spa;
        if (spa == null) return;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 89, l + 35, 208, spa.getMix() ? 16 : 32, 8, 8, 256, 256);
        var fs = spa.fluid();
        if (!fs.isEmpty()) {
            var model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fs.getFluid().defaultFluidState());
            var sprite = model.stillMaterial().sprite();
            int color = model.fluidTintSource() != null ? model.fluidTintSource().colorAsStack(fs) | 0xFF000000 : 0xFFFFFFFF;
            float bar = fs.getAmount() / 5000.0f;
            g.pose().pushMatrix();
            g.pose().translate(k + 107, l + 15);
            g.pose().scale(0.5f, 0.5f);
            for (int a = 0; a < 6; ++a) g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, 0, a * 16, 16, 16, color);
            g.pose().popMatrix();
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 107, l + 15, 107, 15, 10, (int) (48.0f - 48.0f * bar), 256, 256);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k + 106, l + 11, 232, 0, 10, 55, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractRenderState(g, mx, my, t);
        var spa = menu.spa;
        if (spa == null) return;
        int x = mx - (leftPos + 104), y = my - (topPos + 10);
        if (x >= 0 && y >= 0 && x < 10 && y < 55 && !spa.fluid().isEmpty()) {
            List<Component> list = new ArrayList<>();
            list.add(spa.fluid().getHoverName());
            list.add(Component.literal(spa.fluid().getAmount() + " mb"));
            g.setComponentTooltipForNextFrame(font, list, mx, my);
        }
        x = mx - (leftPos + 88); y = my - (topPos + 34);
        if (x >= 0 && y >= 0 && x < 10 && y < 10)
            g.setComponentTooltipForNextFrame(font, List.of(Component.translatable(spa.getMix() ? "text.spa.mix.true" : "text.spa.mix.false")), mx, my);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        int x = (int) e.x() - (leftPos + 89), y = (int) e.y() - (topPos + 35);
        if (x >= 0 && y >= 0 && x < 8 && y < 8) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 1);
            Minecraft.getInstance().player.playSound(ModSounds.get("clack"), 0.4f, 1.0f);
            return true;
        }
        return super.mouseClicked(e, dbl);
    }
}
