package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.block.EssentiaLampBlock;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.golems.CropUtils;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * TC6 TileLampGrowth and TileLampFertility, both IEssentiaTransport sinks on the lamp's mounting side.
 * Growth: 1 herba = 20 charges (plus one reserve point); each tick a random column of the 13x13 area 6 blocks around is
 * searched downwards for an unripe plant, which gets a growth tick for one charge.
 * Fertility: up to 10 charges of desiderium; every 300 ticks two idle adults of one kind (at most 9 of them within 7
 * blocks) are put in love for 5 charges.
 */
public final class EssentiaLampBlockEntity extends BlockEntity implements EssentiaPort {
    public int charges;
    private boolean reserve;
    private int drawDelay, count;
    private final List<BlockPos> checklist = new ArrayList<>();
    private BlockPos lastPlant = BlockPos.ZERO;
    private BlockState lastState;

    public EssentiaLampBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAMP_ESSENTIA.get(), pos, state);
        charges = growth() ? -1 : 0;
    }

    private boolean growth() { return getBlockState().getBlock() instanceof EssentiaLampBlock b && b.kind == EssentiaLampBlock.Kind.GROWTH; }
    private Direction facing() { return getBlockState().getValue(EssentiaLampBlock.FACING); }
    private String wantedAspect() { return growth() ? "herba" : "desiderium"; }
    private boolean powered() { return level.hasNeighborSignal(worldPosition); }

    public static void tick(Level level, BlockPos pos, BlockState state, EssentiaLampBlockEntity te) {
        if (level instanceof ServerLevel sl) { if (te.growth()) te.tickGrowth(sl); else te.tickFertility(sl); }
    }

    private void setEnabled(boolean on) {
        BlockState s = getBlockState();
        if (s.getValue(EssentiaLampBlock.ENABLED) != on) level.setBlock(worldPosition, s.setValue(EssentiaLampBlock.ENABLED, on), 3);
    }

    private void tickGrowth(ServerLevel sl) {
        if (charges <= 0) {
            if (reserve) { charges = 20; reserve = false; sync(); }
            else if (drawEssentia()) { charges = 20; sync(); }
            if (charges <= 0) setEnabled(false);
            else if (!powered()) setEnabled(true);
        }
        if (!reserve && drawEssentia()) { reserve = true; setChanged(); }
        if (charges == 0) { charges = -1; sync(); }
        if (!powered() && charges > 0) updatePlant(sl);
    }

    private void tickFertility(ServerLevel sl) {
        if (charges < 10) {
            if (drawEssentia()) { ++charges; sync(); }
            if (charges <= 1) setEnabled(false);
            else if (!powered()) setEnabled(true);
        }
        if (!powered() && charges > 1 && count++ % 300 == 0) updateAnimals(sl);
    }

    private boolean isPlant(BlockState b) {
        if (b.getBlock() instanceof GrassBlock) return false;
        return b.getBlock() instanceof BonemealableBlock || b.getBlock() instanceof CactusBlock || b.getBlock() instanceof SugarCaneBlock
                || b.is(BlockTags.CROPS) || b.is(BlockTags.FLOWERS);
    }

    private void updatePlant(ServerLevel sl) {
        BlockState bs = sl.getBlockState(lastPlant);
        if (lastState != null && bs != lastState) {
            // PacketFXBlockMist (green) when the plant last fed has changed.
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, lastPlant.getX() + 0.5, lastPlant.getY() + 0.5, lastPlant.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.0);
            lastState = bs;
        }
        int distance = 6;
        if (checklist.isEmpty()) {
            for (int a = -distance; a <= distance; ++a) for (int b = -distance; b <= distance; ++b) checklist.add(worldPosition.offset(a, distance, b));
            Collections.shuffle(checklist, new java.util.Random(sl.getRandom().nextLong()));
        }
        BlockPos col = checklist.remove(0);
        for (int y = col.getY(); y >= worldPosition.getY() - distance; --y) {
            BlockPos bp = new BlockPos(col.getX(), y, col.getZ());
            BlockState s = sl.getBlockState(bp);
            if (!s.isAir() && isPlant(s) && net.minecraft.world.phys.Vec3.atCenterOf(worldPosition).distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bp)) < distance * distance && !CropUtils.isGrownCrop(sl, bp)) {
                --charges;
                lastPlant = bp;
                lastState = s;
                // world.scheduleUpdate(bp, block, 1): crops grow on their random tick in this version.
                s.randomTick(sl, bp, sl.getRandom());
                setChanged();
                return;
            }
        }
    }

    private void updateAnimals(ServerLevel sl) {
        int distance = 7;
        List<Animal> animals = sl.getEntitiesOfClass(Animal.class, new AABB(worldPosition).inflate(distance));
        for (Animal a : animals) {
            if (a.getAge() != 0 || a.isInLove()) continue;
            List<Animal> same = new ArrayList<>();
            for (Animal o : animals) if (o.getClass().equals(a.getClass())) same.add(o);
            if (same.size() > 9) continue;
            Animal partner = null;
            for (Animal o : same) {
                if (o.getAge() != 0 || o.isInLove()) continue;
                if (partner != null) {
                    charges -= 5;
                    o.setInLove(null);
                    partner.setInLove(null);
                    sync();
                    return;
                }
                partner = o;
            }
        }
    }

    /** drawEssentia: every 5 ticks, one point from the connected transport on the mounting side if it pulls less. */
    private boolean drawEssentia() {
        if (++drawDelay % 5 != 0) return false;
        Direction f = facing();
        EssentiaPort ic = EssentiaTubeBlockEntity.neighbor(level, worldPosition, f);
        if (ic == null) return false;
        Direction back = f.getOpposite();
        String aspect = wantedAspect();
        return ic.suction(back) < suction(f) && aspect.equals(ic.offered(aspect, back)) && ic.extractOne(aspect, back);
    }

    private void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    // ---- EssentiaPort: input only, on the mounting side ----
    @Override public boolean connects(Direction side) { return side == facing(); }
    @Override public int suction() { return suction(facing()); }
    @Override public int suction(Direction side) {
        if (side != facing()) return 0;
        return growth() ? (!reserve || charges <= 0 ? 128 : 0) : 128 - charges * 10;
    }
    @Override public String wanted() { return wantedAspect(); }
    @Override public String aspect() { return ""; }
    @Override public boolean extractOne(String aspect, Direction side) { return false; }

    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("charges", charges); out.putBoolean("reserve", reserve); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); charges = in.getIntOr("charges", charges); reserve = in.getBooleanOr("reserve", false); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
