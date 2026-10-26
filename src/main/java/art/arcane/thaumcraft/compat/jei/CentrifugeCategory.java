package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Essentia centrifuge, shown with crystallised essentia so the aspects can be looked up as items. */
final class CentrifugeCategory extends AbstractRecipeCategory<CentrifugeDisplay> {
    static final int W = 120, H = 40;

    CentrifugeCategory(IRecipeType<CentrifugeDisplay> type, IGuiHelper gui) {
        super(type, Component.translatable("jei.thaumcraft.centrifuge"), gui.createDrawableItemLike(ModBlocks.CENTRIFUGE.get()), W, H);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, CentrifugeDisplay d, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().add(CrystalEssenceItem.of(d.input(), 1));
        builder.addOutputSlot(68, 6).setOutputSlotBackground().add(CrystalEssenceItem.of(d.outputA(), 1))
                .addRichTooltipCallback((v, t) -> t.add(Component.translatable("jei.thaumcraft.chance", 50).withStyle(ChatFormatting.GRAY)));
        builder.addOutputSlot(94, 6).setOutputSlotBackground().add(CrystalEssenceItem.of(d.outputB(), 1))
                .addRichTooltipCallback((v, t) -> t.add(Component.translatable("jei.thaumcraft.chance", 50).withStyle(ChatFormatting.GRAY)));
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, CentrifugeDisplay d, IFocusGroup focuses) {
        builder.addAnimatedRecipeArrowWidget(40).setPosition(34, 6);
    }

    @Override public void draw(CentrifugeDisplay d, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        Component t = Component.translatable("jei.thaumcraft.centrifuge.either");
        g.text(font, t, 86 - font.width(t) / 2, 30, 0xFF505050, false);
    }
}
