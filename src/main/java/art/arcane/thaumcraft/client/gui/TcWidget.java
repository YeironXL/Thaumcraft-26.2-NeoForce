package art.arcane.thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

/** Minimal equivalents of the TC6 GuiButton plugins (positions and behaviour kept from 1.12). */
public abstract class TcWidget {
    public static final Identifier BASE = Identifier.parse("thaumcraft:textures/gui/gui_base.png");
    public final int id;
    public int x, y, width, height;
    public boolean visible = true, enabled = true, hovered;

    protected TcWidget(int id, int x, int y, int w, int h) { this.id = id; this.x = x; this.y = y; this.width = w; this.height = h; }
    public abstract void render(GuiGraphicsExtractor g, int mx, int my);
    /** GuiButton.mousePressed. */
    public boolean press(double mx, double my) { return enabled && visible && mx >= x && my >= y && mx < x + width && my < y + height; }
    public void release(double mx, double my) {}
    public void drag(double mx, double my) {}
    public boolean isMouseOver() { return hovered; }
    public List<Component> tooltip() { return List.of(); }

    /** GuiSliderTC. */
    public static final class Slider extends TcWidget {
        private float pos;
        public boolean down;
        private final float min, max;
        private final boolean vertical;
        public Slider(int id, int x, int y, int w, int h, float min, float max, float def, boolean vertical) {
            super(id, x, y, w, h); this.min = min; this.max = max; this.vertical = vertical; pos = (def - min) / (max - min);
        }
        public float getMax() { return max; }
        public float getMin() { return min; }
        public float getSliderValue() { return min + (max - min) * pos; }
        public void setSliderValue(float v) { pos = (v - min) / (max - min); }
        private void set(double mx, double my) {
            pos = vertical ? (float) (my - (y + 4)) / (height - 8) : (float) (mx - (x + 4)) / (width - 8);
            pos = Math.clamp(pos, 0f, 1f);
        }
        @Override public boolean press(double mx, double my) { if (super.press(mx, my)) { set(mx, my); return down = true; } return false; }
        @Override public void release(double mx, double my) { down = false; }
        @Override public void drag(double mx, double my) { if (down) set(mx, my); }
        @Override public void render(GuiGraphicsExtractor g, int mx, int my) {
            if (!visible) return;
            hovered = mx >= x && my >= y && mx < x + width && my < y + height;
            if (vertical) {
                g.pose().pushMatrix(); g.pose().translate(x + 2, y); g.pose().scale(1.0f, height / 32.0f);
                g.blit(RenderPipelines.GUI_TEXTURED, BASE, 0, 0, 240, 176, 4, 32, 256, 256); g.pose().popMatrix();
                g.blit(RenderPipelines.GUI_TEXTURED, BASE, x, y + (int) (pos * (height - 8)), 20, 20, 8, 8, 256, 256);
            } else {
                g.pose().pushMatrix(); g.pose().translate(x, y + 2); g.pose().scale(width / 32.0f, 1.0f);
                g.blit(RenderPipelines.GUI_TEXTURED, BASE, 0, 0, 208, 176, 32, 4, 256, 256); g.pose().popMatrix();
                g.blit(RenderPipelines.GUI_TEXTURED, BASE, x + (int) (pos * (width - 8)), y, 20, 20, 8, 8, 256, 256);
            }
        }
    }

    /** GuiFocusSettingSpinnerButton: [-] value [+] for a NodeSetting. */
    public static final class Spinner extends TcWidget {
        private final art.arcane.thaumcraft.casters.NodeSetting setting;
        public Spinner(int id, int x, int y, int w, art.arcane.thaumcraft.casters.NodeSetting ns) { super(id, x, y, w, 10); setting = ns; }
        @Override public void render(GuiGraphicsExtractor g, int mx, int my) {
            if (!visible) return;
            hovered = mx >= x && my >= y && mx < x + width + 10 && my < y + height;
            int c = hovered ? 0xFFFFFFFF : 0xE6E6E6E6;
            g.blit(RenderPipelines.GUI_TEXTURED, BASE, x, y, 20, 0, 10, 10, 256, 256, c);
            g.blit(RenderPipelines.GUI_TEXTURED, BASE, x + width, y, 30, 0, 10, 10, 256, 256, c);
            var font = Minecraft.getInstance().font;
            var s = setting.getValueText();
            g.text(font, s, x + (width + 10) / 2 - font.width(s) / 2, y + 1, 0xFFFFFFFF, true);
        }
        @Override public boolean press(double mx, double my) {
            if (!enabled || !visible) return false;
            if (mx >= x && my >= y && mx < x + 10 && my < y + height) { setting.decrement(); return true; }
            if (mx >= x + width && my >= y && mx < x + width + 10 && my < y + height) { setting.increment(); return true; }
            return false;
        }
    }

    /** GuiHoverButton: icon (texture or item) centred on (x, y) with a tooltip, not clickable. */
    public static final class Hover extends TcWidget {
        private final Component text, description;
        private final Object tex;
        public Hover(int id, int x, int y, int w, int h, Component text, Component description, Object tex) { super(id, x, y, w, h); this.text = text; this.description = description; this.tex = tex; }
        @Override public void render(GuiGraphicsExtractor g, int mx, int my) {
            if (!visible) return;
            hovered = mx >= x - width / 2 && my >= y - height / 2 && mx < x - width / 2 + width && my < y - height / 2 + height;
            int col = hovered ? 0xFFFFFFFF : ARGB.colorFromFloat(0.9f, 0.9f, 0.9f, 0.9f);
            if (tex instanceof Identifier id) g.blit(RenderPipelines.GUI_TEXTURED, id, x - width / 2, y - height / 2, 0, 0, 16, 16, 16, 16, col);
            if (tex instanceof ItemStack st) g.item(st, x - width / 2, y - height / 2 - (hovered ? 1 : 0));
        }
        @Override public boolean press(double mx, double my) { return false; }
        @Override public List<Component> tooltip() {
            List<Component> l = new ArrayList<>();
            if (tex instanceof ItemStack st) l.add(st.getHoverName()); else l.add(text);
            if (description != null) l.add(description.copy().withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.BLUE));
            return l;
        }
    }

    /** GuiImageButton: sprite centred on (x, y) with a half-size label. */
    public static final class Image extends TcWidget {
        private final Component label, description;
        private final Identifier loc;
        private final int lx, ly, ww, hh;
        public boolean active = true;
        public Image(int id, int x, int y, int w, int h, Component label, Component description, Identifier loc, int lx, int ly, int ww, int hh) {
            super(id, x, y, w, h); this.label = label; this.description = description; this.loc = loc; this.lx = lx; this.ly = ly; this.ww = ww; this.hh = hh;
        }
        @Override public void render(GuiGraphicsExtractor g, int mx, int my) {
            if (!visible) return;
            hovered = mx >= x - width / 2 && my >= y - height / 2 && mx < x - width / 2 + width && my < y - height / 2 + height;
            float cc = hovered ? 1.0f : 0.9f, ac = 1.0f;
            if (!active) { cc = 0.5f; ac = 0.9f; }
            g.blit(RenderPipelines.GUI_TEXTURED, loc, x - ww / 2, y - hh / 2, lx, ly, ww, hh, 256, 256, ARGB.colorFromFloat(ac, cc, cc, cc));
            if (label != null) {
                var font = Minecraft.getInstance().font;
                int j = !enabled ? 0xFFA0A0A0 : hovered ? 0xFFFFFFA0 : 0xFFFFFFFF;
                g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(0.5f, 0.5f);
                g.text(font, label, -font.width(label) / 2, -4, j, true);
                g.pose().popMatrix();
            }
        }
        @Override public boolean press(double mx, double my) {
            return active && enabled && visible && mx >= x - width / 2 && my >= y - height / 2 && mx < x - width / 2 + width && my < y - height / 2 + height;
        }
        @Override public List<Component> tooltip() {
            List<Component> l = new ArrayList<>();
            if (label != null) l.add(label);
            if (description != null) l.add(description.copy().withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.BLUE));
            return l;
        }
    }
}
