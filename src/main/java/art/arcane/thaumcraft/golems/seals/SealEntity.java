package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** TC6 SealEntity: a placed seal with its shared settings (priority, colour, lock, redstone, owner, area). */
public class SealEntity implements ISealEntity {
    SealPos sealPos;
    ISeal seal;
    byte priority = 0, color = 0;
    boolean locked = false, redstone = false, stopped = false;
    String owner = "";
    private BlockPos area = new BlockPos(1, 1, 1);

    public SealEntity() {}
    public SealEntity(SealPos sealPos, ISeal seal) {
        this.sealPos = sealPos;
        this.seal = seal;
        if (seal instanceof ISealConfigArea) {
            int x = sealPos.face.getStepX() == 0 ? 3 : 1;
            int y = sealPos.face.getStepY() == 0 ? 3 : 1;
            int z = sealPos.face.getStepZ() == 0 ? 3 : 1;
            area = new BlockPos(x, y, z);
        }
    }

    @Override
    public void tickSealEntity(Level world) {
        if (seal == null) return;
        if (isStoppedByRedstone(world)) {
            if (!stopped) for (Task t : TaskHandler.getTasks(world).values()) if (t.getSealPos() != null && t.getSealPos().equals(sealPos)) t.setSuspended(true);
            stopped = true;
            return;
        }
        stopped = false;
        seal.tickSeal(world, this);
    }
    @Override public boolean isStoppedByRedstone(Level world) {
        return isRedstoneSensitive() && (world.hasNeighborSignal(sealPos.pos) || world.hasNeighborSignal(sealPos.pos.relative(sealPos.face)));
    }
    @Override public ISeal getSeal() { return seal; }
    @Override public SealPos getSealPos() { return sealPos; }
    @Override public byte getPriority() { return priority; }
    @Override public void setPriority(byte p) { priority = p; }
    @Override public byte getColor() { return color; }
    @Override public void setColor(byte c) { color = c; }
    @Override public String getOwner() { return owner; }
    @Override public void setOwner(String o) { owner = o; }
    @Override public boolean isLocked() { return locked; }
    @Override public void setLocked(boolean l) { locked = l; }
    @Override public boolean isRedstoneSensitive() { return redstone; }
    @Override public void setRedstoneSensitive(boolean r) { redstone = r; }
    @Override public BlockPos getArea() { return area; }
    @Override public void setArea(BlockPos v) { area = v; }

    public void readNBT(CompoundTag nbt, HolderLookup.Provider reg) {
        BlockPos p = BlockPos.of(nbt.getLongOr("pos", 0L));
        Direction face = Direction.from3DDataValue(nbt.getByteOr("face", (byte) 0));
        sealPos = new SealPos(p, face);
        setPriority(nbt.getByteOr("priority", (byte) 0));
        setColor(nbt.getByteOr("color", (byte) 0));
        setLocked(nbt.getBooleanOr("locked", false));
        setRedstoneSensitive(nbt.getBooleanOr("redstone", false));
        setOwner(nbt.getStringOr("owner", ""));
        seal = SealHandler.newSeal(nbt.getStringOr("type", ""));
        if (seal != null) {
            seal.readCustomNBT(nbt, reg);
            if (seal instanceof ISealConfigArea) area = BlockPos.of(nbt.getLongOr("area", BlockPos.asLong(1, 1, 1)));
            if (seal instanceof ISealConfigToggles t) for (var prop : t.getToggles()) if (nbt.contains(prop.getKey())) prop.setValue(nbt.getBooleanOr(prop.getKey(), prop.getValue()));
        }
    }
    public CompoundTag writeNBT(HolderLookup.Provider reg) {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("pos", sealPos.pos.asLong());
        nbt.putByte("face", (byte) sealPos.face.get3DDataValue());
        nbt.putString("type", seal.getKey());
        nbt.putByte("priority", getPriority());
        nbt.putByte("color", getColor());
        nbt.putBoolean("locked", isLocked());
        nbt.putBoolean("redstone", isRedstoneSensitive());
        nbt.putString("owner", getOwner());
        seal.writeCustomNBT(nbt, reg);
        if (seal instanceof ISealConfigArea) nbt.putLong("area", area.asLong());
        if (seal instanceof ISealConfigToggles t) for (var prop : t.getToggles()) nbt.putBoolean(prop.getKey(), prop.getValue());
        return nbt;
    }
    @Override public void syncToClient(Level world) { SealHandler.syncToClient(world, this); }
}
