package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.projectile.GolemOrbEntity;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityEldritchGolem (the Eldritch Construct): a fire-immune 400 hp iron colossus that smashes in melee (x0.75
 * damage, a lift). The killing blow instead blows its head off (a small explosion, 100 more invulnerable ticks); headless,
 * it knocks targets back, crackles with arcs and charges a 150-point beam it spends on homing golem orbs.
 */
public class EldritchGolemEntity extends ThaumcraftBossEntity implements EldritchMob, RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(EldritchGolemEntity.class, EntityDataSerializers.BOOLEAN);
    public int beamCharge = 0;
    public boolean chargingBeam = false;
    int arcing = 0, ax = 0, ay = 0, az = 0;
    private int attackTimer;
    private boolean rangedAdded;

    public EldritchGolemEntity(EntityType<? extends EldritchGolemEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MAX_HEALTH, 400.0).add(Attributes.ARMOR, 6.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void generateName() {
        ChampionModifier m = ChampionModifier.of(this);
        if (m != null) setCustomName(Component.translatable("entity.EldritchGolem.name.custom", m.displayName()));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(HEADLESS, false); }
    public boolean isHeadless() { return entityData.get(HEADLESS); }
    public void setHeadless(boolean v) { entityData.set(HEADLESS, v); }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putBoolean("headless", isHeadless()); }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setHeadless(in.getBooleanOr("headless", false));
        if (isHeadless()) makeHeadless();
    }

    /** getEyeHeight: 3.0, 3.33 once headless. */
    public float eyeHeight() { return isHeadless() ? 3.33f : 3.0f; }
    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(eyeHeight());
    }
    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEADLESS.equals(key)) refreshDimensions();
    }

    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 1.0f); }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        spawnTimer = 100;
        return super.finalizeSpawn(level, diff, reason, data);
    }

    private static boolean isLoot(BlockState bs) {
        return bs.is(ModBlocks.LOOT_CRATE_COMMON.get()) || bs.is(ModBlocks.LOOT_CRATE_UNCOMMON.get()) || bs.is(ModBlocks.LOOT_CRATE_RARE.get())
                || bs.is(ModBlocks.LOOT_URN_COMMON.get()) || bs.is(ModBlocks.LOOT_URN_UNCOMMON.get()) || bs.is(ModBlocks.LOOT_URN_RARE.get());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackTimer > 0) --attackTimer;
        Vec3 m = getDeltaMovement();
        if (m.x * m.x + m.z * m.z > 2.500000277905201E-7 && random.nextInt(5) == 0) {
            BlockState bs = level().getBlockState(blockPosition());
            if (!bs.isAir()) level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), getX() + (random.nextFloat() - 0.5) * getBbWidth(),
                    getBoundingBox().minY + 0.1, getZ() + (random.nextFloat() - 0.5) * getBbWidth(), 4.0 * (random.nextFloat() - 0.5), 0.5, (random.nextFloat() - 0.5) * 4.0);
            if (!level().isClientSide() && isLoot(bs)) level().destroyBlock(blockPosition(), true);
        }
        if (!level().isClientSide()) {
            BlockState bs = level().getBlockState(blockPosition());
            float h = bs.getDestroySpeed(level(), blockPosition());
            if (!bs.isAir() && h >= 0.0f && h <= 0.15f) level().destroyBlock(blockPosition(), true);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (damage > getHealth() && !isHeadless()) {
            setHeadless(true);
            spawnTimer = 100;
            double xx = Mth.cos(getYRot() % 360.0f / 180.0f * 3.1415927f) * 0.75f;
            double zz = Mth.sin(getYRot() % 360.0f / 180.0f * 3.1415927f) * 0.75f;
            level.explode(this, getX() + xx, getY() + eyeHeight(), getZ() + zz, 2.0f, Level.ExplosionInteraction.NONE);
            makeHeadless();
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    void makeHeadless() {
        if (rangedAdded) return;
        rangedAdded = true;
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 3.0, 1.0, 5, 5, 24.0f));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (attackTimer > 0) return false;
        attackTimer = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        boolean flag = target.hurtServer(level, damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75f);
        if (flag) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.2000000059604645, 0.0));
            if (isHeadless()) target.push(-Mth.sin(getYRot() * 3.1415927f / 180.0f) * 1.5f, 0.1, Mth.cos(getYRot() * 3.1415927f / 180.0f) * 1.5f);
            target.hurtMarked = true;
        }
        return flag;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (hasLineOfSight(target) && !chargingBeam && beamCharge > 0) {
            beamCharge -= 15 + random.nextInt(5);
            getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0f, target.getZ(), 30.0f, 30.0f);
            Vec3 v = getViewVector(1.0f);
            GolemOrbEntity blast = new GolemOrbEntity(sl, this, target, false);
            blast.setPos(blast.getX() + v.x, blast.getY(), blast.getZ() + v.z);
            var tm = target.getDeltaMovement();
            double d0 = target.getX() + tm.x - getX();
            double d2 = target.getY() - getY() - target.getBbHeight() / 2.0f;
            double d3 = target.getZ() + tm.z - getZ();
            blast.shoot(d0, d2, d3, 0.66f, 5.0f);
            playSound(ModSounds.get("egattack"), 1.0f, 1.0f + random.nextFloat() * 0.1f);
            sl.addFreshEntity(blast);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            attackTimer = 10;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 1.0f);
        } else if (id == 18) spawnTimer = 150;
        else if (id == 19) {
            if (arcing == 0) {
                float radius = 2.0f + random.nextFloat() * 2.0f;
                double radians = Math.toRadians(random.nextInt(360));
                int bx = Mth.floor(getX() + radius * Math.cos(radians));
                int by = Mth.floor(getY());
                int bz = Mth.floor(getZ() + radius * Math.sin(radians));
                BlockPos bp = new BlockPos(bx, by, bz);
                for (int c = 0; c < 5 && level().isEmptyBlock(bp); ++c, --by) {} // (sic: bp itself is never moved)
                if (level().isEmptyBlock(bp.above()) && !level().isEmptyBlock(bp)) {
                    ax = bx; ay = by; az = bz;
                    arcing = 8 + random.nextInt(5);
                    level().playLocalSound(getX(), getY(), getZ(), ModSounds.get("jacobs"), getSoundSource(), 0.8f, 1.0f + (random.nextFloat() - random.nextFloat()) * 0.05f, false);
                }
            }
        } else super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        if (getSpawnTimer() == 150) level().broadcastEntityEvent(this, (byte) 18);
        if (getSpawnTimer() > 0) heal(2.0f);
        super.tick();
        if (level().isClientSide()) {
            if (isHeadless()) {
                setXRot(0.0f);
                float f1 = Mth.cos(-yBodyRot * 0.017453292f - 3.1415927f);
                float f2 = Mth.sin(-yBodyRot * 0.017453292f - 3.1415927f);
                float f3 = -Mth.cos(-getXRot() * 0.017453292f);
                float f4 = Mth.sin(-getXRot() * 0.017453292f);
                Vec3 v = new Vec3(f2 * f3, f4, f1 * f3);
                if (random.nextInt(20) == 0) {
                    float a = (random.nextFloat() - random.nextFloat()) / 3.0f, b = (random.nextFloat() - random.nextFloat()) / 3.0f;
                    art.arcane.thaumcraft.client.fx.InfusionFx.spark((float) (getX() + v.x + a), (float) getY() + eyeHeight() - 0.75f, (float) (getZ() + v.z + b), 3.0f, 0.65f + random.nextFloat() * 0.1f, 1.0f, 1.0f, 0.8f);
                }
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawVentParticles((float) getX() + v.x * 0.4, (float) getY() + eyeHeight() - 1.25f, (float) getZ() + v.z * 0.4, 0.0, 0.001, 0.0, 5592405, 4.0f);
                if (arcing > 0) {
                    art.arcane.thaumcraft.client.fx.ClientHooks.arcLightning(getX(), getY() + getBbHeight() / 2.0f, getZ(), ax + 0.5, ay + 1, az + 0.5, 0.65f + random.nextFloat() * 0.1f, 1.0f, 1.0f, 1.0f - arcing / 10.0f);
                    --arcing;
                }
            }
        } else {
            if (isHeadless() && beamCharge <= 0) chargingBeam = true;
            if (isHeadless() && chargingBeam) {
                ++beamCharge;
                level().broadcastEntityEvent(this, (byte) 19);
                if (beamCharge == 150) chargingBeam = false;
            }
        }
    }

    public int getAttackTimer() { return attackTimer; }
}
