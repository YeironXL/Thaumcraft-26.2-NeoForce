package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.entity.GrappleEntity;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** TC6 ItemGrappleGun: 100 charge, one charge per shot; "loaded" while its grapple exists. */
public final class GrappleGunItem extends Item implements IRechargable {
    public GrappleGunItem(Properties p) { super(p.stacksTo(1).rarity(Rarity.UNCOMMON)); }
    @Override public int getMaxCharge(ItemStack stack, LivingEntity player) { return 100; }
    @Override public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) { return EnumChargeDisplay.NORMAL; }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!GrappleEntity.grapples.containsKey(entity.getId()) && stack.has(ModComponents.GRAPPLE_LOADED.get())) stack.remove(ModComponents.GRAPPLE_LOADED.get());
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        player.playSound(ModSounds.get("ice"), 3.0f, 0.8f + world.getRandom().nextFloat() * 0.1f);
        ItemStack held = player.getItemInHand(hand);
        if (!world.isClientSide() && RechargeHelper.getCharge(held) > 0) {
            GrappleEntity grapple = new GrappleEntity(world, player, hand);
            grapple.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 1.5f, 0.0f);
            double px = -Mth.cos((player.getYRot() - 0.5f) / 180.0f * (float) Math.PI) * 0.2f * (grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1);
            double pz = -Mth.sin((player.getYRot() - 0.5f) / 180.0f * (float) Math.PI) * 0.3f * (grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1);
            Vec3 vl = player.getLookAngle();
            grapple.setPos(grapple.getX() + px + vl.x, grapple.getY(), grapple.getZ() + pz + vl.y);
            if (world.addFreshEntity(grapple)) {
                RechargeHelper.consumeCharge(held, player, 1);
                held.set(ModComponents.GRAPPLE_LOADED.get(), Unit.INSTANCE);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
