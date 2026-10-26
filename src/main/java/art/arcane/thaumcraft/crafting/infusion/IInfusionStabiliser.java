package art.arcane.thaumcraft.crafting.infusion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** TC6 api.crafting.IInfusionStabiliser: blocks that stabilise infusion when placed symmetrically around the matrix. */
public interface IInfusionStabiliser {
    boolean canStabaliseInfusion(Level world, BlockPos pos);
}
