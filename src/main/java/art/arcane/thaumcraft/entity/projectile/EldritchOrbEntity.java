package art.arcane.thaumcraft.entity.projectile;

import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** TC6 EntityEldritchOrb: gravity-less entropic orb; on impact hurts (2/3 of the thrower's attack) and weakens everything within 2 blocks. */
public class EldritchOrbEntity extends ThrowableProjectile {
    public EldritchOrbEntity(EntityType<? extends EldritchOrbEntity> type, Level level) { super(type, level); }

    /** EntityThrowable(world, thrower) + shoot(thrower, pitch, yaw, -5, 0.75, 0). */
    public EldritchOrbEntity(Level level, LivingEntity thrower) {
        super(ModEntities.ELDRITCH_ORB.get(), thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), level);
        setOwner(thrower);
        shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), -5.0f, 0.75f, 0.0f);
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
    @Override protected double getDefaultGravity() { return 0.0; }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 100) discard();
    }

    @Override
    protected void onHit(HitResult mop) {
        if (level() instanceof ServerLevel sl && getOwner() instanceof LivingEntity thrower) {
            for (Entity e : sl.getEntities(thrower, getBoundingBox().inflate(2.0, 2.0, 2.0))) {
                if (e instanceof LivingEntity le && !le.isInvertedHealAndHarm()) {
                    double atk = thrower.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
                    le.hurtServer(sl, sl.damageSources().indirectMagic(this, thrower), (float) atk * 0.666f);
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
                }
            }
            playSound(SoundEvents.LAVA_EXTINGUISH, 0.5f, 2.6f + (random.nextFloat() - random.nextFloat()) * 0.8f);
            discard();
        }
    }
}
