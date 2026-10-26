package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.TaintacleEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityTaintacleGiant: a 6-block taintacle (175 hp, always a persistent champion) with a purple boss bar, slow
 * regeneration and the boss enrage/damage cap. Never spawns naturally; the last one alive in 48 blocks drops a pearl.
 */
public class TaintacleGiantEntity extends TaintacleEntity implements EldritchMob {
    private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(TaintacleGiantEntity.class, EntityDataSerializers.INT);
    protected final ServerBossEvent bossInfo;

    public TaintacleGiantEntity(EntityType<? extends TaintacleGiantEntity> type, Level level) {
        super(type, level);
        bossInfo = (ServerBossEvent) new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintacleEntity.createAttributes().add(Attributes.MAX_HEALTH, 175.0).add(Attributes.ATTACK_DAMAGE, 9.0);
    }

    public ServerBossEvent bossEvent() { return bossInfo; }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        ChampionModifier.makeChampion(this, true);
        bossInfo.setName(getDisplayName());
        return data;
    }

    @Override public void setCustomName(@Nullable Component name) { super.setCustomName(name); bossInfo.setName(getDisplayName()); }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(AGGRO, 0); }
    public int getAnger() { return entityData.get(AGGRO); }
    public void setAnger(int v) { entityData.set(AGGRO, v); }

    @Override
    public void tick() {
        super.tick();
        if (getAnger() > 0) setAnger(getAnger() - 1);
        if (level().isClientSide() && random.nextInt(15) == 0 && getAnger() > 0) {
            double d0 = random.nextGaussian() * 0.02, d2 = random.nextGaussian() * 0.02, d3 = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
                    getBoundingBox().minY + getBbHeight() + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0, d0, d2, d3);
        }
        if (!level().isClientSide()) {
            if (tickCount % 30 == 0) heal(1.0f);
            bossInfo.setProgress(getHealth() / getMaxHealth());
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        var ents = level.getEntitiesOfClass(TaintacleGiantEntity.class, getBoundingBox().inflate(48.0), e -> e != this && e.distanceToSqr(this) <= 48.0 * 48.0);
        if (ents.isEmpty()) ThaumcraftBossEntity.dropSpecialItem(level, this, new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0f);
    }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override protected int decreaseAirSupply(int air) { return air; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (damage > 35.0f) {
            if (getAnger() == 0) {
                addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (damage / 15.0f)));
                addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, (int) (damage / 10.0f)));
                addEffect(new MobEffectInstance(MobEffects.HASTE, 200, (int) (damage / 40.0f)));
                setAnger(200);
                if (source.getEntity() instanceof Player p)
                    p.sendOverlayMessage(Component.empty().append(getName()).append(" ").append(Component.translatable("tc.boss.enrage")));
            }
            damage = 35.0f;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossInfo.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossInfo.removePlayer(player); }
}
