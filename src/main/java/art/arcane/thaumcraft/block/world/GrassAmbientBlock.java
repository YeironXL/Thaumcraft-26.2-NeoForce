package art.arcane.thaumcraft.block.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 BlockGrassAmbient: a grass block that, in the dark, sets wispy motes drifting over the plain grass around it. */
public final class GrassAmbientBlock extends GrassBlock {
    public static final MapCodec<GrassBlock> CODEC = simpleCodec(GrassAmbientBlock::new);
    public GrassAmbientBlock(Properties p) { super(p.strength(0.6F).sound(SoundType.GRASS).randomTicks()); }
    @Override public MapCodec<GrassBlock> codec() { return CODEC; }

    @Override public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        int i = world.getBrightness(LightLayer.SKY, pos.above()) - world.getSkyDarken();
        // TC6 also scales by cos(sun angle); getSkyDarken already darkens the sky light at night in 26.2.
        i = Mth.clamp(i, 0, 15);
        if (4 + i * 2 >= 1 + rand.nextInt(13)) return;
        BlockPos pp = pos.offset(Mth.nextInt(rand, -8, 8), 5, Mth.nextInt(rand, -8, 8));
        for (int q = 0; q < 10 && pp.getY() > 50 && !world.getBlockState(pp).is(Blocks.GRASS_BLOCK); ++q) pp = pp.below();
        if (world.getBlockState(pp).is(Blocks.GRASS_BLOCK))
            art.arcane.thaumcraft.client.fx.FXDispatcher.drawWispyMotes(pp.getX() + rand.nextFloat(), pp.getY() + 1.05, pp.getZ() + rand.nextFloat(), 0, 0, 0, 400, -0.01f);
    }
}
