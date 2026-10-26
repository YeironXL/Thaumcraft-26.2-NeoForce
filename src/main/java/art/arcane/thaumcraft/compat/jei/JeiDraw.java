package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.client.ThaumcraftTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Shared drawing for the Thaumcraft JEI categories: TC6 book overlay art, aspect rows and their tooltips. */
final class JeiDraw {
    static final Identifier OVERLAY = Identifier.parse("thaumcraft:textures/gui/gui_researchbook_overlay.png");
    static final Identifier PAPER = Identifier.parse("thaumcraft:textures/gui/paper.png");

    /** A region of the 512px overlay in the book's 256-unit coordinates, scaled to w x h. */
    static void overlay(GuiGraphicsExtractor g, int x, int y, int u, int v, int uw, int vh, int w, int h, int argb) {
        g.blit(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y, u, v, w, h, uw, vh, 256, 256, argb);
    }

    record Placed(String aspect, int amount, int x, int y) {}

    /** Lays out aspects left to right in rows of {@code perRow}, each row centred in {@code width}. */
    static List<Placed> layout(Map<String, Integer> aspects, int x, int y, int width, int perRow) {
        var out = new ArrayList<Placed>();
        var list = new ArrayList<>(aspects.entrySet());
        for (int i = 0; i < list.size(); i++) {
            int row = i / perRow, inRow = Math.min(perRow, list.size() - row * perRow);
            int rx = x + (width - inRow * 18) / 2 + 1;
            out.add(new Placed(list.get(i).getKey(), list.get(i).getValue(), rx + (i % perRow) * 18, y + row * 18));
        }
        return out;
    }

    static void aspects(GuiGraphicsExtractor g, List<Placed> placed) {
        var font = Minecraft.getInstance().font;
        for (var p : placed) ThaumcraftTheme.drawTag(g, font, p.aspect(), p.x(), p.y(), p.amount());
    }

    static boolean aspectTooltip(ITooltipBuilder tooltip, List<Placed> placed, double mx, double my) {
        for (var p : placed) if (mx >= p.x() && mx < p.x() + 16 && my >= p.y() && my < p.y() + 16) {
            tooltip.add(Component.literal(cap(p.aspect())).withStyle(s -> s.withColor(art.arcane.thaumcraft.aspect.AspectRegistry.color(p.aspect()))));
            tooltip.add(Component.translatable("tc.aspect." + p.aspect()).append(" x" + p.amount()).withStyle(ChatFormatting.GRAY));
            var a = art.arcane.thaumcraft.aspect.AspectRegistry.get(p.aspect());
            if (a != null && !a.isPrimal()) tooltip.add(Component.literal(cap(a.parentA().id()) + " + " + cap(a.parentB().id())).withStyle(ChatFormatting.DARK_GRAY));
            return true;
        }
        return false;
    }

    /** "KEY" or "KEY@stage" research to its book title. */
    static Component research(String research) {
        if (research == null || research.isEmpty()) return Component.empty();
        String key = research.contains("@") ? research.substring(0, research.indexOf('@')) : research;
        String lang = "research." + key + ".title";
        return net.minecraft.locale.Language.getInstance().has(lang) ? Component.translatable(lang) : Component.literal(key);
    }

    static void researchLine(GuiGraphicsExtractor g, String research, int x, int y, int width) {
        if (research == null || research.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        Component line = Component.translatable("jei.thaumcraft.research", research(research));
        var text = font.split(line, width);
        if (!text.isEmpty()) g.text(font, text.get(0), x + Math.max(0, (width - font.width(text.get(0))) / 2), y, 0xFF606060, false);
    }

    static String cap(String a) { return a.isEmpty() ? a : Character.toUpperCase(a.charAt(0)) + a.substring(1); }

    private JeiDraw() {}
}
