package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ArcaneWorkbenchBlock extends BaseEntityBlock {
    private static final MapCodec<ArcaneWorkbenchBlock> CODEC = simpleCodec(ArcaneWorkbenchBlock::new);
    public ArcaneWorkbenchBlock(Properties properties) { super(properties.strength(2.5F).sound(SoundType.WOOD).noOcclusion()); }
    @Override protected MapCodec<ArcaneWorkbenchBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneWorkbenchBlockEntity(pos,state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity bench) player.openMenu(bench);
        return InteractionResult.SUCCESS;
    }
}
