package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.entity.CausalityCollapserProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 ItemCausalityCollapser: stacks to 16 and is thrown (speed 0.8, -5 pitch offset) to collapse flux rifts. */
public final class CausalityCollapserItem extends Item {
    public CausalityCollapserItem(Properties p) { super(p.stacksTo(16)); }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.3F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel server) Projectile.spawnProjectileFromRotation(CausalityCollapserProjectile::new, server, stack, player, -5.0F, 0.8F, 2.0F);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
