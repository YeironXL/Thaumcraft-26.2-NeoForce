package art.arcane.thaumcraft.entity.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/** TC6 EntityCultist: shared base of the crimson knight and cleric (no natural spawning in the port yet). */
public abstract class CultistEntity extends Monster {
    protected CultistEntity(EntityType<? extends CultistEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        getNavigation().setCanOpenDoors(true);
        setDropChance(EquipmentSlot.CHEST, 0.05f);
        setDropChance(EquipmentSlot.FEET, 0.05f);
        setDropChance(EquipmentSlot.HEAD, 0.05f);
        setDropChance(EquipmentSlot.LEGS, 0.05f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override public boolean canPickUpLoot() { return false; }
    @Override public boolean removeWhenFarAway(double distSqr) { return true; }

    protected void setLoot(DifficultyInstance diff) {}
    protected void setEnchantmentBasedOnDifficulty(DifficultyInstance diff) {}

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        setLoot(diff);
        setEnchantmentBasedOnDifficulty(diff);
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override protected boolean considersEntityAsAlly(Entity other) { return other instanceof CultistEntity || super.considersEntityAsAlly(other); }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return !(target instanceof CultistEntity) && super.canAttack(target);
    }

    /** EntityCultist.spawnExplosionParticle: entity event 20 shows cultistSpawn puffs instead of the vanilla poof. */
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
