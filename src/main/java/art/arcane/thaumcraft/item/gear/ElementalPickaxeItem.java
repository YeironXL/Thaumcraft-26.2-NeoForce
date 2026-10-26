package art.arcane.thaumcraft.item.gear;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 ItemElementalPickaxe (Pickaxe of the Core): THAUMIUM_ELEMENTAL pickaxe, rare. Struck creatures are set on fire for
 * two seconds (players only when PvP is on). Cluster refining and ore sounding come from the REFINING 1 / SOUNDING 2
 * infusion enchantments the recipe applies.
 */
public class ElementalPickaxeItem extends Item {
    public ElementalPickaxeItem(Properties p) { super(p.pickaxe(ElementalGear.TOOL, 1.0F, -2.8F)); }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (mob.level() instanceof ServerLevel sl && (!(mob instanceof Player) || sl.isPvpAllowed())) mob.igniteForSeconds(2.0f);
        super.hurtEnemy(stack, mob, attacker);
    }
}
