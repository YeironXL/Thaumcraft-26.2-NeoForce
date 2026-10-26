package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.aura.NativeAura;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * TC6 BlockVisBattery: stores up to 10 vis. Unpowered it charges from a chunk above 90% of its base aura and gives vis
 * back when the chunk drops under 75%; powered it empties into the aura one point every 5 ticks. Glows with its charge
 * and drives a comparator.
 */
public final class VisBatteryBlock extends Block {
    public static final MapCodec<VisBatteryBlock> CODEC = simpleCodec(VisBatteryBlock::new);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);

    public VisBatteryBlock(Properties p) {
        super(p.strength(0.5F).sound(SoundType.STONE).randomTicks().lightLevel(s -> s.getValue(CHARGE)));
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(CHARGE); }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) { update(state, level, pos, rand); }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) { update(state, level, pos, rand); }

    /** updateTick. */
    private void update(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int charge = state.getValue(CHARGE);
        if (level.hasNeighborSignal(pos)) {
            if (charge > 0) {
                NativeAura.addVis(level, pos, 1.0f);
                level.setBlock(pos, state.setValue(CHARGE, charge - 1), 3);
                level.scheduleTick(pos, this, 5);
            }
            return;
        }
        float aura = NativeAura.vis(level, pos);
        int base = NativeAura.base(level, pos);
        if (charge < 10 && aura > base * 0.9 && aura > 1.0f) {
            NativeAura.drainVis(level, pos, 1.0f, false);
            level.setBlock(pos, state.setValue(CHARGE, charge + 1), 3);
            level.scheduleTick(pos, this, 100 + rand.nextInt(100));
        } else if (charge > 0 && aura < base * 0.75) {
            NativeAura.addVis(level, pos, 1.0f);
            level.setBlock(pos, state.setValue(CHARGE, charge - 1), 3);
            level.scheduleTick(pos, this, 20 + rand.nextInt(20));
        }
    }

    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation o, boolean moved) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) level.scheduleTick(pos, this, 1);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction dir) { return state.getValue(CHARGE); }
}
