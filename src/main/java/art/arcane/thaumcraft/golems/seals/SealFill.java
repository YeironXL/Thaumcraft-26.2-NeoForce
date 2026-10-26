package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealFill: golems carrying matching items put them into the container the seal is on (or drop them in front). */
public class SealFill extends SealFiltered {
    int delay = new Random(System.nanoTime()).nextInt(50);
    int watchedTask = Integer.MIN_VALUE;
    protected ISealConfigToggles.SealToggle[] props = {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "pexist", "golem.prop.exist")};
    @Override public String getKey() { return "thaumcraft:fill"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 20 != 0) return;
        Task old = TaskHandler.getTask(world, watchedTask);
        if (old == null || old.isReserved() || old.isSuspended() || old.isCompleted()) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world, task);
            watchedTask = task.getId();
        }
    }
    @Override
    public void onTaskStarted(Level world, GolemEntity golem, Task task) {
        ISealEntity se = SealHandler.getSealEntity(world, task.getSealPos());
        if (se != null && !se.isStoppedByRedstone(world)) {
            Task nt = new Task(task.getSealPos(), task.getSealPos().pos);
            nt.setPriority(se.getPriority());
            TaskHandler.addTask(world, nt);
            watchedTask = nt.getId();
        }
    }
    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        var f = invFilter(props);
        var tuple = InvHelper.findFirstMatchFromFilterTuple(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), f);
        if (!tuple.stack().isEmpty()) {
            var inv = InvHelper.getItemHandlerAt(world, task.getSealPos().pos, task.getSealPos().face);
            int limit = tuple.stack().getCount();
            if (hasStacksizeLimiters() && tuple.size() > 0) {
                int c = inv == null ? InvHelper.countStackInWorld(world, task.getSealPos().pos, tuple.stack(), 1.5, f) : InvHelper.countTotalItemsIn(inv, tuple.stack(), f);
                limit = c < tuple.size() ? tuple.size() - c : 0;
            }
            if (limit > 0) {
                ItemStack t = tuple.stack().copy();
                t.setCount(limit);
                ItemStack s = golem.dropItem(t);
                if (inv == null) {
                    Direction face = task.getSealPos().face;
                    BlockPos p = task.getSealPos().pos;
                    ItemEntity ie = new ItemEntity(world, p.getX() + 0.5 + face.getStepX(), p.getY() + 0.5 + face.getStepY(), p.getZ() + 0.5 + face.getStepZ(), s);
                    ie.setDeltaMovement(ie.getDeltaMovement().multiply(0.2, 0.5, 0.2));
                    world.addFreshEntity(ie);
                } else golem.holdItem(InvHelper.insertStacked(inv, s, false));
                golem.playSound(SoundEvents.ITEM_PICKUP, 0.125f, (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f);
                golem.addRankXp(1);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        var f = invFilter(props);
        var tuple = InvHelper.findFirstMatchFromFilterTuple(getInv(), getSizes(), isBlacklist(), golem.getCarrying(), f);
        if (tuple.stack().isEmpty()) return false;
        Level world = golem.level();
        var inv = InvHelper.getItemHandlerAt(world, task.getSealPos().pos, task.getSealPos().face);
        if (inv != null) {
            if (props[4].value && InvHelper.countTotalItemsIn(inv, tuple.stack(), f) <= 0) return false;
            if (InvHelper.hasRoomForSome(world, task.getSealPos().pos, task.getSealPos().face, tuple.stack())) {
                if (!hasStacksizeLimiters() || tuple.size() <= 0) return true;
                return InvHelper.countTotalItemsIn(inv, tuple.stack(), f) < tuple.size();
            }
            return false;
        }
        return !hasStacksizeLimiters() || tuple.size() <= 0 || InvHelper.countStackInWorld(world, task.getSealPos().pos, tuple.stack(), 1.5, f) < tuple.size();
    }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_fill"); }
    @Override public int[] getGuiCategories() { return new int[]{1, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public boolean hasStacksizeLimiters() { return !isBlacklist(); }
}
