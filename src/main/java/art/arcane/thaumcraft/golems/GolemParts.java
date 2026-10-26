package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.entity.projectile.GolemDartEntity;
import art.arcane.thaumcraft.golems.ai.ArrowAttackGoal;
import art.arcane.thaumcraft.golems.parts.PartFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** The TC6 part functions: GolemArmDart, GolemLegWheels, GolemLegLevitator. */
public final class GolemParts {
    /** Dart launcher arms: ranged attack AI and the dart shot. */
    public static final class ArmDart implements PartFunction {
        @Override public void onUpdateTick(GolemEntity golem) {}
        @Override public void onRangedAttack(GolemEntity golem, LivingEntity target, float range) {
            var level = golem.level();
            GolemDartEntity dart = new GolemDartEntity(level, golem);
            float dmg = (float) golem.getAttributeValue(Attributes.ATTACK_DAMAGE) / 3.0f;
            dart.setBaseDamage(dmg + range + level.getRandom().nextGaussian() * 0.25);
            double d0 = target.getX() - golem.getX();
            double d2 = target.getBoundingBox().minY + target.getEyeHeight() + range * range - dart.getY();
            double d3 = target.getZ() - golem.getZ();
            dart.shoot(d0, d2, d3, 1.6f, 3.0f);
            level.addFreshEntity(dart);
            golem.playSound(SoundEvents.ARROW_SHOOT, 1.0f, 1.0f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        }
        @Override public Goal getRangedAttackAI(GolemEntity golem) { return new ArrowAttackGoal(golem, 1.0, 20, 25, 16.0f); }
    }
    /** Wheels: client-side wheel rotation and dust. */
    public static final class LegWheels implements PartFunction {
        @Override public void onUpdateTick(GolemEntity g) {
            if (!g.level().isClientSide()) return;
            double dx = g.getX() - g.xo, dy = g.getY() - g.yo, dz = g.getZ() - g.zo;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float dirTravel = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
            double dir = 360.0f - (g.getYRot() - dirTravel);
            g.wheelRot += (float) (dist / 1.571 * dir);
            if (g.wheelRot > 360.0f) g.wheelRot -= 360.0f;
            if (g.onGround() && !g.isInWater() && dist > 0.25) {
                BlockPos bp = BlockPos.containing(g.getX(), g.getY() - 0.2, g.getZ());
                BlockState st = g.level().getBlockState(bp);
                if (st.getRenderShape() != RenderShape.INVISIBLE)
                    g.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st), g.getX() + (g.getRandom().nextFloat() - 0.5) * g.getBbWidth(), g.getBoundingBox().minY + 0.1,
                            g.getZ() + (g.getRandom().nextFloat() - 0.5) * g.getBbWidth(), -g.getDeltaMovement().x * 4.0, 1.5, -g.getDeltaMovement().z * 4.0);
            }
        }
    }
    /** Levitator legs: exhaust particles. */
    public static final class LegLevitator implements PartFunction {
        @Override public void onUpdateTick(GolemEntity g) {
            if (g.level().isClientSide() && (!g.onGround() || g.tickCount % 5 == 0))
                art.arcane.thaumcraft.golems.client.GolemClientFx.fly(g.getX(), g.getY() + 0.1, g.getZ(), g.getRandom().nextGaussian() / 100.0, -0.1, g.getRandom().nextGaussian() / 100.0);
        }
    }
    private GolemParts() {}
}
