package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockArcaneEar / BlockArcaneEarToggle: mounted on a solid face; clicking raises its note and plays it. */
public final class ArcaneEarBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public static final MapCodec<ArcaneEarBlock> CODEC = simpleCodec(p -> new ArcaneEarBlock(p, false));
    public final boolean toggle;

    public ArcaneEarBlock(Properties p, boolean toggle) {
        super(p.strength(1.0f, 20.0f).sound(SoundType.WOOD).noOcclusion());
        this.toggle = toggle;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ArcaneEarBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ARCANE_EAR.get(), ArcaneEarBlockEntity::tick);
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case DOWN -> Block.box(2, 10, 2, 14, 16, 14);
            case UP -> Block.box(2, 0, 2, 14, 6, 14);
            case NORTH -> Block.box(2, 2, 10, 14, 14, 16);
            case SOUTH -> Block.box(2, 2, 0, 14, 14, 6);
            case WEST -> Block.box(10, 2, 2, 16, 14, 14);
            case EAST -> Block.box(0, 2, 2, 6, 14, 14);
        };
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getClickedFace()).setValue(ENABLED, false); }
    @Override protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        Direction f = s.getValue(FACING);
        BlockPos on = p.relative(f.getOpposite());
        return l.getBlockState(on).isFaceSturdy(l, on, f);
    }
    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) {
        if (!l.isClientSide() && l.getBlockEntity(pos) instanceof ArcaneEarBlockEntity te) te.updateTone();
    }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (l.isClientSide()) return;
        if (l.getBlockEntity(pos) instanceof ArcaneEarBlockEntity te) te.updateTone();
        if (!canSurvive(s, l, pos)) l.destroyBlock(pos, true);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof ArcaneEarBlockEntity te) { te.changePitch(); te.triggerNote(true); }
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean isSignalSource(BlockState s) { return true; }
    @Override protected int getSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) { return s.getValue(ENABLED) ? 15 : 0; }
    @Override protected int getDirectSignal(BlockState s, BlockGetter l, BlockPos p, Direction d) { return s.getValue(ENABLED) ? 15 : 0; }

    /** Plays the note: TC6 eventReceived (instrument index, note 0-24). */
    @Override
    protected boolean triggerEvent(BlockState state, Level world, BlockPos pos, int instrument, int note) {
        var all = NoteBlockInstrument.values();
        var inst = instrument < 0 || instrument >= all.length ? NoteBlockInstrument.HARP : all[instrument];
        float pitch = (float) Math.pow(2.0, (note - 12) / 12.0);
        world.playSound(null, pos, inst.getSoundEvent().value(), SoundSource.BLOCKS, 3.0f, pitch);
        world.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, note / 24.0, 0.0, 0.0);
        return true;
    }

    public static void setEnabled(Level world, BlockPos pos, BlockState s, boolean on) {
        if (!(s.getBlock() instanceof ArcaneEarBlock)) return;
        world.setBlock(pos, s.setValue(ENABLED, on), 3);
        world.updateNeighborsAt(pos, s.getBlock());
        world.updateNeighborsAt(pos.relative(s.getValue(FACING).getOpposite()), s.getBlock());
    }
}
