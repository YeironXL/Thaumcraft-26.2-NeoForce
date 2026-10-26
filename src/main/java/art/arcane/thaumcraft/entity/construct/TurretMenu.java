package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerTurretBasic / ContainerTurretAdvanced / ContainerArcaneBore: the construct's held item slot; advanced turret target toggles (buttons 1-4). */
public final class TurretMenu extends AbstractContainerMenu {
    public final OwnedConstruct construct;
    public TurretMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, (OwnedConstruct) inv.player.level().getEntity(buf.readVarInt())); }
    public TurretMenu(int id, Inventory inv, OwnedConstruct c) {
        super(ModMenus.TURRET.get(), id);
        construct = c;
        boolean bore = c instanceof ArcaneBoreEntity;
        int sx = c instanceof TurretCrossbowAdvancedEntity ? 42 : 80;
        Container held = new Container() {
            public int getContainerSize() { return 1; }
            public boolean isEmpty() { return c.getMainHandItem().isEmpty(); }
            public ItemStack getItem(int i) { return c.getMainHandItem(); }
            public ItemStack removeItem(int i, int n) { ItemStack r = c.getMainHandItem().split(n); return r; }
            public ItemStack removeItemNoUpdate(int i) { ItemStack r = c.getMainHandItem(); c.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); return r; }
            public void setItem(int i, ItemStack s) { c.setItemInHand(InteractionHand.MAIN_HAND, s); }
            public void setChanged() {}
            public boolean stillValid(Player p) { return c.isAlive(); }
            public void clearContent() {}
        };
        addSlot(new Slot(held, 0, sx, 29) {
            @Override public boolean mayPlace(ItemStack s) { return bore ? s.is(ItemTags.PICKAXES) : s.getItem() instanceof ArrowItem; }
        });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
    @Override public boolean clickMenuButton(Player p, int b) {
        if (construct instanceof TurretCrossbowAdvancedEntity t) {
            switch (b) {
                case 1 -> t.setTargetAnimal(!t.getTargetAnimal());
                case 2 -> t.setTargetMob(!t.getTargetMob());
                case 3 -> t.setTargetPlayer(!t.getTargetPlayer());
                case 4 -> t.setTargetFriendly(!t.getTargetFriendly());
                default -> { return false; }
            }
            return true;
        }
        return false;
    }
    @Override public boolean stillValid(Player p) { return construct != null && construct.isAlive() && p.distanceToSqr(construct) < 64; }
    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot s = slots.get(slot);
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!slots.get(0).mayPlace(in) || !moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        }
        return stack;
    }
}
