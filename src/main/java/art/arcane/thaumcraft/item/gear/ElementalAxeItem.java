package art.arcane.thaumcraft.item.gear;

import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemElementalAxe (Axe of the Stream): THAUMIUM_ELEMENTAL axe (8 damage, -3 speed), rare. Holding right click draws
 * every loose item within 10 blocks towards the user. Tree felling and wood collection come from the BURROWING and COLLECTOR
 * infusion enchantments the infusion recipe puts on it (InfusionEnchantmentEvents).
 */
public class ElementalAxeItem extends AxeItem {
    public ElementalAxeItem(Properties p) { super(ElementalGear.TOOL, 5.0F, -3.0F, p); }

    /** 1.12 axes cannot strip logs: a right click on a block falls through to use(). */
    @Override public InteractionResult useOn(UseOnContext context) { return InteractionResult.PASS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity user) { return 72000; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int remaining) {
        pull(level, player);
    }

    /** onUsingTick: EntityUtils.getEntitiesInRange(..., EntityItem, 10) pulled at 0.3 per tick, clamped to 0.25. */
    public static void pull(Level level, LivingEntity player) {
        List<ItemEntity> stuff = level.getEntitiesOfClass(ItemEntity.class, new AABB(player.position(), player.position()).inflate(10.0),
                e -> e.distanceToSqr(player) <= 100.0);
        for (ItemEntity e : stuff) {
            if (!e.isAlive()) continue;
            double d6 = e.getX() - player.getX();
            double d7 = e.getY() - player.getY() + player.getBbHeight() / 2.0f;
            double d8 = e.getZ() - player.getZ();
            double d9 = Math.sqrt(d6 * d6 + d7 * d7 + d8 * d8);
            if (d9 < 1.0E-4) continue;
            d6 /= d9; d7 /= d9; d8 /= d9;
            double d10 = 0.3;
            Vec3 m = e.getDeltaMovement();
            double mx = Mth.clamp(m.x - d6 * d10, -0.25, 0.25);
            double my = Mth.clamp(m.y - (d7 * d10 - 0.1), -0.25, 0.25);
            double mz = Mth.clamp(m.z - d8 * d10, -0.25, 0.25);
            e.setDeltaMovement(mx, my, mz);
            e.hurtMarked = true;
            if (level.isClientSide()) {
                var r = level.getRandom();
                art.arcane.thaumcraft.client.fx.ClientHooks.crucibleBubble((float) e.getX() + (r.nextFloat() - r.nextFloat()) * 0.2f,
                        (float) e.getY() + e.getBbHeight() + (r.nextFloat() - r.nextFloat()) * 0.2f, (float) e.getZ() + (r.nextFloat() - r.nextFloat()) * 0.2f, 0.33f, 0.33f, 1.0f);
            }
        }
    }
}
