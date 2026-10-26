package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** TC6 EntityTaintSeedPrime: the larger seed flux rifts spawn, with twice the spreading area. */
public class TaintSeedPrimeEntity extends TaintSeedEntity {
    public TaintSeedPrimeEntity(EntityType<? extends TaintSeedPrimeEntity> type, Level level) { super(type, level); xpReward = 12; }

    @Override protected int getArea() { return 2; }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintSeedEntity.createAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        spawnAtLocation(level, CrystalEssenceItem.of("vitium", 1), getBbHeight() / 2.0f);
        if (random.nextBoolean()) spawnAtLocation(level, CrystalEssenceItem.of("vitium", 1), getBbHeight() / 2.0f);
        if (random.nextBoolean()) spawnAtLocation(level, CrystalEssenceItem.of("vitium", 1), getBbHeight() / 2.0f);
    }
}
