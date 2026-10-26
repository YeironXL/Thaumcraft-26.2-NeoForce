package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.VoidSiphonBlock;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import art.arcane.thaumcraft.menu.VoidSiphonMenu;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 TileVoidSiphon: once a second while enabled, every flux rift of size 2+ within 8 blocks that it can see adds
 * sqrt(size) progress, loses stability (and now and then a size point); each 2000 progress makes a void seed in the
 * output slot. Hoppers may only extract.
 */
public final class VoidSiphonBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int PROGREQ = 2000;
    private ItemStack stack = ItemStack.EMPTY;
    public int progress;
    private int counter;

    public VoidSiphonBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.VOID_SIPHON.get(), pos, state); }

    private boolean room() { return stack.isEmpty() || (stack.is(ModItems.VOID_SEED.get()) && stack.getCount() < stack.getMaxStackSize()); }

    public static void tick(Level level, BlockPos pos, BlockState state, VoidSiphonBlockEntity te) {
        ++te.counter;
        if (!(level instanceof ServerLevel sl) || !state.getValue(VoidSiphonBlock.ENABLED)) return;
        if (te.counter % 20 != 0 || te.progress >= PROGREQ || !te.room()) return;
        boolean b = false;
        for (FluxRiftEntity fr : te.validRifts(sl)) {
            double d = Math.sqrt(fr.getRiftSize());
            te.progress += (int) d;
            fr.setRiftStability((float) (fr.getRiftStability() - d / 15.0));
            if (sl.getRandom().nextInt(33) == 0) fr.setRiftSize(fr.getRiftSize() - 1);
            b = d >= 1.0;
        }
        if (b && te.counter % 40 == 0) sl.blockEvent(pos, state.getBlock(), 5, te.counter);
        b = false;
        while (te.progress >= PROGREQ && te.room()) {
            te.progress -= PROGREQ;
            if (te.stack.isEmpty()) te.stack = new ItemStack(ModItems.VOID_SEED.get()); else te.stack.grow(1);
            b = true;
        }
        te.setChanged();
        if (b) sl.sendBlockUpdated(pos, state, state, 3);
    }

    public List<FluxRiftEntity> validRifts(Level level) {
        List<FluxRiftEntity> ret = new ArrayList<>();
        Vec3 c = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
        for (FluxRiftEntity fr : level.getEntitiesOfClass(FluxRiftEntity.class, new AABB(worldPosition).inflate(8.0))) {
            if (!fr.isAlive() || fr.getRiftSize() < 2 || fr.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 64.0) continue;
            Vec3 v1 = c.add(fr.position().subtract(c).normalize());
            // EntityUtils.canEntityBeSeen(rift, v1): a clear line from the rift to just in front of the siphon.
            var hit = level.clip(new ClipContext(fr.position(), v1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fr));
            if (hit.getType() != HitResult.Type.MISS) continue;
            ret.add(fr);
        }
        return ret;
    }

    @Override public boolean triggerEvent(int id, int param) {
        if (id == 5) {
            if (level != null && level.isClientSide()) art.arcane.thaumcraft.client.fx.ClientHooks.voidSiphon(this, param);
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ---- Container (one output slot) ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return stack.isEmpty(); }
    @Override public ItemStack getItem(int i) { return stack; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = stack.split(n); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = stack; stack = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { stack = s; setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return s.is(ModItems.VOID_SEED.get()); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    private static final int[] SLOTS = {0};
    @Override public int[] getSlotsForFace(Direction d) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction d) { return false; }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction d) { return true; }
    @Override public void clearContent() { stack = ItemStack.EMPTY; }

    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.void_siphon"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new VoidSiphonMenu(id, inv, this); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!stack.isEmpty()) out.store("item", ItemStack.CODEC, stack);
        out.putShort("progress", (short) progress);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stack = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = in.getShortOr("progress", (short) 0);
    }
}
