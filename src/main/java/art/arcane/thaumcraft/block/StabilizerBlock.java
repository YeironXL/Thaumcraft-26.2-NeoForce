package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.StabilizerBlockEntity;
import art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliserExt;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 BlockStabilizer: source block of the inlay network; strong infusion stabiliser (0.25). */
public final class StabilizerBlock extends BaseEntityBlock implements IInfusionStabiliserExt {
    public static final MapCodec<StabilizerBlock> CODEC = simpleCodec(StabilizerBlock::new);
    public StabilizerBlock(Properties p) { super(p.strength(2.0f, 10.0f).sound(SoundType.STONE).noOcclusion().lightLevel(s -> 4)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StabilizerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STABILIZER.get(), StabilizerBlockEntity::tick);
    }
    /** colorMultiplier, same as the inlay. */
    public static int colorMultiplier(int charge) { return InlayBlock.colorMultiplier(charge); }
    @Override public boolean canStabaliseInfusion(Level world, BlockPos pos) { return true; }
    @Override public float getStabilizationAmount(Level world, BlockPos pos) { return 0.25f; }
}
