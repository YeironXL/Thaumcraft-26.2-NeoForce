package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** TC4 "Crusted Opening" (tile.eldritch.crabspawner): a crusted mound with a black hole that Eldritch Crabs crawl out of. */
public final class EldritchCrabSpawnerBlock extends BaseEntityBlock {
    public static final MapCodec<EldritchCrabSpawnerBlock> CODEC = simpleCodec(EldritchCrabSpawnerBlock::new);
    public EldritchCrabSpawnerBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new EldritchCrabSpawnerBlockEntity(pos, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ELDRITCH_CRAB_SPAWNER.get(), EldritchCrabSpawnerBlockEntity::tick);
    }
    /** dark smoke seeping out of the hole */
    @Override public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.3 + r.nextDouble() * 0.4, pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.01, 0);
        if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (r.nextDouble() - 0.5), (r.nextDouble() - 0.5), (r.nextDouble() - 0.5));
    }
}
