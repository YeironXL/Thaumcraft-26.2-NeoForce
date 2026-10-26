package art.arcane.thaumcraft.entity.projectile;

import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** TC6 EntityGolemDart: a small arrow fired by dart-launcher golem arms; it can be picked up as an arrow. */
public class GolemDartEntity extends AbstractArrow {
    public GolemDartEntity(EntityType<? extends GolemDartEntity> type, Level level) { super(type, level); }
    public GolemDartEntity(Level level, LivingEntity shooter) { super(ModEntities.GOLEM_DART.get(), shooter, level, new ItemStack(Items.ARROW), null); }
    @Override protected ItemStack getDefaultPickupItem() { return new ItemStack(Items.ARROW); }
}
