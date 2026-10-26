package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.InfernalFurnaceBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockInfernalFurnace: the lava core of the 3x3x3 multiblock; breaking any part reverts the structure. */
public final class InfernalFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<InfernalFurnaceBlock> CODEC = simpleCodec(InfernalFurnaceBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    public static boolean ignore;

    public InfernalFurnaceBlock(Properties p) {
        super(p.strength(2.0f, 10.0f).sound(SoundType.STONE).noOcclusion().lightLevel(s -> 13).pushReaction(PushReaction.BLOCK).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new InfernalFurnaceBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.INFERNAL_FURNACE.get(), InfernalFurnaceBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    /** destroyFurnace: placeholders back to nether brick / obsidian, iron bars restored in front, core back to lava. */
    public static void destroyFurnace(Level world, BlockPos pos, BlockState state, BlockPos startpos) {
        if (ignore || world.isClientSide()) return;
        ignore = true;
        try {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (p.equals(startpos)) continue;
                BlockState bs = world.getBlockState(p);
                if (bs.is(ModBlocks.PLACEHOLDER_BRICK.get())) world.setBlock(p, Blocks.NETHER_BRICKS.defaultBlockState(), 3);
                if (bs.is(ModBlocks.PLACEHOLDER_OBSIDIAN.get())) world.setBlock(p, Blocks.OBSIDIAN.defaultBlockState(), 3);
            }
            BlockPos front = pos.relative(state.getValue(FACING).getOpposite());
            if (world.isEmptyBlock(front)) world.setBlock(front, Blocks.IRON_BARS.defaultBlockState(), 3);
            world.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
        } finally { ignore = false; }
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!level.getBlockState(pos).is(this)) destroyFurnace(level, pos, state, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        var m = entity.getDeltaMovement();
        double dx = 0, dz = 0;
        if (entity.getX() < pos.getX() + 0.3f) dx += 1.0e-4;
        if (entity.getX() > pos.getX() + 0.7f) dx -= 1.0e-4;
        if (entity.getZ() < pos.getZ() + 0.3f) dz += 1.0e-4;
        if (entity.getZ() > pos.getZ() + 0.7f) dz -= 1.0e-4;
        if (dx != 0 || dz != 0) entity.setDeltaMovement(m.x + dx, m.y, m.z + dz);
        if (world instanceof ServerLevel sl && entity.tickCount % 10 == 0) {
            if (entity instanceof ItemEntity item) {
                entity.setDeltaMovement(entity.getDeltaMovement().x, 0.025, entity.getDeltaMovement().z);
                if (entity.onGround() && world.getBlockEntity(pos) instanceof InfernalFurnaceBlockEntity te) {
                    var rest = te.addItemsToInventory(sl, item.getItem());
                    if (rest.isEmpty()) item.discard(); else item.setItem(rest);
                }
            } else if (entity instanceof LivingEntity && !entity.fireImmune()) {
                entity.hurtServer(sl, sl.damageSources().lava(), 3.0f);
                entity.igniteForSeconds(10);
            }
        }
    }
}
