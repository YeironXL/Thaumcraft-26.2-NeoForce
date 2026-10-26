package art.arcane.thaumcraft.block.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 ITaintBlock; implementors also stand in for ThaumcraftMaterials.MATERIAL_TAINT. */
public interface ITaintBlock {
    void die(Level level, BlockPos pos, BlockState state);

    /** {@code getMaterial() == MATERIAL_TAINT}. */
    static boolean isTaint(BlockState s) { return s.getBlock() instanceof ITaintBlock; }
}
