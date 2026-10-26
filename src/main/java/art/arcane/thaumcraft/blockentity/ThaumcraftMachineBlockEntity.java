package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared persistence/tick shell for Thaumcraft machines.
 * The individual 1.12.2 TileEntity implementations will be split out later.
 */
public class ThaumcraftMachineBlockEntity extends BlockEntity {
    private int progress;
    private int heat;
    private int vis;

    public ThaumcraftMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE.get(), pos, state);
    }

    public int getProgress() { return progress; }
    public int getHeat() { return heat; }
    public int getVis() { return vis; }

    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Progress", progress);
        output.putInt("Heat", heat);
        output.putInt("Vis", vis);
    }

    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        progress = Math.max(0, input.getIntOr("Progress", 0));
        heat = Math.max(0, input.getIntOr("Heat", 0));
        vis = Math.max(0, input.getIntOr("Vis", 0));
    }

    public void setProgress(int value) { progress = Math.max(0, value); setChanged(); }
    public void setHeat(int value) { heat = Math.max(0, value); setChanged(); }
    public void setVis(int value) { vis = Math.max(0, value); setChanged(); }
}
