package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.VisGeneratorBlock;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** TC6 TileVisGenerator: turns 1 vis into 1000 FE (buffer 1000) and pushes up to 20 FE/tick into the block it faces. */
public final class VisGeneratorBlockEntity extends BlockEntity {
    int energy;
    /** The capability exposed on the facing side: extraction and insertion are both refused, as in TC6. */
    public final EnergyHandler handler = new EnergyHandler() {
        @Override public long getAmountAsLong() { return energy; }
        @Override public long getCapacityAsLong() { return 1000; }
        @Override public int insert(int amount, TransactionContext tx) { return 0; }
        @Override public int extract(int amount, TransactionContext tx) { return 0; }
    };

    public VisGeneratorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VIS_GENERATOR.get(), pos, state); }
    public int energy() { return energy; }

    public static void tick(Level level, BlockPos pos, BlockState state, VisGeneratorBlockEntity te) {
        if (!(level instanceof ServerLevel sl) || !state.getValue(VisGeneratorBlock.ENABLED)) return;
        te.recharge(sl);
        var face = state.getValue(VisGeneratorBlock.FACING);
        EnergyHandler target = sl.getCapability(Capabilities.Energy.BLOCK, pos.relative(face), face.getOpposite());
        if (target == null) return;
        int extracted = Math.min(te.energy, 20);
        try (var tx = Transaction.openRoot()) {
            extracted = target.insert(extracted, tx);
            tx.commit();
        }
        if (extracted > 0) {
            te.energy -= extracted;
            te.setChanged();
            if (te.energy == 0) te.sync();
        }
    }

    private void recharge(ServerLevel l) {
        if (energy == 0) {
            float vis = NativeAura.drainVis(l, worldPosition, 1.0f, false);
            energy = (int) (vis * 1000.0f);
            sync();
        }
    }

    private void sync() { setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("energy", energy); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); energy = in.getIntOr("energy", 0); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
