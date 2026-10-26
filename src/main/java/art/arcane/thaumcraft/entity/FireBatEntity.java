package art.arcane.thaumcraft.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityFireBat: nether hell-bat that hangs from ceilings, chases the nearest player and sometimes explodes on attack. */
public class FireBatEntity extends Monster {
    private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(FireBatEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    public LivingEntity owner;
    public int damBonus;
    private int attackTime;

    public FireBatEntity(EntityType<? extends FireBatEntity> type, Level level) {
        super(type, level);
        setIsBatHanging(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    /** getCanSpawnHere: raw light <= rand(7) on top of the usual mob placement rules. */
    public static boolean checkFireBatSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        return level.getRawBrightness(pos, 0) <= random.nextInt(7) && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(HANGING, false); }
    public boolean getIsBatHanging() { return entityData.get(HANGING); }
    public void setIsBatHanging(boolean v) { entityData.set(HANGING, v); }

    @Override protected float getSoundVolume() { return 0.1f; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.95f; }
    @Override protected SoundEvent getAmbientSound() { return getIsBatHanging() && random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
    @Override public boolean isPushable() { return false; }

    @Override
    public void aiStep() {
        if (isInWaterOrRain() && level() instanceof ServerLevel sl) hurtServer(sl, damageSources().drown(), 1.0f);
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (getIsBatHanging()) {
            setDeltaMovement(Vec3.ZERO);
            setPosRaw(getX(), Mth.floor(getY()) + 1.0 - getBbHeight(), getZ());
        } else {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6000000238418579, 1));
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (attackTime > 0) --attackTime;
        BlockPos pos = blockPosition();
        BlockPos above = pos.above();
        if (getIsBatHanging()) {
            if (!level.getBlockState(above).isRedstoneConductor(level, above)) {
                setIsBatHanging(false);
                level.levelEvent(null, 1025, pos, 0);
            } else {
                if (random.nextInt(200) == 0) yHeadRot = random.nextInt(360);
                if (level.getNearestPlayer(this, 4.0) != null) {
                    setIsBatHanging(false);
                    level.levelEvent(null, 1025, pos, 0);
                }
            }
        } else {
            LivingEntity target = getTarget();
            Vec3 m = getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z, dx, dy, dz;
            if (target == null) {
                if (currentFlightTarget != null && (!level.isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level.getMinY() + 1)) currentFlightTarget = null;
                if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.distToCenterSqr(position()) < 4.0)
                    currentFlightTarget = new BlockPos((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2, (int) getZ() + random.nextInt(7) - random.nextInt(7));
                dx = currentFlightTarget.getX() + 0.5 - getX(); dy = currentFlightTarget.getY() + 0.1 - getY(); dz = currentFlightTarget.getZ() + 0.5 - getZ();
            } else {
                dx = target.getX() - getX(); dy = target.getY() + target.getEyeHeight() * 0.66f - getY(); dz = target.getZ() - getZ();
            }
            mx += (Math.signum(dx) * 0.5 - mx) * 0.10000000149011612;
            my += (Math.signum(dy) * 0.699999988079071 - my) * 0.10000000149011612;
            mz += (Math.signum(dz) * 0.5 - mz) * 0.10000000149011612;
            setDeltaMovement(mx, my, mz);
            float var4 = (float) (Math.atan2(mz, mx) * 180.0 / Math.PI) - 90.0f;
            float var5 = Mth.wrapDegrees(var4 - getYRot());
            zza = 0.5f;
            setYRot(getYRot() + var5);
            if (target == null && random.nextInt(100) == 0 && level.getBlockState(above).isRedstoneConductor(level, above)) setIsBatHanging(true);
        }
        LivingEntity target = getTarget();
        if (target == null) setTarget(findPlayerToAttack(level));
        else if (target.isAlive()) {
            float f = target.distanceTo(this);
            if (isAlive() && hasLineOfSight(target)) attackEntity(level, target, f);
        } else setTarget(null);
        if (getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
    }

    protected void attackEntity(ServerLevel level, Entity entity, float dist) {
        if (attackTime <= 0 && dist < Math.max(2.5f, entity.getBbWidth() * 1.1f) && entity.getBoundingBox().maxY > getBoundingBox().minY && entity.getBoundingBox().minY < getBoundingBox().maxY) {
            attackTime = 20 + level.getRandom().nextInt(20);
            if (level.getRandom().nextInt(10) == 0) {
                entity.invulnerableTime = 0;
                level.explode(this, getX(), getY(), getZ(), 1.5f, false, Level.ExplosionInteraction.NONE);
                discard();
            }
            playSound(SoundEvents.BAT_HURT, 0.5f, 0.9f + level.getRandom().nextFloat() * 0.2f);
            doHurtTarget(level, entity);
        }
    }

    protected LivingEntity findPlayerToAttack(ServerLevel level) { return level.getNearestPlayer(this, 12.0); }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableTo(level, source) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
        if (getIsBatHanging()) setIsBatHanging(false);
        return super.hurtServer(level, source, amount);
    }

    @Override public boolean causeFallDamage(double d, float f, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState s, BlockPos p) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("hang", getIsBatHanging());
        out.putByte("damBonus", (byte) damBonus);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setIsBatHanging(in.getBooleanOr("hang", false));
        damBonus = in.getByteOr("damBonus", (byte) 0);
    }
}
