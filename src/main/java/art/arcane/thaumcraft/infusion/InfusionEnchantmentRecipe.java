package art.arcane.thaumcraft.infusion;

import art.arcane.thaumcraft.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * TC6 InfusionEnchantmentRecipe: adds one level of an infusion enchantment to any suitable tool on the central pedestal.
 * Cost scales with the new level plus a third per other infusion enchantment; each existing one gives a 10% chance to
 * add a point of warping (TC.WARP).
 */
public record InfusionEnchantmentRecipe(InfusionEnchantment enchantment, Map<String, Integer> baseAspects, List<Ingredient> components) implements InfusionRecipe {
    static final Codec<InfusionEnchantment> ENCH = Codec.STRING.comapFlatMap(
            s -> { var e = InfusionEnchantment.byId(s); return e == null ? DataResult.error(() -> "Unknown infusion enchantment " + s) : DataResult.success(e); },
            InfusionEnchantment::id);
    public static final MapCodec<InfusionEnchantmentRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ENCH.fieldOf("enchantment").forGetter(InfusionEnchantmentRecipe::enchantment),
            StandardInfusionRecipe.ORDERED_ASPECTS.fieldOf("aspects").forGetter(InfusionEnchantmentRecipe::baseAspects),
            Ingredient.CODEC.listOf().fieldOf("components").forGetter(InfusionEnchantmentRecipe::components)
    ).apply(i, InfusionEnchantmentRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionEnchantmentRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public InfusionEnchantmentRecipe {
        components = List.copyOf(components);
        baseAspects = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(baseAspects));
    }

    @Override public String research() { return enchantment.research; }

    @Override
    public boolean matches(List<ItemStack> comps, ItemStack central, Level level, Player player) {
        if (central == null || central.isEmpty() || !InfusionRecipe.knowsResearch(player, research())) return false;
        if (InfusionEnchantment.getInfusionEnchantmentLevel(central, enchantment) >= enchantment.maxLevel) return false;
        if (!enchantment.appliesTo(central)) return false;
        return RecipeMatcher.findMatches(comps, components) != null;
    }

    @Override
    public Output output(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null) return null;
        ItemStack out = input.copy();
        int cl = InfusionEnchantment.getInfusionEnchantmentLevel(out, enchantment);
        if (cl >= enchantment.maxLevel) return null;
        var el = InfusionEnchantment.getInfusionEnchantments(input);
        Random rand = new Random(System.nanoTime());
        if (rand.nextInt(10) < el.size()) {
            int base = 1 + input.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByteOr("TC.WARP", (byte) 0);
            CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> tag.putByte("TC.WARP", (byte) base));
        }
        InfusionEnchantment.addInfusionEnchantment(out, enchantment, cl + 1);
        return new Output(out, false);
    }

    @Override
    public LinkedHashMap<String, Integer> aspects(Player player, ItemStack input, List<ItemStack> comps) {
        var out = new LinkedHashMap<String, Integer>();
        if (input == null || input.isEmpty()) return out;
        int cl = InfusionEnchantment.getInfusionEnchantmentLevel(input, enchantment) + 1;
        if (cl > enchantment.maxLevel) return out;
        var el = InfusionEnchantment.getInfusionEnchantments(input);
        int other = el.size();
        if (el.contains(enchantment)) --other;
        float modifier = cl + other * 0.33f;
        baseAspects.forEach((a, n) -> { int v = (int) (n * modifier); if (v > 0) out.merge(a, v, Integer::sum); });
        return out;
    }

    @Override public int instability(Player player, ItemStack central, List<ItemStack> comps) { return 4; }
    @Override public RecipeSerializer<InfusionEnchantmentRecipe> getSerializer() { return ModRecipes.INFUSION_ENCHANTMENT.get(); }
}
