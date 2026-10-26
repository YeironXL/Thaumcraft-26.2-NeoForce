package art.arcane.thaumcraft.entity.monster;

import art.arcane.thaumcraft.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 EntityBrainyZombie ("Angry Zombie"): a tougher zombie (25 hp, 5 damage, +1 armour) that often drops its brain. */
public class BrainyZombieEntity extends Zombie {
    public BrainyZombieEntity(EntityType<? extends BrainyZombieEntity> type, Level level) {
        super(type, level);
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
                .add(Attributes.ARMOR, 3.0); // zombie 2 + getTotalArmorValue() + 1
    }

    /** No drowned in 1.12: TC6 zombies never convert in water. */
    @Override protected boolean convertsInWater() { return false; }

    /** dropLoot: a brain when rand(10) - looting <= 4, before the usual zombie loot. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = art.arcane.thaumcraft.util.EntityUtils.lootingLevel(level, source);
        if (level.getRandom().nextInt(10) - looting <= 4) spawnAtLocation(level, new ItemStack(ModItems.BRAIN.get()), 1.5f);
    }
}
