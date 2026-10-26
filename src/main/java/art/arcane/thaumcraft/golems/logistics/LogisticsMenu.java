package art.arcane.thaumcraft.golems.logistics;

import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.seals.SealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.seals.SealProvide;
import art.arcane.thaumcraft.registry.ModMenus;
import java.util.Locale;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** TC6 ContainerLogistics: lists what the player's provider seals within 32 blocks can supply (81 ghost slots, scrollable, searchable). */
public final class LogisticsMenu extends AbstractContainerMenu {
    public final SimpleContainer input = new SimpleContainer(81) { @Override public int getMaxStackSize() { return Integer.MAX_VALUE / 2; } };
    private final Player player;
    TreeMap<String, ItemStack> items = new TreeMap<>();
    int lastTotal;
    public int start, end;
    public String searchText = "";
    public final @Nullable BlockPos target;
    public final @Nullable Direction side;

    public static void open(ServerPlayer sp, @Nullable BlockPos pos, @Nullable Direction side) {
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new LogisticsMenu(id, inv, pos, side), Component.translatable("container.thaumcraft.logistics")), buf -> {
            buf.writeBoolean(pos != null);
            if (pos != null) { buf.writeBlockPos(pos); buf.writeEnum(side); }
        });
    }
    private record Target(BlockPos pos, Direction side) { static Target read(RegistryFriendlyByteBuf b) { return b.readBoolean() ? new Target(b.readBlockPos(), b.readEnum(Direction.class)) : new Target(null, null); } }
    public LogisticsMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, Target.read(buf)); }
    private LogisticsMenu(int id, Inventory inv, Target t) { this(id, inv, t.pos, t.side); }
    public LogisticsMenu(int id, Inventory inv, @Nullable BlockPos pos, @Nullable Direction side) {
        super(ModMenus.LOGISTICS.get(), id);
        player = inv.player;
        target = pos; this.side = side;
        for (int a = 0; a < 81; a++) addSlot(new Slot(input, a, 19 + a % 9 * 19, 19 + a / 9 * 19) {
            @Override public boolean mayPickup(Player p) { return false; }
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addDataSlot(new DataSlot() { public int get() { return start; } public void set(int v) { start = v; } });
        addDataSlot(new DataSlot() { public int get() { return end; } public void set(int v) { end = v; } });
        if (!player.level().isClientSide()) refreshItemList(true);
    }

    public void refreshItemList(boolean full) {
        int newTotal = lastTotal;
        TreeMap<String, ItemStack> ti = new TreeMap<>();
        if (full) {
            newTotal = 0;
            for (SealEntity seal : SealHandler.getSealsInRange(player.level(), player.blockPosition(), 32)) {
                if (!(seal.getSeal() instanceof SealProvide prov) || !seal.getOwner().equals(player.getUUID().toString())) continue;
                var h = InvHelper.getItemHandlerAt(player.level(), seal.getSealPos().pos, seal.getSealPos().face);
                if (h == null) continue;
                for (int slot = 0; slot < h.size(); slot++) {
                    ItemStack stack = h.getResource(slot).toStack(h.getAmountAsInt(slot));
                    if (!prov.matchesFilters(stack)) continue;
                    String name = stack.getHoverName().getString();
                    if (!searchText.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(searchText.toLowerCase(Locale.ROOT))) continue;
                    String key = name + stack.getDamageValue() + stack.getComponentsPatch();
                    if (ti.containsKey(key)) stack.grow(ti.get(key).getCount());
                    ti.put(key, stack);
                    newTotal += stack.getCount();
                }
            }
        }
        if (lastTotal != newTotal || full) {
            lastTotal = newTotal;
            if (full) items = ti;
            input.clearContent();
            int j = 0, q = 0;
            for (String k : items.keySet()) {
                if (++j <= start * 9) continue;
                input.setItem(q, items.get(k));
                if (++q >= 81) break;
            }
            end = items.size() / 9 - 8;
        }
    }
    @Override public boolean clickMenuButton(Player p, int b) {
        if (b == 22) { refreshItemList(true); return true; }
        if (b == 0) { if (start < items.size() / 9 - 8) { ++start; refreshItemList(true); } return true; }
        if (b == 1) { if (start > 0) { --start; refreshItemList(true); } return true; }
        if (b >= 100) { int s = b - 100; if (s >= 0 && s <= items.size() / 9 - 8) { start = s; refreshItemList(true); } return true; }
        return false;
    }
    @Override public void clicked(int slotId, int button, ContainerInput mode, Player p) { if (slotId < 0 || slotId >= 81) super.clicked(slotId, button, mode, p); }
    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player p) { return true; }
}
