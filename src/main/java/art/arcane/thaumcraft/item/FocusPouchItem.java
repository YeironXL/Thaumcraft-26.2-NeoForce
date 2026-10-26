package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.registry.ModComponents;
import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** TC6 ItemFocusPouch: 18 focus slots, worn as a belt bauble or carried in the inventory. */
public final class FocusPouchItem extends Item {
    public FocusPouchItem(Properties p) { super(p.stacksTo(1).rarity(Rarity.UNCOMMON)); }

    public NonNullList<ItemStack> getInventory(ItemStack item) {
        NonNullList<ItemStack> list = NonNullList.withSize(18, ItemStack.EMPTY);
        var c = item.get(ModComponents.POUCH.get());
        if (c != null) c.copyInto(list);
        return list;
    }

    public void setInventory(ItemStack item, NonNullList<ItemStack> list) {
        item.set(ModComponents.POUCH.get(), ItemContainerContents.fromItems(new ArrayList<>(list)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp && hand == InteractionHand.MAIN_HAND)
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new art.arcane.thaumcraft.menu.FocusPouchMenu(id, inv),
                    net.minecraft.network.chat.Component.translatable("item.thaumcraft.focus_pouch")));
        return InteractionResult.SUCCESS;
    }
}
