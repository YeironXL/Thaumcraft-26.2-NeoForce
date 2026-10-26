package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.util.Baubles;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * TC6 baubles (baubles.api.IBauble): ItemBaubles, ItemAmuletVis, ItemVerdantCharm, ItemVoidseerCharm, ItemCloudRing,
 * ItemCharmUndying and ItemCuriosityBand. The bauble slot is given by the item's curios tag; {@link #onWornTick} is
 * IBauble.onWornTick, called by the accessory backend for every worn stack.
 */
public class BaubleItem extends Item implements VisDiscountGear {
    private final int discount;
    public BaubleItem(Properties p) { this(p, 0); }
    public BaubleItem(Properties p, int discount) { super(p.stacksTo(1)); this.discount = discount; }
    @Override public int visDiscountPercent() { return discount; }
    /** IBauble.onWornTick. */
    public void onWornTick(ItemStack stack, LivingEntity wearer) {}

    /** TC6 variant (verdant charm NBT byte "type", vis amulet metadata), kept in the thaumcraft:variant component; old stacks with a custom data "type" byte still read. */
    public static int type(ItemStack stack) {
        Integer v = stack.get(art.arcane.thaumcraft.registry.ModComponents.VARIANT.get());
        if (v != null) return v;
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getByteOr("type", (byte) 0);
    }
    public static ItemStack withType(ItemStack stack, int type) {
        if (type == 0) stack.remove(art.arcane.thaumcraft.registry.ModComponents.VARIANT.get());
        else stack.set(art.arcane.thaumcraft.registry.ModComponents.VARIANT.get(), type);
        // ItemAmuletVis.getRarity: found UNCOMMON, crafted RARE.
        if (stack.getItem() instanceof VisAmulet) stack.set(DataComponents.RARITY, type == 0 ? net.minecraft.world.item.Rarity.UNCOMMON : net.minecraft.world.item.Rarity.RARE);
        return stack;
    }

    /** ItemAmuletVis: type 0 (found) recharges every 40 ticks, type 1 (crafted) every 5; hotbar, then baubles, then armour. */
    public static final class VisAmulet extends BaubleItem {
        public VisAmulet(Properties p) { super(p); }
        /** ItemTCBase variant names: item.amulet_vis.found / item.amulet_vis.crafted. */
        @Override public Component getName(ItemStack stack) {
            return Component.translatable(type(stack) == 0 ? "item.amulet_vis.found.name" : "item.amulet_vis.crafted.name");
        }
        @Override public void onWornTick(ItemStack stack, LivingEntity wearer) {
            if (!(wearer instanceof Player player) || !(player.level() instanceof ServerLevel level)) return;
            if (player.tickCount % (type(stack) == 0 ? 40 : 5) != 0) return;
            var pos = player.blockPosition();
            for (int a = 0; a < 9; ++a)
                if (RechargeHelper.rechargeItem(level, player.getInventory().getNonEquipmentItems().get(a), pos, player, 1) > 0) return;
            for (var w : Baubles.worn(player))
                if (RechargeHelper.rechargeItem(level, w.stack(), pos, player, 1) > 0) return;
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[] { net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD })
                if (RechargeHelper.rechargeItem(level, player.getItemBySlot(slot), pos, player, 1) > 0) return;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
        }
    }

    /** ItemVerdantCharm: 200 vis; cures wither (20), poison (10), flux taint (5); Lifegiver heals, Sustainer feeds and gives air. */
    public static final class VerdantCharm extends BaubleItem implements IRechargable {
        public VerdantCharm(Properties p) { super(p); }
        @Override public int getMaxCharge(ItemStack stack, LivingEntity player) { return 200; }
        @Override public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) { return EnumChargeDisplay.NORMAL; }
        @Override public void onWornTick(ItemStack stack, LivingEntity player) {
            if (player.level().isClientSide() || player.tickCount % 20 != 0 || !(player instanceof Player pl)) return;
            if (player.getEffect(MobEffects.WITHER) != null && RechargeHelper.consumeCharge(stack, player, 20)) { player.removeEffect(MobEffects.WITHER); return; }
            if (player.getEffect(MobEffects.POISON) != null && RechargeHelper.consumeCharge(stack, player, 10)) { player.removeEffect(MobEffects.POISON); return; }
            if (player.getEffect(ModEffects.FLUX_TAINT) != null && RechargeHelper.consumeCharge(stack, player, 5)) { player.removeEffect(ModEffects.FLUX_TAINT); return; }
            int type = type(stack);
            if (type == 1 && player.getHealth() < player.getMaxHealth() && RechargeHelper.consumeCharge(stack, player, 5)) { player.heal(1.0f); return; }
            if (type == 2) {
                if (player.getAirSupply() < 100 && RechargeHelper.consumeCharge(stack, player, 1)) { player.setAirSupply(300); return; }
                if (pl.getFoodData().needsFood() && RechargeHelper.consumeCharge(stack, player, 1)) pl.getFoodData().eat(1, 0.3f);
            }
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
            int type = type(stack);
            if (type == 1) lines.accept(Component.translatable("item.verdant_charm.life.text").withStyle(ChatFormatting.GOLD));
            if (type == 2) lines.accept(Component.translatable("item.verdant_charm.sustain.text").withStyle(ChatFormatting.GOLD));
        }
    }

    /** ItemVoidseerCharm: discount = permanent warp (max 100) / 4 percent, warp = discount / 5. */
    public static final class VoidseerCharm extends BaubleItem implements WarpingGear {
        public VoidseerCharm(Properties p) { super(p); }
        @Override public int getVisDiscount(ItemStack stack, Player player) {
            if (player == null) return 0;
            int pw;
            if (player instanceof ServerPlayer sp) pw = PlayerProgressManager.getWarp(sp, PlayerProgressManager.WarpType.PERMANENT);
            else if (player.level().isClientSide()) pw = art.arcane.thaumcraft.client.ClientResearch.progress().permanentWarp();
            else pw = 0;
            return (int) (Math.min(100, pw) / 100.0f * 25.0f);
        }
        @Override public int getWarp(ItemStack stack, Player player) { return getVisDiscount(stack, player) / 5; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("item.voidseer_charm.text").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
        }
    }
}
