package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.MirrorBlockEntity;
import art.arcane.thaumcraft.menu.HandMirrorMenu;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * TC6 ItemHandMirror: right-click a magic mirror to bind to it (it glints while bound); right-click in the air to open the
 * looking glass and push items through to the bound mirror. It can only send, and breaks its link if the mirror is gone.
 */
public final class HandMirrorItem extends Item {
    public HandMirrorItem(Properties p) { super(p); }

    @Override public boolean isFoil(ItemStack stack) { return stack.has(ModComponents.MIRROR_LINK.get()); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (!world.getBlockState(pos).is(ModBlocks.MIRROR.get()) || player == null) return InteractionResult.FAIL;
        if (world.isClientSide()) { player.swing(ctx.getHand()); return InteractionResult.PASS; }
        if (world.getBlockEntity(pos) instanceof MirrorBlockEntity) {
            player.getItemInHand(ctx.getHand()).set(ModComponents.MIRROR_LINK.get(), GlobalPos.of(world.dimension(), pos));
            world.playSound(null, pos, ModSounds.get("jar"), SoundSource.BLOCKS, 1.0f, 2.0f);
            player.sendSystemMessage(Component.translatable("tc.handmirrorlinked"));
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide() && stack.has(ModComponents.MIRROR_LINK.get())) {
            if (mirror(stack, player, (ServerLevel) world) == null) return InteractionResult.PASS;
            player.openMenu(new SimpleMenuProvider((id, inv, p) -> new HandMirrorMenu(id, inv), Component.translatable("item.thaumcraft.hand_mirror")));
        }
        return InteractionResult.PASS;
    }

    /** The bound mirror; a missing one breaks the link with a zap. */
    public static MirrorBlockEntity mirror(ItemStack stack, Player player, ServerLevel any) {
        GlobalPos gp = stack.get(ModComponents.MIRROR_LINK.get());
        if (gp == null) return null;
        ServerLevel target = any.getServer().getLevel(gp.dimension());
        if (target == null) return null;
        if (!(target.getBlockEntity(gp.pos()) instanceof MirrorBlockEntity tm) || tm instanceof art.arcane.thaumcraft.blockentity.EssentiaMirrorBlockEntity) {
            stack.remove(ModComponents.MIRROR_LINK.get());
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.get("zap"), SoundSource.PLAYERS, 1.0f, 0.8f);
            player.sendSystemMessage(Component.translatable("tc.handmirrorerror"));
            return null;
        }
        return tm;
    }

    /** ItemHandMirror.transport: queue the stack in the bound mirror. */
    public static boolean transport(ItemStack mirror, ItemStack items, Player player, ServerLevel world) {
        MirrorBlockEntity tm = mirror(mirror, player, world);
        if (tm == null) return false;
        if (tm.transportDirect(items)) world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.1f, 1.0f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> tooltip, TooltipFlag flag) {
        GlobalPos gp = stack.get(ModComponents.MIRROR_LINK.get());
        if (gp != null) tooltip.accept(Component.translatable("tc.handmirrorlinkedto").append(" " + gp.pos().getX() + "," + gp.pos().getY() + "," + gp.pos().getZ() + " in " + gp.dimension().identifier().getPath()));
    }
}
