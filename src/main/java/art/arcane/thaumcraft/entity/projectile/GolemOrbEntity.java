package art.arcane.thaumcraft.entity.projectile;

import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntityGolemOrb: a homing electric orb (red when fired by crimson clerics); can be batted away. */
public class GolemOrbEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
    LivingEntity target;
    public boolean red;

    public GolemOrbEntity(EntityType<? extends GolemOrbEntity> type, Level level) { super(type, level); }

    public GolemOrbEntity(Level level, LivingEntity thrower, LivingEntity t, boolean r) {
        super(ModEntities.GOLEM_ORB.get(), thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ(), level);
        setOwner(thrower);
        target = t;
        red = r;
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
    @Override protected double getDefaultGravity() { return 0.0; }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf data) { data.writeInt(target != null ? target.getId() : -1); data.writeBoolean(red); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf data) {
        int id = data.readInt();
        if (id >= 0 && level().getEntity(id) instanceof LivingEntity le) target = le;
        red = data.readBoolean();
    }

    @Override
    protected void onHit(HitResult mop) {
        if (level() instanceof ServerLevel sl && getOwner() instanceof LivingEntity thrower && mop instanceof EntityHitResult eh) {
            double atk = thrower.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
            eh.getEntity().hurtServer(sl, sl.damageSources().indirectMagic(this, thrower), (float) atk * (red ? 1.0f : 0.6f));
        }
        playSound(ModSounds.get("shock"), 1.0f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f);
        if (level().isClientSide()) art.arcane.thaumcraft.client.fx.ClientHooks.burst(getX(), getY(), getZ(), 1.0f);
        if (!level().isClientSide()) discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > (red ? 240 : 160)) discard();
        if (target != null) {
            double d = distanceToSqr(target);
            double dx = target.getX() - getX();
            double dy = target.getBoundingBox().minY + target.getBbHeight() * 0.6 - getY();
            double dz = target.getZ() - getZ();
            double d2 = 0.2;
            dx /= d; dy /= d; dz /= d;
            Vec3 m = getDeltaMovement();
            setDeltaMovement(Mth.clamp((float) (m.x + dx * d2), -0.25f, 0.25f), Mth.clamp((float) (m.y + dy * d2), -0.25f, 0.25f), Mth.clamp((float) (m.z + dz * d2), -0.25f, 0.25f));
        }
    }

    @Override public boolean isPickable() { return true; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableToBase(source)) return false;
        if (source.getEntity() != null) {
            Vec3 v = source.getEntity().getLookAngle();
            setDeltaMovement(v.scale(0.9));
            playSound(ModSounds.get("zap"), 1.0f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.2f);
            return true;
        }
        return false;
    }
}
