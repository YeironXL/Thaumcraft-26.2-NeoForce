package art.arcane.thaumcraft.golems.trunk;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** Places a Travelling Trunk owned by the user; contents, upgrade and name travel with the item. */
public class TravellingTrunkItem extends Item {
    public TravellingTrunkItem(Properties p) { super(p.stacksTo(1)); }

    @Override public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getPlayer() == null) return InteractionResult.FAIL;
        if (!(ctx.getLevel() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos p = ctx.getClickedPos().relative(ctx.getClickedFace());
        ItemStack stack = ctx.getItemInHand();
        TravellingTrunkEntity t = TravellingTrunkEntity.spawn(sl, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, ctx.getPlayer().getYRot() + 180f, ctx.getPlayer(), stack);
        if (t == null) return InteractionResult.FAIL;
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        Integer v = stack.get(art.arcane.thaumcraft.registry.ModComponents.VARIANT.get());
        if (v != null && v > 0 && v <= TravellingTrunkEntity.UPGRADES.length)
            out.accept(Component.translatable("tc.trunk.upgrade." + TravellingTrunkEntity.UPGRADES[v - 1]).withStyle(net.minecraft.ChatFormatting.GOLD));
        var c = stack.get(DataComponents.CONTAINER);
        if (c != null) out.accept(Component.translatable("tc.trunk.items", c.nonEmptyItemCopyStream().count()).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
