package art.arcane.thaumcraft.golems.parts;

import art.arcane.thaumcraft.golems.GolemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/** TC6 IGenericFunction / GolemArm.IArmFunction. */
public interface PartFunction {
    void onUpdateTick(GolemEntity golem);
    default void onMeleeAttack(GolemEntity golem, Entity target) {}
    default void onRangedAttack(GolemEntity golem, LivingEntity target, float range) {}
    default Goal getRangedAttackAI(GolemEntity golem) { return null; }
}
