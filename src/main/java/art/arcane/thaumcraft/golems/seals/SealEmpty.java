package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealEmpty: golems take matching items out of the container the seal is on. */
public class SealEmpty extends SealFiltered {
    int delay = new Random(System.nanoTime()).nextInt(30);
    int filterInc = 0;
    HashMap<Integer, ItemStack> cache = new HashMap<>();
    protected ISealConfigToggles.SealToggle[] props = {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "pcycle", "golem.prop.cycle"), new ISealConfigToggles.SealToggle(false, "pleave", "golem.prop.leave")};
    @Override public String getKey() { return "thaumcraft:empty"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(world, id) == null);
        if (delay++ % 20 != 0) return;
        ItemStack stack = InvHelper.findFirstMatchFromFilter(getInv(filterInc), isBlacklist(), InvHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face), seal.getSealPos().face, invFilter(props), props[5].value);
        if (!stack.isEmpty()) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(seal.getPriority());
            task.setLifespan((short) 5);
            TaskHandler.addTask(world, task);
            cache.put(task.getId(), stack);
        }
    }
    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        ItemStack stack = cache.get(task.getId());
        var f = invFilter(props);
        int sa = InvHelper.countTotalItemsIn(world, task.getSealPos().pos, task.getSealPos().face, stack == null ? ItemStack.EMPTY : stack, f);
        if (stack != null && !stack.isEmpty() && props[5].value && sa <= stack.getCount()) { stack = stack.copy(); stack.setCount(sa - 1); }
        if (stack != null && !stack.isEmpty()) {
            int limit = golem.canCarryAmount(stack);
            if (limit > 0) {
                ItemStack s = golem.holdItem(InvHelper.removeStackFrom(world, task.getSealPos().pos, task.getSealPos().face, InvHelper.copyLimitedStack(stack, limit), f, false));
                if (!s.isEmpty()) InvHelper.ejectStackAt(world, task.getSealPos().pos.relative(task.getSealPos().face), task.getSealPos().face.getOpposite(), s);
                golem.playSound(SoundEvents.ITEM_PICKUP, 0.125f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                golem.swingArm();
            }
        }
        cache.remove(task.getId());
        ++filterInc;
        task.setSuspended(true);
        return true;
    }
    @Override public boolean canGolemPerformTask(GolemEntity golem, Task task) { ItemStack s = cache.get(task.getId()); return s != null && !s.isEmpty() && golem.canCarry(s, true); }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return InvHelper.getItemHandlerAt(world, pos, side) != null; }
    public NonNullList<ItemStack> getInv(int c) { return super.getInv(); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_empty"); }
    @Override public int[] getGuiCategories() { return new int[]{1, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public void onTaskSuspension(Level world, Task task) { cache.remove(task.getId()); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
}
