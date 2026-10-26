package art.arcane.thaumcraft.infusion;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * TC6 InfusionRunicAugmentRecipe: armour or a bauble plus salis mundus and (1 + current charge) amber gains one point of
 * runic shielding. Cost doubles with each charge.
 */
public final class InfusionRunicAugmentRecipe implements InfusionRecipe {
    public static final InfusionRunicAugmentRecipe INSTANCE = new InfusionRunicAugmentRecipe();
    public static final MapCodec<InfusionRunicAugmentRecipe> CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, InfusionRunicAugmentRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    /** PlayerEvents.getRunicCharge. */
    public static int getRunicCharge(ItemStack stack) {
        return stack == null || stack.isEmpty() ? 0 : stack.getOrDefault(ModComponents.RUNIC_CHARGE.get(), 0);
    }

    /** ItemArmor or IBauble. */
    public static boolean isArmorOrBauble(ItemStack stack) {
        var eq = stack.get(DataComponents.EQUIPPABLE);
        if (eq != null && eq.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return true;
        return stack.getItem() instanceof art.arcane.thaumcraft.item.BaubleItem;
    }

    public static List<Ingredient> components(ItemStack input) {
        List<Ingredient> com = new ArrayList<>();
        com.add(Ingredient.of(ModItems.SALIS_MUNDUS.get()));
        com.add(Ingredient.of(ModItems.AMBER.get()));
        int fc = getRunicCharge(input);
        for (int c = 0; c < fc; ++c) com.add(Ingredient.of(ModItems.AMBER.get()));
        return com;
    }

    @Override public String research() { return "RUNICSHIELDING"; }

    @Override
    public boolean matches(List<ItemStack> comps, ItemStack central, Level level, Player player) {
        return InfusionRecipe.isResearchKnown(player, research()) && central != null && isArmorOrBauble(central)
                && RecipeMatcher.findMatches(comps, components(central)) != null;
    }

    @Override
    public Output output(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null) return null;
        ItemStack out = input.copy();
        out.set(ModComponents.RUNIC_CHARGE.get(), getRunicCharge(input) + 1);
        return new Output(out, false);
    }

    @Override
    public LinkedHashMap<String, Integer> aspects(Player player, ItemStack input, List<ItemStack> comps) {
        var out = new LinkedHashMap<String, Integer>();
        int vis = 20 + (int) (20.0 * Math.pow(2.0, getRunicCharge(input)));
        if (vis > 0) { out.put("praemunio", vis); out.put("vitreus", vis / 2); out.put("potentia", vis / 2); }
        return out;
    }

    @Override public int instability(Player player, ItemStack input, List<ItemStack> comps) { return 5 + getRunicCharge(input) / 2; }
    @Override public RecipeSerializer<InfusionRunicAugmentRecipe> getSerializer() { return ModRecipes.INFUSION_RUNIC.get(); }
}
