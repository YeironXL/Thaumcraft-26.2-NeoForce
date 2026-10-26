package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 TileThaumatoriumTop: forwards essentia connections to the machine below. */
public final class ThaumatoriumTopBlockEntity extends BlockEntity implements EssentiaPort {
    public ThaumatoriumTopBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.THAUMATORIUM_TOP.get(), pos, state); }
    private ThaumatoriumBlockEntity base() { return level != null && level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumBlockEntity t ? t : null; }
    @Override public boolean connects(Direction side) { var b = base(); return b != null && b.connects(side); }
    @Override public int suction() { var b = base(); return b == null ? 0 : b.suction(); }
    @Override public String wanted() { var b = base(); return b == null ? "" : b.wanted(); }
    @Override public String aspect() { return ""; }
    @Override public String offered(String requested, Direction side) { return ""; }
    @Override public boolean extractOne(String aspect, Direction side) { return false; }
}
