package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity;
import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** TC6 EntityEldritchGuardian: a ghostly eldritch warrior that hurls entropic orbs or screeches (wither + warp). */
public class EldritchGuardianEntity extends Monster implements RangedAttackMob, EldritchMob {
    public float armLiftL;
    public float armLiftR;
    boolean lastBlast;

    public EldritchGuardianEntity(EntityType<? extends EldritchGuardianEntity> type, Level level) {
        super(type, level);
        xpReward = 20;
        getNavigation().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 8.0, 1.0, 20, 40, 24.0f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CultistEntity.class, true));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypeTags.WITCH_RESISTANT_TO)) damage /= 2.0f;
        return super.hurtServer(level, source, damage);
    }

    /** TC6 onInitialSpawn bonus shield in the Outer Lands (applied once). */
    private boolean outerShield;

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (armLiftL > 0.0f) armLiftL -= 0.05f;
            if (armLiftR > 0.0f) armLiftR -= 0.05f;
            float x = (float) (getX() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            float z = (float) (getZ() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            art.arcane.thaumcraft.client.fx.ClientHooks.wispFXEG(x, getY() + 0.22 * getBbHeight(), z, this);
        } else if (art.arcane.thaumcraft.world.outer.OuterLands.is(level())) {
            // TC6 updateAITasks: in the Outer Lands the runic shield regrows to half max health
            int bh = (int) getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) / 2;
            if (!outerShield) { outerShield = true; var ma = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION); if (ma != null && ma.getBaseValue() < bh) ma.setBaseValue(bh); setAbsorptionAmount(getAbsorptionAmount() + bh); }
            if (invulnerableTime <= 0 && tickCount % 25 == 0 && getAbsorptionAmount() < bh) setAbsorptionAmount(getAbsorptionAmount() + 1.0f);
        } else if ((tickCount == 0 || tickCount % 100 == 0) && level().getDifficulty() != Difficulty.EASY) {
            double d6 = level().getDifficulty() == Difficulty.HARD ? 576.0 : 256.0;
            for (Player p : level().players()) {
                if (p.isAlive() && p instanceof ServerPlayer sp && sp.connection != null && p.distanceToSqr(getX(), getY(), getZ()) < d6)
                    PacketDistributor.sendToPlayer(sp, new FxPayload("misc_event", sp.getX(), sp.getY(), sp.getZ(), List.of(2f)));
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean flag = super.doHurtTarget(level, target);
        if (flag) {
            int i = level.getDifficulty().getId();
            if (getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < i * 0.3f) target.igniteForSeconds(2 * i);
        }
        return flag;
    }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("egidle"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("egdeath"); }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override protected float getSoundVolume() { return 1.5f; }
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distSqr) { return !hasHome(); }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 15) armLiftL = 0.5f;
        else if (id == 16) armLiftR = 0.5f;
        else if (id == 17) { armLiftL = 0.9f; armLiftR = 0.9f; }
        else super.handleEntityEvent(id);
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        List<EldritchGuardianEntity> ents = level.getEntitiesOfClass(EldritchGuardianEntity.class,
                new AABB(getX(), getY(), getZ(), getX() + 1.0, getY() + 1.0, getZ() + 1.0).inflate(32.0, 16.0, 32.0));
        return ents.isEmpty() && super.checkSpawnRules(level, reason);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (random.nextFloat() > 0.15f) {
            EldritchOrbEntity blast = new EldritchOrbEntity(sl, this);
            lastBlast = !lastBlast;
            sl.broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
            int rr = lastBlast ? 90 : 180;
            double xx = Mth.cos((getYRot() + rr) % 360.0f / 180.0f * 3.1415927f) * 0.5f;
            double zz = Mth.sin((getYRot() + rr) % 360.0f / 180.0f * 3.1415927f) * 0.5f;
            blast.setPos(blast.getX() - xx, blast.getY(), blast.getZ() - zz);
            Vec3 tm = target.getDeltaMovement();
            Vec3 v = target.position().add(tm.x * 10.0, tm.y * 10.0, tm.z * 10.0).subtract(position()).normalize();
            blast.shoot(v.x, v.y, v.z, 1.1f, 2.0f);
            playSound(ModSounds.get("egattack"), 2.0f, 1.0f + random.nextFloat() * 0.1f);
            sl.addFreshEntity(blast);
        } else if (hasLineOfSight(target)) {
            PacketDistributor.sendToPlayersNear(sl, null, getX(), getY(), getZ(), 32.0,
                    new FxPayload("sonic", getX(), getY(), getZ(), List.of((float) getId())));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            if (target instanceof ServerPlayer p)
                PlayerProgressManager.addWarp(p, PlayerProgressManager.WarpType.TEMPORARY, 1 + sl.getRandom().nextInt(3));
            playSound(ModSounds.get("egscreech"), 3.0f, 1.0f + random.nextFloat() * 0.1f);
        }
    }

    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof EldritchMob || super.considersEntityAsAlly(other); }
}
