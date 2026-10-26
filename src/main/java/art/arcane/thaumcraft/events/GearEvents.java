package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.item.RechargeHelper;
import art.arcane.thaumcraft.item.gear.FortressArmorItem;
import art.arcane.thaumcraft.item.gear.TravellerBootsItem;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Fortress armour: ISpecialArmor set bonus and magic ratio, and the EntityEvents.entityHurt mask effects. */
public final class GearEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(GearEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(GearEvents::hurt);
        NeoForge.EVENT_BUS.addListener(GearEvents::jump);
        NeoForge.EVENT_BUS.addListener(GearEvents::armorHurt);
    }

    private static void playerTick(PlayerTickEvent.Post e) {
        if (!e.getEntity().level().isClientSide()) FortressArmorItem.updateSetBonus(e.getEntity());
        TravellerBootsItem.tick(e.getEntity());
        art.arcane.thaumcraft.item.gear.HoverHarnessItem.tick(e.getEntity());
    }

    private static int helmMask(LivingEntity e) {
        ItemStack helm = e.getItemBySlot(EquipmentSlot.HEAD);
        return helm.getItem() instanceof FortressArmorItem ? FortressArmorItem.getMask(helm) : -1;
    }

    private static void hurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        var src = event.getSource();
        float amount = event.getAmount();
        // Sipping Fiend: the masked attacker sometimes drinks a heart of life.
        if (src.getEntity() instanceof Player leecher && helmMask(leecher) == 2 && leecher.getRandom().nextFloat() < amount / 12.0f) leecher.heal(1.0f);
        // PlayerEvents.onFallDamage: traveller's boots halve fall damage and take one more off.
        if (victim instanceof Player fp && src.is(DamageTypes.FALL) && TravellerBootsItem.worn(fp)) {
            float f = Math.max(0.0f, amount / 2.0f - 1.0f);
            if (f < 1.0f) { event.setCanceled(true); return; }
            event.setAmount(f);
            amount = f;
        }
        if (victim instanceof Player player) {
            // Angry Ghost: whoever strikes the wearer may wither.
            if (src.getEntity() instanceof LivingEntity attacker && helmMask(player) == 1 && player.getRandom().nextFloat() < amount / 10.0f)
                attacker.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            // ISpecialArmor: fortress armour and void robes take over the whole armour stage of the hit.
            if (art.arcane.thaumcraft.item.gear.SpecialArmor.wearsSpecial(player) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setAmount(art.arcane.thaumcraft.item.gear.SpecialArmor.applyArmor(player, src, event.getAmount()));
                event.getContainer().addModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR, (c, r) -> 0.0f);
            }
        }
    }
    /** Vanilla armour wear is already spent by SpecialArmor.applyArmor for this hit. */
    private static void armorHurt(net.neoforged.neoforge.event.entity.living.ArmorHurtEvent e) {
        if (art.arcane.thaumcraft.item.gear.SpecialArmor.handledThisTick(e.getEntity())) e.setCanceled(true);
    }
    /** PlayerEvents.playerJumps: charged traveller's boots add 0.275 to the jump. */
    private static void jump(LivingEvent.LivingJumpEvent e) {
        if (e.getEntity() instanceof Player p && TravellerBootsItem.worn(p) && RechargeHelper.getCharge(p.getItemBySlot(EquipmentSlot.FEET)) > 0)
            p.setDeltaMovement(p.getDeltaMovement().add(0, 0.2750000059604645, 0));
    }
    private GearEvents() {}
}
