package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.item.IRechargable;
import art.arcane.thaumcraft.item.RechargeHelper;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 TileRechargePedestal: every 10 ticks pulls up to 5 vis from the aura into the held IRechargable item. */
public final class RechargePedestalBlockEntity extends PedestalBlockEntity {
    private int counter;
    public RechargePedestalBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.RECHARGE_PEDESTAL.get(), pos, state); }
    @Override public float renderScale() { return 1.5f; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof IRechargable; }

    public static void tick(Level level, BlockPos pos, BlockState state, RechargePedestalBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        if (te.counter++ % 10 == 0 && RechargeHelper.rechargeItem(sl, te.item, pos, null, 5) > 0.0f) {
            te.sync();
            var primals = new art.arcane.thaumcraft.aspect.Aspect[] {AspectRegistry.AER, AspectRegistry.TERRA, AspectRegistry.IGNIS, AspectRegistry.AQUA, AspectRegistry.ORDO, AspectRegistry.PERDITIO};
            level.blockEvent(pos, state.getBlock(), 5, AspectRegistry.color(primals[level.getRandom().nextInt(primals.length)].id()));
        }
    }

    /** receiveClientEvent 5: a vis sparkle flying from nearby into the pedestal. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 5) {
            if (level != null && level.isClientSide()) {
                var r = level.getRandom();
                art.arcane.thaumcraft.client.fx.FXDispatcher.visSparkle(worldPosition.getX() + r.nextInt(3) - r.nextInt(3), worldPosition.above().getY() + r.nextInt(3), worldPosition.getZ() + r.nextInt(3) - r.nextInt(3), worldPosition.getX(), worldPosition.above().getY(), worldPosition.getZ(), param);
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
}
