package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.ai.CultistHurtByTargetGoal;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.projectile.GolemOrbEntity;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * TC6 EntityCultistCleric: crimson robes, casts homing red orbs or a volley of small fireballs.
 */
public class CultistClericEntity extends CultistEntity implements RangedAttackMob, IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Boolean> RITUALIST = SynchedEntityData.defineId(CultistClericEntity.class, EntityDataSerializers.BOOLEAN);
    public int rage;

    public CultistClericEntity(EntityType<? extends CultistClericEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() { return CultistEntity.createAttributes().add(Attributes.MAX_HEALTH, 24.0); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new art.arcane.thaumcraft.entity.ai.AltarFocusGoal(this));
        goalSelector.addGoal(2, new LongRangeAttackGoal(this, 2.0, 1.0, 20, 40, 24.0f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(4, new art.arcane.thaumcraft.entity.ai.RestrictOpenDoorGoal(this));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new CultistHurtByTargetGoal(this, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, EldritchGuardianEntity.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(RITUALIST, false); }
    public boolean getIsRitualist() { return entityData.get(RITUALIST); }
    public void setIsRitualist(boolean v) { entityData.set(RITUALIST, v); }

    @Override
    protected void setLoot(DifficultyInstance diff) {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_ROBE_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_ROBE_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_ROBE_LEGS.get()));
        if (random.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.3f : 0.1f))
            setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
    }

    @Override
    public void performRangedAttack(LivingEntity t, float f) {
        if (!(level() instanceof ServerLevel sl)) return;
        double d0 = t.getX() - getX();
        double d2 = t.getBoundingBox().minY + t.getBbHeight() / 2.0f - (getY() + getBbHeight() / 2.0f);
        double d3 = t.getZ() - getZ();
        swing(InteractionHand.MAIN_HAND);
        float rf = random.nextFloat();
        if (rf > 0.66f) {
            GolemOrbEntity blast = new GolemOrbEntity(sl, this, t, true);
            Vec3 tm = t.getDeltaMovement();
            Vec3 v = t.position().add(tm.x * 10.0, tm.y * 10.0, tm.z * 10.0).subtract(position()).normalize();
            blast.setPos(blast.getX() + v.x, blast.getY() + v.y, blast.getZ() + v.z);
            blast.shoot(v.x, v.y, v.z, 0.66f, 3.0f);
            playSound(ModSounds.get("egattack"), 1.0f, 1.0f + random.nextFloat() * 0.1f);
            sl.addFreshEntity(blast);
        } else {
            float f2 = Mth.sqrt(f) * 0.5f;
            sl.levelEvent(null, 1009, blockPosition(), 0);
            for (int i = 0; i < 3; ++i) {
                SmallFireball fb = new SmallFireball(sl, this, new Vec3(d0 + random.nextGaussian() * f2, d2, d3 + random.nextGaussian() * f2));
                fb.setPos(fb.getX(), getY() + getBbHeight() / 2.0f + 0.5, fb.getZ());
                sl.addFreshEntity(fb);
            }
        }
    }

    @Override public boolean removeWhenFarAway(double distSqr) { return !getIsRitualist(); }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableTo(level, source)) return false;
        setIsRitualist(false);
        return super.hurtServer(level, source, damage);
    }

    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setIsRitualist(in.getBooleanOr("ritualist", false)); }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putBoolean("ritualist", getIsRitualist()); }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf data) { BlockPos h = getHomePosition(); data.writeInt(h.getX()); data.writeInt(h.getY()); data.writeInt(h.getZ()); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf data) { setHomeTo(new BlockPos(data.readInt(), data.readInt(), data.readInt()), 8); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && getIsRitualist()) {
            BlockPos h = getHomePosition();
            double d0 = h.getX() + 0.5 - getX();
            double d2 = h.getY() + 1.5 - (getY() + getEyeHeight());
            double d3 = h.getZ() + 0.5 - getZ();
            double d4 = Math.sqrt(d0 * d0 + d3 * d3);
            float f = (float) (Math.atan2(d3, d0) * 180.0 / Math.PI) - 90.0f;
            float f2 = (float) (-(Math.atan2(d2, d4) * 180.0 / Math.PI));
            setXRot(updateRotation(getXRot(), f2, 10.0f));
            yHeadRot = updateRotation(yHeadRot, f, getMaxHeadXRot());
        }
        if (!level().isClientSide() && getIsRitualist() && rage >= 5) setIsRitualist(false);
    }

    private static float updateRotation(float a, float b, float max) {
        float f3 = Mth.clamp(Mth.wrapDegrees(b - a), -max, max);
        return a + f3;
    }

    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("chant"); }
    @Override public int getAmbientSoundInterval() { return 500; }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 19) {
            for (int i = 0; i < 3; ++i) {
                double d0 = random.nextGaussian() * 0.02, d2 = random.nextGaussian() * 0.02, d3 = random.nextGaussian() * 0.02;
                float w = getBbWidth();
                level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * w * 2.0f - w, getY() + 0.5 + random.nextFloat() * getBbHeight(),
                        getZ() + random.nextFloat() * w * 2.0f - w, d0, d2, d3);
            }
        } else super.handleEntityEvent(id);
    }
}
