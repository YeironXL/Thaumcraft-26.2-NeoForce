package art.arcane.thaumcraft.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla 1.12 EntityAIRestrictOpenDoor (removed from modern Minecraft together with the village door registry).
 * At night, when the mob stands within 1.5 blocks of a village door, it stops breaking/entering doors while it stays
 * on the inside of that door; resetting re-enables both. The 1.12 VillageDoorInfo is replaced by the door block
 * itself plus {@link ServerLevel#isVillage(BlockPos)}; "inside" is the side the closed door faces away from.
 */
public class RestrictOpenDoorGoal extends Goal {
    private final PathfinderMob mob;
    private BlockPos door;
    private Direction inside;

    public RestrictOpenDoorGoal(PathfinderMob mob) { this.mob = mob; }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel sl) || sl.isBrightOutside()) return false;
        BlockPos pos = mob.blockPosition();
        if (!sl.isVillage(pos)) return false;
        BlockPos best = null; double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3))) {
            BlockState st = sl.getBlockState(p);
            if (st.getBlock() instanceof DoorBlock && st.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
                double d = p.distSqr(pos);
                if (d < bd) { bd = d; best = p.immutable(); }
            }
        }
        if (best == null) return false;
        door = best;
        inside = sl.getBlockState(best).getValue(DoorBlock.FACING).getOpposite();
        return insideBlock().distSqr(pos) < 2.25;
    }

    private BlockPos insideBlock() { return door.relative(inside); }

    @Override
    public boolean canContinueToUse() {
        if (mob.level().isBrightOutside() || door == null) return false;
        if (!(mob.level().getBlockState(door).getBlock() instanceof DoorBlock)) return false;
        BlockPos pos = mob.blockPosition();
        int dx = pos.getX() - door.getX(), dz = pos.getZ() - door.getZ();
        return dx * inside.getStepX() + dz * inside.getStepZ() >= 0;
    }

    @Override
    public void start() {
        mob.getNavigation().setCanOpenDoors(false);
        mob.getNavigation().getNodeEvaluator().setCanPassDoors(false);
    }

    @Override
    public void stop() {
        mob.getNavigation().setCanOpenDoors(true);
        mob.getNavigation().getNodeEvaluator().setCanPassDoors(true);
        door = null;
    }
}
