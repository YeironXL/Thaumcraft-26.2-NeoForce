package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.casters.FocusEffect;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusPackage;
import art.arcane.thaumcraft.casters.Trajectory;
import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.util.EntityUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntityFocusProjectile: carries the rest of the focus package; options 1 bouncy, 2 seek hostile, 3 seek friendly. */
public final class FocusProjectileEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Integer> SPECIAL = SynchedEntityData.defineId(FocusProjectileEntity.class, EntityDataSerializers.INT);
    FocusPackage focusPackage;
    private LivingEntity target;
    private FocusEffect[] effects;

    public FocusProjectileEntity(EntityType<? extends FocusProjectileEntity> type, Level level) { super(type, level); }

    public FocusProjectileEntity(FocusPackage pack, float speed, Trajectory trajectory, int special) {
        super(ModEntities.FOCUS_PROJECTILE.get(), pack.world);
        focusPackage = pack;
        var caster = pack.getCaster();
        setOwner(caster);
        double w = caster.getBbWidth() * 2.1;
        setPos(trajectory.source.x + trajectory.direction.x * w, trajectory.source.y + trajectory.direction.y * w, trajectory.source.z + trajectory.direction.z * w);
        shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, speed, 0.0f);
        setSpecial(special);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(SPECIAL, 0); }
    public void setSpecial(int s) { entityData.set(SPECIAL, s); }
    public int getSpecial() { return entityData.get(SPECIAL); }
    @Override protected double getDefaultGravity() { return getSpecial() > 1 ? 0.005 : 0.01; }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buf) { ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, focusPackage == null ? new CompoundTag() : focusPackage.serialize()); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buf) { focusPackage = new FocusPackage(); focusPackage.deserialize(ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf)); }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (focusPackage != null) out.store("pack", CompoundTag.CODEC, focusPackage.serialize());
        out.putInt("special", getSpecial());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setSpecial(in.getIntOr("special", 0));
        focusPackage = new FocusPackage();
        in.read("pack", CompoundTag.CODEC).ifPresent(focusPackage::deserialize);
    }

    @Override
    protected void onHit(HitResult mop) {
        if (getSpecial() == 1 && mop instanceof BlockHitResult bh && mop.getType() == HitResult.Type.BLOCK) {
            var bs = level().getBlockState(bh.getBlockPos());
            if (bs.getCollisionShape(level(), bh.getBlockPos()).isEmpty()) return;
            Vec3 m = getDeltaMovement();
            setPos(getX() - m.x, getY() - m.y, getZ() - m.z);
            double mx = m.x, my = m.y, mz = m.z;
            var side = bh.getDirection();
            if (side.getStepZ() != 0) mz *= -1.0;
            if (side.getStepX() != 0) mx *= -1.0;
            if (side.getStepY() != 0) my *= -0.9;
            mx *= 0.9; my *= 0.9; mz *= 0.9;
            setDeltaMovement(mx, my, mz);
            double v = Math.sqrt(mx * mx + my * my + mz * mz);
            setPos(getX() - mx / v * 0.05, getY() - my / v * 0.05, getZ() - mz / v * 0.05);
            if (!level().isClientSide()) playSound(SoundEvents.LEAD_TIED, 0.25f, 1.0f);
            if (!level().isClientSide() && v < 0.2) discard();
            return;
        }
        if (level() instanceof ServerLevel sl && focusPackage != null) {
            HitResult hit = mop instanceof EntityHitResult eh ? new EntityHitResult(eh.getEntity(), position()) : mop;
            Vec3 pv = new Vec3(xo, yo, zo);
            Vec3 vf = getDeltaMovement().normalize();
            FocusPackage pack = focusPackage;
            ServerEvents.addRunnableServer(sl, () -> FocusEngine.runFocusPackage(pack, new Trajectory[] {new Trajectory(pv, vf)}, new HitResult[] {hit}), 0);
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 1200 || (!level().isClientSide() && getOwner() == null)) discard();
        if (level().isClientSide()) renderParticle();
        if (target == null && tickCount % 5 == 0 && getSpecial() > 1) {
            for (LivingEntity pt : EntityUtils.getEntitiesInRangeSorted(level(), this, LivingEntity.class, 16.0)) {
                if (pt.isAlive() && EntityUtils.isVisibleTo(1.75f, this, pt, 16.0f) && EntityUtils.canEntityBeSeen(this, pt)) {
                    boolean f = EntityUtils.isFriendly(getOwner(), pt);
                    if (f && getSpecial() == 3) { target = pt; break; }
                    if (!f && getSpecial() == 2) { target = pt; break; }
                }
            }
        }
        if (target != null) {
            Vec3 v = new Vec3(target.getX() - getX(), target.getBoundingBox().minY + target.getBbHeight() * 0.6 - getY(), target.getZ() - getZ()).normalize();
            Vec3 mv = getDeltaMovement();
            double lv = mv.length();
            mv = mv.normalize().add(v.scale(0.275)).normalize().scale(lv);
            setDeltaMovement(mv);
            if (tickCount % 5 == 0 && (!target.isAlive() || !EntityUtils.isVisibleTo(1.75f, this, target, 16.0f) || !EntityUtils.canEntityBeSeen(this, target))) target = null;
        }
    }

    @Override public Vec3 getLookAngle() { return getDeltaMovement().normalize(); }

    /** RenderNoProjectile → renderParticle: coloured fire motes plus one effect particle. */
    private void renderParticle() {
        if (focusPackage == null) return;
        if (effects == null) effects = focusPackage.getFocusEffects();
        if (effects == null || effects.length == 0) return;
        for (int i = 0; i < 3; i++) {
            float f = i / 3.0f;
            double x = xo + (getX() - xo) * f, y = yo + (getY() - yo) * f + getBbHeight() / 2.0f, z = zo + (getZ() - zo) * f;
            FocusEffect eff = effects[random.nextInt(effects.length)];
            int c = FocusEngine.getElementColor(eff.getKey());
            art.arcane.thaumcraft.client.fx.FXDispatcher.drawFireMote((float) x, (float) y, (float) z, 0.0125f * (random.nextFloat() - 0.5f), 0.0125f * (random.nextFloat() - 0.5f),
                    0.0125f * (random.nextFloat() - 0.5f), ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 0.5f, 7.0f);
            if (i == 0) eff.renderParticleFX(level(), x + random.nextGaussian() * 0.1, y + random.nextGaussian() * 0.1, z + random.nextGaussian() * 0.1,
                    random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01);
        }
    }
}
