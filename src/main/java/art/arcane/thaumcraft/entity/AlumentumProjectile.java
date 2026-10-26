package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** TC6 projectile: speed 0.75, impact explosion strength 1.1, no fire. */
public final class AlumentumProjectile extends ThrowableItemProjectile {
    public AlumentumProjectile(EntityType<? extends AlumentumProjectile> type, Level level) { super(type, level); }
    public AlumentumProjectile(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntities.ALUMENTUM.get(), owner, level, stack);
    }
    @Override protected Item getDefaultItem() { return ModItems.ALUMENTUM.get(); }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide() && !isRemoved()) {
            for (int i=0;i<3;i++) {
                double fraction=i/3.0;
                var motion=getDeltaMovement();
                level().addParticle(ParticleTypes.SMOKE,getX()-motion.x*fraction,getY()-motion.y*fraction,getZ()-motion.z*fraction,0,0,0);
                level().addParticle(ParticleTypes.FLAME,getX(),getY(),getZ(),0,0,0);
            }
        }
    }
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide() && !isRemoved()) {
            level().explode(this,getX(),getY(),getZ(),1.1F,false,Level.ExplosionInteraction.TNT);
            discard();
        }
    }
}
