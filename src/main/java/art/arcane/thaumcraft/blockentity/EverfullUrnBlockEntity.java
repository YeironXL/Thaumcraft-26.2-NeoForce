package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * TC6 TileWaterJug (everfull urn): refills its 1000 mB water tank from vis (0.1 vis per 100 mB) and every 5 ticks scans
 * one spot of the 5x3x5 area around it, remembering fluid handlers and cauldrons; it then tops them up with water.
 */
public final class EverfullUrnBlockEntity extends BlockEntity {
    private int zone, counter, zc, tcount;
    private final List<Integer> handlers = new ArrayList<>();
    /** Internal tank; the exposed handler (top side) only allows draining, as TC6 fill() returns 0. */
    public final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, 1000) {
        @Override public boolean isValid(int index, FluidResource resource) { return resource.getFluid() == Fluids.WATER; }
        @Override protected void onContentsChanged(int index, FluidStack previous) {
            setChanged();
            boolean wasFull = previous.getAmount() >= 1000, full = getAmountAsLong(0) >= 1000;
            if (wasFull != full) sync();
        }
    };
    public final ResourceHandler<FluidResource> drainOnly = new net.neoforged.neoforge.transfer.DelegatingResourceHandler<>(tank) {
        @Override public int insert(int index, FluidResource resource, int amount, TransactionContext tx) { return 0; }
        @Override public int insert(FluidResource resource, int amount, TransactionContext tx) { return 0; }
    };

    public EverfullUrnBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.EVERFULL_URN.get(), pos, state); }

    public int amount() { return (int) tank.getAmountAsLong(0); }

    private int drain(int amt) {
        try (var tx = Transaction.openRoot()) {
            int d = tank.extract(0, FluidResource.of(Fluids.WATER), amt, tx);
            tx.commit();
            return d;
        }
    }
    private void fill(int amt) {
        try (var tx = Transaction.openRoot()) { tank.insert(0, FluidResource.of(Fluids.WATER), amt, tx); tx.commit(); }
    }

    private BlockPos zonePos(int z) { return worldPosition.offset(z / 5 % 5 - 2, z / 5 / 5 % 3 - 1, z % 5 - 2); }

    public static void tick(Level world, BlockPos pos, BlockState state, EverfullUrnBlockEntity te) {
        ++te.counter;
        if (world.isClientSide()) {
            if (te.tcount > 0) {
                if (te.tcount % 5 == 0) art.arcane.thaumcraft.client.fx.FXDispatcher.waterTrailFx(pos, te.zonePos(te.zc), te.counter, 0x286FF6, 0.1f);
                --te.tcount;
            }
            return;
        }
        if (te.counter % 5 != 0) return;
        ServerLevel sl = (ServerLevel) world;
        ++te.zone;
        BlockPos zp = te.zonePos(te.zone);
        BlockState bs = world.getBlockState(zp);
        boolean handler = sl.getCapability(Capabilities.Fluid.BLOCK, zp, Direction.UP) != null;
        if ((handler || isCauldron(bs)) && !te.handlers.contains(te.zone)) { te.handlers.add(te.zone); te.setChanged(); }
        int i = 0;
        while (i < te.handlers.size() && te.amount() >= 25) {
            int zz = te.handlers.get(i);
            BlockPos hp = te.zonePos(zz);
            BlockState bs2 = world.getBlockState(hp);
            // Cauldrons use their own level logic (TC6 had no fluid capability on them).
            var fh = hp.equals(pos) || isCauldron(bs2) ? null : sl.getCapability(Capabilities.Fluid.BLOCK, hp, Direction.UP);
            if (fh != null) {
                int q;
                try (var tx = Transaction.openRoot()) { q = fh.insert(FluidResource.of(Fluids.WATER), 25, tx); tx.commit(); }
                if (q > 0) { te.drain(q); te.setChanged(); world.blockEvent(pos, state.getBlock(), 1, zz); break; }
            } else {
                if (!isCauldron(bs2) || te.amount() < 333) { te.handlers.remove(i); te.setChanged(); continue; }
                int level = bs2.is(Blocks.CAULDRON) ? 0 : bs2.is(Blocks.WATER_CAULDRON) ? bs2.getValue(LayeredCauldronBlock.LEVEL) : 3;
                if (level < 3) {
                    world.setBlock(hp, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, level + 1), 2);
                    world.updateNeighbourForOutputSignal(hp, bs2.getBlock());
                    world.blockEvent(pos, state.getBlock(), 1, zz);
                    te.drain(333);
                }
            }
            ++i;
        }
        if (te.amount() < 1000) {
            float da = Math.min(0.1f, (1000 - te.amount()) / 1000.0f);
            float dv = NativeAura.drainVis(sl, pos, da, false);
            int wa = (int) (1000.0f * dv);
            if (wa > 0) te.fill(wa);
        }
    }

    /** TC6 checks Blocks.CAULDRON (every fill level); here the empty and water cauldrons. */
    private static boolean isCauldron(BlockState s) { return s.is(Blocks.CAULDRON) || s.is(Blocks.WATER_CAULDRON); }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) { if (level != null && level.isClientSide()) { zc = param; tcount = 5; } return true; }
        return super.triggerEvent(id, param);
    }

    void sync() { if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        tank.serialize(out);
        out.store("handlers", com.mojang.serialization.Codec.INT.listOf(), List.copyOf(handlers));
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        tank.deserialize(in);
        handlers.clear();
        in.read("handlers", com.mojang.serialization.Codec.INT.listOf()).ifPresent(handlers::addAll);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
