package art.arcane.thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Util;

public class ThaumcraftButton extends Button {
    public ThaumcraftButton(Builder builder) { super(builder); }

    @Override
    protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        ThaumcraftTheme.button(g, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active);
        var font = Minecraft.getInstance().font;
        var label = ThaumcraftTheme.ink(getMessage());
        int textWidth = font.width(label);
        int available = Math.max(1, getWidth() - 4);
        int x = getX() + (getWidth() - textWidth) / 2;
        if (textWidth > available) {
            int overflow = textWidth - available;
            double period = Math.max(overflow * 0.5, 3.0);
            double phase = Util.getMillis() / 1000.0 / period * Math.PI * 2;
            double position = (Math.sin(Math.PI / 2 * Math.cos(phase)) + 1) / 2;
            x = getX() + 2 - (int) (position * overflow);
        }
        g.enableScissor(getX() + 2, getY(), getX() + getWidth() - 2, getY() + getHeight());
        int color = ((int) (alpha * 255) << 24) | (active ? 0x392B22 : 0x796C5E);
        g.text(font, label, x, getY() + (getHeight() - 9) / 2 + 1, color, false);
        g.disableScissor();
    }
}
