package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.ai.CultistHurtByTargetGoal;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.CultistClericEntity;
import art.arcane.thaumcraft.entity.monster.CultistEntity;
import art.arcane.thaumcraft.entity.monster.CultistKnightEntity;
import art.arcane.thaumcraft.entity.projectile.GolemOrbEntity;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityCultistLeader (the Crimson Praetor): the greater portal's final summon. Praetor armour and a crimson blade
 * (void sword on easy), red homing orbs at range, regeneration II for cultists within 8 blocks; drops a rare loot bag.
 */
public class CultistLeaderEntity extends ThaumcraftBossEntity implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(CultistLeaderEntity.class, EntityDataSerializers.BYTE);
    public static final String[] TITLES = {"Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando", "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"};

    public CultistLeaderEntity(EntityType<? extends CultistLeaderEntity> type, Level level) {
        super(type, level);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 16.0, 1.0, 30, 40, 24.0f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(NAME, (byte) 0); }
    public String getTitle() { return TITLES[Mth.clamp(entityData.get(NAME), 0, TITLES.length - 1)]; }
    private void setTitle(int t) { entityData.set(NAME, (byte) t); }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putByte("title", entityData.get(NAME)); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setTitle(in.getByteOr("title", (byte) 0)); }

    @Override
    public void generateName() {
        ChampionModifier m = ChampionModifier.of(this);
        if (m != null) setCustomName(Component.translatable("entity.CultistLeader.name.custom", getTitle(), m.displayName()));
    }

    protected void setEquipment() {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PRAETOR_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PRAETOR_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PRAETOR_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
        setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(level().getDifficulty() == Difficulty.EASY ? ModItems.VOID_SWORD.get() : ModItems.CRIMSON_BLADE.get()));
    }

    protected void enchantByDifficulty(DifficultyInstance diff) {
        float f = diff.getSpecialMultiplier();
        if (!getMainHandItem().isEmpty() && random.nextFloat() < 0.5f * f)
            setItemInHand(InteractionHand.MAIN_HAND, EnchantmentHelper.enchantItem(random, getMainHandItem(), (int) (7.0f + f * random.nextInt(22)), level().registryAccess(), Optional.empty()));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setEquipment();
        enchantByDifficulty(diff);
        setTitle(random.nextInt(TITLES.length));
        return super.finalizeSpawn(level, diff, reason, data);
    }

    /** isOnSameTeam: cultists and praetors only (not the eldritch). */
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof CultistEntity || other instanceof CultistLeaderEntity; }
    @Override public boolean canAttack(LivingEntity t) {
        return !(t instanceof CultistClericEntity) && !(t instanceof CultistLeaderEntity) && !(t instanceof CultistKnightEntity) && super.canAttack(t);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        spawnAtLocation(level, new ItemStack(ModItems.LOOT_BAG_RARE.get()), 1.5f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl) {
            for (CultistEntity e : sl.getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(8.0), e -> e.distanceToSqr(this) <= 64.0)) {
                if (!e.hasEffect(MobEffects.REGENERATION)) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (!(level() instanceof ServerLevel sl) || !hasLineOfSight(target)) return;
        swing(InteractionHand.MAIN_HAND);
        getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0f, target.getZ(), 30.0f, 30.0f);
        GolemOrbEntity blast = new GolemOrbEntity(sl, this, target, true);
        var bm = blast.getDeltaMovement();
        blast.setPos(blast.getX() + bm.x / 2.0, blast.getY(), blast.getZ() + bm.z / 2.0);
        double d0 = target.getX() - getX();
        double d2 = target.getBoundingBox().minY + target.getBbHeight() / 2.0f - (getY() + getBbHeight() / 2.0f);
        double d3 = target.getZ() - getZ();
        blast.shoot(d0, d2 + 2.0, d3, 0.66f, 3.0f);
        playSound(ModSounds.get("egattack"), 1.0f, 1.0f + random.nextFloat() * 0.1f);
        sl.addFreshEntity(blast);
    }

    /** spawnExplosionParticle: entity event 20 shows the cultist spawn puffs. */
    public void spawnExplosionParticle() {
        if (level().isClientSide()) {
            for (int i = 0; i < 20; ++i) {
                double d0 = random.nextGaussian() * 0.05, d2 = random.nextGaussian() * 0.05, d3 = random.nextGaussian() * 0.05, d4 = 2.0;
                float w = getBbWidth();
                art.arcane.thaumcraft.client.fx.ClientHooks.cultistSpawn(getX() + random.nextFloat() * w * 2.0f - w + d0 * d4,
                        getY() + random.nextFloat() * getBbHeight() + d2 * d4, getZ() + random.nextFloat() * w * 2.0f - w + d3 * d4, d0, d2, d3);
            }
        } else level().broadcastEntityEvent(this, (byte) 20);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) spawnExplosionParticle();
        else super.handleEntityEvent(id);
    }
}
