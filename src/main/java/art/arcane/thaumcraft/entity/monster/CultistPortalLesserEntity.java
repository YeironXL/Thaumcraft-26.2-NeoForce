package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityCultistPortalLesser: a stationary crimson portal that wakes near players and summons cultists. */
public class CultistPortalLesserEntity extends Monster {
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(CultistPortalLesserEntity.class, EntityDataSerializers.BOOLEAN);
    int stagecounter = 100;
    public int activeCounter;
    public int pulse;

    public CultistPortalLesserEntity(EntityType<? extends CultistPortalLesserEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 4.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(ACTIVE, false); }
    public boolean isActive() { return entityData.get(ACTIVE); }
    public void setActive(boolean active) { entityData.set(ACTIVE, active); }

    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putBoolean("active", isActive()); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setActive(in.getBooleanOr("active", false)); }
    @Override public boolean isPushable() { return false; }
    @Override public void move(MoverType type, Vec3 delta) {}
    @Override public void aiStep() {}
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 4096.0; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }

    @Override
    public void tick() {
        super.tick();
        if (isActive()) ++activeCounter;
        if (level() instanceof ServerLevel sl) {
            if (!isActive()) {
                if (tickCount % 10 == 0 && sl.getNearestPlayer(this, 32.0) != null) {
                    setActive(true);
                    playSound(ModSounds.get("craftstart"), 1.0f, 1.0f);
                }
            } else if (stagecounter-- <= 0) {
                Player p = sl.getNearestPlayer(this, 32.0);
                if (p != null && hasLineOfSight(p)) {
                    int count = sl.getDifficulty() == Difficulty.HARD ? 6 : sl.getDifficulty() == Difficulty.NORMAL ? 4 : 2;
                    count -= sl.getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(32.0, 32.0, 32.0)).size();
                    if (count > 0) {
                        sl.broadcastEntityEvent(this, (byte) 16);
                        spawnMinions(sl);
                    }
                }
                stagecounter = 50 + random.nextInt(50);
            }
        }
        if (pulse > 0) --pulse;
    }

    void spawnMinions(ServerLevel sl) {
        CultistEntity cultist = random.nextFloat() > 0.33
                ? ModEntities.CULTIST_KNIGHT.get().create(sl, EntitySpawnReason.MOB_SUMMONED)
                : ModEntities.CULTIST_CLERIC.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (cultist == null) return;
        cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
        cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        sl.addFreshEntity(cultist);
        cultist.spawnExplosionParticle();
        cultist.playSound(ModSounds.get("wandfail"), 1.0f, 1.0f);
        hurtServer(sl, sl.damageSources().fellOutOfWorld(), (float) (5 + random.nextInt(5)));
    }

    /** onCollideWithPlayer: zaps players that touch it. */
    @Override
    public void playerTouch(Player p) {
        if (level() instanceof ServerLevel sl && distanceToSqr(p) < 3.0 && p.hurtServer(sl, sl.damageSources().indirectMagic(this, this), 4.0f))
            playSound(ModSounds.get("zap"), 1.0f, (random.nextFloat() - random.nextFloat()) * 0.1f + 1.0f);
    }

    @Override protected float getSoundVolume() { return 0.75f; }
    @Override public int getAmbientSoundInterval() { return 540; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("monolith"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.get("zap"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("shock"); }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) pulse = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) sl.explode(this, getX(), getY(), getZ(), 1.5f, false, Level.ExplosionInteraction.NONE);
        super.die(source);
    }

}
