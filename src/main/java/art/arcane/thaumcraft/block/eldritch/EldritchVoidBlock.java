package art.arcane.thaumcraft.block.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC4 "Portal to Nothing": starry void in the entry room pits; 8 hearts of armour-ignoring damage, destroys items. */
public final class EldritchVoidBlock extends Block {
    public EldritchVoidBlock(Properties p) { super(p); }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected void entityInside(BlockState s, Level level, BlockPos pos, Entity e, InsideBlockEffectApplier eff, boolean precise) {
        if (!(level instanceof ServerLevel sl)) return;
        if (e instanceof ItemEntity) { e.discard(); return; }
        if (e instanceof LivingEntity le && le.tickCount % 10 == 0) le.hurtServer(sl, sl.damageSources().fellOutOfWorld(), 16.0f);
    }
}
