package art.arcane.thaumcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** TC6 InventoryUtils helpers. */
public final class InventoryUtils {
    /** ejectStackAt: insert into the inventory on that side, otherwise throw the item out of that face. */
    public static void ejectStackAt(ServerLevel world, BlockPos pos, Direction side, ItemStack out) {
        var handler = world.getCapability(Capabilities.Item.BLOCK, pos.relative(side), side.getOpposite());
        if (handler != null && !out.isEmpty()) {
            try (var tx = Transaction.openRoot()) {
                int moved = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(out), out.getCount(), tx);
                tx.commit();
                out = out.copyWithCount(out.getCount() - moved);
            }
        }
        if (out.isEmpty()) return;
        BlockPos p = pos;
        if (world.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(world, pos.relative(side))) p = pos.relative(side.getOpposite());
        var e = new ItemEntity(world, p.getX() + 0.5 + side.getStepX(), p.getY() + side.getStepY(), p.getZ() + 0.5 + side.getStepZ(), out);
        e.setDeltaMovement(0.3 * side.getStepX(), 0.3 * side.getStepY(), 0.3 * side.getStepZ());
        world.addFreshEntity(e);
    }
    private InventoryUtils() {}
}
