package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/** TC6 AIGotoEntity: entity tasks (item pickup, guard / butcher targets). */
public class GotoEntityGoal extends GotoGoal {
    public GotoEntityGoal(GolemEntity g) { super(g, (byte) 1); }
    @Override public void tick() {
        super.tick();
        Task t = golem.getTask();
        if (t != null && t.getEntity() != null) golem.getLookControl().setLookAt(t.getEntity(), 10.0f, golem.getMaxHeadXRot());
    }
    @Override protected void moveTo() {
        Task t = golem.getTask();
        if (t != null && t.getEntity() != null) golem.getNavigation().moveTo(t.getEntity(), golem.getGolemMoveSpeed());
    }
    @Override protected boolean findDestination() {
        for (Task ticket : TaskHandler.getEntityTasksSorted(golem.level(), golem.getUUID(), golem)) {
            if (areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(golem) && golem.isWithinHome(ticket.getEntity().blockPosition())
                    && isValidDestination(golem.level(), ticket.getEntity().blockPosition()) && canEasilyReach(ticket.getEntity())) {
                golem.setTask(ticket);
                golem.getTask().setReserved(true);
                float w = ticket.getEntity().getBbWidth() / 2.0f;
                minDist = 3.5 + w * w;
                golem.emote(5);
                return true;
            }
        }
        return false;
    }
    private boolean canEasilyReach(Entity e) {
        if (golem.distanceToSqr(e) < minDist) return true;
        Path path = golem.getNavigation().createPath(e, 0);
        if (path == null) return false;
        Node end = path.getEndNode();
        if (end == null) return false;
        int i = end.x - Mth.floor(e.getX()), j = end.z - Mth.floor(e.getZ());
        return i * i + j * j < minDist;
    }
}
