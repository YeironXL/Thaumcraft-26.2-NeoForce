package art.arcane.thaumcraft.golems.tasks;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** TC6 TaskHandler: the per-dimension pool of open tasks golems pick from (nearest first, minus 256 per priority point). */
public final class TaskHandler {
    static final int TASK_LIMIT = 10000;
    public static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<Integer, Task>> tasks = new ConcurrentHashMap<>();

    public static void addTask(Level level, Task ticket) {
        var dc = getTasks(level);
        if (dc.size() > TASK_LIMIT) {
            try { Iterator<Task> i = dc.values().iterator(); if (i.hasNext()) { i.next(); i.remove(); } } catch (Exception ignored) {}
        }
        dc.put(ticket.getId(), ticket);
    }
    public static Task getTask(Level level, int id) { return getTasks(level).get(id); }
    public static ConcurrentHashMap<Integer, Task> getTasks(Level level) { return tasks.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>()); }

    private static ArrayList<Task> sorted(Level level, UUID uuid, Entity golem, int type) {
        ArrayList<Task> out = new ArrayList<>();
        outer:
        for (Task ticket : getTasks(level).values()) {
            if (ticket.isReserved() || ticket.getType() != type) continue;
            if (uuid != null && ticket.getGolemUUID() != null && !uuid.equals(ticket.getGolemUUID())) continue;
            if (type == 1 && (ticket.getEntity() == null || !ticket.getEntity().isAlive())) { ticket.setSuspended(true); continue; }
            if (out.isEmpty()) { out.add(ticket); continue; }
            double d = ticket.getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ()) - ticket.getPriority() * 256;
            for (int a = 0; a < out.size(); a++) {
                double d2 = out.get(a).getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ()) - out.get(a).getPriority() * 256;
                if (d < d2) { out.add(a, ticket); continue outer; }
            }
            out.add(ticket);
        }
        return out;
    }
    public static ArrayList<Task> getBlockTasksSorted(Level level, UUID uuid, Entity golem) { return sorted(level, uuid, golem, 0); }
    public static ArrayList<Task> getEntityTasksSorted(Level level, UUID uuid, Entity golem) { return sorted(level, uuid, golem, 1); }

    public static void completeTask(Task task, GolemEntity golem) {
        if (task.isCompleted() || task.isSuspended()) return;
        ISealEntity se = SealHandler.getSealEntity(golem.level(), task.getSealPos());
        if (se != null) task.setCompletion(se.getSeal().onTaskCompletion(golem.level(), golem, task));
        else task.setCompletion(true);
    }

    public static void clearSuspendedOrExpiredTasks(Level level) {
        var temp = new ConcurrentHashMap<Integer, Task>();
        for (Task ticket : getTasks(level).values()) {
            if (!ticket.isSuspended() && ticket.getLifespan() > 0L) {
                ticket.setLifespan((short) (ticket.getLifespan() - 1L));
                temp.put(ticket.getId(), ticket);
            } else {
                ISealEntity se = SealHandler.getSealEntity(level, ticket.getSealPos());
                if (se != null) se.getSeal().onTaskSuspension(level, ticket);
            }
        }
        tasks.put(level.dimension(), temp);
    }
    private TaskHandler() {}
}
