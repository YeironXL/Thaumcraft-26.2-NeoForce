package art.arcane.thaumcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** TC6 client.gui.ResearchToast: hud.png frame (0,224,160,32), research icon, "research.complete" and the entry name. */
public final class ResearchToast implements Toast {
    private static final Identifier HUD = Identifier.parse("thaumcraft:textures/gui/hud.png");
    private final String key;
    private final Component name;
    private long first = -1;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    public ResearchToast(String key, Component name) { this.key = key; this.name = name; }
    @Override public Toast.Visibility getWantedVisibility() { return visibility; }
    @Override public void update(ToastManager manager, long time) {
        if (first < 0) first = time;
        visibility = time - first < 5000L ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }
    @Override public int width() { return 160; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, Font font, long time) {
        g.blit(RenderPipelines.GUI_TEXTURED, HUD, 0, 0, 0, 224, 160, 32, 256, 256);
        ResearchIcons.draw(g, key, 6, 8, 0xFFFFFFFF);
        g.text(font, Component.translatable("research.complete"), 30, 7, 0xFF000000 | 10631665, false);
        float w = font.width(name);
        if (w > 124f) {
            float s = 124f / w;
            g.pose().pushMatrix(); g.pose().translate(30f, 18f); g.pose().scale(s, s);
            g.text(font, name, 0, 0, 0xFF000000 | 16755465, false);
            g.pose().popMatrix();
        } else g.text(font, name, 30, 18, 0xFF000000 | 16755465, false);
    }
}
