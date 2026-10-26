package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Runic matrix infusion: central item on the middle pedestal, components on the pedestals around it, essentia below. */
final class InfusionCategory extends AbstractRecipeCategory<InfusionDisplay> {
    static final int W = 160, H = 146, CX = 52, CY = 58, R = 38;

    InfusionCategory(IRecipeType<InfusionDisplay> type, IGuiHelper gui) {
        super(type, Component.translatable("jei.thaumcraft.infusion"), gui.createDrawableItemLike(ModBlocks.INFUSION_MATRIX.get()), W, H);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, InfusionDisplay d, IFocusGroup focuses) {
        var central = builder.addInputSlot(CX - 8, CY - 8).setOutputSlotBackground().addItemStacks(d.central());
        int n = d.components().size();
        for (int i = 0; i < n; i++) {
            double ang = -Math.PI / 2 + 2 * Math.PI * i / n;
            int x = (int) Math.round(CX + R * Math.cos(ang)) - 8, y = (int) Math.round(CY + R * Math.sin(ang)) - 8;
            builder.addInputSlot(x, y).setStandardSlotBackground().addItemStacks(d.components().get(i));
        }
        var out = builder.addOutputSlot(136, CY - 8).setOutputSlotBackground().addItemStacks(d.outputs())
                .addRichTooltipCallback((v, t) -> { if (!d.research().isEmpty()) t.add(Component.translatable("jei.thaumcraft.research", JeiDraw.research(d.research())).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE)); });
        if (d.central().size() > 1 && d.central().size() == d.outputs().size()) builder.createFocusLink(central, out);
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, InfusionDisplay d, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(106, CY - 8);
    }

    private static List<JeiDraw.Placed> placed(InfusionDisplay d) { return JeiDraw.layout(d.orderedAspects(), 0, 106, W, 8); }

    @Override public void draw(InfusionDisplay d, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mx, double my) {
        // the infusion rune diamond from the research book overlay under the central pedestal
        JeiDraw.overlay(g, CX - 22, CY - 22, 207, 77, 42, 42, 44, 44, 0x90FFFFFF);
        JeiDraw.aspects(g, placed(d));
        var font = Minecraft.getInstance().font;
        int inst = Math.min(5, d.instability() / 2);
        Component t = Component.translatable("tc.inst").append(Component.translatable("tc.inst." + inst));
        g.text(font, t, (W - font.width(t)) / 2, 0, 0xFF404040, false);
        if (!d.note().isEmpty()) {
            var lines = font.split(Component.translatable(d.note()), W);
            for (int i = 0; i < lines.size() && i < 2; i++) g.text(font, lines.get(i), (W - font.width(lines.get(i))) / 2, H - 18 + i * 9, 0xFF505050, false);
        }
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, InfusionDisplay d, IRecipeSlotsView slots, double mx, double my) {
        JeiDraw.aspectTooltip(tooltip, placed(d), mx, my);
    }

    @Override public Identifier getIdentifier(InfusionDisplay d) { return d.id(); }
}
