package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** TC6 TileLampArcane: every 5 ticks, when unpowered, places an invisible glimmer light at a random dark spot it can see. */
public final class ArcaneLampBlockEntity extends BlockEntity {
    public ArcaneLampBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.LAMP_ARCANE.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, ArcaneLampBlockEntity te) {
        if (world.isClientSide() || world.getGameTime() % 5L != 0L || world.hasNeighborSignal(pos)) return;
        var r = world.getRandom();
        BlockPos bp = pos.offset(r.nextInt(16) - r.nextInt(16), r.nextInt(16) - r.nextInt(16), r.nextInt(16) - r.nextInt(16));
        BlockPos top = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, bp);
        if (bp.getY() > top.getY() + 4) bp = top.above(4);
        if (bp.getY() < world.getMinY() + 5) bp = new BlockPos(bp.getX(), world.getMinY() + 5, bp.getZ());
        if (world.isEmptyBlock(bp) && !world.getBlockState(bp).is(ModBlocks.EFFECT_GLIMMER.get()) && world.getBrightness(LightLayer.BLOCK, bp) < 11 && BlockUtils.hasLOS(world, pos, bp))
            world.setBlock(bp, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), 3);
    }

    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) { super.preRemoveSideEffects(pos, state); if (level != null && !level.isClientSide()) removeLights(); }

    public void removeLights() {
        for (int x = -15; x <= 15; ++x) for (int y = -15; y <= 15; ++y) for (int z = -15; z <= 15; ++z) {
            BlockPos bp = worldPosition.offset(x, y, z);
            if (level.getBlockState(bp).is(ModBlocks.EFFECT_GLIMMER.get())) level.removeBlock(bp, false);
        }
    }
}
