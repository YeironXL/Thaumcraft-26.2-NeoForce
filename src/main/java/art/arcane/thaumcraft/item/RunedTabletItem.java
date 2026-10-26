package art.arcane.thaumcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** TC4 ItemRunedTablet: the only key of the Ancient Locking Mechanism; found on pedestals in the Outer Lands. */
public final class RunedTabletItem extends Item {
    public RunedTabletItem(Properties p) { super(p); }
    @Override public void appendHoverText(ItemStack s, TooltipContext c, TooltipDisplay d, Consumer<Component> b, TooltipFlag f) {
        b.accept(Component.translatable("item.runed_tablet.text").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
