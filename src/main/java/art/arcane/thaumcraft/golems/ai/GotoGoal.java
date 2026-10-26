package art.arcane.thaumcraft.golems.ai;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Arrays;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** TC6 AIGoto: claims a task, walks to it and completes it (retrying until the task says it is done). */
public abstract class GotoGoal extends Goal {
    protected final GolemEntity golem;
    protected int taskCounter = -1;
    protected final byte type;
    protected int cooldown;
    protected double minDist = 4.0;
    private BlockPos prevRamble;
    protected BlockPos targetBlock;
    int pause = 0;

    protected GotoGoal(GolemEntity g, byte type) {
        golem = g;
        this.type = type;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { --cooldown; return false; }
        cooldown = 5;
        if (golem.getTask() != null && !golem.getTask().isSuspended()) return false;
        targetBlock = null;
        boolean start = findDestination();
        if (start && golem.getTask() != null && golem.getTask().getSealPos() != null) {
            ISealEntity se = SealHandler.getSealEntity(golem.level(), golem.getTask().getSealPos());
            if (se != null) se.getSeal().onTaskStarted(golem.level(), golem, golem.getTask());
        }
        return start;
    }
    @Override public void start() { moveTo(); taskCounter = 0; }
    protected abstract void moveTo();

    @Override
    public boolean canContinueToUse() {
        return taskCounter >= 0 && taskCounter <= 1000 && golem.getTask() != null && !golem.getTask().isSuspended() && isValidDestination(golem.level(), golem.getTask().getPos());
    }

    @Override
    public void tick() {
        Task t = golem.getTask();
        if (t == null) return;
        if (pause-- <= 0) {
            double dist = t.getType() == 0 ? (targetBlock == null ? t.getPos() : targetBlock).distToCenterSqr(golem.position()) : golem.distanceToSqr(t.getEntity());
            if (dist > minDist) {
                t.setCompletion(false);
                ++taskCounter;
                if (taskCounter % 5 == 0) {
                    if (prevRamble != null && prevRamble.equals(golem.blockPosition())) {
                        Vec3 v = DefaultRandomPos.getPosTowards(golem, 6, 4, Vec3.atLowerCornerOf(t.getPos()), Math.PI / 2);
                        if (v != null) golem.getNavigation().moveTo(v.x + 0.5, v.y + 0.5, v.z + 0.5, golem.getGolemMoveSpeed());
                    } else moveTo();
                    prevRamble = golem.blockPosition();
                }
            } else {
                TaskHandler.completeTask(t, golem);
                if (golem.getTask() != null && golem.getTask().isCompleted()) {
                    if (taskCounter >= 0) taskCounter = 0;
                    pause = 0;
                } else {
                    pause = 10;
                    ++taskCounter;
                }
                --taskCounter;
            }
        }
    }

    @Override
    public void stop() {
        Task t = golem.getTask();
        if (t != null) {
            if (!t.isCompleted() && t.isReserved()) golem.emote(6);
            if (t.isCompleted() && !t.isSuspended()) t.setSuspended(true);
            t.setReserved(false);
        }
    }

    protected abstract boolean findDestination();
    protected boolean isValidDestination(Level world, BlockPos pos) { return true; }

    protected boolean areGolemTagsValidForTask(Task ticket) {
        ISealEntity se = SealHandler.getSealEntity(golem.level(), ticket.getSealPos());
        if (se == null || se.getSeal() == null) return true;
        if (se.isLocked() && (golem.getOwnerId() == null || !golem.getOwnerId().toString().equals(se.getOwner()))) return false;
        if (se.getSeal().getRequiredTags() != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) return false;
        if (se.getSeal().getForbiddenTags() != null) for (GolemTrait tag : se.getSeal().getForbiddenTags()) if (golem.getProperties().getTraits().contains(tag)) return false;
        return true;
    }
}
