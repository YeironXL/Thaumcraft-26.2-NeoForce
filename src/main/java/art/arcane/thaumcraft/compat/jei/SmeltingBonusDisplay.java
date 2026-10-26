package art.arcane.thaumcraft.compat.jei;

import art.arcane.thaumcraft.crafting.SmeltingBonus;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Infernal furnace smelting bonus: the extra nuggets/chunks rolled when this input is smelted. */
record SmeltingBonusDisplay(ItemStack input, List<SmeltingBonus.Chance> bonuses) {}
