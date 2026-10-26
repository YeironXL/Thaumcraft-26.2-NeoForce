package art.arcane.thaumcraft.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** TC6 api.items.IRechargable: items holding vis charge, refilled by the recharge pedestal. */
public interface IRechargable {
    enum EnumChargeDisplay { NEVER, NORMAL, PERIODIC }
    int getMaxCharge(ItemStack stack, LivingEntity player);
    EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player);
}
