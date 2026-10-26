package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.world.outer.OuterLands;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC4 BlockEldritchPortal ("Eldritch Portal"): walking in travels to the Outer Lands maze, or back out of it. */
public final class EldritchPortalBlock extends BaseEntityBlock {
    public static final MapCodec<EldritchPortalBlock> CODEC = simpleCodec(EldritchPortalBlock::new);
    public EldritchPortalBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new EldritchPortalBlockEntity(pos, s); }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e, InsideBlockEffectApplier eff, boolean precise) {
        if (level.isClientSide() || e.isPassenger() || e.isVehicle() || !e.canUsePortal(false) || e.isOnPortalCooldown()) return;
        if (!(level.getBlockEntity(pos) instanceof EldritchPortalBlockEntity pe) || pe.mazeId() < 0) return;
        if (pe.exit()) OuterLands.leave(e, pe.mazeId()); else OuterLands.enter(e, pe.mazeId());
    }
    @Override public void animateTick(BlockState s, Level w, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 3; i++) w.addParticle(ParticleTypes.PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5), (r.nextDouble() - 0.5) * 2);
    }
}
