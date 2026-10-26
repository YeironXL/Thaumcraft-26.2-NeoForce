package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.block.MirrorBlock;
import art.arcane.thaumcraft.blockentity.MirrorBlockEntity;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * TC6 BlockMirrorItem: right-clicking a placed, unlinked mirror of the same kind takes one mirror from the stack and gives
 * back a linked one (metadata 1) pointing at it; placing that links the pair.
 */
public final class MirrorItem extends BlockItem {
    public MirrorItem(Block block, Properties p) { super(block, p); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!(world.getBlockState(pos).getBlock() instanceof MirrorBlock) || player == null) return InteractionResult.PASS;
        if (world.isClientSide()) { player.swing(ctx.getHand()); return InteractionResult.PASS; }
        if (world.getBlockState(pos).getBlock() != getBlock()) return InteractionResult.PASS;
        if (world.getBlockEntity(pos) instanceof MirrorBlockEntity tm) {
            if (!tm.isLinkValid()) {
                ItemStack st = player.getItemInHand(ctx.getHand()).copyWithCount(1);
                st.set(ModComponents.MIRROR_LINK.get(), GlobalPos.of(world.dimension(), pos));
                world.playSound(null, pos, ModSounds.get("jar"), SoundSource.BLOCKS, 1.0f, 2.0f);
                if (!player.getInventory().add(st)) player.drop(st, false);
                if (!player.getAbilities().instabuild) player.getItemInHand(ctx.getHand()).shrink(1);
                player.inventoryMenu.broadcastChanges();
            } else {
                player.sendSystemMessage(Component.literal("§5§oThat mirror is already linked to a valid destination."));
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack item, TooltipContext ctx, TooltipDisplay d, Consumer<Component> list, TooltipFlag flag) {
        GlobalPos gp = item.get(ModComponents.MIRROR_LINK.get());
        if (gp != null) list.accept(Component.literal("Linked to " + gp.pos().getX() + "," + gp.pos().getY() + "," + gp.pos().getZ() + " in " + gp.dimension().identifier().getPath()));
    }
}
