package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.EldritchMob;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;

/**
 * TC6 EntityInhabitedZombie ("Shambling Husk"): a crimson-plated zombie carcass worn by an eldritch crab. It drops
 * nothing itself; when it dies the helmeted crab bursts out of its head.
 */
public class InhabitedZombieEntity extends Zombie implements EldritchMob {
    public InhabitedZombieEntity(EntityType<? extends InhabitedZombieEntity> type, Level level) {
        super(type, level);
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, CultistEntity.class, true));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override protected boolean convertsInWater() { return false; }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, SpawnGroupData data) {
        float d = level.getDifficulty() == Difficulty.HARD ? 0.9f : 0.6f;
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_PLATE_HELM.get()));
        if (random.nextFloat() <= d) setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CRIMSON_PLATE_CHEST.get()));
        if (random.nextFloat() <= d) setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CRIMSON_PLATE_LEGS.get()));
        return super.finalizeSpawn(level, diff, reason, data);
    }

    /** The plate is part of the husk (TC6 never dropped equipment from it: dropFewItems/onDeath are empty). */
    @Override protected void dropEquipment(ServerLevel level) {}
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {}
    @Override protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {}

    /** onDeathUpdate: immediately release the crab (with its helm) at eye height, drop the xp and vanish in smoke. */
    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel sl) {
            EldritchCrabEntity crab = ModEntities.ELDRITCH_CRAB.get().create(sl, EntitySpawnReason.EVENT);
            if (crab != null) {
                crab.snapTo(getX(), getY() + getEyeHeight(), getZ(), getYRot(), getXRot());
                crab.setHelm(true);
                sl.addFreshEntity(crab);
            }
            if ((lastHurtByPlayerMemoryTime > 0 || isAlwaysExperienceDropper()) && sl.getGameRules().get(GameRules.MOB_DROPS))
                ExperienceOrb.award(sl, position(), getExperienceReward(sl, getLastHurtByPlayer()));
            for (int k = 0; k < 20; ++k)
                sl.sendParticles(ParticleTypes.POOF, getX() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(), getY() + random.nextFloat() * getBbHeight(),
                        getZ() + random.nextFloat() * getBbWidth() * 2.0f - getBbWidth(), 1, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, 0.0);
        }
        remove(RemovalReason.KILLED);
    }

    /** onDeath is empty in TC6 (no death loot, no kill credit events beyond the vanilla ones). */
    @Override protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {}

    @Override protected SoundEvent getAmbientSound() { return ModSounds.get("crabtalk"); }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.GENERIC_HURT; }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        List<InhabitedZombieEntity> ents = level.getEntitiesOfClass(InhabitedZombieEntity.class, new AABB(getX(), getY(), getZ(), getX() + 1.0, getY() + 1.0, getZ() + 1.0).inflate(32.0, 16.0, 32.0));
        return ents.isEmpty() && super.checkSpawnRules(level, reason);
    }

    @Override protected boolean considersEntityAsAlly(net.minecraft.world.entity.Entity other) { return other instanceof EldritchMob || super.considersEntityAsAlly(other); }
}
