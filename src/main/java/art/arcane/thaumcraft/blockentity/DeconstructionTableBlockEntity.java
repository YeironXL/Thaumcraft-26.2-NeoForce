package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.menu.DeconstructionTableMenu;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ObservationFormula;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.research.ResearchEvents;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * TC4 TileDeconstructionTable, adapted to TC6 knowledge. One item at a time is broken down over {@link #DURATION}
 * ticks; when done the item is destroyed and its owner (the last player to open the table) gains OBSERVATION knowledge
 * in every category, computed with the TC6 scan formula ({@link ObservationFormula}) on that item's aspects.
 */
public final class DeconstructionTableBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer, MenuProvider {
    public static final int DURATION = 100;
    private ItemStack stack = ItemStack.EMPTY;
    private int progress;
    private UUID owner;
    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return i == 0 ? progress : DURATION; }
        @Override public void set(int i, int v) { if (i == 0) progress = v; }
        @Override public int getCount() { return 2; }
    };

    public DeconstructionTableBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.DECONSTRUCTION_TABLE.get(), pos, state); }

    public int progress() { return progress; }
    public void setOwner(UUID id) { owner = id; setChanged(); }

    public static boolean breakable(ItemStack s) { return !s.isEmpty() && !ObjectAspects.get(s).isEmpty(); }

    /** Knowledge gained per category for deconstructing one item. */
    public static Map<String, Integer> knowledgeFor(ItemStack s) {
        var aspects = ObjectAspects.get(s.copyWithCount(1));
        var out = new LinkedHashMap<String, Integer>();
        int total = 0;
        for (String c : ResearchCatalog.CATEGORIES) { int n = ObservationFormula.calculate(c, aspects); if (n > 0) { out.put(c, n); total += n; } }
        if (total == 0 && !aspects.isEmpty()) out.put("BASICS", 1);
        return out;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeconstructionTableBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        ServerPlayer player = te.owner == null ? null : sl.getServer().getPlayerList().getPlayer(te.owner);
        if (player == null && te.owner != null && sl.getPlayerByUUID(te.owner) instanceof ServerPlayer sp) player = sp;
        te.work(player);
    }

    /** One tick of work for the given owner (null = owner absent, which resets progress). */
    public void work(ServerPlayer player) {
        DeconstructionTableBlockEntity te = this;
        Level level = this.level;
        BlockPos pos = worldPosition;
        if (!breakable(te.stack) || player == null) { if (te.progress != 0) { te.progress = 0; te.setChanged(); } return; }
        if (++te.progress < DURATION) return;
        te.progress = 0;
        var research = PlayerProgressManager.get(player);
        var gained = knowledgeFor(te.stack);
        gained.forEach((c, n) -> research.addKnowledge("OBSERVATION", c, n));
        ResearchEvents.markDirty(player);
        ResearchEvents.sync(player);
        if (player.connection != null) player.sendOverlayMessage(Component.translatable("tc.deconstruction.gained", te.stack.getHoverName()));
        te.stack.shrink(1);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        te.setChanged();
    }

    // ---- Container (one slot) ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return stack.isEmpty(); }
    @Override public ItemStack getItem(int i) { return stack; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = stack.split(n); progress = 0; setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = stack; stack = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { if (!ItemStack.isSameItemSameComponents(s, stack)) progress = 0; stack = s; setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return breakable(s); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    // ---- Automation: hoppers/pipes may insert breakable items from any side; nothing is extracted. ----
    private static final int[] SLOTS = {0};
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction d) { return SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, net.minecraft.core.Direction d) { return breakable(s); }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, net.minecraft.core.Direction d) { return false; }
    @Override public void clearContent() { stack = ItemStack.EMPTY; }

    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.deconstruction_table"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new DeconstructionTableMenu(id, inv, this, data); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!stack.isEmpty()) out.store("item", ItemStack.CODEC, stack);
        out.putInt("progress", progress);
        if (owner != null) out.store("owner", UUIDUtil.CODEC, owner);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        stack = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        progress = in.getIntOr("progress", 0);
        owner = in.read("owner", UUIDUtil.CODEC).orElse(null);
    }
}
