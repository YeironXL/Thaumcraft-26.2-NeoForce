package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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

/** Infernal furnace: input on the left, the possible bonus drops (with their odds) on the right. */
final class SmeltingBonusCategory extends AbstractRecipeCategory<SmeltingBonusDisplay> {
    static final int W = 130, H = 36, OX = 58;

    SmeltingBonusCategory(IRecipeType<SmeltingBonusDisplay> type, IGuiHelper gui) {
        super(type, Component.translatable("jei.thaumcraft.smelting_bonus"), gui.createDrawableItemLike(ModBlocks.INFERNAL_FURNACE.get()), W, H);
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, SmeltingBonusDisplay d, IFocusGroup focuses) {
        builder.addInputSlot(6, 6).setStandardSlotBackground().add(d.input());
        for (int i = 0; i < d.bonuses().size() && i < 3; i++) {
            var b = d.bonuses().get(i);
            String odds = odds(b.chance());
            builder.addOutputSlot(OX + i * 24, 6).setStandardSlotBackground().add(b.stack())
                    .addRichTooltipCallback((v, t) -> t.add(Component.translatable("jei.thaumcraft.chance_str", odds).withStyle(ChatFormatting.GRAY)));
        }
    }

    static String odds(float chance) {
        float c = chance * 100;
        return c == Math.floor(c) ? (int) c + "%" : String.format(java.util.Locale.ROOT, "%.1f%%", c);
    }

    @Override public void createRecipeExtras(IRecipeExtrasBuilder builder, SmeltingBonusDisplay d, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(28, 6);
    }

    @Override public void draw(SmeltingBonusDisplay d, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mx, double my) {
        var font = Minecraft.getInstance().font;
        for (int i = 0; i < d.bonuses().size() && i < 3; i++) {
            String s = odds(d.bonuses().get(i).chance());
            g.text(font, s, OX + i * 24 + 8 - font.width(s) / 2, 26, 0xFF606060, false);
        }
    }
}
