package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * TC6 BlockArcaneWorkbenchCharger: sits on an arcane workbench or focal manipulator and lets it draw vis from the 3x3
 * surrounding chunks. Breaks off when the block below goes away; using it opens the device below.
 */
public final class WorkbenchChargerBlock extends Block {
    public WorkbenchChargerBlock(Properties p) { super(p.strength(1.25f, 10.0f).sound(SoundType.WOOD).noOcclusion()); }

    private static boolean supported(LevelReader l, BlockPos pos) {
        BlockState below = l.getBlockState(pos.below());
        return below.is(ModBlocks.ARCANE_WORKBENCH.get()) || below.is(ModBlocks.WAND_WORKBENCH.get());
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos pos) { return supported(l, pos); }
    @Override
    protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos pos, Direction d, BlockPos np, BlockState ns, RandomSource r) {
        return d == Direction.DOWN && !supported(l, pos) ? Blocks.AIR.defaultBlockState() : s;
    }
    @Override
    protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState old, boolean moved) {
        // syncTile(true) of the device below so its client view of the aura source updates.
        if (!level.isClientSide()) level.sendBlockUpdated(pos.below(), level.getBlockState(pos.below()), level.getBlockState(pos.below()), 3);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!supported(level, pos)) return InteractionResult.SUCCESS;
        BlockPos below = pos.below();
        return level.getBlockState(below).useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), below, false));
    }
}
