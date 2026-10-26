package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** TC6 api.items.RechargeHelper. */
public final class RechargeHelper {
    public static float rechargeItem(ServerLevel world, ItemStack is, BlockPos pos, Player player, int amt) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable c)) return 0;
        if (player != null && shouldPreserveAura(world, player, pos)) return 0;
        amt = Math.min(amt, c.getMaxCharge(is, player) - getCharge(is));
        int drained = (int) NativeAura.drainVis(world, pos, amt, false);
        if (drained > 0) { addCharge(is, player, drained); return drained; }
        return 0;
    }
    /** AuraHandler.shouldPreserveAura. */
    public static boolean shouldPreserveAura(ServerLevel world, Player player, BlockPos pos) {
        boolean known = player == null || (player instanceof ServerPlayer sp && PlayerProgressManager.has(sp, "AURAPRESERVE"));
        return known && NativeAura.vis(world, pos) / (float) NativeAura.base(world, pos) < 0.1;
    }
    public static float rechargeItemBlindly(ItemStack is, Player player, int amt) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable c)) return 0;
        amt = Math.min(amt, c.getMaxCharge(is, player) - getCharge(is));
        if (amt > 0) addCharge(is, player, amt);
        return amt;
    }
    private static void addCharge(ItemStack is, LivingEntity player, int amt) {
        if (!(is.getItem() instanceof IRechargable c)) return;
        is.set(ModComponents.CHARGE.get(), Math.min(c.getMaxCharge(is, player), amt + getCharge(is)));
    }
    public static int getCharge(ItemStack is) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable)) return -1;
        return is.getOrDefault(ModComponents.CHARGE.get(), 0);
    }
    public static float getChargePercentage(ItemStack is, Player player) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable c)) return -1;
        return getCharge(is) / (float) c.getMaxCharge(is, player);
    }
    public static boolean consumeCharge(ItemStack is, LivingEntity player, int amt) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable)) return false;
        int charge = is.getOrDefault(ModComponents.CHARGE.get(), 0);
        if (charge >= amt && is.has(ModComponents.CHARGE.get())) { is.set(ModComponents.CHARGE.get(), charge - amt); return true; }
        return false;
    }
    private RechargeHelper() {}
}
