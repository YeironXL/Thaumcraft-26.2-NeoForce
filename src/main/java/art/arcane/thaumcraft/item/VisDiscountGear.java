package art.arcane.thaumcraft.item;

/** TC6 api.items.IVisDiscountGear. */
public interface VisDiscountGear {
    int visDiscountPercent();
    /** IVisDiscountGear.getVisDiscount(stack, player); defaults to the fixed percentage. */
    default int getVisDiscount(net.minecraft.world.item.ItemStack stack, net.minecraft.world.entity.player.Player player) { return visDiscountPercent(); }
}
