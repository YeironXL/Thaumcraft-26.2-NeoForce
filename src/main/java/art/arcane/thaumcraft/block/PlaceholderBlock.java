package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

/** TC6 BlockPlaceholder: invisible stand-in inside a multiblock; breaking a furnace placeholder reverts the infernal furnace. */
public final class PlaceholderBlock extends Block {
    public PlaceholderBlock(Properties p) { super(p.strength(2.5f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK)); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    private boolean furnacePart() { return this == ModBlocks.PLACEHOLDER_BRICK.get() || this == ModBlocks.PLACEHOLDER_OBSIDIAN.get(); }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (furnacePart() && !InfernalFurnaceBlock.ignore && !level.getBlockState(pos).is(this)) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockState s = level.getBlockState(p);
                if (s.is(ModBlocks.INFERNAL_FURNACE.get())) { InfernalFurnaceBlock.destroyFurnace(level, p.immutable(), s, pos); break; }
            }
        }
        if (!furnacePart() && !art.arcane.thaumcraft.golems.builder.GolemBuilderBlock.ignore && !level.getBlockState(pos).is(this)) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (level.getBlockState(p).is(ModBlocks.GOLEM_BUILDER.get())) { art.arcane.thaumcraft.golems.builder.GolemBuilderBlock.destroy(level, p.immutable(), pos); break; }
            }
        }
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
    /** Using a golem press part opens the press. */
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s, net.minecraft.world.level.Level world, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (furnacePart()) return net.minecraft.world.InteractionResult.PASS;
        if (!world.isClientSide()) for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (world.getBlockEntity(p) instanceof art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity te) { BlockPos bp = p.immutable(); player.openMenu(te, buf -> buf.writeBlockPos(bp)); break; }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
