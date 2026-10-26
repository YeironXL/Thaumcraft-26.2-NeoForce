package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.RedstoneRelayBlockEntity;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 BlockRedstoneRelay: a diode (2 tick delay) that turns on when its input reaches the set threshold and then emits
 * the set output strength. Clicking the front-left dial raises the output, the rear-right dial raises the threshold.
 */
public final class RedstoneRelayBlock extends DiodeBlock implements EntityBlock {
    public static final MapCodec<RedstoneRelayBlock> CODEC = simpleCodec(RedstoneRelayBlock::new);
    public RedstoneRelayBlock(Properties p) {
        super(p.instabreak().sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }
    @Override protected MapCodec<? extends DiodeBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, POWERED); }
    @Override protected int getDelay(BlockState s) { return 2; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RedstoneRelayBlockEntity(pos, state); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, ctx.isSecondaryUseActive() ? d : d.getOpposite());
    }
    @Override protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        int pr = level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity te ? te.getIn() : 1;
        return getInputSignal(level, pos, state) >= pr;
    }
    @Override protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity te ? te.getOut() : 0;
    }

    /** TC6 collisionRayTrace traced the two dial cuboids too: they are part of the outline (picking) shape, not of the collision. */
    private static final java.util.Map<Direction, net.minecraft.world.phys.shapes.VoxelShape> OUTLINES = new java.util.EnumMap<>(Direction.class);
    private static net.minecraft.world.phys.shapes.VoxelShape outline(Direction f) {
        return OUTLINES.computeIfAbsent(f, d -> {
            int q = switch (d) { case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> 0; };
            var shape = Block.box(0, 0, 0, 16, 2, 16);
            for (double[] c : new double[][] {{-0.375, -0.375, -0.125, -0.125}, {-0.125, 0.125, 0.125, 0.375}}) {
                double x0 = c[0], z0 = c[1], x1 = c[2], z1 = c[3];
                for (int i = 0; i < q; i++) { double a = x0, b = x1; x0 = -z1; x1 = -z0; z0 = a; z1 = b; } // inverse of dial()'s quarter turns
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, net.minecraft.world.phys.shapes.Shapes.box(x0 + 0.5, 0.0625, z0 + 0.5, x1 + 0.5, 0.25, z1 + 0.5));
            }
            return shape;
        });
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return outline(s.getValue(FACING)); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return super.getShape(s, l, p, c); }

    /** Which dial (0 = output, 1 = threshold) contains the hit point; cuboids from TileRedstoneRelay rotated by facing. */
    public static int dial(BlockState state, BlockPos pos, Vec3 hit) {
        double x = hit.x - (pos.getX() + 0.5), y = hit.y - pos.getY(), z = hit.z - (pos.getZ() + 0.5);
        int q = switch (state.getValue(FACING)) { case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> 0; };
        // Undo the blockstate's clockwise quarter turns so the point can be tested against the south-facing cuboids.
        double rx = x, rz = z;
        for (int i = 0; i < q; i++) { double t = rx; rx = rz; rz = -t; }
        double e = 0.02;
        if (y >= 0.0625 - e && y <= 0.25 + e) {
            if (rx >= -0.375 - e && rx <= -0.125 + e && rz >= -0.375 - e && rz <= -0.125 + e) return 0;
            if (rx >= -0.125 - e && rx <= 0.125 + e && rz >= 0.125 - e && rz <= 0.375 + e) return 1;
        }
        return -1;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        int d = dial(state, pos, hit.getLocation());
        if (d < 0 || !(world.getBlockEntity(pos) instanceof RedstoneRelayBlockEntity te)) return InteractionResult.PASS;
        if (!world.isClientSide()) {
            if (d == 0) te.increaseOut(); else te.increaseIn();
            world.playSound(null, pos, ModSounds.get("key"), SoundSource.BLOCKS, 0.5f, 1.0f);
            checkTickOnNeighbor(world, pos, state);
            updateNeighborsInFront(world, pos, state);
        }
        return InteractionResult.SUCCESS;
    }
}
