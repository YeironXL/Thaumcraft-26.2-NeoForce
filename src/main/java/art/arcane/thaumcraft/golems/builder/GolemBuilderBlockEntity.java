package art.arcane.thaumcraft.golems.builder;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.fx.FXDispatcher;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.golems.GolemPlacerItem;
import art.arcane.thaumcraft.golems.GolemProperties;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileGolemBuilder: consumes the golem's components from adjacent inventories or the player, then 1 machina per point of cost. */
public final class GolemBuilderBlockEntity extends BlockEntity implements Container, MenuProvider, EssentiaPort {
    public long golem = -1L;
    public int cost = 0, maxCost = 0, press = 0;
    boolean bufferedEssentia = false;
    int ticks = 0;
    private ItemStack output = ItemStack.EMPTY;

    public GolemBuilderBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.GOLEM_BUILDER.get(), pos, state); }

    public static void tick(Level level, BlockPos pos, BlockState state, GolemBuilderBlockEntity te) { te.update(); }

    private void update() {
        boolean complete = false;
        if (!level.isClientSide()) {
            ++ticks;
            if (ticks % 5 == 0 && cost > 0 && golem >= 0L) {
                if (bufferedEssentia || drawEssentia()) { bufferedEssentia = false; --cost; setChanged(); }
                if (cost <= 0) {
                    ItemStack placer = GolemPlacerItem.of(golem);
                    if (output.isEmpty() || (output.getCount() < output.getMaxStackSize() && ItemStack.isSameItemSameComponents(output, placer))) {
                        if (output.isEmpty()) output = placer.copy(); else output.grow(1);
                        complete = true;
                        level.playSound(null, worldPosition, ModSounds.get("wand"), SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
            }
        } else {
            var r = level.getRandom();
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1, z = worldPosition.getZ() + 0.5;
            if (press < 90 && cost > 0 && golem > 0L) {
                press += 6;
                if (press >= 60) {
                    level.playLocalSound(x, worldPosition.getY() + 0.5, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.66f, 1.0f + r.nextFloat() * 0.1f, false);
                    for (int a = 0; a < 16; ++a) FXDispatcher.drawVentParticles(x, y, z, r.nextGaussian() * 0.1, 0.0, r.nextGaussian() * 0.1, 11184810);
                }
            }
            if (press >= 90 && r.nextInt(8) == 0) {
                FXDispatcher.drawVentParticles(x, y, z, r.nextGaussian() * 0.1, 0.0, r.nextGaussian() * 0.1, 11184810);
                level.playLocalSound(x, worldPosition.getY() + 0.5, z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1f, 1.0f + r.nextFloat() * 0.1f, false);
            }
            if (press > 0 && (cost <= 0 || golem == -1L)) {
                if (press >= 90) for (int a = 0; a < 10; ++a) FXDispatcher.drawVentParticles(x, y, z, r.nextGaussian() * 0.1, 0.0, r.nextGaussian() * 0.1, 11184810);
                press -= 3;
            }
        }
        if (complete) { cost = 0; golem = -1L; sync(); }
    }

    public boolean[] checkCraft(long id) {
        ItemStack[] cc = GolemProperties.fromLong(id).generateComponents();
        boolean[] ret = new boolean[cc.length];
        for (int a = 0; a < cc.length; a++) ret[a] = InvHelper.checkAdjacentChests(level, worldPosition, cc[a]);
        return ret;
    }
    public boolean startCraft(long id, Player p) {
        ItemStack placer = GolemPlacerItem.of(id);
        if (!output.isEmpty() && (output.getCount() >= output.getMaxStackSize() || !ItemStack.isSameItemSameComponents(output, placer))) { cost = 0; golem = -1L; return false; }
        GolemProperties props = GolemProperties.fromLong(id);
        ItemStack[] components = props.generateComponents();
        if (!InvHelper.consumeItemsFromAdjacentInventoryOrPlayer(level, worldPosition, p, true, components)) { cost = 0; golem = -1L; return false; }
        golem = id;
        cost = props.getTraits().size() * 2;
        for (ItemStack s : components) cost += s.getCount();
        InvHelper.consumeItemsFromAdjacentInventoryOrPlayer(level, worldPosition, p, false, components);
        maxCost = cost;
        sync();
        level.playSound(null, worldPosition, ModSounds.get("wand"), SoundSource.BLOCKS, 0.25f, 1.0f);
        return true;
    }

    boolean drawEssentia() {
        for (Direction face : Direction.values()) {
            if (!connects(face)) continue;
            var ic = EssentiaTubeBlockEntity.neighbor(level, worldPosition, face);
            if (ic == null) continue;
            Direction back = face.getOpposite();
            if (ic.suction(back) < suction(face) && "machina".equals(ic.offered("machina", back)) && ic.extractOne("machina", back)) return true;
        }
        return false;
    }
    void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    // EssentiaPort: input from the sides and below
    @Override public boolean connects(Direction side) { return side != Direction.UP; }
    @Override public int suction() { return cost > 0 && golem >= 0L ? 128 : 0; }
    @Override public String wanted() { return "machina"; }
    @Override public String aspect() { return ""; }
    @Override public String offered(String requested, Direction side) { return ""; }
    @Override public boolean extractOne(String aspect, Direction side) { return false; }
    /** addEssentia: tubes may push one machina into the buffer. */
    public int addEssentia(String aspect, int amount) {
        if (!bufferedEssentia && cost > 0 && golem >= 0L && "machina".equals(aspect)) { bufferedEssentia = true; return 1; }
        return 0;
    }

    // Container: the single output slot
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return output.isEmpty(); }
    @Override public ItemStack getItem(int i) { return output; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = output.split(n); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = output; output = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { output = s; setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return s.getItem() instanceof GolemPlacerItem; }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { output = ItemStack.EMPTY; }
    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.golem_builder"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new GolemBuilderMenu(id, inv, this); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putLong("golem", golem); out.putInt("cost", cost); out.putInt("mcost", maxCost);
        if (!output.isEmpty()) out.store("out", ItemStack.CODEC, output);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        golem = in.getLongOr("golem", -1L); cost = in.getIntOr("cost", 0); maxCost = in.getIntOr("mcost", 0);
        output = in.read("out", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @SuppressWarnings("unused") private static final Object UNUSED = ModItems.GOLEM;
}
