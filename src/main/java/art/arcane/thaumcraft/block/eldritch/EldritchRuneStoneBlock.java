package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** TC4 rune room "rune stone": ancient stone with grey runes; floating purple glyphs appear when a player is near. */
public final class EldritchRuneStoneBlock extends BaseEntityBlock {
    public static final MapCodec<EldritchRuneStoneBlock> CODEC = simpleCodec(EldritchRuneStoneBlock::new);
    public EldritchRuneStoneBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new EldritchRuneStoneBlockEntity(pos, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ELDRITCH_RUNE_STONE.get(), EldritchRuneStoneBlockEntity::tick);
    }
    @Override public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        Player p = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EldritchRuneStoneBlockEntity.RANGE, false);
        if (p == null) return;
        // purple glyphs drifting in the air around the stone (ENCHANT glyphs tinted by the portal particle)
        for (int i = 0; i < 6; i++) {
            double x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 5, y = pos.getY() + 1 + r.nextDouble() * 2.5, z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 5;
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, (pos.getX() + 0.5 - x) * 0.2, 0.1, (pos.getZ() + 0.5 - z) * 0.2);
        }
        level.addParticle(ParticleTypes.WITCH, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 2, pos.getY() + 1.2, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 2, 0, 0.05, 0);
    }
}
