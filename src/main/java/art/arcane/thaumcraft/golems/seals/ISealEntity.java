package art.arcane.thaumcraft.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ISealEntity {
    void tickSealEntity(Level world);
    ISeal getSeal();
    SealPos getSealPos();
    byte getPriority();
    void setPriority(byte p);
    void syncToClient(Level world);
    BlockPos getArea();
    void setArea(BlockPos v);
    boolean isLocked();
    void setLocked(boolean l);
    boolean isRedstoneSensitive();
    void setRedstoneSensitive(boolean r);
    String getOwner();
    void setOwner(String owner);
    byte getColor();
    void setColor(byte c);
    boolean isStoppedByRedstone(Level world);
}
