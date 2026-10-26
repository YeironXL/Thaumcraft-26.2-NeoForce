package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/** TC6 AIGotoBlock: block tasks, approached from the closest open horizontal side. */
public class GotoBlockGoal extends GotoGoal {
    public GotoBlockGoal(GolemEntity g) { super(g, (byte) 0); }
    @Override public void tick() {
        super.tick();
        Task t = golem.getTask();
        if (t != null) golem.getLookControl().setLookAt(t.getPos().getX() + 0.5, t.getPos().getY() + 0.5, t.getPos().getZ() + 0.5, 10.0f, golem.getMaxHeadXRot());
    }
    @Override protected void moveTo() {
        BlockPos p = targetBlock != null ? targetBlock : golem.getTask().getPos();
        golem.getNavigation().moveTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, golem.getGolemMoveSpeed());
    }
    @Override protected boolean findDestination() {
        for (Task ticket : TaskHandler.getBlockTasksSorted(golem.level(), golem.getUUID(), golem)) {
            if (areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(golem) && golem.isWithinHome(ticket.getPos()) && isValidDestination(golem.level(), ticket.getPos()) && canEasilyReach(ticket.getPos())) {
                targetBlock = getAdjacentSpace(ticket.getPos());
                golem.setTask(ticket);
                golem.getTask().setReserved(true);
                golem.emote(5);
                return true;
            }
        }
        return false;
    }
    private BlockPos getAdjacentSpace(BlockPos pos) {
        double d = Double.MAX_VALUE;
        BlockPos closest = null;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (!golem.level().getBlockState(pos.relative(face)).blocksMotion()) {
                double dist = pos.relative(face).distToCenterSqr(golem.position());
                if (dist < d) { closest = pos.relative(face); d = dist; }
            }
        }
        return closest;
    }
    private boolean canEasilyReach(BlockPos pos) {
        if (pos.distToCenterSqr(golem.position()) < minDist) return true;
        Path path = golem.getNavigation().createPath(pos, 0);
        if (path == null) return false;
        Node end = path.getEndNode();
        if (end == null) return false;
        int i = end.x - pos.getX(), j = end.z - pos.getZ(), k = end.y - pos.getY();
        if (i == 0 && j == 0 && k == 2) --k;
        return i * i + j * j + k * k < 2.25;
    }
}
