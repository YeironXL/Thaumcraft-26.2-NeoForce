package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/** TC6 EntityCausalityCollapser: explodes (strength 2, breaking blocks) and collapses every flux rift within 3 blocks. */
public final class CausalityCollapserProjectile extends ThrowableItemProjectile {
    public CausalityCollapserProjectile(EntityType<? extends CausalityCollapserProjectile> type, Level level) { super(type, level); }
    public CausalityCollapserProjectile(Level level, LivingEntity owner, ItemStack stack) { super(ModEntities.CAUSALITY_COLLAPSER.get(), owner, level, stack); }
    @Override protected Item getDefaultItem() { return ModItems.CAUSALITY_COLLAPSER.get(); }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide() && !isRemoved()) {
            var r = getRandom();
            for (int i = 0; i < 3; i++) {
                double c = i / 3.0;
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawAlumentum((float) (xo + (getX() - xo) * c), (float) (yo + (getY() - yo) * c) + getBbHeight() / 2.0f, (float) (zo + (getZ() - zo) * c),
                        0.0125f * (r.nextFloat() - 0.5f), 0.0125f * (r.nextFloat() - 0.5f), 0.0125f * (r.nextFloat() - 0.5f), 0.8f + r.nextFloat() * 0.2f, 0.3f + r.nextFloat() * 0.1f, r.nextFloat() * 0.1f, 0.5f, 4.0f);
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawGenericParticles(getX() + r.nextGaussian() * 0.2, getY() + r.nextGaussian() * 0.2, getZ() + r.nextGaussian() * 0.2,
                        0.0, 0.0, 0.0, 1.0f, 1.0f, 1.0f, 0.7f, false, 448, 8, 1, 8, 0, 0.3f, 0.0f, 1);
            }
        }
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide() && !isRemoved()) {
            level().explode(this, getX(), getY(), getZ(), 2.0F, false, Level.ExplosionInteraction.TNT);
            collapseRifts(level(), getX(), getY(), getZ());
            discard();
        }
    }

    public static void collapseRifts(Level level, double x, double y, double z) {
        for (FluxRiftEntity fr : level.getEntitiesOfClass(FluxRiftEntity.class, new AABB(x, y, z, x, y, z).inflate(3.0), e -> e.distanceToSqr(x, y, z) <= 9.0))
            fr.setCollapse(true);
    }
}
