package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.entity.EldritchMob;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TC6 EntityMindSpider: a small spider summoned by warp. Hallucinated ones are harmless, despawn after a minute
 * and are only rendered for their viewer (see MindSpiderRenderer).
 */
public class MindSpiderEntity extends Spider implements EldritchMob {
    private static final EntityDataAccessor<Boolean> HARMLESS = SynchedEntityData.defineId(MindSpiderEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(MindSpiderEntity.class, EntityDataSerializers.STRING);
    private int lifeSpan = Integer.MAX_VALUE;

    public MindSpiderEntity(EntityType<? extends MindSpiderEntity> type, Level level) {
        super(type, level);
        xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(HARMLESS, false);
        b.define(VIEWER, "");
    }

    public String getViewer() { return entityData.get(VIEWER); }
    public void setViewer(String player) { entityData.set(VIEWER, String.valueOf(player)); }
    public boolean isHarmless() { return entityData.get(HARMLESS); }
    public void setHarmless(boolean h) {
        if (h) lifeSpan = 1200;
        entityData.set(HARMLESS, h);
    }

    @Override protected int getBaseExperienceReward(ServerLevel level) { return isHarmless() ? 0 : super.getBaseExperienceReward(level); }
    @Override public float getVoicePitch() { return 0.7f; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > lifeSpan) discard();
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean killedByPlayer) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.NONE; }
    @Override public boolean doHurtTarget(ServerLevel level, Entity target) { return !isHarmless() && super.doHurtTarget(level, target); }

    @Override protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setHarmless(in.getBooleanOr("harmless", false));
        setViewer(in.getStringOr("viewer", ""));
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("harmless", isHarmless());
        out.putString("viewer", getViewer());
    }
}
