package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.crafting.ArcaneCraftingRecipe;
import art.arcane.thaumcraft.crafting.ArcaneRecipe;
import art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Arcane workbench: the 3x3 grid, the vis drawn from the aura and the primal crystals in the side slots. */
final class ArcaneCategory extends AbstractRecipeCategory<RecipeHolder<ArcaneCraftingRecipe>> {
    static final int W = 140, H = 96;
    static final List<String> PRIMALS = List.of("aer", "terra", "ignis", "aqua", "ordo", "perditio");
    private final ICraftingGridHelper grid;

    ArcaneCategory(IRecipeHolderType<ArcaneCraftingRecipe> type, IGuiHelper gui) {
        super(type, Component.translatable("jei.thaumcraft.arcane"), gui.createDrawableItemLike(ModBlocks.ARCANE_WORKBENCH.get()), W, H);
        grid = gui.createCraftingGridHelper();
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ArcaneCraftingRecipe> holder, IFocusGroup focuses) {
        var r = holder.value();
        List<List<ItemStack>> inputs = new ArrayList<>();
        int w = 3, h = 3;
        if (r instanceof ArcaneRecipe shaped) {
            w = shaped.pattern().width(); h = shaped.pattern().height();
            for (var o : shaped.pattern().ingredients()) inputs.add(o.map(ArcaneCategory::stacks).orElse(List.of()));
        } else if (r instanceof ShapelessArcaneRecipe shapeless) {
            for (var i : shapeless.ingredients()) inputs.add(stacks(i));
            builder.setShapeless(100, 4);
        }
        var slots = grid.createAndSetInputs(builder, inputs, w, h);
        for (int i = 0; i < 9 && i < slots.size(); i++) slots.get(i).setPosition(5 + (i % 3) * 18, 5 + (i / 3) * 18);
        builder.addOutputSlot(110, 24).setOutputSlotBackground().add(r instanceof ArcaneRecipe a ? a.result().create() : ((ShapelessArcaneRecipe) r).result().create());
        // primal crystals the recipe consumes, in TC6 primal order
        int k = 0;
        for (String a : PRIMALS) {
            Integer n = r.crystals().get(a);
            if (n == null) continue;
            builder.addInputSlot(5 + k * 18, 66).setStandardSlotBackground().add(CrystalEssenceItem.of(a, n));
            k++;
        }
    }

    static List<ItemStack> stacks(Ingredient i) {
        return i.items().map(h -> new ItemStack(h)).toList();
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeHolder<ArcaneCraftingRecipe> recipe, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(78, 24);
    }

    @Override public void draw(RecipeHolder<ArcaneCraftingRecipe> holder, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mx, double my) {
        // TC6 rune grid behind the slots (book overlay 112,15 52x52)
        JeiDraw.overlay(g, 0, 0, 112, 15, 52, 52, 64, 64, 0x80FFFFFF);
        var font = Minecraft.getInstance().font;
        int vis = holder.value().vis();
        if (vis > 0) {
            Component t = Component.translatable("jei.thaumcraft.vis", vis);
            g.text(font, t, 120 - font.width(t) / 2, 48, 0xFF8040A0, false);
        }
        JeiDraw.researchLine(g, holder.value().research(), 0, H - 9, W);
    }

    @Override public void getTooltip(mezz.jei.api.gui.builder.ITooltipBuilder tooltip, RecipeHolder<ArcaneCraftingRecipe> holder, IRecipeSlotsView slots, double mx, double my) {
        if (mx >= 64 && mx < 136 && my >= 44 && my < 58) tooltip.add(Component.translatable("jei.thaumcraft.vis.tooltip"));
    }
}
