package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.ProvisionRequest;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealProvide: answers provisioning requests within 64 blocks from the container it is on, delivering to seals, blocks or entities. */
public class SealProvide extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(88);
    protected SealToggle[] props = {new SealToggle(true, "pmeta", "golem.prop.meta"), new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"), new SealToggle(false, "pmod", "golem.prop.mod"), new SealToggle(false, "psing", "golem.prop.single"),
            new SealToggle(false, "pleave", "golem.prop.leave")};
    @Override public String getKey() { return "thaumcraft:provider"; }
    @Override public int getFilterSize() { return 9; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        var list = GolemHelper.provisionRequests.get(world.dimension());
        if (delay % 100 == 0 && list != null) list.removeIf(pr -> pr.isInvalid() || pr.getLinkedTask() == null || pr.getLinkedTask().isSuspended() || pr.getLinkedTask().isCompleted() || pr.getTimeout() < System.currentTimeMillis());
        if (delay++ % 20 != 0) return;
        var inv = InvHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face);
        if (inv == null || list == null) return;
        var it = list.listIterator();
        BlockPos sp = seal.getSealPos().pos;
        while (it.hasNext()) {
            ProvisionRequest pr = it.next();
            if (pr.isInvalid()) { it.remove(); continue; }
            if (pr.getLinkedTask() != null) continue;
            if ((pr.getSeal() == null || pr.getSeal().getSealPos().pos.distSqr(sp) >= 4096.0)
                    && (pr.getEntity() == null || sp.distToCenterSqr(pr.getEntity().getX() - 0.5, pr.getEntity().getY() - 0.5, pr.getEntity().getZ() - 0.5) >= 4096.0)
                    && (pr.getPos() == null || sp.distSqr(pr.getPos()) >= 4096.0)) continue;
            NonNullList<ItemStack> stacks = NonNullList.withSize(1, pr.getStack());
            if (!InvHelper.findFirstMatchFromFilter(getInv(), getSizes(), blacklist, stacks, invFilter(props)).isEmpty()
                    && InvHelper.countTotalItemsIn(inv, pr.getStack(), InvHelper.InvFilter.STRICT) > (props[5].value ? 1 : 0)) {
                Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
                task.setPriority(pr.getSeal() != null ? pr.getSeal().getPriority() : 5);
                task.setLifespan((short) (pr.getSeal() != null ? 10 : 31000));
                TaskHandler.addTask(world, task);
                pr.setLinkedTask(task);
                task.setLinkedProvision(pr);
                break;
            }
        }
    }
    public boolean matchesFilters(ItemStack stack) { return InvHelper.matchesFilters(getInv(), blacklist, stack, invFilter(props)); }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        if (task.getLinkedProvision() != null) {
            if (task.getData() == 0) {
                var inv = InvHelper.getItemHandlerAt(world, task.getSealPos().pos, task.getSealPos().face);
                if (inv != null) {
                    ItemStack stack = task.getLinkedProvision().getStack().copy();
                    if (props[4].value) stack.setCount(1);
                    int sa;
                    if (!stack.isEmpty() && props[5].value && (sa = InvHelper.countTotalItemsIn(inv, stack, InvHelper.InvFilter.STRICT)) <= stack.getCount()) stack.setCount(sa - 1);
                    if (!stack.isEmpty()) {
                        int limit = golem.canCarryAmount(stack);
                        if (limit > 0) {
                            ItemStack s = golem.holdItem(InvHelper.removeStackFrom(inv, InvHelper.copyLimitedStack(stack, limit), InvHelper.InvFilter.STRICT, false));
                            if (!s.isEmpty()) InvHelper.ejectStackAt(world, task.getSealPos().pos.relative(task.getSealPos().face), task.getSealPos().face.getOpposite(), s);
                            golem.playSound(SoundEvents.ITEM_PICKUP, 0.125f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                            golem.addRankXp(1);
                            golem.swingArm();
                            ProvisionRequest pr2 = task.getLinkedProvision();
                            if (pr2.getEntity() != null || pr2.getPos() != null) {
                                Task task2 = pr2.getEntity() != null ? new Task(task.getSealPos(), pr2.getEntity()) : new Task(task.getSealPos(), pr2.getPos());
                                task2.setPriority(task.getPriority());
                                task2.setData(pr2.getEntity() != null ? 1 : 2);
                                task2.setLifespan((short) 31000);
                                TaskHandler.addTask(world, task2);
                                pr2.setLinkedTask(task2);
                                task2.setLinkedProvision(pr2);
                            }
                        }
                    }
                }
            } else {
                ProvisionRequest pr3 = task.getLinkedProvision();
                ItemStack cs = pr3.getStack();
                ItemStack s2 = golem.dropItem(cs);
                if (s2.getCount() < cs.getCount()) {
                    ItemStack ps = cs.copy();
                    ps.setCount(cs.getCount() - s2.getCount());
                    if (task.getData() == 1) GolemHelper.requestProvisioning(world, pr3.getEntity(), ps);
                    else GolemHelper.requestProvisioning(world, pr3.getPos(), pr3.getSide(), ps);
                }
                if (task.getData() == 1) InvHelper.dropItemAtEntity(world, s2, pr3.getEntity());
                else {
                    ItemStack back = InvHelper.ejectStackAt(world, pr3.getPos().relative(pr3.getSide()), pr3.getSide().getOpposite(), s2, true);
                    if (!back.isEmpty()) golem.holdItem(back);
                }
                golem.playSound(SoundEvents.ITEM_PICKUP, 0.125f, (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f);
                golem.swingArm();
                pr3.setInvalid(true);
            }
        }
        task.setSuspended(true);
        return true;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        ProvisionRequest pr = task.getLinkedProvision();
        boolean b = pr != null && ((pr.getSeal() != null && golem.isWithinHome(pr.getSeal().getSealPos().pos))
                || (pr.getEntity() != null && golem.isWithinHome(pr.getEntity().blockPosition()))
                || (pr.getPos() != null && golem.isWithinHome(pr.getPos())));
        if (task.getData() == 0) return b && areGolemTagsValidForTask(pr.getSeal(), golem) && !pr.getStack().isEmpty() && !golem.isCarrying(pr.getStack()) && golem.canCarry(pr.getStack(), true);
        return b && areGolemTagsValidForTask(pr.getSeal(), golem) && !pr.getStack().isEmpty() && golem.isCarrying(pr.getStack());
    }
    private boolean areGolemTagsValidForTask(ISealEntity se, GolemEntity golem) {
        if (se == null) return true;
        if (se.isLocked() && (golem.getOwnerId() == null || !golem.getOwnerId().toString().equals(se.getOwner()))) return false;
        if (se.getSeal().getRequiredTags() != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) return false;
        if (se.getSeal().getForbiddenTags() != null) for (GolemTrait tag : se.getSeal().getForbiddenTags()) if (golem.getProperties().getTraits().contains(tag)) return false;
        return true;
    }
    @Override public void onTaskSuspension(Level world, Task task) { if (task.getLinkedProvision() != null) task.getLinkedProvision().setLinkedTask(null); task.setLinkedProvision(null); }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return InvHelper.getItemHandlerAt(world, pos, side) != null; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_provider"); }
    @Override public int[] getGuiCategories() { return new int[]{1, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
}
