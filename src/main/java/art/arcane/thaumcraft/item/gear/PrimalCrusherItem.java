package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.item.WarpingGear;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * TC6 ItemPrimalCrusher: a PRIMALVOID pick/shovel hybrid (7.5 damage, -2.8 speed, enchantability 20) that mines stone,
 * metal and earth at full speed, repairs itself by one point a second while carried and carries warp 2. The recipe applies
 * DESTRUCTIVE (3x3 mining) and REFINING.
 */
public class PrimalCrusherItem extends Item implements WarpingGear {
    public PrimalCrusherItem(Properties p) { super(p.tool(ElementalGear.PRIMAL, ElementalGear.CRUSHER_MINEABLE, 3.5F, -2.8F, 0.0F)); }

    @Override public int getWarp(ItemStack stack, Player player) { return 2; }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (stack.isDamaged() && owner instanceof LivingEntity && owner.tickCount % 20 == 0) stack.setDamageValue(stack.getDamageValue() - 1);
    }
}
