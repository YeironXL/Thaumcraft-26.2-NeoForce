package art.arcane.thaumcraft.crafting.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** TC6 api.crafting.IInfusionStabiliserExt. */
public interface IInfusionStabiliserExt extends IInfusionStabiliser {
    float getStabilizationAmount(Level world, BlockPos pos);
    default boolean hasSymmetryPenalty(Level world, BlockPos pos1, BlockPos pos2) { return false; }
    default float getSymmetryPenalty(Level world, BlockPos pos) { return 0.0f; }
}
