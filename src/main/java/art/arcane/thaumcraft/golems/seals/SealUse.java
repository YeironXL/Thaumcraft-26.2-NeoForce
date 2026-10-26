package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemInteractionHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealUse: a deft golem left/right-clicks the block with a held (filtered) item, or an empty hand. */
public class SealUse extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(49);
    int watchedTask = Integer.MIN_VALUE;
    protected SealToggle[] props = {new SealToggle(true, "pmeta", "golem.prop.meta"), new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"), new SealToggle(false, "pmod", "golem.prop.mod"), new SealToggle(false, "pleft", "golem.prop.left"),
            new SealToggle(false, "pempty", "golem.prop.empty"), new SealToggle(false, "pemptyhand", "golem.prop.emptyhand"), new SealToggle(false, "psneak", "golem.prop.sneak"),
            new SealToggle(false, "ppro", "golem.prop.provision.wl")};
    @Override public String getKey() { return "thaumcraft:use"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 5 != 0) return;
        Task old = TaskHandler.getTask(world, watchedTask);
        if (old == null || old.isSuspended() || old.isCompleted()) {
            if (getToggles()[5].value != world.isEmptyBlock(seal.getSealPos().pos)) return;
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world, task);
            watchedTask = task.getId();
        }
    }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        if (getToggles()[5].value == world.isEmptyBlock(task.getPos())) {
            ItemStack clickStack = golem.getCarrying().get(0);
            if (!filter.get(0).isEmpty()) clickStack = InvHelper.findFirstMatchFromFilter(filter, filterSize, blacklist, golem.getCarrying(), invFilter(props));
            if (!clickStack.isEmpty() || props[6].value) {
                ItemStack ss = ItemStack.EMPTY;
                if (!clickStack.isEmpty()) { ss = clickStack.copy(); golem.dropItem(clickStack.copy()); }
                GolemInteractionHelper.golemClick(world, golem, task.getPos(), task.getSealPos().face, props[6].value ? ItemStack.EMPTY : ss, props[7].value, !getToggles()[4].value);
            }
        }
        task.setSuspended(true);
        return true;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        if (props[6].value) return true;
        boolean found = !InvHelper.findFirstMatchFromFilter(filter, filterSize, blacklist, golem.getCarrying(), invFilter(props)).isEmpty();
        if (!found && getToggles()[8].value && !blacklist && !getInv().get(0).isEmpty()) {
            ISealEntity se = SealHandler.getSealEntity(golem.level(), task.getSealPos());
            if (se != null) GolemHelper.requestProvisioning(golem.level(), se, getInv().get(0).copy());
        }
        return found;
    }
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return true; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_use"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{1, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.DEFT, GolemTrait.SMART}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
}
