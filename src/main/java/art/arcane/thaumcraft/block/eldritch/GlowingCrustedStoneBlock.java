package art.arcane.thaumcraft.block.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC4 "Glowing Crusted Stone": crusted stone with a yellow light in it, not as large as a full block; drops itself. */
public final class GlowingCrustedStoneBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);
    public GlowingCrustedStoneBlock(Properties p) { super(p); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
}
