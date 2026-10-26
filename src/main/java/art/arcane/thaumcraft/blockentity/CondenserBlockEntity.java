package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.CondenserBlock;
import art.arcane.thaumcraft.block.CondenserLatticeBlock;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
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

/**
 * TC6 TileCondenser: fed any essentia (except vitium, which clogs it) through tubes on its sides, it spends that
 * essentia to pull flux out of the chunk into its own store, which tubes can draw from below as vitium. The lattice
 * tower above sets the speed: interval = 600 - 15 per effective lattice (at most 40), cost = 4 + sqrt(lattices).
 * Each conversion has a 1 in 50 chance to clog a lattice.
 */
public final class CondenserBlockEntity extends BlockEntity implements EssentiaPort {
    private static final int MAX = 100;
    private int essentia, flux, count;
    private final Set<Long> history = new HashSet<>();
    private final List<Long> blockList = new ArrayList<>(), uncloggedList = new ArrayList<>();
    public float latticeCount = -1;
    public int interval, cost;

    public CondenserBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CONDENSER.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, CondenserBlockEntity te) {
        if (te.latticeCount < 0) te.triggerCheck();
        ++te.count;
        if (!state.getValue(CondenserBlock.ENABLED) || te.latticeCount <= 0) return;
        if (world.isClientSide()) {
            if (te.essentia > 0 && !te.uncloggedList.isEmpty() && te.count % Math.max(3, te.interval / 50) == 0) {
                BlockPos p = BlockPos.of(te.uncloggedList.get(world.getRandom().nextInt(te.uncloggedList.size())));
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, 0, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) world;
        if (te.count % 5 == 0 && te.essentia < MAX) te.fill();
        if (te.interval > 0 && te.essentia >= te.cost && te.flux < MAX && te.count % te.interval == 0 && NativeAura.flux(sl, pos) >= 1.0f) {
            NativeAura.drainFlux(sl, pos, 1.0f, false);
            te.essentia -= te.cost;
            ++te.flux;
            if (sl.getRandom().nextInt(50) == 0) te.makeLatticeDirty();
            te.sync();
        }
    }

    private void makeLatticeDirty() {
        if (uncloggedList.isEmpty()) return;
        int q = level.getRandom().nextInt(uncloggedList.size());
        if (q == 0) q = level.getRandom().nextInt(uncloggedList.size());
        BlockPos p = BlockPos.of(uncloggedList.get(q));
        if (level.getBlockState(p).is(ModBlocks.CONDENSER_LATTICE.get())) {
            BlockState old = level.getBlockState(p);
            BlockState d = ModBlocks.CONDENSER_LATTICE_DIRTY.get().defaultBlockState();
            for (var prop : old.getProperties()) if (d.hasProperty(prop)) d = copy(d, old, prop);
            level.setBlock(p, d, 3);
            latticeCount = -1;
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copy(BlockState to, BlockState from, net.minecraft.world.level.block.state.properties.Property p) { return to.setValue(p, from.getValue(p)); }

    /** fill: one point from each horizontal tube with essentia that pulls less than 128. Vitium clogs a lattice instead. */
    private void fill() {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            EssentiaPort ic = EssentiaTubeBlockEntity.neighbor(level, worldPosition, face);
            if (ic == null) continue;
            Direction back = face.getOpposite();
            if (ic.aspect().isEmpty() || ic.suction(back) >= suction(face) || suction(face) < ic.minimumSuction()) continue;
            String ta = ic.offered("", back);
            if (ta == null || ta.isEmpty()) continue;
            if (!"vitium".equals(ta)) { if (ic.extractOne(ta, back)) essentia++; }
            else makeLatticeDirty();
            sync();
            if (essentia >= MAX) break;
        }
    }

    public void triggerCheck() {
        history.clear(); blockList.clear(); uncloggedList.clear();
        latticeCount = 0; interval = 0;
        performCheck(worldPosition, true, false);
        history.clear();
        if (latticeCount <= 0) latticeCount = 0;
        else {
            if (latticeCount > 40) latticeCount = 40;
            interval = Math.max(5, Math.round(600.0f - latticeCount * 15.0f));
            cost = (int) (4.0 + Math.sqrt(blockList.size()));
        }
    }

    private void performCheck(BlockPos pos, boolean skip, boolean clogged) {
        if (latticeCount < 0) return;
        history.add(pos.asLong());
        boolean found = false;
        int sides = 0;
        for (Direction face : Direction.values()) {
            if (skip && face != Direction.UP) continue;
            BlockPos p2 = pos.relative(face);
            BlockState bs = level.getBlockState(p2);
            boolean lattice = bs.is(ModBlocks.CONDENSER_LATTICE.get()), dirty = bs.is(ModBlocks.CONDENSER_LATTICE_DIRTY.get());
            if (skip && dirty) clogged = true;
            if (lattice || dirty) ++sides;
            if (history.contains(p2.asLong())) continue;
            if (face == Direction.DOWN && bs.is(ModBlocks.CONDENSER.get())) { latticeCount = -99; return; }
            if (worldPosition.getY() < p2.getY() && worldPosition.distSqr(p2) <= 74.0 && (lattice || dirty)) {
                blockList.add(p2.asLong());
                if (lattice) uncloggedList.add(p2.asLong());
                found = true;
                performCheck(p2, false, clogged || dirty);
                if (skip) break;
            }
        }
        if (found && !clogged) latticeCount += 1.0f - 0.15f * sides;
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    public int essentia() { return essentia; }
    public int flux() { return flux; }

    // ---- EssentiaPort: essentia in from the sides, vitium (the stored flux) out below ----
    @Override public boolean connects(Direction side) { return side != Direction.UP; }
    @Override public int suction() { return suction(Direction.NORTH); }
    @Override public int suction(Direction side) { return side == Direction.DOWN || side == Direction.UP || essentia >= MAX ? 0 : 128; }
    @Override public String wanted() { return ""; }
    @Override public String aspect() { return flux > 0 ? "vitium" : ""; }
    @Override public String offered(String requested, Direction side) { return side == Direction.DOWN && flux > 0 ? "vitium" : ""; }
    @Override public boolean extractOne(String aspect, Direction side) {
        if (side != Direction.DOWN || flux <= 0 || !"vitium".equals(aspect)) return false;
        --flux;
        sync();
        return true;
    }

    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putShort("essentia", (short) essentia); out.putShort("flux", (short) flux); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); essentia = in.getShortOr("essentia", (short) 0); flux = in.getShortOr("flux", (short) 0); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
