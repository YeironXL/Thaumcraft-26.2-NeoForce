package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** TC6 SealGuard: posts attack tasks for hostile mobs (and optionally animals / players) in its area. */
public class SealGuard implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(22);
    protected ISealConfigToggles.SealToggle[] props = {new ISealConfigToggles.SealToggle(true, "pmob", "golem.prop.mob"),
            new ISealConfigToggles.SealToggle(false, "panimal", "golem.prop.animal"), new ISealConfigToggles.SealToggle(false, "pplayer", "golem.prop.player")};
    @Override public String getKey() { return "thaumcraft:guard"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 20 != 0) return;
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, GolemHelper.getBoundsForArea(seal))) {
            if (isValidTarget(world, target)) {
                Task task = new Task(seal.getSealPos(), target);
                task.setPriority(seal.getPriority());
                task.setLifespan((short) 10);
                TaskHandler.addTask(world, task);
            }
        }
    }
    private boolean isValidTarget(Level world, LivingEntity target) {
        boolean valid = false;
        if (props[0].value && target instanceof Enemy) valid = true;
        if (props[1].value && SealButcher.isAnimal(target)) valid = true;
        if (props[2].value && world instanceof ServerLevel sl && sl.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP) && target instanceof Player) valid = true;
        return valid;
    }
    @Override
    public void onTaskStarted(Level world, GolemEntity golem, Task task) {
        if (task.getEntity() instanceof LivingEntity le && isValidTarget(world, le)) { golem.setTarget(le); golem.addRankXp(1); }
        task.setSuspended(true);
    }
    @Override public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) { task.setSuspended(true); return true; }
    @Override public boolean canGolemPerformTask(GolemEntity golem, Task task) { return !golem.isAlliedTo(task.getEntity()); }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_guard"); }
    @Override public int[] getGuiCategories() { return new int[]{2, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.FIGHTER}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
}
