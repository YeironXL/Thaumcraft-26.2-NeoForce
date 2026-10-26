package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.MirrorBlock;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TC6 TileMirror: a magic mirror linked to exactly one partner (possibly in another dimension). Items thrown or piped into
 * it are queued in the partner, which ejects them one by one out of its face. Transported items stress the aura:
 * every 128 points of instability pollute one flux. Links are re-established periodically (backing off to 30 seconds).
 */
public class MirrorBlockEntity extends BlockEntity implements Container {
    public boolean linked;
    public int linkX, linkY, linkZ;
    public ResourceKey<Level> linkDim = Level.OVERWORLD;
    public int instability;
    int count;
    int inc = 40;
    private List<ItemStack> outputStacks = new ArrayList<>();

    public MirrorBlockEntity(BlockPos pos, BlockState state) { this(ModBlockEntities.MIRROR.get(), pos, state); }
    protected MirrorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    public Direction facing() { return getBlockState().getValue(MirrorBlock.FACING); }
    public BlockPos linkPos() { return new BlockPos(linkX, linkY, linkZ); }
    /** The partner kind this mirror links to (essentia mirrors only link to essentia mirrors). */
    protected boolean isPartner(BlockEntity te) { return te != null && te.getClass() == getClass(); }

    protected ServerLevel targetWorld() {
        if (!(level instanceof ServerLevel sl)) return null;
        return sl.getServer().getLevel(linkDim);
    }
    protected MirrorBlockEntity partner() {
        ServerLevel w = targetWorld();
        if (w == null) return null;
        BlockEntity te = w.getBlockEntity(linkPos());
        return isPartner(te) ? (MirrorBlockEntity) te : null;
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void setLink(GlobalPos to) {
        linkX = to.pos().getX(); linkY = to.pos().getY(); linkZ = to.pos().getZ(); linkDim = to.dimension();
    }

    public void restoreLink() {
        if (!isDestinationValid()) return;
        MirrorBlockEntity tm = partner();
        if (tm == null) return;
        tm.linked = true;
        tm.linkX = worldPosition.getX(); tm.linkY = worldPosition.getY(); tm.linkZ = worldPosition.getZ();
        tm.linkDim = level.dimension();
        tm.onLinked(this);
        tm.sync();
        linked = true;
        onLinked(tm);
        sync();
    }
    protected void onLinked(MirrorBlockEntity other) {}

    public void invalidateLink() {
        ServerLevel w = targetWorld();
        if (w == null || !w.isLoaded(linkPos())) return;
        BlockEntity te = w.getBlockEntity(linkPos());
        if (isPartner(te)) {
            MirrorBlockEntity tm = (MirrorBlockEntity) te;
            tm.linked = false;
            tm.onUnlinked();
            setChanged();
            tm.sync();
        }
    }
    protected void onUnlinked() {}

    private boolean unlink() { linked = false; sync(); return false; }

    public boolean isLinkValid() {
        if (!linked) return false;
        ServerLevel w = targetWorld();
        if (w == null) return false;
        BlockEntity te = w.getBlockEntity(linkPos());
        if (!isPartner(te)) return unlink();
        MirrorBlockEntity tm = (MirrorBlockEntity) te;
        if (!tm.linked) return unlink();
        if (tm.linkX != worldPosition.getX() || tm.linkY != worldPosition.getY() || tm.linkZ != worldPosition.getZ() || tm.linkDim != level.dimension()) return unlink();
        return true;
    }

    public boolean isLinkValidSimple() {
        if (!linked) return false;
        MirrorBlockEntity tm = partner();
        return tm != null && tm.linked && tm.linkX == worldPosition.getX() && tm.linkY == worldPosition.getY() && tm.linkZ == worldPosition.getZ() && tm.linkDim == level.dimension();
    }

    public boolean isDestinationValid() {
        ServerLevel w = targetWorld();
        if (w == null) return false;
        BlockEntity te = w.getBlockEntity(linkPos());
        if (!isPartner(te)) { linked = false; sync(); return false; }
        return !((MirrorBlockEntity) te).isLinkValid();
    }

    /** transport: an item entity touching the mirror vanishes into the partner's queue. */
    public boolean transport(ItemEntity ie) {
        ItemStack items = ie.getItem();
        if (!linked || !isLinkValid()) return false;
        MirrorBlockEntity target = partner();
        if (target == null) return false;
        target.addStack(items.copy());
        addInstability(null, items.getCount());
        ie.discard();
        setChanged();
        target.setChanged();
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        return true;
    }

    /** transportDirect: the hand mirror drops a stack straight into this mirror's output queue. */
    public boolean transportDirect(ItemStack items) {
        if (items == null || items.isEmpty()) return false;
        addStack(items.copy());
        setChanged();
        return true;
    }

    public void eject() {
        if (!outputStacks.isEmpty() && count > 20) {
            int i = level.getRandom().nextInt(outputStacks.size());
            ItemStack s = outputStacks.get(i);
            if (!s.isEmpty()) {
                ItemStack outItem = s.copy();
                outItem.setCount(1);
                if (spawnItem(outItem)) {
                    s.shrink(1);
                    addInstability(null, 1);
                    level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
                    if (s.isEmpty()) outputStacks.remove(i);
                }
            } else outputStacks.remove(i);
            setChanged();
        }
    }

    public boolean spawnItem(ItemStack stack) {
        Direction face = facing();
        ItemEntity ie2 = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5, stack);
        ie2.setDeltaMovement(face.getStepX() * 0.15f, face.getStepY() * 0.15f, face.getStepZ() * 0.15f);
        ie2.setPortalCooldown(20);
        return level.addFreshEntity(ie2);
    }

    protected void addInstability(Level targetWorld, int amt) {
        instability += amt;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MirrorBlockEntity te) { te.serverTick(); }

    protected void serverTick() {
        eject();
        checkInstability();
        if (count++ % inc == 0) {
            if (!isLinkValidSimple()) {
                if (inc < 600) inc += 20;
                restoreLink();
            } else inc = 40;
        }
    }

    protected int instabilityThreshold() { return 128; }

    public void checkInstability() {
        if (instability > instabilityThreshold()) {
            NativeAura.addFlux((ServerLevel) level, worldPosition, 1.0f);
            instability -= instabilityThreshold();
            setChanged();
        }
        if (instability > 0 && count % 100 == 0) --instability;
    }

    public List<ItemStack> outputStacks() { return outputStacks; }
    public void addStack(ItemStack stack) { outputStacks.add(stack); setChanged(); }

    /** receiveClientEvent 1: wispy motes drift out of the glass. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            if (level != null && level.isClientSide()) art.arcane.thaumcraft.client.fx.ClientHooks.mirrorMotes(this);
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ---- sync / persistence (readSyncNBT / writeSyncNBT plus the queued items) ----
    protected void saveSync(ValueOutput o) {
        o.putBoolean("linked", linked);
        o.putInt("linkX", linkX); o.putInt("linkY", linkY); o.putInt("linkZ", linkZ);
        o.putString("linkDim", linkDim.identifier().toString());
        o.putInt("instability", instability);
    }
    protected void loadSync(ValueInput i) {
        linked = i.getBooleanOr("linked", false);
        linkX = i.getIntOr("linkX", 0); linkY = i.getIntOr("linkY", 0); linkZ = i.getIntOr("linkZ", 0);
        linkDim = ResourceKey.create(Registries.DIMENSION, Identifier.parse(i.getStringOr("linkDim", "minecraft:overworld")));
        instability = i.getIntOr("instability", 0);
    }
    @Override protected void saveAdditional(ValueOutput o) {
        super.saveAdditional(o);
        saveSync(o);
        var list = o.list("Items", ItemStack.CODEC);
        for (ItemStack s : outputStacks) if (!s.isEmpty()) list.add(s);
    }
    @Override protected void loadAdditional(ValueInput i) {
        super.loadAdditional(i);
        loadSync(i);
        outputStacks = new ArrayList<>();
        i.listOrEmpty("Items", ItemStack.CODEC).forEach(outputStacks::add);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) {
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, r);
        saveSync(out);
        return out.buildResult();
    }

    /** BlockMirror.dropMirror: a linked mirror drops a linked (metadata 1) item remembering its partner. */
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder b) {
        super.collectImplicitComponents(b);
        if (linked) b.set(ModComponents.MIRROR_LINK.get(), GlobalPos.of(linkDim, linkPos()));
    }
    /** BlockMirrorItem.placeBlockAt: placing a linked item points the new mirror at the remembered partner (restored on the first tick). */
    @Override
    protected void applyImplicitComponents(DataComponentGetter c) {
        super.applyImplicitComponents(c);
        GlobalPos gp = c.get(ModComponents.MIRROR_LINK.get());
        if (gp != null) setLink(gp);
    }
    @Override public void removeComponentsFromTag(ValueOutput o) {}

    /** Breaking the mirror unlinks the partner (BlockMirror.dropMirror -> invalidateLink). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (linked && level != null && !level.isClientSide()) invalidateLink();
    }

    // ---- IInventory: piping into the mirror sends the items to the partner ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return false; }
    @Override public ItemStack getItem(int slot) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int n) { return ItemStack.EMPTY; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ItemStack.EMPTY; }
    @Override
    public void setItem(int slot, ItemStack stack2) {
        if (stack2.isEmpty() || level == null || level.isClientSide()) return;
        MirrorBlockEntity target = partner();
        if (target != null) {
            target.addStack(stack2.copy());
            addInstability(null, stack2.getCount());
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        } else spawnItem(stack2.copy());
    }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean stillValid(Player p) { return false; }
    @Override public boolean canPlaceItem(int slot, ItemStack s) { return level != null && !level.isClientSide() && partner() != null; }
    @Override public void clearContent() {}
}
