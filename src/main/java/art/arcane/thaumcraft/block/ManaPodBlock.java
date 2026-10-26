package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.ManaPodBlockEntity;
import art.arcane.thaumcraft.item.ManaBeanItem;
import art.arcane.thaumcraft.world.BiomeHandler;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC4 BlockManaPod recreation. Hangs under a greatwood/silverwood log (or any log inside a magical biome) and grows
 * through 8 stages on random ticks. An un-seeded pod picks an aspect from the biome (BiomeHandler.getRandomBiomeTag,
 * primal fallback). When ripening, a neighbouring pod with another aspect may cross-breed it into their compound.
 * Mature pods drop 1-3 mana beans of their aspect; unripe pods drop nothing.
 */
public final class ManaPodBlock extends BaseEntityBlock {
    public static final MapCodec<ManaPodBlock> CODEC = simpleCodec(ManaPodBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MATURE = 7;
    private static final String[] PRIMALS = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    private static final VoxelShape[] SHAPES = new VoxelShape[8];
    static {
        for (int i = 0; i < 8; i++) {
            double r = 1.5 + i * 0.6, h = 3 + i * 1.3;
            SHAPES[i] = Block.box(8 - r, 16 - h, 8 - r, 8 + r, 16, 8 + r);
        }
    }

    public ManaPodBlock(Properties p) {
        super(p.strength(0.5f).sound(SoundType.WOOD).noOcclusion().randomTicks().lightLevel(s -> s.getValue(AGE) == MATURE ? 4 : 0));
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AGE); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ManaPodBlockEntity(pos, state); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter w, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(AGE)]; }

    /** Valid host: the block above is a greatwood or silverwood log, or any log in a magical biome. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.is(ModBlocks.LOG_GREATWOOD.get()) || above.is(ModBlocks.LOG_SILVERWOOD.get())) return true;
        return above.is(BlockTags.LOGS) && level.getBiome(pos).is(BiomeHandler.MAGICAL);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        return dir == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, level, ticks, pos, dir, np, ns, r);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) { level.destroyBlock(pos, true); return; }
        if (state.getValue(AGE) < MATURE && random.nextInt(4) == 0) grow(state, level, pos, random);
    }

    /** One growth step (public for tests and bonemeal-like accelerators). */
    public static void grow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= MATURE || !(level.getBlockEntity(pos) instanceof ManaPodBlockEntity be)) return;
        if (be.aspect().isEmpty()) be.setAspect(biomeAspect(level, pos, random));
        if (age + 1 == MATURE) crossBreed(level, pos, be, random);
        level.setBlock(pos, state.setValue(AGE, age + 1), 2);
    }

    /** BiomeHandler.getRandomBiomeTag at the pod, or a random primal when the biome has no aspect. */
    public static String biomeAspect(LevelReader level, BlockPos pos, RandomSource random) {
        String tag = BiomeHandler.randomTag(level.getBiome(pos), random);
        return tag != null && AspectRegistry.get(tag) != null ? tag : PRIMALS[random.nextInt(PRIMALS.length)];
    }

    private static void crossBreed(ServerLevel level, BlockPos pos, ManaPodBlockEntity be, RandomSource random) {
        Aspect mine = AspectRegistry.get(be.aspect());
        if (mine == null || random.nextInt(3) != 0) return;
        List<Aspect> options = new ArrayList<>();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockEntity(pos.relative(d)) instanceof ManaPodBlockEntity other)) continue;
            Aspect theirs = AspectRegistry.get(other.aspect());
            if (theirs == null || theirs == mine) continue;
            for (Aspect a : AspectRegistry.all().values())
                if ((a.parentA() == mine && a.parentB() == theirs) || (a.parentA() == theirs && a.parentB() == mine)) options.add(a);
        }
        if (!options.isEmpty()) be.setAspect(options.get(random.nextInt(options.size())).id());
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (state.getValue(AGE) < MATURE) return List.of();
        String aspect = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ManaPodBlockEntity be ? be.aspect() : "";
        var origin = params.getOptionalParameter(LootContextParams.ORIGIN);
        if (aspect.isEmpty() && origin != null) aspect = biomeAspect(params.getLevel(), BlockPos.containing(origin), params.getLevel().getRandom());
        return List.of(ManaBeanItem.of(aspect, 1 + params.getLevel().getRandom().nextInt(3)));
    }
}
