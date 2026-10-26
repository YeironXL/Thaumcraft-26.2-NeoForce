package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.node.NodeModifier;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

/** TC4 "Node in a Jar" (ItemJarNode): carries a captured node; using it on a block face releases the node there. */
public final class JarredNodeItem extends Item {
    public JarredNodeItem(Item.Properties p) { super(p.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)); }

    public static CompoundTag data(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) pos = pos.relative(ctx.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        level.setBlock(pos, ModBlocks.AURA_NODE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node) {
            node.fromTag(data(ctx.getItemInHand()));
            if (node.base.isEmpty()) node.initialized = false;
            node.sync();
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        if (ctx.getPlayer() == null || !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tip, TooltipFlag flag) {
        var t = data(stack);
        if (t.isEmpty()) return;
        var type = NodeType.byId(t.getIntOr("nodetype", 0));
        var mod = NodeModifier.byId(t.getIntOr("nodemod", 0));
        Component name = Component.translatable("tc.node.type." + type.key());
        if (mod != NodeModifier.NONE) name = Component.translatable("tc.node.mod." + mod.key()).append(" ").append(name);
        tip.accept(name.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        t.getCompound("aspects").ifPresent(a -> { for (String k : a.keySet())
            tip.accept(Component.literal(Character.toUpperCase(k.charAt(0)) + k.substring(1) + ": " + a.getIntOr(k, 0)).withStyle(ChatFormatting.GRAY)); });
    }
}
