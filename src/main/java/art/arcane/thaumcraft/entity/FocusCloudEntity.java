package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.casters.FocusEffect;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusPackage;
import art.arcane.thaumcraft.casters.Trajectory;
import art.arcane.thaumcraft.events.ServerEvents;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntityFocusCloud: lingering cloud applying the remaining package every 5 ticks to creatures and random blocks inside. */
public final class FocusCloudEntity extends Entity implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(FocusCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final Map<Long, Long> cooldownMap = new HashMap<>();
    FocusPackage focusPackage;
    private LivingEntity owner;
    private UUID ownerUniqueId;
    private int duration;
    private FocusEffect[] effects;

    public FocusCloudEntity(EntityType<? extends FocusCloudEntity> type, Level level) { super(type, level); }

    public FocusCloudEntity(FocusPackage pack, Trajectory trajectory, float rad, int dur) {
        super(ModEntities.FOCUS_CLOUD.get(), pack.world);
        focusPackage = pack;
        setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        setOwner(pack.getCaster());
        setRadius(rad);
        duration = dur;
    }

    public void setOwner(LivingEntity o) { owner = o; ownerUniqueId = o == null ? null : o.getUUID(); }
    public LivingEntity getOwner() {
        if (owner == null && ownerUniqueId != null && level() instanceof ServerLevel sl && sl.getEntity(ownerUniqueId) instanceof LivingEntity le) owner = le;
        return owner;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(RADIUS, 0.5f); }
    public void setRadius(float r) { if (!level().isClientSide()) entityData.set(RADIUS, r); refreshDimensions(); }
    public float getRadius() { return entityData.get(RADIUS); }
    @Override public EntityDimensions getDimensions(Pose pose) { return EntityDimensions.scalable(getRadius() * 2.0f, 0.5f); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { if (RADIUS.equals(key)) refreshDimensions(); super.onSyncedDataUpdated(key); }
    @Override public boolean hurtServer(ServerLevel l, DamageSource s, float a) { return false; }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buf) { ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, focusPackage == null ? new CompoundTag() : focusPackage.serialize()); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buf) { focusPackage = new FocusPackage(); focusPackage.deserialize(ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf)); }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Age", tickCount); out.putInt("Duration", duration); out.putFloat("Radius", getRadius());
        if (ownerUniqueId != null) out.store("OwnerUUID", UUIDUtil.CODEC, ownerUniqueId);
        if (focusPackage != null) out.store("pack", CompoundTag.CODEC, focusPackage.serialize());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        tickCount = in.getIntOr("Age", 0); duration = in.getIntOr("Duration", 0); setRadius(in.getFloatOr("Radius", 0.5f));
        ownerUniqueId = in.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        focusPackage = new FocusPackage();
        in.read("pack", CompoundTag.CODEC).ifPresent(focusPackage::deserialize);
    }

    @Override
    public void tick() {
        super.tick();
        float rad = getRadius();
        if (!level().isClientSide() && (tickCount > duration * 20 || getOwner() == null)) discard();
        if (!isAlive()) return;
        if (level().isClientSide()) {
            if (focusPackage == null) return;
            if (effects == null) effects = focusPackage.getFocusEffects();
            if (effects != null && effects.length > 0) {
                var r = random;
                for (int a = 0; a < rad; ++a) {
                    FocusEffect eff = effects[r.nextInt(effects.length)];
                    int c = FocusEngine.getElementColor(eff.getKey());
                    art.arcane.thaumcraft.client.fx.FXDispatcher.generic(getX() + r.nextGaussian() * rad / 2.0 * 0.85, getY() + r.nextGaussian() * rad / 2.0 * 0.85,
                            getZ() + r.nextGaussian() * rad / 2.0 * 0.85, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01,
                            GenericParticleOptions.builder().maxAge(20 + r.nextInt(10)).color(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f)
                                    .alpha(0.0f, 0.66f, 0.0f).grid(16).particles(56 + r.nextInt(4), 1, 1).scale(5.0f + r.nextFloat(), 10.0f + r.nextFloat())
                                    .layer(0).slowDown(0.99).wind(0.001, 0.001).rotation(r.nextFloat(), r.nextBoolean() ? -0.25f : 0.25f).build(), 0);
                    eff.renderParticleFX(level(), getX() + r.nextGaussian() * rad / 2.0, getY() + r.nextGaussian() * rad / 2.0, getZ() + r.nextGaussian() * rad / 2.0,
                            r.nextGaussian() * 0.01, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01);
                }
            }
        } else if (tickCount % 5 == 0 && level() instanceof ServerLevel sl) {
            long t = System.currentTimeMillis();
            List<Trajectory> trajectories = new ArrayList<>();
            List<HitResult> targets = new ArrayList<>();
            for (Entity e : sl.getEntities(this, new AABB(position(), position()).inflate(rad))) {
                if (!e.isAlive()) continue;
                if (e instanceof FocusCloudEntity other) {
                    Vec3 v = e.position().subtract(position());
                    e.move(MoverType.SELF, new Vec3(v.x / 50.0, v.y / 50.0, v.z / 50.0));
                }
                if (!(e instanceof LivingEntity)) continue;
                // TC6 stores entity cooldowns under a long key but checks an int key, so creatures are hit every pulse.
                cooldownMap.put((long) e.getId(), t + 2000L);
                Vec3 hv = e.position().add(0, e.getBbHeight() / 2.0f, 0);
                targets.add(new EntityHitResult(e, hv));
                trajectories.add(new Trajectory(position(), hv.subtract(position())));
            }
            for (int a = 0; a < rad; ++a) {
                Vec3 dV = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                HitResult br = EntityUtils.rayTraceBlocks(sl, position(), position().add(dV.scale(rad)), this);
                long bl = 0L;
                if (br instanceof net.minecraft.world.phys.BlockHitResult bh) {
                    bl = bh.getBlockPos().asLong();
                    if (cooldownMap.containsKey(bl)) {
                        if (cooldownMap.get(bl) <= t) cooldownMap.remove(bl); else br = null;
                    }
                }
                if (br != null) { targets.add(br); trajectories.add(new Trajectory(position(), dV)); cooldownMap.put(bl, t + 2000L); }
            }
            if (!targets.isEmpty() && focusPackage != null) {
                FocusPackage pack = focusPackage;
                LivingEntity own = getOwner();
                ServerEvents.addRunnableServer(sl, () -> FocusEngine.runFocusPackage(pack.copy(own), trajectories.toArray(new Trajectory[0]), targets.toArray(new HitResult[0])), 0);
            }
        }
    }
}
