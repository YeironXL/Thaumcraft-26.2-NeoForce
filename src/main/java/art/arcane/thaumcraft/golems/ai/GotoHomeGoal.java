package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.golems.GolemEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

/** TC6 AIGotoHome: idle golems wander back to their home point. */
public class GotoHomeGoal extends Goal {
    protected final GolemEntity golem;
    private double x, y, z;
    protected int idleCounter = 10;
    public GotoHomeGoal(GolemEntity g) { golem = g; setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
    @Override public boolean canUse() {
        if (idleCounter > 0) { --idleCounter; return false; }
        idleCounter = 50;
        double dd = golem.getHomePosition().distToCenterSqr(golem.position());
        if (dd < 5.0) return false;
        if (dd <= 1024.0) { x = golem.getHomePosition().getX(); y = golem.getHomePosition().getY(); z = golem.getHomePosition().getZ(); return true; }
        Vec3 v = DefaultRandomPos.getPosTowards(golem, 16, 7, Vec3.atLowerCornerOf(golem.getHomePosition()), Math.PI / 2);
        if (v == null) return false;
        x = v.x; y = v.y; z = v.z;
        return true;
    }
    @Override public void start() { golem.getNavigation().moveTo(x, y, z, golem.getGolemMoveSpeed()); }
    @Override public boolean canContinueToUse() { return golem.getTask() == null && !golem.getNavigation().isDone() && golem.getHomePosition().distToCenterSqr(golem.position()) > 3.0; }
    @Override public void stop() { idleCounter = 50; golem.getNavigation().stop(); }
}
