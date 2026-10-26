package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.crafting.CrucibleRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;

/** TC6 crucible page: the catalyst dropped into the boiling cauldron with the dissolved aspects, giving the result. */
final class CrucibleCategory extends AbstractRecipeCategory<RecipeHolder<CrucibleRecipe>> {
    static final int W = 130, H = 98;

    CrucibleCategory(IRecipeHolderType<CrucibleRecipe> type, IGuiHelper gui) {
        super(type, Component.translatable("jei.thaumcraft.crucible"), gui.createDrawableItemLike(ModBlocks.CRUCIBLE.get()), W, H);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CrucibleRecipe> holder, IFocusGroup focuses) {
        var r = holder.value();
        var catalyst = builder.addInputSlot(42, 1).setStandardSlotBackground();
        if (r.catalystAspect().isEmpty()) catalyst.add(r.catalyst());
        else catalyst.add(art.arcane.thaumcraft.item.CrystalEssenceItem.of(r.catalystAspect(), 1));
        builder.addOutputSlot(108, 24).setOutputSlotBackground().add(r.result());
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<CrucibleRecipe> recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(80, 24);
    }

    private static java.util.List<JeiDraw.Placed> placed(CrucibleRecipe r) { return JeiDraw.layout(r.aspects(), 0, 60, W, 7); }

    @Override public void draw(RecipeHolder<CrucibleRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mx, double my) {
        // TileCrucible art from the research book overlay (cauldron at 0,20 56x48 in book units)
        JeiDraw.overlay(g, 22, 12, 0, 20, 56, 48, 56, 48, 0xFFFFFFFF);
        var placed = placed(holder.value());
        JeiDraw.aspects(g, placed);
        JeiDraw.researchLine(g, holder.value().research(), 0, H - 9, W);
    }

    @Override public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<CrucibleRecipe> holder, IRecipeSlotsView slots, double mx, double my) {
        JeiDraw.aspectTooltip(tooltip, placed(holder.value()), mx, my);
    }
}
