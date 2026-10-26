package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.tasks.Task;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** TC6 ProvisionRequest: a seal, block face or entity asking provider seals for an item. */
public final class ProvisionRequest {
    private ISealEntity seal;
    private Entity entity;
    private BlockPos pos;
    private Direction side;
    private final ItemStack stack;
    private int id, ui;
    private Task linkedTask;
    private boolean invalid;
    private long timeout;

    ProvisionRequest(ISealEntity seal, ItemStack stack) {
        this.seal = seal; this.stack = stack.copy();
        id = (seal.getSealPos().pos.toString() + seal.getSealPos().face.name() + stack + stack.getComponentsPatch()).hashCode();
        timeout = System.currentTimeMillis() + 10000;
    }
    ProvisionRequest(BlockPos pos, Direction side, ItemStack stack) {
        this.pos = pos; this.side = side; this.stack = stack.copy();
        id = (pos.toString() + side.name() + stack + stack.getComponentsPatch()).hashCode();
        timeout = System.currentTimeMillis() + 10000;
    }
    ProvisionRequest(Entity entity, ItemStack stack) {
        this.entity = entity; this.stack = stack.copy();
        id = (entity.getId() + stack.toString() + stack.getComponentsPatch()).hashCode();
        timeout = System.currentTimeMillis() + 10000;
    }
    public long getTimeout() { return timeout; }
    public int getId() { return id; }
    public void setUI(int ui) { this.ui = ui; }
    public ISealEntity getSeal() { return seal; }
    public Entity getEntity() { return entity; }
    public ItemStack getStack() { return stack; }
    public BlockPos getPos() { return pos; }
    public Direction getSide() { return side; }
    public Task getLinkedTask() { return linkedTask; }
    public void setLinkedTask(Task t) { linkedTask = t; timeout = System.currentTimeMillis() + 120000; }
    public boolean isInvalid() { return invalid; }
    public void setInvalid(boolean i) { invalid = i; }
    @Override public boolean equals(Object o) { return this == o || o instanceof ProvisionRequest pr && id == pr.id && ui == pr.ui; }
    @Override public int hashCode() { return id * 31 + ui; }
}
