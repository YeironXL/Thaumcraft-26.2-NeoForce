package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.world.taint.TaintHelper;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityTaintSeed: a rooted taint pod. It registers itself with TaintHelper (so nearby taint blocks live on),
 * spreads fibres around itself every second while the aura holds flux (or while boosted by a rift), starves without
 * flux and gives flux taint to creatures nearby.
 */
public class TaintSeedEntity extends Monster implements ITaintedMob {
    public int boost;
    boolean firstRun;
    public float attackAnim;

    public TaintSeedEntity(EntityType<? extends TaintSeedEntity> type, Level level) { super(type, level); xpReward = 8; }

    protected int getArea() { return 1; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        targetSelector.addGoal(0, new HurtByTargetGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); boost = in.getIntOr("boost", 0); }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("boost", boost); }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, (byte) 16);
        playSound(ModSounds.get("tentacle"), getSoundVolume(), getVoicePitch());
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) attackAnim = 0.5f;
        else super.handleEntityEvent(id);
    }

    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof ITaintedMob) && super.canAttack(target); }
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof ITaintedMob || super.considersEntityAsAlly(other); }

    /** getCanSpawnHere: not peaceful, no liquid or entity in the way, no other seed within 80% of the spread area. */
    public boolean getCanSpawnHere() {
        Level l = level();
        return l.getDifficulty() != Difficulty.PEACEFUL && !l.containsAnyLiquid(getBoundingBox()) && l.isUnobstructed(this)
                && TaintHelper.inRange(l, blockPosition(), TaintSeedEntity.class, TaintHelper.TAINT_SPREAD_AREA * 0.8, null).isEmpty();
    }

    /** discard()/unload bypass die(): drop the registration too (it is renewed on the next first tick after a reload). */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!level().isClientSide()) TaintHelper.removeTaintSeed(level(), blockPosition());
        super.remove(reason);
    }

    @Override
    public void die(DamageSource cause) {
        TaintHelper.removeTaintSeed(level(), blockPosition());
        super.die(cause);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl) {
            if (!firstRun || tickCount % 1200 == 0) {
                TaintHelper.removeTaintSeed(sl, blockPosition());
                TaintHelper.addTaintSeed(sl, blockPosition());
                firstRun = true;
            }
            if (isAlive()) {
                boolean tickFlag = tickCount % 20 == 0;
                if (boost > 0 || tickFlag) {
                    float mod = boost > 0 ? 1.0f : NativeAura.fluxSaturation(sl, blockPosition());
                    if (boost > 0) --boost;
                    if (mod <= 0.0f) {
                        hurtServer(sl, damageSources().starve(), 0.5f);
                        NativeAura.addFlux(sl, blockPosition(), 0.1f);
                    } else {
                        int a = getArea();
                        TaintHelper.spreadFibres(sl, blockPosition().offset(Mth.randomBetweenInclusive(random, -a * 3, a * 3), Mth.randomBetweenInclusive(random, -a, a),
                                Mth.randomBetweenInclusive(random, -a * 3, a * 3)), true);
                    }
                }
                if (tickFlag) {
                    // spawnTentacles (small taintacles at a target standing on taint) waits for the taintacles
                    for (LivingEntity elb : TaintHelper.inRange(sl, blockPosition(), LivingEntity.class, getArea() * 4, this))
                        elb.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 100, getArea() - 1, false, true));
                }
            }
        } else {
            if (attackAnim > 0.0f) attackAnim *= 0.75f;
            if (attackAnim < 0.001) attackAnim = 0.0f;
            float xx = 1.0f * Mth.sin(tickCount * 0.05f - 0.5f) / 5.0f;
            float zz = 1.0f * Mth.sin(tickCount * 0.06f - 0.5f) / 5.0f + hurtTime / 200.0f + attackAnim;
            float h = getBbHeight();
            if (random.nextFloat() < 0.033)
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawLightningFlash((float) getX() + xx, (float) getY() + h + 0.25f, (float) getZ() + zz, 0.7f, 0.1f, 0.9f, 0.5f, 1.5f + random.nextFloat());
            else
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawTaintParticles((float) getX() + xx, (float) getY() + h + 0.25f, (float) getZ() + zz,
                        (float) random.nextGaussian() * 0.05f, 0.1f + 0.01f * random.nextFloat(), (float) random.nextGaussian() * 0.05f, 2.0f);
        }
    }

    @Override public int getAmbientSoundInterval() { return 200; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CHORUS_FLOWER_DEATH; }
    @Override public float getVoicePitch() { return 1.3f - getBbHeight() / 10.0f; }
    @Override protected float getSoundVolume() { return getBbHeight() / 8.0f; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.get("tentacle"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("tentacle"); }

    /** dropFewItems: one flux crystal. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        spawnAtLocation(level, CrystalEssenceItem.of("vitium", 1), getBbHeight() / 2.0f);
    }

    @Override public boolean isPushable() { return false; }
    @Override public void moveRelative(float speed, Vec3 input) {}

    /** Rooted: no sideways movement and never rising, only settling down. */
    @Override
    public void move(MoverType type, Vec3 d) { super.move(type, new Vec3(0.0, Math.min(d.y, 0.0), 0.0)); }

    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
}
