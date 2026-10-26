package art.arcane.thaumcraft.block.eldritch;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** TC4 BlockEldritch obelisk parts (base, body, capstone); ANCIENT = the ancient-stone obelisk of the Outer Lands exit. */
public final class EldritchObeliskBlock extends Block {
    public static final BooleanProperty ANCIENT = BooleanProperty.create("ancient");
    public EldritchObeliskBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(ANCIENT, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ANCIENT); }
    @Override public void animateTick(BlockState s, net.minecraft.world.level.Level w, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource r) {
        if (r.nextInt(4) == 0) w.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, -0.3, 0);
    }
}
