package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealPickup: posts entity tasks for loose items on the ground in its area; the golem picks them up. */
public class SealPickup extends SealFiltered implements ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(100);
    HashMap<Integer, Integer> itemEntities = new HashMap<>();
    protected ISealConfigToggles.SealToggle[] props = {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod")};
    @Override public String getKey() { return "thaumcraft:pickup"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 5 != 0) return;
        var area = GolemHelper.getBoundsForArea(seal);
        for (ItemEntity ent : world.getEntitiesOfClass(ItemEntity.class, area)) {
            if (ent != null && ent.onGround() && !ent.hasPickUpDelay() && !ent.getItem().isEmpty() && !itemEntities.containsValue(ent.getId())) {
                ItemStack stack = InvHelper.findFirstMatchFromFilter(filter, filterSize, isBlacklist(), NonNullList.withSize(1, ent.getItem()), invFilter(props));
                if (!stack.isEmpty()) {
                    Task task = new Task(seal.getSealPos(), ent);
                    task.setPriority(seal.getPriority());
                    itemEntities.put(task.getId(), ent.getId());
                    TaskHandler.addTask(world, task);
                    break;
                }
            }
        }
        if (delay % 100 != 0) {
            Iterator<Integer> it = itemEntities.values().iterator();
            while (it.hasNext()) {
                Entity e = world.getEntity(it.next());
                if (e != null && e.isAlive()) continue;
                try { it.remove(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        ItemEntity ei = getItemEntity(world, task);
        if (ei != null && !ei.getItem().isEmpty()) {
            ItemStack stack = InvHelper.findFirstMatchFromFilter(filter, filterSize, isBlacklist(), NonNullList.withSize(1, ei.getItem()), invFilter(props));
            if (!stack.isEmpty()) {
                ItemStack is = golem.holdItem(ei.getItem());
                if (!is.isEmpty()) ei.setItem(is);
                else ei.discard();
                golem.playSound(SoundEvents.ITEM_PICKUP, 0.125f, ((world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.7f + 1.0f) * 2.0f);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        itemEntities.remove(task.getId());
        for (Task ticket : TaskHandler.getEntityTasksSorted(world, null, golem)) {
            if (itemEntities.containsKey(ticket.getId()) && ticket.canGolemPerformTask(golem) && golem.isWithinHome(ticket.getEntity().blockPosition())) {
                golem.setTask(ticket);
                golem.getTask().setReserved(true);
                golem.emote(5);
                break;
            }
        }
        return true;
    }
    protected ItemEntity getItemEntity(Level world, Task task) {
        Integer ei = itemEntities.get(task.getId());
        if (ei != null && world.getEntity(ei) instanceof ItemEntity ie) return ie;
        return null;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        ItemEntity ei = getItemEntity(golem.level(), task);
        if (ei == null) return false;
        if (!ei.isAlive()) { task.setSuspended(true); return false; }
        return golem.canCarry(ei.getItem(), true);
    }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_pickup"); }
    @Override public int[] getGuiCategories() { return new int[]{2, 1, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
}
