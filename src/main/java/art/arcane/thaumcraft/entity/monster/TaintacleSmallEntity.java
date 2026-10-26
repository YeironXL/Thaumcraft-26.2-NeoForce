package art.arcane.thaumcraft.entity.monster;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** TC6 EntityTaintacleSmall ("Taint Tendril"): a 1-block tentacle summoned by a taintacle that withers after 200 ticks. */
public class TaintacleSmallEntity extends TaintacleEntity {
    public int lifetime = 200;

    public TaintacleSmallEntity(EntityType<? extends TaintacleSmallEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TaintacleEntity.createAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (lifetime-- <= 0 && level() instanceof ServerLevel sl) hurtServer(sl, damageSources().magic(), 10.0f);
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {}
}
