package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class FocalManipulatorBlock extends BaseEntityBlock {
    private static final MapCodec<FocalManipulatorBlock> CODEC = simpleCodec(FocalManipulatorBlock::new);
    public FocalManipulatorBlock(Properties properties) { super(properties.strength(2.0f, 12.0f).sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion().requiresCorrectToolForDrops()); }
    @Override protected MapCodec<FocalManipulatorBlock> codec() { return CODEC; }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity table)player.openMenu(table, buf -> buf.writeBlockPos(pos));
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FocalManipulatorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FOCAL_MANIPULATOR.get(), FocalManipulatorBlockEntity::tick);
    }
}
