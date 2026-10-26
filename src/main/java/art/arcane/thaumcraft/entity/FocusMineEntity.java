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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntityFocusMine: arms on impact and triggers the remaining package on the first valid creature within 1 block. */
public final class FocusMineEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(FocusMineEntity.class, EntityDataSerializers.BOOLEAN);
    FocusPackage focusPackage;
    boolean friendly;
    public int counter = 40;
    private FocusEffect[] effects;

    public FocusMineEntity(EntityType<? extends FocusMineEntity> type, Level level) { super(type, level); }

    public FocusMineEntity(FocusPackage pack, Trajectory trajectory, boolean friendly) {
        super(ModEntities.FOCUS_MINE.get(), pack.world);
        focusPackage = pack;
        this.friendly = friendly;
        setOwner(pack.getCaster());
        setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, 0.0f, 0.0f);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(ARMED, false); }
    public boolean getIsArmed() { return entityData.get(ARMED); }
    public void setIsArmed(boolean v) { entityData.set(ARMED, v); }
    @Override protected double getDefaultGravity() { return 0.01; }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buf) { ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, focusPackage == null ? new CompoundTag() : focusPackage.serialize()); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buf) { focusPackage = new FocusPackage(); focusPackage.deserialize(ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf)); }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("armed", getIsArmed());
        if (focusPackage != null) out.store("pack", CompoundTag.CODEC, focusPackage.serialize());
        out.putBoolean("friendly", friendly);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        friendly = in.getBooleanOr("friendly", false);
        setIsArmed(in.getBooleanOr("armed", false));
        if (getIsArmed()) counter = 0;
        focusPackage = new FocusPackage();
        in.read("pack", CompoundTag.CODEC).ifPresent(focusPackage::deserialize);
    }

    @Override protected void onHit(HitResult mop) {
        if (getOwner() != null) setIsArmed(true);
        if (mop.getType() == HitResult.Type.BLOCK) setDeltaMovement(getDeltaMovement().scale(0.25)); // pushOutOfBlocks damping
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 1200 || (!level().isClientSide() && getOwner() == null)) discard();
        if (!isAlive() || !getIsArmed()) return;
        if (counter > 0) --counter;
        if (counter > 0 || tickCount % 5 != 0) return;
        if (level().isClientSide()) {
            if (focusPackage == null) return;
            if (effects == null) effects = focusPackage.getFocusEffects();
            if (effects != null && effects.length > 0) {
                FocusEffect eff = effects[random.nextInt(effects.length)];
                eff.renderParticleFX(level(), getX() + random.nextGaussian() * 0.1, getY() + random.nextGaussian() * 0.1, getZ() + random.nextGaussian() * 0.1,
                        random.nextGaussian() * 0.01, random.nextGaussian() * 0.01, random.nextGaussian() * 0.01);
            }
        } else if (level() instanceof ServerLevel sl && focusPackage != null) {
            int d = 0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(1.0))) {
                if (!e.isAlive()) continue;
                boolean f = EntityUtils.isFriendly(focusPackage.getCaster(), e);
                if (friendly != f) continue;
                Vec3 epv = e.position().add(0, e.getBbHeight() / 2.0f, 0);
                Vec3 here = position();
                FocusPackage pack = focusPackage;
                var thrower = getOwner() instanceof LivingEntity le ? le : null;
                ServerEvents.addRunnableServer(sl, () -> FocusEngine.runFocusPackage(pack.copy(thrower), new Trajectory[] {new Trajectory(here, epv.subtract(here).normalize())},
                        new HitResult[] {new EntityHitResult(e, epv)}), d++);
            }
            if (d > 0) discard();
        }
    }
}
