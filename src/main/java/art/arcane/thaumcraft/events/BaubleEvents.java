package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.item.IRechargable;
import art.arcane.thaumcraft.item.RechargeHelper;
import art.arcane.thaumcraft.item.VisDiscountGear;
import art.arcane.thaumcraft.network.ResearchSnapshotPayload;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.util.Baubles;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The bauble parts of TC6 PlayerEvents: ring of clouds fall damage, curiosity band, charm of undying, tooltips. */
public final class BaubleEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(BaubleEvents::onFallDamage);
        NeoForge.EVENT_BUS.addListener(BaubleEvents::pickupXP);
        NeoForge.EVENT_BUS.addListener(BaubleEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(BaubleEvents::tooltip);
    }

    /** PlayerEvents.onFallDamage: the ring of clouds reduces fall damage to amount/3 - 2. */
    public static void onFallDamage(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypes.FALL) || !(event.getEntity() instanceof Player p)) return;
        // Thaumostatic Girdle (TC4): prevents fall damage.
        if (art.arcane.thaumcraft.item.gear.HoverGirdleItem.isWorn(p)) { event.setCanceled(true); event.setAmount(0.0f); return; }
        if (!Baubles.isEquipped(p, ModItems.CLOUD_RING.get())) return;
        float f = Math.max(0.0f, event.getAmount() / 3.0f - 2.0f);
        if (f < 1.0f) { event.setCanceled(true); event.setAmount(0.0f); }
        else if (f < event.getAmount()) event.setAmount(f);
        if (event.getAmount() < 1.0f) { event.setCanceled(true); event.setAmount(0.0f); }
    }

    /** PlayerEvents.pickupXP: the curiosity band halves xp orbs (value > 1) and may grant theory or observation. */
    public static void pickupXP(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !Baubles.isEquipped(player, ModItems.CURIOSITY_BAND.get())) return;
        var orb = event.getOrb();
        if (orb.getValue() <= 1) return;
        int d = orb.getValue() / 2;
        orb.setValue(orb.getValue() - d);
        float r = player.getRandom().nextFloat();
        String type = r < 0.05 * d ? "THEORY" : r < 0.2 * d ? "OBSERVATION" : null;
        if (type == null) return;
        String cat = ResearchCatalog.CATEGORIES.get(player.getRandom().nextInt(ResearchCatalog.CATEGORIES.size()));
        PlayerProgressManager.get(player).addKnowledge(type, cat, 1);
        if (player.connection != null) PacketDistributor.sendToPlayer(player, ResearchSnapshotPayload.of(player, false));
    }

    /** PlayerEvents.onDeath: a worn charm of undying is consumed like a totem. */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        var worn = Baubles.find(player, ModItems.CHARM_UNDYING.get());
        if (worn.isEmpty()) return;
        ItemStack charm = worn.get().stack();
        if (player instanceof ServerPlayer sp) {
            sp.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
            CriteriaTriggers.USED_TOTEM.trigger(sp, charm);
        }
        ItemStack rest = charm.copy();
        rest.shrink(1);
        worn.get().setter().accept(rest);
        player.setHealth(1.0f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.level().broadcastEntityEvent(player, (byte) 35);
        event.setCanceled(true);
    }

    /** PlayerEvents.tooltipEvent: vis discount and stored vis charge lines (warp is added by WarpEvents). */
    public static void tooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof VisDiscountGear g) {
            int al = g.getVisDiscount(stack, event.getEntity());
            if (al > 0) event.getToolTip().add(Component.translatable("tc.visdiscount").append(": " + al + "%").withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (stack.getItem() instanceof IRechargable) {
            int c = Math.round((float) RechargeHelper.getCharge(stack));
            if (c >= 0) event.getToolTip().add(Component.translatable("tc.charge").append(" " + c).withStyle(ChatFormatting.YELLOW));
        }
    }

    /** PacketPlayerFlagToServer flag 1 (ring of clouds double jump): reset fall distance. */
    public static void cloudJump(ServerPlayer player) {
        if (Baubles.isEquipped(player, ModItems.CLOUD_RING.get())) player.fallDistance = 0.0;
    }

    private BaubleEvents() {}
}
