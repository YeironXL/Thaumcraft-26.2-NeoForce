package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.entity.BottleTaintEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 ItemBottleTaint: thrown like a splash potion. */
public final class BottleTaintItem extends Item {
    public BottleTaintItem(Properties p) { super(p); }
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack thrown = held.copyWithCount(1);
        if (!player.getAbilities().instabuild) held.shrink(1);
        player.playSound(SoundEvents.EGG_THROW, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClientSide()) {
            var bottle = new BottleTaintEntity(world, player, thrown);
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0f, 0.66f, 1.0f);
            world.addFreshEntity(bottle);
        }
        return InteractionResult.SUCCESS;
    }
}
