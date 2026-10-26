package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

/** Native six-direction attachment, with TC6 aspect drops and aura-driven growth. */
public final class CrystalClusterBlock extends AmethystClusterBlock {
    public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);
    public static final IntegerProperty GENERATION = IntegerProperty.create("generation", 1, 4);
    public static final MapCodec<CrystalClusterBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("aspect").forGetter(b -> b.aspect), propertiesCodec()).apply(i, CrystalClusterBlock::new));
    private final String aspect;
    public CrystalClusterBlock(String aspect, BlockBehaviour.Properties properties) {
        super(12, 1, properties.strength(0.25F).sound(SoundType.GLASS).noOcclusion().noCollision().randomTicks());
        this.aspect = aspect;
        registerDefaultState(defaultBlockState().setValue(SIZE, 0).setValue(GENERATION, 1));
    }
    @Override public MapCodec<AmethystClusterBlock> codec() { return CODEC.xmap(b -> b, b -> (CrystalClusterBlock)b); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(SIZE, GENERATION);
    }
    /** BlockUtils.isBlockTouching(world, pos, Material.ROCK, true): a solid rock face on any side. */
    public static boolean touchingRock(LevelReader level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            var n = pos.relative(d); var st = level.getBlockState(n);
            if (st.is(BlockTags.MINEABLE_WITH_PICKAXE) && st.requiresCorrectToolForDrops() && st.isFaceSturdy(level, n, d.getOpposite())) return true;
        }
        return false;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return touchingRock(level, pos); }
    /** Orient the model toward a supporting face (TC6 drew a cluster on every touching rock face). */
    public BlockState attached(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction d : Direction.values()) {
            var n = pos.relative(d.getOpposite()); var st = level.getBlockState(n);
            if (st.is(BlockTags.MINEABLE_WITH_PICKAXE) && st.isFaceSturdy(level, n, d)) return state.setValue(FACING, d);
        }
        return state;
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(CrystalEssenceItem.of(aspect, state.getValue(SIZE) + 1));
    }
    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int generation = state.getValue(GENERATION);
        if (rand.nextInt(3 + generation) != 0) return;
        final int threshold = 10;
        int growth = state.getValue(SIZE);
        boolean flux = aspect.equals("vitium");
        float pool = flux ? NativeAura.flux(level, pos) : NativeAura.vis(level, pos);
        if (pool <= threshold) {
            if (growth > 0) {
                level.setBlock(pos, state.setValue(SIZE, growth - 1), 3);
                if (flux) NativeAura.addFlux(level, pos, threshold); else NativeAura.addVis(level, pos, threshold);
            } else if (touchingSelf(level, pos)) {
                level.removeBlock(pos, false);
                if (flux) NativeAura.addFlux(level, pos, threshold); else NativeAura.addVis(level, pos, threshold);
            }
        } else if (pool > NativeAura.base(level, pos) + threshold) {
            if (growth < 3 && growth < 5 - generation + pos.asLong() % 3L) {
                if ((flux ? NativeAura.drainFlux(level, pos, threshold, false) : NativeAura.drainVis(level, pos, threshold, false)) > 0)
                    level.setBlock(pos, state.setValue(SIZE, growth + 1), 3);
            } else if (generation < 4) {
                BlockPos p2 = spread(level, pos, rand);
                if (p2 != null && (flux ? NativeAura.drainFlux(level, pos, threshold, false) : NativeAura.drainVis(level, pos, threshold, false)) > 0) {
                    if (rand.nextInt(6) == 0) --generation;
                    level.setBlock(p2, attached(level, p2, defaultBlockState().setValue(GENERATION, Math.max(1, generation + 1))), 3);
                }
            }
        }
    }
    private boolean touchingSelf(ServerLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) if (level.getBlockState(pos.relative(d)).is(this)) return true;
        return false;
    }
    /** BlockCrystal.spreadCrystal. */
    private static BlockPos spread(ServerLevel world, BlockPos pos, RandomSource r) {
        BlockPos t = pos.offset(r.nextInt(3) - 1, r.nextInt(3) - 1, r.nextInt(3) - 1);
        if (t.equals(pos)) return null;
        var bs = world.getBlockState(t);
        if (bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced()) && r.nextInt(16) == 0 && touchingRock(world, t)) return t;
        return null;
    }
}
