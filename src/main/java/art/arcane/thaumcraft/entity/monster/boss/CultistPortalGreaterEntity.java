package art.arcane.thaumcraft.entity.monster.boss;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.monster.CultistEntity;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityCultistPortalGreater: the crimson rites boss portal. It raises four cult banners and scatters loot crates,
 * then (while a player is within 48 blocks) summons waves of knights and clerics; on stage 12 it calls a Crimson Praetor,
 * after which every summon costs it 5-9 health. Red notched boss bar, 500 hp, zaps players that touch it, drops a
 * primordial pearl. (TC6 BETA26 never spawns it naturally: only by spawn egg or command.)
 */
public class CultistPortalGreaterEntity extends Monster {
    protected final ServerBossEvent bossInfo;
    public int stage = 0;
    public int stagecounter = 200;
    public int pulse = 0;

    public CultistPortalGreaterEntity(EntityType<? extends CultistPortalGreaterEntity> type, Level level) {
        super(type, level);
        bossInfo = (ServerBossEvent) new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6).setDarkenScreen(true);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 5.0);
    }

    public ServerBossEvent bossEvent() { return bossInfo; }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putInt("stage", stage); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); stage = in.getIntOr("stage", 0); }

    @Override public boolean isPushable() { return false; }
    @Override public void move(MoverType type, Vec3 delta) {}
    @Override public void aiStep() {}
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 4096.0; }
    @Override public boolean canBeAffected(MobEffectInstance effect) { return false; }
    @Override public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }
    @Override public boolean removeWhenFarAway(double distSqr) { return false; }

    @Override public void startSeenByPlayer(ServerPlayer player) { super.startSeenByPlayer(player); bossInfo.addPlayer(player); }
    @Override public void stopSeenByPlayer(ServerPlayer player) { super.stopSeenByPlayer(player); bossInfo.removePlayer(player); }

    private void arc(ServerLevel sl, BlockPos bp) {
        Fx.send(sl, "block_arc", bp.getX(), bp.getY(), bp.getZ(), (float) getX(), (float) (getBoundingBox().minY + getBbHeight() / 2.0f), (float) getZ(),
                0.5f + random.nextFloat() * 0.2f, 0.0f, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl) {
            bossInfo.setProgress(getHealth() / getMaxHealth());
            if (stagecounter > 0) {
                --stagecounter;
                if (stagecounter == 160 && stage == 0) {
                    sl.broadcastEntityEvent(this, (byte) 16);
                    for (Direction dir : new Direction[]{Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST}) {
                        BlockPos bp = new BlockPos((int) getX() - dir.getStepX() * 6, (int) getY(), (int) getZ() + dir.getStepZ() * 6);
                        sl.setBlock(bp, ModBlocks.BANNER_CRIMSON_CULT.get().defaultBlockState(), 3);
                        // (TC6 then turned the banner tile to face the portal; the port's cult banner is a plain block)
                        arc(sl, bp);
                        playSound(ModSounds.get("wandfail"), 1.0f, 1.0f);
                    }
                }
                if (stagecounter > 20 && stagecounter < 150 && stage == 0 && stagecounter % 13 == 0) {
                    int a = (int) getX() + random.nextInt(5) - random.nextInt(5);
                    int b = (int) getZ() + random.nextInt(5) - random.nextInt(5);
                    BlockPos bp2 = new BlockPos(a, (int) getY(), b);
                    if (a != (int) getX() && b != (int) getZ() && sl.isEmptyBlock(bp2)) {
                        sl.broadcastEntityEvent(this, (byte) 16);
                        float rr = sl.getRandom().nextFloat();
                        Block bb = rr < 0.05f ? ModBlocks.LOOT_CRATE_RARE.get() : rr < 0.2f ? ModBlocks.LOOT_CRATE_UNCOMMON.get() : ModBlocks.LOOT_CRATE_COMMON.get();
                        sl.setBlockAndUpdate(bp2, bb.defaultBlockState());
                        arc(sl, bp2);
                        playSound(ModSounds.get("wandfail"), 1.0f, 1.0f);
                    }
                }
            } else if (sl.getNearestPlayer(this, 48.0) != null) {
                sl.broadcastEntityEvent(this, (byte) 16);
                switch (stage) {
                    case 0, 1, 2, 3, 4 -> { stagecounter = 15 + random.nextInt(10 - stage) - stage; spawnMinions(sl); }
                    case 12 -> { stagecounter = 50 + getTiming(sl) * 2 + random.nextInt(50); spawnBoss(sl); }
                    default -> { int t = getTiming(sl); stagecounter = t + random.nextInt(5 + t / 3); spawnMinions(sl); }
                }
                ++stage;
            } else stagecounter = 30 + random.nextInt(30);
            if (stage < 12) heal(1.0f);
        }
        if (pulse > 0) --pulse;
    }

    int getTiming(ServerLevel sl) {
        return sl.getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(32.0), e -> e.distanceToSqr(this) <= 1024.0).size() * 20;
    }

    public void spawnMinions(ServerLevel sl) {
        CultistEntity cultist = random.nextFloat() > 0.33
                ? ModEntities.CULTIST_KNIGHT.get().create(sl, EntitySpawnReason.MOB_SUMMONED)
                : ModEntities.CULTIST_CLERIC.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (cultist == null) return;
        cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
        cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        cultist.setHomeTo(blockPosition(), 32);
        sl.addFreshEntity(cultist);
        cultist.spawnExplosionParticle();
        cultist.playSound(ModSounds.get("wandfail"), 1.0f, 1.0f);
        if (stage > 12) hurtServer(sl, sl.damageSources().fellOutOfWorld(), (float) (5 + random.nextInt(5)));
    }

    public void spawnBoss(ServerLevel sl) {
        CultistLeaderEntity leader = ModEntities.CULTIST_LEADER.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (leader == null) return;
        leader.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
        leader.finalizeSpawn(sl, sl.getCurrentDifficultyAt(leader.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        leader.setHomeTo(blockPosition(), 32);
        sl.addFreshEntity(leader);
        leader.spawnExplosionParticle();
        leader.playSound(ModSounds.get("wandfail"), 1.0f, 1.0f);
    }

    @Override
    public void playerTouch(Player p) {
        if (level() instanceof ServerLevel sl && distanceToSqr(p) < 3.0 && p.hurtServer(sl, sl.damageSources().indirectMagic(this, this), 8.0f))
            playSound(ModSounds.get("zap"), 1.0f, (random.nextFloat() - random.nextFloat()) * 0.1f + 1.0f);
    }

    @Override protected float getSoundVolume() { return 0.75f; }
    @Override public int getAmbientSoundInterval() { return 540; }
    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("monolith"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return ModSounds.get("zap"); }
    @Override protected SoundEvent getDeathSound() { return ModSounds.get("shock"); }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        ThaumcraftBossEntity.dropSpecialItem(level, this, new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0f);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) pulse = 10;
        else super.handleEntityEvent(id);
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel sl) sl.explode(this, getX(), getY(), getZ(), 2.0f, false, Level.ExplosionInteraction.NONE);
        super.die(source);
    }
}
