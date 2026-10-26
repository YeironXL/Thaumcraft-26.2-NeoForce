package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity;
import art.arcane.thaumcraft.blockentity.MirrorBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockMirror (magic mirror and essentia mirror): a 2px pane hung on the face of a solid block, drawn by its tile
 * renderer. Item entities touching a magic mirror are sent through to its partner.
 */
public final class MirrorBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final MapCodec<MirrorBlock> CODEC = simpleCodec(p -> new MirrorBlock(p, false));
    public final boolean essentia;
    /** SoundsTC.JAR: every sound is the jar clink. */
    public static final net.minecraft.world.level.block.SoundType JAR = new net.neoforged.neoforge.common.util.DeferredSoundType(0.5f, 1.0f,
            () -> art.arcane.thaumcraft.registry.ModSounds.get("jar"), () -> art.arcane.thaumcraft.registry.ModSounds.get("jar"), () -> art.arcane.thaumcraft.registry.ModSounds.get("jar"),
            () -> art.arcane.thaumcraft.registry.ModSounds.get("jar"), () -> art.arcane.thaumcraft.registry.ModSounds.get("jar"));

    public MirrorBlock(Properties p, boolean essentia) {
        super(p.strength(0.1f).sound(JAR).noOcclusion());
        this.essentia = essentia;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    /** getRenderType INVISIBLE: only the tile renderer draws it (the block model carries just the particle texture). */
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return essentia ? new EssentiaMirrorBlockEntity(pos, state) : new MirrorBlockEntity(pos, state); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return essentia ? createTickerHelper(type, ModBlockEntities.MIRROR_ESSENTIA.get(), (l, p, s, te) -> MirrorBlockEntity.tick(l, p, s, te))
                : createTickerHelper(type, ModBlockEntities.MIRROR.get(), MirrorBlockEntity::tick);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case DOWN -> Block.box(0, 14, 0, 16, 16, 16);
            case UP -> Block.box(0, 0, 0, 16, 2, 16);
            case NORTH -> Block.box(0, 0, 14, 16, 16, 16);
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 2);
            case WEST -> Block.box(14, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 2, 16, 16);
        };
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getClickedFace());
        return canSurvive(s, ctx.getLevel(), ctx.getClickedPos()) ? s : null;
    }

    /** canPlaceBlockOnSide / neighborChanged: needs a solid face behind it. */
    @Override
    protected boolean canSurvive(BlockState s, LevelReader l, BlockPos p) {
        Direction f = s.getValue(FACING);
        BlockPos on = p.relative(f.getOpposite());
        return l.getBlockState(on).isFaceSturdy(l, on, f);
    }

    @Override
    protected BlockState updateShape(BlockState s, LevelReader l, ScheduledTickAccess t, BlockPos p, Direction d, BlockPos np, BlockState ns, RandomSource r) {
        return d == s.getValue(FACING).getOpposite() && !canSurvive(s, l, p) ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : super.updateShape(s, l, t, p, d, np, ns, r);
    }

    /** onEntityCollidedWithBlock: magic mirrors swallow item entities that are not on portal cooldown. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier eff, boolean precise) {
        if (!level.isClientSide() && !essentia && entity instanceof ItemEntity ie && ie.isAlive() && !ie.isOnPortalCooldown()
                && level.getBlockEntity(pos) instanceof MirrorBlockEntity taf)
            taf.transport(ie);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        BlockEntity te = level.getBlockEntity(pos);
        return te != null && te.triggerEvent(id, param);
    }
}
