package art.arcane.thaumcraft.entity.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * TC6 EntityGiantBrainyZombie ("Furious Zombie"): every hit makes it angrier (up to 2); above 1 it grows to (1 + anger)
 * times zombie size and hits harder (7 + (anger - 1) * 5), calming down by 0.002 a tick.
 */
public class GiantBrainyZombieEntity extends BrainyZombieEntity {
    private static final EntityDataAccessor<Float> ANGER = SynchedEntityData.defineId(GiantBrainyZombieEntity.class, EntityDataSerializers.FLOAT);
    /** The anger the size was last set from (setSize is only called while anger > 1, so the size sticks below that). */
    private float sizeAnger;

    public GiantBrainyZombieEntity(EntityType<? extends GiantBrainyZombieEntity> type, Level level) {
        super(type, level);
        xpReward = 15;
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4f));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BrainyZombieEntity.createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(ANGER, 0.0f); }
    public float getAnger() { return entityData.get(ANGER); }
    public void setAnger(float v) { entityData.set(ANGER, v); }

    @Override
    public void aiStep() {
        super.aiStep();
        if (getAnger() > 1.0f) {
            if (!level().isClientSide()) setAnger(getAnger() - 0.002f);
            if (Math.abs(sizeAnger - getAnger()) > 1e-4f) { sizeAnger = getAnger(); refreshDimensions(); }
        }
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0f + (getAnger() - 1.0f) * 5.0f);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions d = super.getDefaultDimensions(pose);
        if (sizeAnger <= 0.0f || isBaby()) return d;
        return EntityDimensions.scalable(0.6f + sizeAnger * 0.6f, 1.95f + sizeAnger * 1.95f).withEyeHeight(1.74f + getAnger() * 1.74f);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        setAnger(Math.min(2.0f, getAnger() + 0.1f));
        return super.hurtServer(level, source, amount);
    }

    // dropFewItems (twelve 50% rolls of rotten flesh) is dead code in TC6: EntityZombie's loot table takes precedence.
}
