package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/** TC6 SealButcher: when three or more of a kind of animal share the area, sends a fighter golem after one adult. */
public class SealButcher implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(200);
    boolean wait = false;
    @Override public String getKey() { return "thaumcraft:butcher"; }

    /** EntityAnimal or IAnimals (squid, bats...), not a monster, not tamed, not a golem, not a child. */
    public static boolean isAnimalLike(LivingEntity t) { return isAnimal(t); }
    static boolean isAnimal(LivingEntity t) { return t instanceof Animal || t instanceof WaterAnimal || t instanceof AgeableWaterCreature || t instanceof AmbientCreature; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 200 != 0 || wait) return;
        var area = GolemHelper.getBoundsForArea(seal);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!isValidTarget(target)) continue;
            List<? extends LivingEntity> same = world.getEntitiesOfClass(target.getClass(), area);
            Iterator<? extends LivingEntity> it = same.iterator();
            int count = 0;
            // TC6 counts the first three of the class without re-checking them
            while (it.hasNext() && count < 3) { it.next(); ++count; }
            if (count > 2) {
                Task task = new Task(seal.getSealPos(), target);
                task.setPriority(seal.getPriority());
                task.setLifespan((short) 10);
                TaskHandler.addTask(world, task);
                wait = true;
                break;
            }
        }
    }
    private boolean isValidTarget(LivingEntity t) {
        return isAnimal(t) && !(t instanceof Enemy) && !(t instanceof TamableAnimal ta && ta.isTame()) && !(t instanceof AbstractGolem) && !(t instanceof Animal && t.isBaby());
    }
    @Override
    public void onTaskStarted(Level world, GolemEntity golem, Task task) {
        if (task.getEntity() instanceof LivingEntity le && isValidTarget(le)) { golem.setTarget(le); golem.addRankXp(1); }
        task.setSuspended(true);
        wait = false;
    }
    @Override public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) { task.setSuspended(true); wait = false; return true; }
    @Override public boolean canGolemPerformTask(GolemEntity golem, Task task) { return true; }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_butcher"); }
    @Override public int[] getGuiCategories() { return new int[]{2, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.FIGHTER, GolemTrait.SMART}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public void onTaskSuspension(Level world, Task task) { wait = false; }
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) { wait = false; }
}
