package art.arcane.thaumcraft.item;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;
/** Original TC6 CraftingEvents fuel duration: 4,800 ticks. */
public final class AlumentumItem extends Item {
    public AlumentumItem(Properties properties){super(properties);}
    @Override public int getBurnTime(ItemStack stack,RecipeType<?> recipeType,FuelValues fuels){return 4800;}
    @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        level.playSound(null,player.getX(),player.getY(),player.getZ(),net.minecraft.sounds.SoundEvents.EGG_THROW,
            net.minecraft.sounds.SoundSource.PLAYERS,0.3F,0.4F/(level.getRandom().nextFloat()*0.4F+0.8F));
        if (level instanceof net.minecraft.server.level.ServerLevel server)
            net.minecraft.world.entity.projectile.Projectile.spawnProjectileFromRotation(art.arcane.thaumcraft.entity.AlumentumProjectile::new,server,stack,player,-5.0F,0.75F,2.0F);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        stack.consume(1,player);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
