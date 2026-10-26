package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.block.world.taint.TaintHelper;
import art.arcane.thaumcraft.entity.ITaintedMob;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityTaintSwarm: an invisible cloud of biting insects (drawn only as FXSwarm particles) that roams around taint
 * seeds and swoops at players within 8 blocks, inflicting weakness.
 */
public class TaintSwarmEntity extends Monster implements ITaintedMob {
    private static final EntityDataAccessor<Boolean> SUMMONED = SynchedEntityData.defineId(TaintSwarmEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    public int damBonus;
    /** Client: the live FXSwarm particles of this swarm (kept topped up to 30). */
    public final List<Object> swarm = new ArrayList<>();
    private int attackTime;

    public TaintSwarmEntity(EntityType<? extends TaintSwarmEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(SUMMONED, false); }
    public boolean getIsSummoned() { return entityData.get(SUMMONED); }
    public void setIsSummoned(boolean v) { entityData.set(SUMMONED, v); }

    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof ITaintedMob) && super.canAttack(target); }
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof ITaintedMob || super.considersEntityAsAlly(other); }

    @Override protected float getSoundVolume() { return 0.1f; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("swarm"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return ModSounds.get("swarmattack"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("swarmattack"); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return true; }
    @Override protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.NONE; }
    @Override public boolean causeFallDamage(double d, float f, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState s, BlockPos p) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(getDeltaMovement().multiply(1, 0.6000000238418579, 1));
        if (level().isClientSide()) art.arcane.thaumcraft.client.fx.ClientHooks.swarmTick(this);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (attackTime > 0) --attackTime;
        LivingEntity target = getTarget();
        Vec3 m = getDeltaMovement();
        double mx = m.x, my = m.y, mz = m.z;
        if (target == null) {
            if (getIsSummoned()) hurtServer(level, damageSources().generic(), 5.0f);
            if (currentFlightTarget != null && (!level.isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level.getMinY() + 1
                    || currentFlightTarget.getY() > level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, currentFlightTarget).above(2).getY()
                    || !TaintHelper.isNearTaintSeed(level, currentFlightTarget)))
                currentFlightTarget = null;
            if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.distToCenterSqr(position()) < 4.0)
                currentFlightTarget = new BlockPos((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2, (int) getZ() + random.nextInt(7) - random.nextInt(7));
            double var1 = currentFlightTarget.getX() + 0.5 - getX(), var2 = currentFlightTarget.getY() + 0.1 - getY(), var3 = currentFlightTarget.getZ() + 0.5 - getZ();
            mx += (Math.signum(var1) * 0.5 - mx) * 0.015000000014901161;
            my += (Math.signum(var2) * 0.699999988079071 - my) * 0.10000000149011612;
            mz += (Math.signum(var3) * 0.5 - mz) * 0.015000000014901161;
        } else {
            double var1 = target.getX() - getX(), var2 = target.getY() + target.getEyeHeight() - getY(), var3 = target.getZ() - getZ();
            mx += (Math.signum(var1) * 0.5 - mx) * 0.025000000149011613;
            my += (Math.signum(var2) * 0.699999988079071 - my) * 0.10000000149011612;
            mz += (Math.signum(var3) * 0.5 - mz) * 0.02500000001490116;
        }
        setDeltaMovement(mx, my, mz);
        float var4 = (float) (Math.atan2(mz, mx) * 180.0 / Math.PI) - 90.0f;
        zza = 0.1f;
        setYRot(getYRot() + Mth.wrapDegrees(var4 - getYRot()));
        target = getTarget();
        if (target == null) setTarget(findPlayerToAttack(level));
        else if (isAlive() && target.isAlive()) {
            float f = target.distanceTo(this);
            if (hasLineOfSight(target)) attackEntity(level, target, f);
        } else setTarget(null);
        if (getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
    }

    protected void attackEntity(ServerLevel level, Entity e, float dist) {
        if (attackTime <= 0 && dist < 3.0f && e.getBoundingBox().maxY > getBoundingBox().minY && e.getBoundingBox().minY < getBoundingBox().maxY) {
            attackTime = 15 + random.nextInt(10);
            Vec3 motion = e.getDeltaMovement();
            if (doHurtTarget(level, e) && e instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            // the attack does not knock the victim about
            e.setDeltaMovement(motion);
            e.hurtMarked = false;
            playSound(ModSounds.get("swarmattack"), 0.3f, 0.9f + random.nextFloat() * 0.2f);
        }
    }

    protected Player findPlayerToAttack(ServerLevel level) { return getIsSummoned() ? null : level.getNearestPlayer(this, 8.0); }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        if (level.getRandom().nextBoolean()) spawnAtLocation(level, CrystalEssenceItem.of(AspectRegistry.VITIUM.id(), 1), getBbHeight() / 2.0f);
    }

    /** getCanSpawnHere: raw light <= rand(7) and the usual mob placement (any light level). */
    public static boolean checkSwarmSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getRawBrightness(pos, 0) <= random.nextInt(7) && Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("summoned", getIsSummoned());
        out.putByte("damBonus", (byte) damBonus);
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setIsSummoned(in.getBooleanOr("summoned", false));
        damBonus = in.getByteOr("damBonus", (byte) 0);
    }
}
