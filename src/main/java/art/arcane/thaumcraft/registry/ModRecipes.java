package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.crafting.MagicDustRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Thaumcraft.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Thaumcraft.MOD_ID);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,net.minecraft.world.item.crafting.RecipeType<art.arcane.thaumcraft.crafting.CrucibleRecipe>> CRUCIBLE_TYPE =
            TYPES.register("crucible",() -> new net.minecraft.world.item.crafting.RecipeType<>() { @Override public String toString() { return "thaumcraft:crucible"; } });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<art.arcane.thaumcraft.crafting.CrucibleRecipe>> CRUCIBLE = SERIALIZERS.register("crucible",() ->
            new RecipeSerializer<>(art.arcane.thaumcraft.crafting.CrucibleRecipe.CODEC,art.arcane.thaumcraft.crafting.CrucibleRecipe.STREAM_CODEC));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<art.arcane.thaumcraft.crafting.ArcaneCraftingRecipe>> ARCANE_TYPE =
            TYPES.register("arcane", () -> new net.minecraft.world.item.crafting.RecipeType<>() { @Override public String toString() { return "thaumcraft:arcane"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<art.arcane.thaumcraft.crafting.ArcaneRecipe>> ARCANE = SERIALIZERS.register("arcane", () ->
            new RecipeSerializer<>(art.arcane.thaumcraft.crafting.ArcaneRecipe.CODEC, art.arcane.thaumcraft.crafting.ArcaneRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe>> SHAPELESS_ARCANE = SERIALIZERS.register("shapeless_arcane", () ->
            new RecipeSerializer<>(art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe.CODEC,art.arcane.thaumcraft.crafting.ShapelessArcaneRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MagicDustRecipe>> MAGIC_DUST = SERIALIZERS.register("magic_dust", () -> {
        var recipe = new MagicDustRecipe();
        return new RecipeSerializer<>(MapCodec.unit(recipe), StreamCodec.unit(recipe));
    });
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>, net.minecraft.world.item.crafting.RecipeType<art.arcane.thaumcraft.infusion.InfusionRecipe>> INFUSION_TYPE =
            TYPES.register("infusion", () -> new net.minecraft.world.item.crafting.RecipeType<>() { @Override public String toString() { return "thaumcraft:infusion"; } });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<art.arcane.thaumcraft.infusion.StandardInfusionRecipe>> INFUSION = SERIALIZERS.register("infusion", () ->
            new RecipeSerializer<>(art.arcane.thaumcraft.infusion.StandardInfusionRecipe.CODEC, art.arcane.thaumcraft.infusion.StandardInfusionRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<art.arcane.thaumcraft.infusion.InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT = SERIALIZERS.register("infusion_enchantment", () ->
            new RecipeSerializer<>(art.arcane.thaumcraft.infusion.InfusionEnchantmentRecipe.CODEC, art.arcane.thaumcraft.infusion.InfusionEnchantmentRecipe.STREAM_CODEC));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<art.arcane.thaumcraft.infusion.InfusionRunicAugmentRecipe>> INFUSION_RUNIC = SERIALIZERS.register("infusion_runic_augment", () ->
            new RecipeSerializer<>(art.arcane.thaumcraft.infusion.InfusionRunicAugmentRecipe.CODEC, art.arcane.thaumcraft.infusion.InfusionRunicAugmentRecipe.STREAM_CODEC));
    public static void register(IEventBus bus) { SERIALIZERS.register(bus); TYPES.register(bus); }
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<art.arcane.thaumcraft.crafting.ScribingToolsRecipe>> SCRIBING_CRAFT=SERIALIZERS.register("scribing_craft",()->{
        var recipe=new art.arcane.thaumcraft.crafting.ScribingToolsRecipe(false);return new RecipeSerializer<>(MapCodec.unit(recipe),StreamCodec.unit(recipe));
    });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<art.arcane.thaumcraft.crafting.ScribingToolsRecipe>> SCRIBING_REFILL=SERIALIZERS.register("scribing_refill",()->{
        var recipe=new art.arcane.thaumcraft.crafting.ScribingToolsRecipe(true);return new RecipeSerializer<>(MapCodec.unit(recipe),StreamCodec.unit(recipe));
    });
    public static final DeferredHolder<RecipeSerializer<?>,RecipeSerializer<art.arcane.thaumcraft.crafting.JarLabelRecipe>> JAR_LABEL=SERIALIZERS.register("jar_label",()->
            new RecipeSerializer<>(art.arcane.thaumcraft.crafting.JarLabelRecipe.CODEC,art.arcane.thaumcraft.crafting.JarLabelRecipe.STREAM_CODEC));
    private ModRecipes() {}
}
