package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.entity.construct.OwnedConstruct;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/** TC6 AIFollowOwner: follow the owner, teleporting next to them when more than 12 blocks away and stuck. */
public class FollowOwnerGoal extends Goal {
    private final OwnedConstruct pet;
    private LivingEntity owner;
    private final double speed;
    private final float minDist, maxDist;
    private int timeToRecalcPath;
    private float oldWaterCost;
    public FollowOwnerGoal(OwnedConstruct pet, double speed, float minDist, float maxDist) {
        this.pet = pet; this.speed = speed; this.minDist = minDist; this.maxDist = maxDist;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private PathNavigation nav() { return pet.getNavigation(); }
    @Override public boolean canUse() {
        LivingEntity o = pet.getOwnerEntity();
        if (o == null || pet.distanceToSqr(o) < minDist * minDist) return false;
        owner = o;
        return true;
    }
    @Override public boolean canContinueToUse() { return !nav().isDone() && pet.distanceToSqr(owner) > maxDist * maxDist; }
    @Override public void start() { timeToRecalcPath = 0; oldWaterCost = pet.getPathfindingMalus(PathType.WATER); pet.setPathfindingMalus(PathType.WATER, 0.0f); }
    @Override public void stop() { owner = null; nav().stop(); pet.setPathfindingMalus(PathType.WATER, oldWaterCost); }
    private boolean clear(BlockPos p) { BlockState s = pet.level().getBlockState(p); return s.isAir() || !s.isCollisionShapeFullBlock(pet.level(), p); }
    @Override public void tick() {
        pet.getLookControl().setLookAt(owner, 10.0f, pet.getMaxHeadXRot());
        if (--timeToRecalcPath <= 0) {
            timeToRecalcPath = 10;
            if (!nav().moveTo(owner, speed) && !pet.isLeashed() && pet.distanceToSqr(owner) >= 144.0) {
                int i = Mth.floor(owner.getX()) - 2, j = Mth.floor(owner.getZ()) - 2, k = Mth.floor(owner.getBoundingBox().minY);
                for (int l = 0; l <= 4; ++l) for (int i2 = 0; i2 <= 4; ++i2) {
                    if ((l < 1 || i2 < 1 || l > 3 || i2 > 3) && pet.level().getBlockState(new BlockPos(i + l, k - 1, j + i2)).isCollisionShapeFullBlock(pet.level(), new BlockPos(i + l, k - 1, j + i2))
                            && clear(new BlockPos(i + l, k, j + i2)) && clear(new BlockPos(i + l, k + 1, j + i2))) {
                        pet.snapTo(i + l + 0.5, k, j + i2 + 0.5, pet.getYRot(), pet.getXRot());
                        nav().stop();
                        return;
                    }
                }
            }
        }
    }
}
