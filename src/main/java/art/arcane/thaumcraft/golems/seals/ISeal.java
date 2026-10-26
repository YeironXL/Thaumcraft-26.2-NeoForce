package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** TC6 thaumcraft.api.golems.seals.ISeal (the GUI hooks are replaced by the common seal menu). */
public interface ISeal {
    String getKey();
    boolean canPlaceAt(Level world, BlockPos pos, Direction side);
    void tickSeal(Level world, ISealEntity seal);
    void onTaskStarted(Level world, GolemEntity golem, Task task);
    boolean onTaskCompletion(Level world, GolemEntity golem, Task task);
    void onTaskSuspension(Level world, Task task);
    boolean canGolemPerformTask(GolemEntity golem, Task task);
    void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg);
    void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg);
    /** Item sprite (thaumcraft:item/seals/...) shown on the block face. */
    Identifier getSealIcon();
    void onRemoval(Level world, BlockPos pos, Direction side);
    GolemTrait[] getRequiredTags();
    GolemTrait[] getForbiddenTags();
}
