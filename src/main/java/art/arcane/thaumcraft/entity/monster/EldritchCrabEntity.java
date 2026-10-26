package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityEldritchCrab: a leaping crab. Bare crabs latch onto (ride) targets they land on and gnaw at them; crabs
 * wearing a crimson helm are slower and armoured until they drop below half health and lose it.
 */
public class EldritchCrabEntity extends Monster implements EldritchMob {
    private static final EntityDataAccessor<Boolean> HELM = SynchedEntityData.defineId(EldritchCrabEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RIDING = SynchedEntityData.defineId(EldritchCrabEntity.class, EntityDataSerializers.INT);
    private int attackTime;

    public EldritchCrabEntity(EntityType<? extends EldritchCrabEntity> type, Level level) {
        super(type, level);
        xpReward = 6;
        if (getNavigation() instanceof GroundPathNavigation g) g.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CultistEntity.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(HELM, false);
        b.define(RIDING, -1);
    }

    public int getRiding() { return entityData.get(RIDING); }
    public void setRiding(int s) { entityData.set(RIDING, s); }
    public boolean hasHelm() { return entityData.get(HELM); }
    public void setHelm(boolean v) {
        entityData.set(HELM, v);
        // applyEntityAttributes / readEntityFromNBT: helmeted crabs move at 0.275, bare ones at 0.3; getTotalArmorValue 5 / 0
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(v ? 0.275 : 0.3);
        getAttribute(Attributes.ARMOR).setBaseValue(v ? 5.0 : 0.0);
    }

    /** getYOffset: half a block higher while riding. */
    @Override public Vec3 getVehicleAttachmentPoint(Entity vehicle) { return super.getVehicleAttachmentPoint(vehicle).add(0.0, -0.5, 0.0); }

    @Override public boolean canPickUpLoot() { return false; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, SpawnGroupData data) {
        if (level.getDifficulty() == Difficulty.HARD) setHelm(true);
        else setHelm(random.nextFloat() < 0.33f);
        if (data == null) {
            data = new Spider.SpiderEffectsGroupData();
            if (level.getDifficulty() == Difficulty.HARD && level.getRandom().nextFloat() < 0.1f * diff.getSpecialMultiplier())
                ((Spider.SpiderEffectsGroupData) data).setRandomEffect(level.getRandom());
        }
        if (data instanceof Spider.SpiderEffectsGroupData sd) {
            Holder<MobEffect> effect = sd.effect;
            if (effect != null) addEffect(new MobEffectInstance(effect, -1));
        }
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        --attackTime;
        if (tickCount < 20) fallDistance = 0.0;
        if (level() instanceof ServerLevel sl) {
            LivingEntity t = getTarget();
            if (getVehicle() == null && t != null && !t.isVehicle() && !onGround() && !hasHelm() && t.isAlive()
                    && getY() - t.getY() >= t.getBbHeight() / 2.0f && distanceToSqr(t) < 4.0) {
                startRiding(t, true, true);
                setRiding(t.getId());
            }
            if (getVehicle() != null && isAlive() && attackTime <= 0) {
                attackTime = 10 + random.nextInt(10);
                doHurtTarget(sl, getVehicle());
                if (random.nextFloat() < 0.2) { stopRiding(); setRiding(-1); }
            }
            if (getVehicle() == null && getRiding() != -1) setRiding(-1);
        } else if (getVehicle() == null && getRiding() != -1) {
            Entity e = level().getEntity(getRiding());
            if (e != null) startRiding(e, true, false);
        } else if (getVehicle() != null && getRiding() == -1) stopRiding();
    }

    /** dropFewItems: when killed by a player, an ender pearl a third of the time (or more with looting). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        int looting = art.arcane.thaumcraft.util.EntityUtils.lootingLevel(level, source);
        if (killedByPlayer && (random.nextInt(3) == 0 || random.nextInt(1 + looting) > 0)) spawnAtLocation(level, new ItemStack(Items.ENDER_PEARL));
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!super.doHurtTarget(level, target)) return false;
        playSound(ModSounds.get("crabclaw"), 1.0f, 0.9f + random.nextFloat() * 0.2f);
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean b = super.hurtServer(level, source, damage);
        if (hasHelm() && getHealth() / getMaxHealth() <= 0.5f) {
            setHelm(false);
            // renderBrokenItemStack(crimson plate chest): item-break particles and sound
            level.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, ModItems.CRIMSON_PLATE_CHEST.get()),
                    getX(), getY() + getEyeHeight(), getZ(), 5, 0.1, 0.1, 0.1, 0.05);
            playSound(SoundEvents.ITEM_BREAK.value(), 0.8f, 0.8f + random.nextFloat() * 0.4f);
        }
        return b;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putBoolean("helm", hasHelm()); }
    @Override
    protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setHelm(in.getBooleanOr("helm", false)); }

    @Override public int getAmbientSoundInterval() { return 160; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("crabtalk"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.GENERIC_HURT; }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("crabdeath"); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) { playSound(SoundEvents.SPIDER_STEP, 0.15f, 1.0f); }

    @Override public boolean canBeAffected(MobEffectInstance e) { return !e.is(MobEffects.POISON) && super.canBeAffected(e); }
    /** isOnSameTeam: only other crabs. */
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof EldritchCrabEntity || super.considersEntityAsAlly(other); }
}
