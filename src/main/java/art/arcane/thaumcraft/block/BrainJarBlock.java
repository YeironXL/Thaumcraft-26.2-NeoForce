package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.BrainJarBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockJar with TileJarBrain: right-clicking releases a random handful (up to 63) of the stored experience. */
public final class BrainJarBlock extends BaseEntityBlock {
    public static final MapCodec<BrainJarBlock> CODEC = simpleCodec(BrainJarBlock::new);
    public BrainJarBlock(Properties p) { super(p.strength(0.3F).sound(SoundType.GLASS).noOcclusion()); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return box(3, 0, 3, 13, 12, 13); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BrainJarBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BRAIN_JAR.get(), BrainJarBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof BrainJarBlockEntity te)) return InteractionResult.PASS;
        te.eatDelay = 40;
        if (world instanceof ServerLevel sl) {
            int n = sl.getRandom().nextInt(Math.min(te.xp + 1, 64));
            if (n > 0) {
                te.xp -= n;
                ExperienceOrb.award(sl, net.minecraft.world.phys.Vec3.atCenterOf(pos), n);
                te.sync();
            }
        } else {
            world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.get("jar"), SoundSource.BLOCKS, 0.2f, 1.0f, false);
        }
        return InteractionResult.SUCCESS;
    }
}
