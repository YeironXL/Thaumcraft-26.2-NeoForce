package art.arcane.thaumcraft.golems.tasks;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.ProvisionRequest;
import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.seals.SealPos;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

/** TC6 thaumcraft.api.golems.tasks.Task: a block (type 0) or entity (type 1) job posted by a seal. */
public class Task {
    private UUID golemUUID;
    private final int id;
    private final byte type;
    private final SealPos sealPos;
    private BlockPos pos;
    private Entity entity;
    private boolean reserved, suspended, completed;
    private int data;
    private ProvisionRequest linkedProvision;
    private short lifespan;
    private byte priority = 0;

    public Task(SealPos sealPos, BlockPos pos) {
        this.sealPos = sealPos;
        this.pos = pos.immutable();
        id = sealPos == null ? (System.currentTimeMillis() + "/BNPOS/" + pos).hashCode()
                : (System.currentTimeMillis() + "/B/" + sealPos.face + "/" + sealPos.pos + "/" + pos).hashCode();
        type = 0;
        lifespan = 300;
    }
    public Task(SealPos sealPos, Entity entity) {
        this.sealPos = sealPos;
        this.entity = entity;
        id = sealPos == null ? (System.currentTimeMillis() + "/ENPOS/" + entity.getId()).hashCode()
                : (System.currentTimeMillis() + "/E/" + sealPos.face + "/" + sealPos.pos + "/" + entity.getId()).hashCode();
        type = 1;
        lifespan = 300;
    }
    public byte getPriority() { return priority; }
    public void setPriority(byte p) { priority = p; }
    public boolean isCompleted() { return completed; }
    public void setCompletion(boolean f) { completed = f; lifespan += 1; }
    public UUID getGolemUUID() { return golemUUID; }
    public void setGolemUUID(UUID u) { golemUUID = u; }
    public BlockPos getPos() { return type == 1 ? entity.blockPosition() : pos; }
    public byte getType() { return type; }
    public Entity getEntity() { return entity; }
    public int getId() { return id; }
    public boolean isReserved() { return reserved; }
    public void setReserved(boolean r) { reserved = r; lifespan += 120; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean s) { setLinkedProvision(null); suspended = s; }
    public SealPos getSealPos() { return sealPos; }
    @Override public boolean equals(Object o) { return o instanceof Task t && t.id == id; }
    @Override public int hashCode() { return id; }
    public long getLifespan() { return lifespan; }
    public void setLifespan(short ls) { lifespan = ls; }
    public boolean canGolemPerformTask(GolemEntity golem) {
        ISealEntity se = SealHandler.getSealEntity(golem.level(), sealPos);
        if (se != null) {
            if (golem.getGolemColor() > 0 && se.getColor() > 0 && golem.getGolemColor() != se.getColor()) return false;
            return se.getSeal().canGolemPerformTask(golem, this);
        }
        return true;
    }
    public int getData() { return data; }
    public void setData(int d) { data = d; }
    public ProvisionRequest getLinkedProvision() { return linkedProvision; }
    public void setLinkedProvision(ProvisionRequest p) { linkedProvision = p; }
}
