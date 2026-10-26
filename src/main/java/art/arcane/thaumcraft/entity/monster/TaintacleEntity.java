package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.block.world.taint.ITaintBlock;
import art.arcane.thaumcraft.entity.ITaintedMob;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityTaintacle: a rooted 3-block tentacle that only lives on taint (it starves elsewhere), lashes at players in reach
 * and, when a seen target is 4..16 blocks away, sprouts small taint tendrils under it if it stands on taint.
 */
public class TaintacleEntity extends Monster implements ITaintedMob {
    public float flailIntensity = 1.0f;

    public TaintacleEntity(EntityType<? extends TaintacleEntity> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof ITaintedMob) && super.canAttack(target); }
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof ITaintedMob || super.considersEntityAsAlly(other); }

    public static boolean onTaint(LevelAccessor level, BlockPos pos) {
        return ITaintBlock.isTaint(level.getBlockState(pos)) || ITaintBlock.isTaint(level.getBlockState(pos.below()));
    }

    /** getCanSpawnHere: on (or in) a taint block, never in peaceful. */
    public static boolean checkTaintacleSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return onTaint(level, pos) && level.getDifficulty() != Difficulty.PEACEFUL;
    }

    /** move(): rooted, it can only drop. */
    @Override
    public void move(MoverType type, Vec3 delta) {
        super.move(type, new Vec3(0.0, Math.min(0.0, delta.y), 0.0));
    }

    @Override public boolean isPushable() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && tickCount % 20 == 0) {
            if (!onTaint(sl, blockPosition())) hurtServer(sl, damageSources().starve(), 1.0f);
            LivingEntity t = getTarget();
            if (!(this instanceof TaintacleSmallEntity) && tickCount % 40 == 0 && t != null && distanceToSqr(t) > 16.0 && distanceToSqr(t) < 256.0 && getSensing().hasLineOfSight(t))
                spawnTentacles(sl, t);
        }
        if (level().isClientSide()) {
            if (flailIntensity > 1.0f) flailIntensity -= 0.01f;
            if (tickCount < getBbHeight() * 10.0f && onGround()) art.arcane.thaumcraft.client.fx.ClientHooks.tentacleAriseFX(this);
        }
    }

    /** spawnTentacles: a taint tendril bursts up under a target standing on taint (the Outer Lands branch is not ported). */
    protected void spawnTentacles(ServerLevel level, Entity entity) {
        if (!onTaint(level, entity.blockPosition())) return;
        TaintacleSmallEntity taintlet = ModEntities.TAINTACLE_SMALL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (taintlet == null) return;
        taintlet.snapTo(entity.getX() + random.nextFloat() - random.nextFloat(), entity.getY(), entity.getZ() + random.nextFloat() - random.nextFloat(), 0.0f, 0.0f);
        level.addFreshEntity(taintlet);
        playSound(ModSounds.get("tentacle"), getSoundVolume(), getVoicePitch());
    }

    /** faceEntity: yaw only. */
    @Override
    public void lookAt(Entity e, float maxYaw, float maxPitch) {
        double d0 = e.getX() - getX(), d2 = e.getZ() - getZ();
        float f2 = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0f;
        float f3 = Mth.wrapDegrees(f2 - getYRot());
        setYRot(getYRot() + Mth.clamp(f3, -maxYaw, maxYaw));
    }

    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CHORUS_FLOWER_DEATH; }
    @Override public float getVoicePitch() { return 1.3f - getBbHeight() / 10.0f; }
    @Override protected float getSoundVolume() { return getBbHeight() / 8.0f; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.get("tentacle"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("tentacle"); }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        spawnAtLocation(level, CrystalEssenceItem.of(AspectRegistry.VITIUM.id(), 1), getBbHeight() / 2.0f);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) flailIntensity = 3.0f;
        else super.handleEntityEvent(id);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, (byte) 16);
        playSound(ModSounds.get("tentacle"), getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(level, target);
    }

    @Override public boolean removeWhenFarAway(double d) { return true; }
}
