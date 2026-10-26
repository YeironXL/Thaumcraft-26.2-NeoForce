package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.casters.FocusEffect;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusPackage;
import art.arcane.thaumcraft.casters.Trajectory;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntitySpellBat: a short-lived bat carrying the rest of the focus package into its first target. */
public final class SpellBatEntity extends Monster implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Boolean> FRIENDLY = SynchedEntityData.defineId(SpellBatEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    private LivingEntity owner;
    FocusPackage focusPackage;
    private UUID ownerUniqueId;
    private int attackTime;
    private FocusEffect[] effects;
    public int color = 0xFFFFFF;

    public SpellBatEntity(EntityType<? extends SpellBatEntity> type, Level level) { super(type, level); }

    public SpellBatEntity(FocusPackage pac, boolean friendly) {
        super(ModEntities.SPELL_BAT.get(), pac.world);
        focusPackage = pac;
        setOwner(pac.getCaster());
        setIsFriendly(friendly);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(FRIENDLY, false); }
    public boolean getIsFriendly() { return entityData.get(FRIENDLY); }
    public void setIsFriendly(boolean v) { entityData.set(FRIENDLY, v); }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buf) { ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(buf, focusPackage == null ? new CompoundTag() : focusPackage.serialize()); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buf) { focusPackage = new FocusPackage(); focusPackage.deserialize(ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(buf)); }

    public void setFocusPackage(FocusPackage p) { focusPackage = p; }
    public void setOwner(LivingEntity o) { owner = o; ownerUniqueId = o == null ? null : o.getUUID(); }
    public LivingEntity getOwner() {
        if (owner == null && ownerUniqueId != null && level() instanceof ServerLevel sl && sl.getEntity(ownerUniqueId) instanceof LivingEntity le) owner = le;
        return owner;
    }

    @Override protected float getSoundVolume() { return 0.1f; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.95f; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
    @Override public boolean isPushable() { return false; }
    @Override public PlayerTeam getTeam() { var o = getOwner(); return o != null ? o.getTeam() : super.getTeam(); }
    @Override protected boolean considersEntityAsAlly(Entity other) {
        var o = getOwner();
        if (other == o) return true;
        if (o != null) return o.isAlliedTo(other) || other.isAlliedTo(o);
        return super.considersEntityAsAlly(other);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && (tickCount > 600 || getOwner() == null)) discard();
        setDeltaMovement(getDeltaMovement().multiply(1, 0.6000000238418579, 1));
        if (isAlive() && level().isClientSide() && focusPackage != null) {
            if (effects == null) {
                effects = focusPackage.getFocusEffects();
                int r = 0, g = 0, b = 0;
                for (FocusEffect ef : effects) { int c = FocusEngine.getElementColor(ef.getKey()); r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; }
                if (effects.length > 0) { r /= effects.length; g /= effects.length; b /= effects.length; }
                color = (r << 16) | (g << 8) | b;
            }
            if (effects.length > 0) {
                FocusEffect eff = effects[random.nextInt(effects.length)];
                eff.renderParticleFX(level(), getX() + random.nextGaussian() * 0.125, getY() + getBbHeight() / 2.0f + random.nextGaussian() * 0.125, getZ() + random.nextGaussian() * 0.125, 0, 0, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (attackTime > 0) --attackTime;
        Vec3 m = getDeltaMovement();
        double mx = m.x, my = m.y, mz = m.z;
        LivingEntity target = getTarget();
        double dx, dy, dz;
        if (target == null) {
            if (currentFlightTarget != null && (!level.isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level.getMinY() + 1)) currentFlightTarget = null;
            if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.distToCenterSqr(position()) < 4.0)
                currentFlightTarget = BlockPos.containing((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2, (int) getZ() + random.nextInt(7) - random.nextInt(7));
            dx = currentFlightTarget.getX() + 0.5 - getX(); dy = currentFlightTarget.getY() + 0.1 - getY(); dz = currentFlightTarget.getZ() + 0.5 - getZ();
        } else {
            dx = target.getX() - getX(); dy = target.getY() + target.getEyeHeight() * 0.66f - getY(); dz = target.getZ() - getZ();
        }
        mx += (Math.signum(dx) * 0.5 - mx) * 0.1; my += (Math.signum(dy) * 0.7 - my) * 0.1; mz += (Math.signum(dz) * 0.5 - mz) * 0.1;
        setDeltaMovement(mx, my, mz);
        float var4 = (float) (Math.atan2(mz, mx) * 180.0 / Math.PI) - 90.0f;
        float var5 = Mth.wrapDegrees(var4 - getYRot());
        zza = 0.5f;
        setYRot(getYRot() + var5);
        if (target == null) setTarget(findTargetToAttack(level));
        else if (target.isAlive()) {
            float f = target.distanceTo(this);
            if (isAlive() && hasLineOfSight(target)) attackEntity(level, target, f);
        } else setTarget(null);
        if (!getIsFriendly() && getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
    }

    private void attackEntity(ServerLevel level, Entity target, float dist) {
        if (attackTime <= 0 && dist < Math.max(2.5f, target.getBbWidth() * 1.1f) && target.getBoundingBox().maxY > getBoundingBox().minY && target.getBoundingBox().minY < getBoundingBox().maxY) {
            attackTime = 40;
            Vec3 hv = target.position().add(0, target.getBbHeight() / 2.0f, 0);
            if (focusPackage != null)
                FocusEngine.runFocusPackage(focusPackage.copy(getOwner()), new Trajectory[] {new Trajectory(position(), hv.subtract(position()))}, new HitResult[] {new EntityHitResult(target, hv)});
            setHealth(getHealth() - 1.0f);
            playSound(SoundEvents.BAT_HURT, 0.5f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    @Override protected void doPush(Entity e) { if (!getIsFriendly()) super.doPush(e); }

    private LivingEntity findTargetToAttack(ServerLevel level) {
        double d = Double.MAX_VALUE;
        LivingEntity ret = null;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(12.0), x -> x != this)) {
            if (!e.isAlive()) continue;
            if (getIsFriendly()) { if (!EntityUtils.isFriendly(getOwner(), e)) continue; }
            else { if (EntityUtils.isFriendly(getOwner(), e) || isAlliedTo(e)) continue; }
            double ed = distanceToSqr(e);
            if (ed >= d) continue;
            d = ed; ret = e;
        }
        return ret;
    }

    @Override public boolean causeFallDamage(double d, float f, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState s, BlockPos p) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected boolean shouldDropLoot(ServerLevel level) { return false; }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (ownerUniqueId != null) out.store("OwnerUUID", UUIDUtil.CODEC, ownerUniqueId);
        if (focusPackage != null) out.store("pack", CompoundTag.CODEC, focusPackage.serialize());
        out.putBoolean("friendly", getIsFriendly());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        ownerUniqueId = in.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        setIsFriendly(in.getBooleanOr("friendly", false));
        focusPackage = new FocusPackage();
        in.read("pack", CompoundTag.CODEC).ifPresent(focusPackage::deserialize);
    }
}
