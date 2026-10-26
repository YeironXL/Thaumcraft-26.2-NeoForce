package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.progression.DustSwaps;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/** First-pass Salis Mundus progression triggers. */
public final class SalisMundusItem extends Item {
    // Equivalents of the TC6 ore dictionary entries "bookshelf" and "workbench".
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BOOKSHELVES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "bookshelves"));
    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> CRAFTING_TABLES = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "player_workstations/crafting_tables"));
    public SalisMundusItem(Item.Properties properties) {
        super(properties.rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(net.minecraft.network.chat.Component.translatable("item.thaumcraft.salis_mundus.hint").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (!player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        // Port change (user request): bookshelf / crafting table / cauldron conversions need sneak + right click,
        // otherwise the crafting table opens its own GUI before the dust is used.
        boolean sneaking = player.isShiftKeyDown();

        var level = (ServerLevel) context.getLevel();
        BlockPos pos = context.getClickedPos();
        var state = level.getBlockState(pos);

        if (state.is(Blocks.BOOKSHELF) || state.is(BOOKSHELVES)) {
            if (!sneaking) return InteractionResult.PASS;
            if (!PlayerProgressManager.has(player, "!gotdream")) return InteractionResult.PASS;
            PlayerProgressManager.get(player).recordCraft("thaumcraft:thaumonomicon");
            DustSwaps.scheduleItem(level, pos, state, player, new ItemStack(ModItems.THAUMONOMICON.get()));
            return used(level, pos, context, player);
        }

        boolean workbench = state.is(Blocks.CRAFTING_TABLE) || state.is(CRAFTING_TABLES);
        if (workbench || state.is(Blocks.CAULDRON)) {
            if (!sneaking) return InteractionResult.PASS;
            String prerequisite = workbench ? "FIRSTSTEPS@1" : "UNLOCKALCHEMY@1";
            if (!PlayerProgressManager.has(player, prerequisite)) return InteractionResult.PASS;
            var result = workbench ? ModBlocks.ARCANE_WORKBENCH.get() : ModBlocks.CRUCIBLE.get();
            PlayerProgressManager.get(player).recordCraft(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(result).toString());
            DustSwaps.scheduleBlock(level, pos, state, player, result);
            return used(level, pos, context, player);
        }

        art.arcane.thaumcraft.crafting.multiblock.Multiblocks.init();
        for (var trigger : art.arcane.thaumcraft.crafting.multiblock.DustTriggerMultiblock.TRIGGERS) {
            var place = trigger.getValidFace(level, player, pos, context.getClickedFace());
            if (place == null) continue;
            consume(context.getItemInHand(), player);
            player.swing(context.getHand(), true);
            trigger.execute(level, player, pos, place, context.getClickedFace());
            art.arcane.thaumcraft.fx.Fx.dust(level, player, context.getHand(), pos, context.getClickLocation(), trigger.sparkle(level, pos, place));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** The dust is spent when a trigger is valid; the change itself happens later (see DustSwaps). */
    private static InteractionResult used(ServerLevel level, BlockPos pos, UseOnContext context, ServerPlayer player) {
        consume(context.getItemInHand(), player);
        player.swing(context.getHand(), true);
        // Client-side doSparkles (hand dust, sound and sparkles on the affected block).
        art.arcane.thaumcraft.fx.Fx.dust(level, player, context.getHand(), pos, context.getClickLocation(), java.util.List.of(pos));
        return InteractionResult.SUCCESS;
    }

    private static void consume(ItemStack stack, ServerPlayer player) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
