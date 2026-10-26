package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/** TC6 EntityBottleTaint: shatters into taint poison for creatures within 5 blocks and splashes flux goo around. */
public final class BottleTaintEntity extends ThrowableItemProjectile {
    public BottleTaintEntity(EntityType<? extends BottleTaintEntity> type, Level level) { super(type, level); }
    public BottleTaintEntity(Level level, LivingEntity owner, ItemStack stack) { super(ModEntities.BOTTLE_TAINT.get(), owner, level, stack); }
    @Override protected Item getDefaultItem() { return ModItems.BOTTLE_TAINT.get(); }

    @Override
    protected void onHit(HitResult ray) {
        super.onHit(ray);
        if (level().isClientSide()) return;
        for (LivingEntity el : level().getEntitiesOfClass(LivingEntity.class, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(5.0)))
            if (!el.isInvertedHealAndHarm()) el.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 100, 0, false, true));
        for (int a = 0; a < 10; ++a) {
            int xx = (int) ((random.nextFloat() - random.nextFloat()) * 4.0f);
            int zz = (int) ((random.nextFloat() - random.nextFloat()) * 4.0f);
            BlockPos p = blockPosition().offset(xx, 0, zz);
            if (level().getRandom().nextBoolean()) {
                if (level().getBlockState(p.below()).isRedstoneConductor(level(), p.below()) && level().getBlockState(p).canBeReplaced()) {
                    level().setBlockAndUpdate(p, ModBlocks.FLUX_GOO.get().defaultBlockState());
                } else {
                    p = p.below();
                    if (level().getBlockState(p.below()).isRedstoneConductor(level(), p.below()) && level().getBlockState(p).canBeReplaced())
                        level().setBlockAndUpdate(p, ModBlocks.FLUX_GOO.get().defaultBlockState());
                }
            }
        }
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != 3) { super.handleEntityEvent(id); return; }
        var r = level().getRandom();
        for (int a = 0; a < 100; ++a) {
            int col = r.nextBoolean() ? 0x99004C : 0x4C004C;
            level().addParticle(new DustParticleOptions(col, 1.0f), getX(), getY() + r.nextFloat() * getBbHeight(), getZ(), Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
        }
        for (int k = 0; k < 8; ++k)
            level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, ModItems.BOTTLE_TAINT.get()), getX(), getY(), getZ(), r.nextGaussian() * 0.15, r.nextDouble() * 0.2, r.nextGaussian() * 0.15);
        level().playLocalSound(getX(), getY(), getZ(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0f, r.nextFloat() * 0.1f + 0.9f, false);
    }
}
