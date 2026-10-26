package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.client.ClientRecipes;
import art.arcane.thaumcraft.crafting.ArcaneCraftingRecipe;
import art.arcane.thaumcraft.crafting.CrucibleRecipe;
import art.arcane.thaumcraft.infusion.InfusionRecipe;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModRecipes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Optional JEI integration (only loaded by JEI itself, so the mod runs without it). Categories for the crucible, arcane
 * workbench, infusion (including infusion enchantments and the runic augment) and the essentia centrifuge. Item aspects
 * are shown in item tooltips (hold Shift, as in TC6) in JEI's screens too.
 */
@JeiPlugin
public final class ThaumcraftJeiPlugin implements IModPlugin {
    static IRecipeHolderType<CrucibleRecipe> CRUCIBLE;
    static IRecipeHolderType<ArcaneCraftingRecipe> ARCANE;
    static final IRecipeType<InfusionDisplay> INFUSION = IRecipeType.create(Thaumcraft.MOD_ID, "infusion", InfusionDisplay.class);
    static final IRecipeType<CentrifugeDisplay> CENTRIFUGE = IRecipeType.create(Thaumcraft.MOD_ID, "centrifuge", CentrifugeDisplay.class);

    static final IRecipeType<SmeltingBonusDisplay> SMELTING_BONUS = IRecipeType.create(Thaumcraft.MOD_ID, "smelting_bonus", SmeltingBonusDisplay.class);

    static InfusionCategory infusionCategory;
    static List<InfusionDisplay> specialInfusions = List.of();

    @Override public Identifier getPluginUid() { return Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID, "jei"); }

    private static void types() {
        if (CRUCIBLE == null) CRUCIBLE = IRecipeHolderType.create(ModRecipes.CRUCIBLE_TYPE.get());
        if (ARCANE == null) ARCANE = IRecipeHolderType.create(ModRecipes.ARCANE_TYPE.get());
    }

    @Override public void registerItemSubtypes(ISubtypeRegistration r) {
        r.registerFromDataComponentTypes(ModItems.CRYSTAL_ESSENCE.get(), ModComponents.ASPECT.get());
        r.registerFromDataComponentTypes(ModItems.PHIAL_FILLED.get(), ModComponents.ASPECT.get());
        r.registerFromDataComponentTypes(ModItems.VERDANT_CHARM.get(), ModComponents.VARIANT.get());
        r.registerFromDataComponentTypes(ModItems.AMULET_VIS.get(), ModComponents.VARIANT.get());
        r.registerFromDataComponentTypes(ModItems.MANA_BEAN.get(), ModComponents.ASPECT.get());
        r.registerFromDataComponentTypes(ModItems.GOLEM.get(), ModComponents.GOLEM_PROPS.get());
    }

    @Override public void registerCategories(IRecipeCategoryRegistration r) {
        types();
        var gui = r.getJeiHelpers().getGuiHelper();
        infusionCategory = new InfusionCategory(INFUSION, gui);
        r.addRecipeCategories(new ArcaneCategory(ARCANE, gui), new CrucibleCategory(CRUCIBLE, gui), infusionCategory, new CentrifugeCategory(CENTRIFUGE, gui),
                new SmeltingBonusCategory(SMELTING_BONUS, gui));
    }

    @Override public void registerRecipes(IRecipeRegistration r) {
        types();
        var map = ClientRecipes.get();
        Comparator<RecipeHolder<?>> byId = Comparator.comparing(h -> h.id().identifier().toString());
        r.addRecipes(ARCANE, map.byType(ModRecipes.ARCANE_TYPE.get()).stream().sorted(byId).toList());
        r.addRecipes(CRUCIBLE, map.byType(ModRecipes.CRUCIBLE_TYPE.get()).stream().sorted(byId).toList());
        List<InfusionDisplay> infusions = new ArrayList<>();
        map.byType(ModRecipes.INFUSION_TYPE.get()).stream().sorted(byId).forEach(h -> infusions.addAll(InfusionDisplay.of((RecipeHolder<InfusionRecipe>) (RecipeHolder<?>) h)));
        r.addRecipes(INFUSION, infusions);
        specialInfusions = infusions.stream().filter(d -> !d.note().isEmpty()).toList();
        List<CentrifugeDisplay> centrifuge = new ArrayList<>();
        AspectRegistry.all().values().forEach(a -> { if (!a.isPrimal()) centrifuge.add(new CentrifugeDisplay(a.id(), a.parentA().id(), a.parentB().id())); });
        r.addRecipes(CENTRIFUGE, centrifuge);
        List<SmeltingBonusDisplay> bonus = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            var st = new ItemStack(item);
            if (st.isEmpty()) continue;
            var b = art.arcane.thaumcraft.crafting.SmeltingBonus.bonuses(st);
            if (!b.isEmpty()) bonus.add(new SmeltingBonusDisplay(st, b));
        }
        r.addRecipes(SMELTING_BONUS, bonus);

        r.addIngredientInfo(new ItemStack(ModItems.SALIS_MUNDUS.get()), VanillaTypes.ITEM_STACK, Component.translatable("jei.thaumcraft.info.salis_mundus"));
        for (var smelter : List.of(ModBlocks.SMELTER_BASIC, ModBlocks.SMELTER_THAUMIUM, ModBlocks.SMELTER_VOID))
            r.addIngredientInfo(smelter.get(), Component.translatable("jei.thaumcraft.info.smelter"));
        r.addIngredientInfo(ModBlocks.CRUCIBLE.get(), Component.translatable("jei.thaumcraft.info.crucible"));
        r.addIngredientInfo(ModBlocks.INFUSION_MATRIX.get(), Component.translatable("jei.thaumcraft.info.infusion"));
        r.addIngredientInfo(ModBlocks.THAUMATORIUM.get(), Component.translatable("jei.thaumcraft.info.thaumatorium"));
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r) {
        types();
        r.addCraftingStation(ARCANE, ModBlocks.ARCANE_WORKBENCH.get());
        r.addCraftingStation(CRUCIBLE, ModBlocks.CRUCIBLE.get(), ModBlocks.THAUMATORIUM.get());
        r.addCraftingStation(INFUSION, ModBlocks.INFUSION_MATRIX.get(), ModBlocks.PEDESTAL_ARCANE.get());
        r.addCraftingStation(CENTRIFUGE, ModBlocks.CENTRIFUGE.get());
        r.addCraftingStation(SMELTING_BONUS, ModBlocks.INFERNAL_FURNACE.get());
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        // technical blocks with items (multiblock placeholders, effect blocks) are not shown
        var hidden = new ArrayList<ItemStack>();
        for (var item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(Thaumcraft.MOD_ID)) continue;
            String p = id.getPath();
            if (p.startsWith("placeholder_") || p.startsWith("effect_") || p.equals("barrier") || p.equals("empty")) hidden.add(new ItemStack(item));
        }
        var present = runtime.getIngredientManager().getAllItemStacks();
        hidden.removeIf(h -> present.stream().noneMatch(s -> s.is(h.getItem())));
        if (!hidden.isEmpty()) runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        JeiDebug.runtime(runtime);
    }

    @Override public void onRuntimeUnavailable() { JeiDebug.runtime(null); }
}
