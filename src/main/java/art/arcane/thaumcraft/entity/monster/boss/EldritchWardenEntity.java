package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.CultistEntity;
import art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity;
import art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity;
import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityEldritchWarden: a giant eldritch guardian (400 hp, a regrowing 264-point ward shown on a second blue
 * notched bar). It rises out of the ground for 150 ticks, leaves wither sap where it walks, fires eldritch orbs from
 * alternating hands or screeches (knockback, wither, weakness, warp), and when its ward first breaks it teleports home and
 * unleashes a field frenzy: invulnerable rings of sap spreading outwards with lightning arcs.
 */
public class EldritchWardenEntity extends ThaumcraftBossEntity implements RangedAttackMob, EldritchMob {
    private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(EldritchWardenEntity.class, EntityDataSerializers.BYTE);
    public static final String[] TITLES = {"Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha", "Oorn", "Shaikorth", "Rhan-Tegoth", "Rhogog", "Shudde M'ell", "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon"};
    protected final ServerBossEvent bossInfo2 = new ServerBossEvent(java.util.UUID.randomUUID(), Component.literal(""), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    boolean fieldFrenzy = false;
    public int fieldFrenzyCounter = 0;
    boolean lastBlast = false;
    public float armLiftL = 0.0f, armLiftR = 0.0f;

    public EldritchWardenEntity(EntityType<? extends EldritchWardenEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ARMOR, 4.0).add(Attributes.MAX_ABSORPTION, 400.0 * 0.66);
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossInfo2.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossInfo2.removePlayer(player); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 3.0, 1.0, 20, 40, 24.0f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CultistEntity.class, true));
    }

    @Override
    public void generateName() {
        ChampionModifier m = ChampionModifier.of(this);
        if (m != null) setCustomName(Component.translatable("entity.EldritchWarden.name.custom", getTitle(), m.displayName()));
    }

    public String getTitle() { return TITLES[Mth.clamp(entityData.get(NAME), 0, TITLES.length - 1)]; }
    private void setTitle(int t) { entityData.set(NAME, (byte) t); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(NAME, (byte) 0); }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putByte("title", entityData.get(NAME)); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setTitle(in.getByteOr("title", (byte) 0)); }

    @Override protected boolean aiFrozen() { return fieldFrenzyCounter > 0 || super.aiFrozen(); }

    private int wardMax() { return (int) (getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 0.66); }

    @Override
    protected void updateBossBars() {
        super.updateBossBars();
        int bh = wardMax();
        if (invulnerableTime <= 0 && tickCount % 25 == 0 && getAbsorptionAmount() < bh) setAbsorptionAmount(getAbsorptionAmount() + 1.0f);
        bossInfo2.setProgress(Mth.clamp(getAbsorptionAmount() / bh, 0.0f, 1.0f));
    }

    public ServerBossEvent wardEvent() { return bossInfo2; }

    @Override
    public void tick() {
        if (getSpawnTimer() == 150) level().broadcastEntityEvent(this, (byte) 18);
        super.tick();
        if (level().isClientSide()) {
            if (armLiftL > 0.0f) armLiftL -= 0.05f;
            if (armLiftR > 0.0f) armLiftR -= 0.05f;
            float x = (float) (getX() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            float z = (float) (getZ() + (random.nextFloat() - random.nextFloat()) * 0.2f);
            art.arcane.thaumcraft.client.fx.ClientHooks.wispFXEG(x, getY() + 0.25 * getBbHeight(), z, this);
            if (spawnTimer > 0) {
                float he = Math.max(1.0f, getBbHeight() * ((150 - spawnTimer) / 150.0f));
                for (int a = 0; a < 33; ++a)
                    art.arcane.thaumcraft.client.fx.FXDispatcher.smokeSpiral(getX(), getBoundingBox().minY + he / 2.0f, getZ(), he, random.nextInt(360), Mth.floor(getBoundingBox().minY) - 1, 2232623);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        for (int l = 0; l < 4; ++l) {
            int i = Mth.floor(getX() + (l % 2 * 2 - 1) * 0.25f);
            int j = Mth.floor(getY());
            int k = Mth.floor(getZ() + (l / 2 % 2 * 2 - 1) * 0.25f);
            BlockPos bp = new BlockPos(i, j, k);
            if (level().isEmptyBlock(bp)) level().setBlockAndUpdate(bp, ModBlocks.EFFECT_SAP.get().defaultBlockState());
        }
        if (level() instanceof ServerLevel sl && fieldFrenzyCounter > 0) {
            if (fieldFrenzyCounter == 150) teleportHome(sl);
            performFieldFrenzy(sl);
        }
    }

    private void performFieldFrenzy(ServerLevel sl) {
        if (fieldFrenzyCounter < 121 && fieldFrenzyCounter % 10 == 0) {
            sl.broadcastEntityEvent(this, (byte) 17);
            double radius = (150 - fieldFrenzyCounter) / 8.0;
            int d = 1 + fieldFrenzyCounter / 8;
            int i = Mth.floor(getX()), j = Mth.floor(getY()), k = Mth.floor(getZ());
            for (int q = 0; q < 180 / d; ++q) {
                double radians = Math.toRadians(q * 2 * d);
                int deltaX = (int) (radius * Math.cos(radians));
                int deltaZ = (int) (radius * Math.sin(radians));
                BlockPos bp = new BlockPos(i + deltaX, j, k + deltaZ);
                if (sl.isEmptyBlock(bp) && sl.getBlockState(bp.below()).isRedstoneConductor(sl, bp.below())) {
                    sl.setBlockAndUpdate(bp, ModBlocks.EFFECT_SAP.get().defaultBlockState());
                    sl.scheduleTick(bp, ModBlocks.EFFECT_SAP.get(), 250 + random.nextInt(150));
                    if (random.nextFloat() < 0.3f)
                        PacketDistributor.sendToPlayersNear(sl, null, getX(), getY(), getZ(), 32.0, new FxPayload("block_arc", bp.getX(), bp.getY(), bp.getZ(),
                                List.of((float) getX(), (float) (getY() + getBbHeight() / 2.0), (float) getZ(), 0.5f + random.nextFloat() * 0.2f, 0.0f, 0.5f + random.nextFloat() * 0.2f)));
                }
            }
            playSound(ModSounds.get("zap"), 1.0f, 0.9f + random.nextFloat() * 0.1f);
        }
        --fieldFrenzyCounter;
    }

    protected void teleportHome(ServerLevel sl) {
        BlockPos home = getHomePosition();
        var event = EventHooks.onEnderTeleport(this, home.getX(), home.getY(), home.getZ());
        if (event.isCanceled()) return;
        double d3 = getX(), d4 = getY(), d5 = getZ();
        setPosRaw(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        boolean flag = false;
        int i = Mth.floor(getX()), j = Mth.floor(getY()), k = Mth.floor(getZ());
        BlockPos bp = new BlockPos(i, j, k);
        if (sl.isLoaded(bp)) {
            boolean flag2 = false;
            int tries = 20;
            while (!flag2 && tries > 0) {
                // (sic: TC6 keeps testing the original block while it re-rolls i/k)
                if (sl.getBlockState(bp.below()).blocksMotion() && !sl.getBlockState(bp).blocksMotion()) flag2 = true;
                else {
                    i = Mth.floor(getX()) + random.nextInt(8) - random.nextInt(8);
                    k = Mth.floor(getZ()) + random.nextInt(8) - random.nextInt(8);
                    --tries;
                }
            }
            if (flag2) {
                setPos(i + 0.5, j + 0.1, k + 0.5);
                if (sl.noCollision(this, getBoundingBox())) flag = true;
            }
        }
        if (!flag) { setPos(d3, d4, d5); return; }
        for (int l = 0; l < 128; ++l) {
            double d6 = l / 127.0;
            float f = (random.nextFloat() - 0.5f) * 0.2f, f2 = (random.nextFloat() - 0.5f) * 0.2f, f3 = (random.nextFloat() - 0.5f) * 0.2f;
            double d7 = d3 + (getX() - d3) * d6 + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
            double d8 = d4 + (getY() - d4) * d6 + random.nextDouble() * getBbHeight();
            double d9 = d5 + (getZ() - d5) * d6 + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
            sl.sendParticles(ParticleTypes.PORTAL, d7, d8, d9, 1, f, f2, f3, 0.0);
        }
        playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource ds) { return fieldFrenzyCounter > 0 || super.isInvulnerableTo(level, ds); }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableTo(level, source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER)) return false;
        boolean aef = super.hurtServer(level, source, damage);
        if (aef && !fieldFrenzy && getAbsorptionAmount() <= 0.0f) {
            fieldFrenzy = true;
            fieldFrenzyCounter = 150;
        }
        return aef;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        spawnTimer = 150;
        setTitle(random.nextInt(TITLES.length));
        setAbsorptionAmount((float) (getAbsorptionAmount() + getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 0.66));
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (random.nextFloat() > 0.2f) {
            EldritchOrbEntity blast = new EldritchOrbEntity(sl, this);
            lastBlast = !lastBlast;
            sl.broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
            int rr = lastBlast ? 90 : 180;
            double xx = Mth.cos((getYRot() + rr) % 360.0f / 180.0f * 3.1415927f) * 0.5f;
            double yy = 0.13;
            double zz = Mth.sin((getYRot() + rr) % 360.0f / 180.0f * 3.1415927f) * 0.5f;
            blast.setPos(blast.getX() - xx, blast.getY() - yy, blast.getZ() - zz);
            var tm = target.getDeltaMovement();
            double d0 = target.getX() + tm.x - getX();
            double d2 = target.getY() - getY() - target.getBbHeight() / 2.0f;
            double d3 = target.getZ() + tm.z - getZ();
            blast.shoot(d0, d2, d3, 1.0f, 2.0f);
            playSound(ModSounds.get("egattack"), 2.0f, 1.0f + random.nextFloat() * 0.1f);
            sl.addFreshEntity(blast);
        } else if (hasLineOfSight(target)) {
            PacketDistributor.sendToPlayersNear(sl, null, getX(), getY(), getZ(), 32.0, new FxPayload("sonic", getX(), getY(), getZ(), List.of((float) getId())));
            target.push(-Mth.sin(getYRot() * 3.1415927f / 180.0f) * 1.5f, 0.1, Mth.cos(getYRot() * 3.1415927f / 180.0f) * 1.5f);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            if (target instanceof ServerPlayer p) PlayerProgressManager.addWarp(p, PlayerProgressManager.WarpType.TEMPORARY, 3 + sl.getRandom().nextInt(3));
            playSound(ModSounds.get("egscreech"), 4.0f, 1.0f + random.nextFloat() * 0.1f);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 15) armLiftL = 0.5f;
        else if (id == 16) armLiftR = 0.5f;
        else if (id == 17) { armLiftL = 0.9f; armLiftR = 0.9f; }
        else if (id == 18) spawnTimer = 150;
        else super.handleEntityEvent(id);
    }

    @Override public boolean canAttack(LivingEntity target) { return !(target instanceof EldritchGuardianEntity) && super.canAttack(target); }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("egidle"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("egdeath"); }
    @Override public int getAmbientSoundInterval() { return 500; }
}
