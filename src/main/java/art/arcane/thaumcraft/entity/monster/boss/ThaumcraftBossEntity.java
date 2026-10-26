package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.entity.SpecialItemEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * TC6 EntityThaumcraftBoss: purple darkening boss bar, an AI freeze (and invulnerability) while the spawn timer runs,
 * slow regeneration, per-attacker aggro that retargets the boss and scales its health/damage with the number of
 * player attackers, enrage (regeneration/strength/haste) when hit for more than 35 (damage is capped at 35), a primordial
 * pearl plus a rare loot bag on death. Never despawns, never drowns, ignores cobwebs.
 */
public abstract class ThaumcraftBossEntity extends Monster {
    private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(ThaumcraftBossEntity.class, EntityDataSerializers.INT);
    /** EntityUtils.HPBUFF[0..4] (+50 health) and DMGBUFF[0..4] (+0.5 damage). */
    public static final AttributeModifier[] HPBUFF = new AttributeModifier[5], DMGBUFF = new AttributeModifier[5];
    static {
        for (int a = 0; a < 5; a++) {
            HPBUFF[a] = new AttributeModifier(Identifier.fromNamespaceAndPath("thaumcraft", "health_buff_" + (a + 1)), 50.0, AttributeModifier.Operation.ADD_VALUE);
            DMGBUFF[a] = new AttributeModifier(Identifier.fromNamespaceAndPath("thaumcraft", "damage_buff_" + (a + 1)), 0.5, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    protected final ServerBossEvent bossInfo;
    final HashMap<Integer, Integer> aggro = new HashMap<>();
    public int spawnTimer = 0;

    protected ThaumcraftBossEntity(EntityType<? extends ThaumcraftBossEntity> type, Level level) {
        super(type, level);
        bossInfo = (ServerBossEvent) new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
        xpReward = 50;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(AGGRO, 0); }
    public int getAnger() { return entityData.get(AGGRO); }
    public void setAnger(int v) { entityData.set(AGGRO, v); }
    public int getSpawnTimer() { return spawnTimer; }
    public ServerBossEvent bossEvent() { return bossInfo; }

    /** updateAITasks only runs while the spawn timer is 0. */
    protected boolean aiFrozen() { return getSpawnTimer() > 0; }
    @Override protected boolean isImmobile() { return super.isImmobile() || (!level().isClientSide() && aiFrozen()); }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossInfo.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossInfo.removePlayer(player); }

    @Override public void setCustomName(@Nullable Component name) { super.setCustomName(name); bossInfo.setName(getDisplayName()); }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setHomeTo(blockPosition(), 24);
        generateName();
        bossInfo.setName(getDisplayName());
        return data;
    }

    public void generateName() {}

    /** The bar progress TC6 sets in updateAITasks (every tick, also while frozen). */
    protected void updateBossBars() { bossInfo.setProgress(getHealth() / getMaxHealth()); }

    @Override
    public void tick() {
        super.tick();
        if (getSpawnTimer() > 0) --spawnTimer;
        if (getAnger() > 0) setAnger(getAnger() - 1);
        if (level().isClientSide() && random.nextInt(15) == 0 && getAnger() > 0) {
            double d0 = random.nextGaussian() * 0.02, d2 = random.nextGaussian() * 0.02, d3 = random.nextGaussian() * 0.02;
            level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0,
                    getBoundingBox().minY + getBbHeight() + random.nextFloat() * 0.5, getZ() + random.nextFloat() * getBbWidth() - getBbWidth() / 2.0, d0, d2, d3);
        }
        if (level() instanceof ServerLevel sl) {
            if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
            updateBossBars();
            if (tickCount % 30 == 0) heal(1.0f);
            if (getTarget() != null && tickCount % 20 == 0) {
                List<Integer> dl = new ArrayList<>();
                int players = 0;
                int hei = getTarget().getId();
                int ad = aggro.getOrDefault(hei, 0);
                int ld = ad;
                Entity newTarget = null;
                for (Integer ei : aggro.keySet()) {
                    int ca = aggro.get(ei);
                    if (ca > ad + 25 && ca > ad * 1.1 && ca > ld) {
                        newTarget = sl.getEntity(hei); // (sic: TC6 looks up the current target id, not ei)
                        if (newTarget == null || !newTarget.isAlive() || distanceToSqr(newTarget) > 16384.0) dl.add(ei);
                        else {
                            hei = ei;
                            ld = ei;
                            if (newTarget instanceof Player) ++players;
                        }
                    }
                }
                for (Integer ei : dl) aggro.remove(ei);
                if (newTarget instanceof LivingEntity le && hei != getTarget().getId()) setTarget(le);
                float om = getMaxHealth();
                AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH), dmg = getAttribute(Attributes.ATTACK_DAMAGE);
                for (int a = 0; a < 5; ++a) { dmg.removeModifier(DMGBUFF[a].id()); hp.removeModifier(HPBUFF[a].id()); }
                for (int a = 0; a < Math.min(5, players - 1); ++a) { hp.addTransientModifier(HPBUFF[a]); dmg.addTransientModifier(DMGBUFF[a]); }
                double mm = getMaxHealth() / om;
                setHealth((float) (getHealth() * mm));
            }
        }
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource ds) { return super.isInvulnerableTo(level, ds) || getSpawnTimer() > 0; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof LivingEntity) {
            int target = source.getEntity().getId();
            int ad = (int) damage;
            if (aggro.containsKey(target)) ad += aggro.get(target);
            aggro.put(target, ad);
        }
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

    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushable() { return super.isPushable() && getSpawnTimer() <= 0; }
    @Override protected int decreaseAirSupply(int air) { return air; }
    @Override public void makeStuckInBlock(BlockState state, Vec3 speed) {}
    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }
    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof EldritchMob || super.considersEntityAsAlly(other); }

    /** EntityUtils.entityDropSpecialItem: a floating, explosion-proof item rising slowly from the given height. */
    public static void dropSpecialItem(ServerLevel level, Entity e, ItemStack stack, float height) {
        SpecialItemEntity it = new SpecialItemEntity(level, e.getX(), e.getY() + height, e.getZ(), stack);
        it.setDefaultPickUpDelay();
        it.setDeltaMovement(0.0, 0.1, 0.0);
        level.addFreshEntity(it);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        dropSpecialItem(level, this, new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0f);
        spawnAtLocation(level, new ItemStack(ModItems.LOOT_BAG_RARE.get()), 1.5f);
    }
}
