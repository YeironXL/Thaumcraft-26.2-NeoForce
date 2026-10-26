package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemElementalSword (Sword of the Zephyr): THAUMIUM_ELEMENTAL sword, rare. Holding right click raises a whirlwind:
 * the user is lifted (fall damage negated as it goes), living non-player entities within 2.5 blocks are flung away, wind
 * plays every second and the blade loses one durability per second. Arcing strikes come from the ARCING 2 infusion
 * enchantment the recipe applies.
 */
public class ElementalSwordItem extends Item {
    public ElementalSwordItem(Properties p) { super(p.sword(ElementalGear.TOOL, 3.0F, -2.4F)); }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int remaining) {
        int ticks = getUseDuration(stack, player) - remaining;
        updraft(player);
        if (player instanceof ServerPlayer sp) art.arcane.thaumcraft.util.Flight.resetFloatCounter(sp);
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(2.5))) {
            if (entity instanceof Player || !(entity instanceof LivingEntity) || !entity.isAlive()) continue;
            if (player.getVehicle() == entity) continue;
            Vec3 p = player.position(), t = entity.position();
            double distance = p.distanceTo(t) + 0.1;
            Vec3 r = t.subtract(p);
            entity.setDeltaMovement(entity.getDeltaMovement().add(r.x / 2.5 / distance, r.y / 2.5 / distance, r.z / 2.5 / distance));
            if (!level.isClientSide()) entity.hurtMarked = true;
        }
        if (level.isClientSide()) {
            var rand = level.getRandom();
            int miny = (int) (player.getBoundingBox().minY - 2.0);
            if (player.onGround()) miny = Mth.floor(player.getBoundingBox().minY);
            for (int a = 0; a < 5; ++a)
                art.arcane.thaumcraft.client.fx.FXDispatcher.smokeSpiral(player.getX(), player.getBoundingBox().minY + player.getBbHeight() / 2.0f, player.getZ(),
                        1.5f, rand.nextInt(360), (int) miny, 14540253);
            if (player.onGround()) {
                float r2 = rand.nextFloat() * 360.0f;
                float mx = -Mth.sin(r2 / 180.0f * (float) Math.PI) / 5.0f;
                float mz = Mth.cos(r2 / 180.0f * (float) Math.PI) / 5.0f;
                level.addParticle(ParticleTypes.SMOKE, player.getX(), player.getBoundingBox().minY + 0.1, player.getZ(), mx, 0.0, mz);
            }
        } else if (ticks == 0 || ticks % 20 == 0) {
            player.playSound(ModSounds.get("wind"), 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        }
        if (ticks % 20 == 0 && !level.isClientSide()) stack.hurtAndBreak(1, player, player.getUsedItemHand().asEquipmentSlot());
    }

    /** The lift: falling slowed by 1/1.2 per tick, +0.08 upward, capped (above 0.5 drops back to 0.2). */
    public static void updraft(LivingEntity player) {
        Vec3 m = player.getDeltaMovement();
        double my = m.y;
        if (my < 0.0) { my /= 1.2000000476837158; player.fallDistance /= 1.2f; }
        my += 0.07999999821186066;
        if (my > 0.5) my = 0.20000000298023224;
        player.setDeltaMovement(m.x, my, m.z);
    }
}
