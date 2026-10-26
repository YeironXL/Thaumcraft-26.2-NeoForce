package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.events.InfusionEnchantmentEvents;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemInteractionHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** TC6 SealLumber: fells trees in the area, one furthest log at a time. */
public class SealLumber implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(33);
    HashMap<Integer, Long> cache = new HashMap<>();
    @Override public String getKey() { return "thaumcraft:lumber"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(world, id) == null);
        ++delay;
        BlockPos p = GolemHelper.getPosInArea(seal, delay);
        if (!cache.containsValue(p.asLong()) && InfusionEnchantmentEvents.isWoodLog(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world, task);
            cache.put(task.getId(), p.asLong());
        }
    }
    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        if (cache.containsKey(task.getId()) && InfusionEnchantmentEvents.isWoodLog(world, task.getPos()) && world instanceof ServerLevel sl) {
            var fp = GolemInteractionHelper.fakePlayer(sl, "FakeThaumcraftGolem");
            fp.setPos(golem.getX(), golem.getY(), golem.getZ());
            golem.swingArm();
            if (InfusionEnchantmentEvents.breakFurthestBlock(sl, task.getPos(), world.getBlockState(task.getPos()), fp)) {
                task.setLifespan((short) Math.max(task.getLifespan(), 10L));
                golem.addRankXp(1);
                return false;
            }
            cache.remove(task.getId());
        }
        task.setSuspended(true);
        return true;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        if (cache.containsKey(task.getId()) && InfusionEnchantmentEvents.isWoodLog(golem.level(), task.getPos())) return true;
        task.setSuspended(true);
        return false;
    }
    @Override public void onTaskSuspension(Level world, Task task) { cache.remove(task.getId()); }
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_lumber"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.BREAKER, GolemTrait.SMART}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
}
