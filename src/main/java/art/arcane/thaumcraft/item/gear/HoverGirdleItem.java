package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.item.BaubleItem;
import art.arcane.thaumcraft.util.Baubles;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Thaumostatic Girdle, recreated from TC4 (ItemGirdleHover). Belt bauble: while a fuelled Thaumostatic Harness is flying
 * the wearer flies much faster and the harness burns a bit more Potentia; the girdle also prevents fall damage (FTB wiki).
 * Worn in the Curios belt slot; without Curios it counts anywhere in the inventory. See docs/CINTURON_FUENTES.md.
 */
public class HoverGirdleItem extends BaubleItem {
    /** Vanilla flying speed is 0.05; the girdle doubles it [port figure]. */
    public static final float FLY_SPEED = 0.1f, BASE_FLY_SPEED = 0.05f;
    /** With the girdle the harness burns 1 Potentia every 15 ticks instead of every 20 [port figure]. */
    public static final int TICKS_PER_FUEL = 15;
    public static final String BOOSTED = "thaumcraft.girdle_speed";

    public HoverGirdleItem(Properties p) { super(p.rarity(net.minecraft.world.item.Rarity.RARE)); }

    public static boolean isWorn(Player p) {
        if (Baubles.available()) return Baubles.isEquipped(p, ModItems.HOVER_GIRDLE.get());
        if (p.getOffhandItem().is(ModItems.HOVER_GIRDLE.get())) return true;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(ModItems.HOVER_GIRDLE.get())) return true;
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tc.girdle.text").withStyle(ChatFormatting.AQUA));
    }
}
