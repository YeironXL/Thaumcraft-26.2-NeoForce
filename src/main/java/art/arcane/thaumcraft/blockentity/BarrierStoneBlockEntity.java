package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** TC6 TileBarrierStone: pushes airborne creatures back and keeps the two barrier blocks above in place while unpowered. */
public final class BarrierStoneBlockEntity extends BlockEntity {
    private int count;
    public BarrierStoneBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.BARRIER_STONE.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, BarrierStoneBlockEntity te) {
        if (te.count == 0) te.count = world.getRandom().nextInt(100);
        if (te.count % 5 == 0 && !world.hasNeighborSignal(pos)) {
            for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1).inflate(0.1))) {
                if (!e.onGround() && !(e instanceof Player)) {
                    e.push(-Mth.sin((e.getYRot() + 180.0f) * Mth.DEG_TO_RAD) * 0.2f, -0.1, Mth.cos((e.getYRot() + 180.0f) * Mth.DEG_TO_RAD) * 0.2f);
                    e.hurtMarked = true;
                }
            }
        }
        if (++te.count % 100 == 0) {
            for (int a = 1; a <= 2; a++) {
                BlockPos up = pos.above(a);
                if (!world.getBlockState(up).is(ModBlocks.BARRIER.get()) && world.isEmptyBlock(up)) world.setBlock(up, ModBlocks.BARRIER.get().defaultBlockState(), 3);
            }
        }
    }
}
