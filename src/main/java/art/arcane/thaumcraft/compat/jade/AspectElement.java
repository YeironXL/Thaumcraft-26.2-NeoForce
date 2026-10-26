package art.arcane.thaumcraft.compat.jade;

import art.arcane.thaumcraft.client.ThaumcraftTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import snownee.jade.api.ui.Element;

/** An aspect icon with its amount in the corner, drawn like TC6's aspect tags. */
final class AspectElement extends Element {
    private final String aspect;
    private final int amount;

    AspectElement(String aspect, int amount) {
        this.aspect = aspect; this.amount = amount;
        this.width = 18; this.height = 17;
    }

    @Override public Component getNarration() { return Component.literal(TcProviders.cap(aspect)).append(" " + amount); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        ThaumcraftTheme.drawTag(g, Minecraft.getInstance().font, aspect, getX(), getY(), amount);
    }
}
