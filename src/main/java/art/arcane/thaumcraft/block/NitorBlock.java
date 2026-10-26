package art.arcane.thaumcraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floating light/heat source, using native colored particles rather than the old particle engine. */
public final class NitorBlock extends Block {
    private static final MapCodec<NitorBlock> CODEC=simpleCodec(NitorBlock::new);
    public NitorBlock(Properties properties) { super(properties.strength(0.1F).sound(SoundType.WOOL).noCollision().noOcclusion().lightLevel(s -> 15)); }
    @Override protected MapCodec<NitorBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return Block.box(5.3,5.3,5.3,10.6,10.6,10.6); }
    public int color() {
        String name=BuiltInRegistries.BLOCK.getKey(this).getPath().substring("nitor_".length());
        if (name.equals("lightblue")) name="light_blue";
        if (name.equals("silver")) name="light_gray";
        return DyeColor.byName(name,DyeColor.YELLOW).getTextureDiffuseColor() & 0xFFFFFF;
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        level.addParticle(new DustParticleOptions(color(),0.8F),pos.getX()+0.5+(random.nextDouble()-0.5)*0.12,
                pos.getY()+0.5,pos.getZ()+0.5+(random.nextDouble()-0.5)*0.12,0,0.02,0);
    }
}
