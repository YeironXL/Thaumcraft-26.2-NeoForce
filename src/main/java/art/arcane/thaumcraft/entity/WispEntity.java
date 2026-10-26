package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityWisp: a floating aspect wisp that drifts about, zaps players in sight and drops a crystal of its aspect. */
public class WispEntity extends Mob implements Enemy {
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(WispEntity.class, EntityDataSerializers.STRING);
    private int aggroCooldown;
    public int prevAttackCounter;
    public int attackCounter;
    private BlockPos currentFlightTarget;

    public WispEntity(EntityType<? extends WispEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        // EntityFlying steers itself: the wisp sets its own motion and moveForward, nothing resets them
        moveControl = new MoveControl(this) { @Override public void tick() {} };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(TYPE, ""); }
    public String getWispType() { return entityData.get(TYPE); }
    public void setWispType(String t) { entityData.set(TYPE, String.valueOf(t)); }
    public Aspect getAspect() { return AspectRegistry.get(getWispType()); }
    public int getAspectColor() { return getAspect() == null ? 0 : AspectRegistry.color(getWispType()); }

    // EntityFlying: no fall damage, no ladders, free flight with 0.91 drag
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState s, BlockPos p) {}
    @Override public boolean causeFallDamage(double d, float f, DamageSource s) { return false; }
    @Override public boolean onClimbable() { return false; }
    @Override public void travel(Vec3 input) { travelFlying(input, 0.02f); }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity le) { setTarget(le); aggroCooldown = 200; }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public void handleEntityEvent(byte id) {
        // the server's death notice (3): the client side of onDeath bursts the wisp
        if (id == 3) art.arcane.thaumcraft.client.fx.FXDispatcher.burst(getX(), getY() + 0.45, getZ(), 1.0f);
        super.handleEntityEvent(id);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount <= 1) art.arcane.thaumcraft.client.fx.FXDispatcher.burst(getX(), getY(), getZ(), 10.0f);
            var r = level().getRandom();
            if (r.nextBoolean() && getAspect() != null)
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawWispParticles(getX() + (r.nextFloat() - r.nextFloat()) * 0.7f, getY() + (r.nextFloat() - r.nextFloat()) * 0.7f,
                        getZ() + (r.nextFloat() - r.nextFloat()) * 0.7f, 0, 0, 0, getAspectColor(), 0);
        }
        setDeltaMovement(getDeltaMovement().multiply(1, 0.6000000238418579, 1));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        var rand = random;
        if (getAspect() == null) {
            var all = AspectRegistry.all().values();
            ArrayList<Aspect> as = new ArrayList<>();
            boolean primal = level.getRandom().nextInt(10) != 0;
            for (Aspect a : all) if (a.isPrimal() == primal) as.add(a);
            setWispType(as.get(level.getRandom().nextInt(as.size())).id());
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) discard();
        prevAttackCounter = attackCounter;
        double attackrange = 16.0;
        LivingEntity target = getTarget();
        Vec3 m = getDeltaMovement();
        if (target == null || !hasLineOfSight(target)) {
            if (currentFlightTarget != null && (!level.isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level.getMinY() + 1
                    || currentFlightTarget.getY() > level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, currentFlightTarget).above(8).getY()))
                currentFlightTarget = null;
            if (currentFlightTarget == null || rand.nextInt(30) == 0 || currentFlightTarget.distToCenterSqr(position()) < 4.0)
                currentFlightTarget = new BlockPos((int) getX() + rand.nextInt(7) - rand.nextInt(7), (int) getY() + rand.nextInt(6) - 2, (int) getZ() + rand.nextInt(7) - rand.nextInt(7));
            steer(m, currentFlightTarget.getX() + 0.5 - getX(), currentFlightTarget.getY() + 0.1 - getY(), currentFlightTarget.getZ() + 0.5 - getZ(), 0.15f);
        } else if (distanceToSqr(target) > attackrange * attackrange / 2.0 && hasLineOfSight(target)) {
            steer(m, target.getX() - getX(), target.getY() + target.getEyeHeight() * 0.66f - getY(), target.getZ() - getZ(), 0.5f);
        }
        if (getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
        if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
        --aggroCooldown;
        if (level.getRandom().nextInt(1000) == 0 && (getTarget() == null || aggroCooldown-- <= 0)) {
            setTarget(level.getNearestPlayer(this, 16.0));
            if (getTarget() != null) aggroCooldown = 50;
        }
        target = getTarget();
        if (isAlive() && target != null && target.distanceToSqr(this) < attackrange * attackrange) {
            double d5 = target.getX() - getX(), d7 = target.getZ() - getZ();
            float n = -(float) Math.atan2(d5, d7) * 180.0f / 3.141593f;
            setYRot(n);
            yBodyRot = n;
            if (hasLineOfSight(target)) {
                ++attackCounter;
                if (attackCounter == 20) zap(level, target);
            } else if (attackCounter > 0) --attackCounter;
        }
    }

    private void steer(Vec3 m, double var1, double var2, double var3, float forward) {
        double mx = m.x + (Math.signum(var1) * 0.5 - m.x) * 0.10000000149011612;
        double my = m.y + (Math.signum(var2) * 0.699999988079071 - m.y) * 0.10000000149011612;
        double mz = m.z + (Math.signum(var3) * 0.5 - m.z) * 0.10000000149011612;
        setDeltaMovement(mx, my, mz);
        float var4 = (float) (Math.atan2(mz, mx) * 180.0 / Math.PI) - 90.0f;
        float var5 = Mth.wrapDegrees(var4 - getYRot());
        zza = forward;
        setYRot(getYRot() + var5);
    }

    /** The zap: PacketFXWispZap (an arc in the aspect colour) and a 40% / 66% chance to hurt, more against a still target. */
    public void zap(ServerLevel level, LivingEntity target) {
        playSound(ModSounds.get("zap"), 1.0f, 1.1f);
        int c = getAspect() == null ? 0xFFFFFF : getAspectColor();
        Fx.send(level, "zap", getX(), getY(), getZ(), (float) target.getX(), (float) target.getY(), (float) target.getZ(), c, 0.6f);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        Vec3 tm = target.getDeltaMovement();
        if (Math.abs(tm.x) > 0.10000000149011612 || Math.abs(tm.y) > 0.10000000149011612 || Math.abs(tm.z) > 0.10000000149011612) {
            if (level.getRandom().nextFloat() < 0.4f) target.hurtServer(level, damageSources().mobAttack(this), damage);
        } else if (level.getRandom().nextFloat() < 0.66f) target.hurtServer(level, damageSources().mobAttack(this), damage + 1.0f);
        attackCounter = -20 + level.getRandom().nextInt(20);
    }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("wisplive"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.LAVA_EXTINGUISH; }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("wispdead"); }
    @Override protected float getSoundVolume() { return 0.25f; }

    /** dropFewItems: one crystal of the wisp's aspect. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (getAspect() != null) spawnAtLocation(level, CrystalEssenceItem.of(getWispType(), 1), 0.0f);
    }

    @Override public int getMaxSpawnClusterSize() { return 2; }

    /** getCanSpawnHere: fewer than 8 wisps within 16, not peaceful, dark enough (isValidLightLevel). */
    public boolean getCanSpawnHere() {
        Level l = level();
        int count = l.getEntitiesOfClass(WispEntity.class, getBoundingBox().inflate(16.0)).size();
        return count < 8 && l.getDifficulty() != Difficulty.PEACEFUL && l instanceof ServerLevel sl && isValidLightLevel(sl, BlockPos.containing(getX(), getBoundingBox().minY, getZ()), random);
    }

    public static boolean isValidLightLevel(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) return false;
        int i = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
        return i <= random.nextInt(8);
    }

    public static boolean checkWispSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        var box = type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5).inflate(16.0);
        if (level.getEntitiesOfClass(WispEntity.class, box).size() >= 8) return false;
        return (EntitySpawnReason.ignoresLightRequirements(reason) || isValidLightLevel(level, pos, random)) && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putString("Type", getWispType()); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setWispType(in.getStringOr("Type", "")); }
}
