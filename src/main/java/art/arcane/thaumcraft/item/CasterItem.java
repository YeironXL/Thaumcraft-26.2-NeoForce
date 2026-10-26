package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.registry.ModComponents;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** TC6 ItemCaster (casting gauntlet). {@code area}: 0 own chunk, 1 plus the four neighbours, 2 the 3x3 chunks. */
public class CasterItem extends Item {
    private final int area;
    private static final DecimalFormat FMT = new DecimalFormat("#######.#");

    public CasterItem(int area, Properties p) { super(p.stacksTo(1).rarity(Rarity.UNCOMMON)); this.area = area; }

    private float getAuraPool(Player player) {
        var w = (ServerLevel) player.level(); var pos = player.blockPosition();
        float tot;
        switch (area) {
            case 1 -> { tot = NativeAura.vis(w, pos); for (Direction f : Direction.Plane.HORIZONTAL) tot += NativeAura.vis(w, pos.relative(f, 16)); }
            case 2 -> { tot = 0; for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) tot += NativeAura.vis(w, pos.offset(x * 16, 0, z * 16)); }
            default -> tot = NativeAura.vis(w, pos);
        }
        return tot;
    }

    public boolean consumeVis(ItemStack is, Player player, float amount, boolean crafting, boolean sim) {
        if (!(player.level() instanceof ServerLevel w)) return false;
        amount *= getConsumptionModifier(is, player, crafting);
        if (getAuraPool(player) < amount) return false;
        if (sim) return true;
        var pos = player.blockPosition();
        switch (area) {
            case 1 -> {
                float i = amount / 5.0f;
                outer:
                while (amount > 0) {
                    if (i > amount) i = amount;
                    amount -= NativeAura.drainVis(w, pos, i, false);
                    if (amount <= 0) break;
                    if (i > amount) i = amount;
                    for (Direction f : Direction.Plane.HORIZONTAL) { amount -= NativeAura.drainVis(w, pos.relative(f, 16), i, false); if (amount <= 0) break outer; }
                }
            }
            case 2 -> {
                float i = amount / 9.0f;
                outer:
                while (amount > 0) {
                    if (i > amount) i = amount;
                    for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) { amount -= NativeAura.drainVis(w, pos.offset(x * 16, 0, z * 16), i, false); if (amount <= 0) break outer; }
                }
            }
            default -> amount -= NativeAura.drainVis(w, pos, amount, false);
        }
        return amount <= 0.0f;
    }

    public float getConsumptionModifier(ItemStack is, Player player, boolean crafting) {
        float m = 1.0f;
        if (player != null) m -= CasterManager.getTotalVisDiscount(player);
        return Math.max(m, 0.1f);
    }

    public FocusItem getFocus(ItemStack stack) {
        ItemStack fs = getFocusStack(stack);
        return !fs.isEmpty() && fs.getItem() instanceof FocusItem f ? f : null;
    }
    public ItemStack getFocusStack(ItemStack stack) { return stack.getOrDefault(ModComponents.CASTER_FOCUS.get(), net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyOne(); }
    public void setFocus(ItemStack stack, ItemStack focus) {
        if (focus == null || focus.isEmpty()) stack.remove(ModComponents.CASTER_FOCUS.get());
        else stack.set(ModComponents.CASTER_FOCUS.get(), net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(focus.copy())));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
        ItemStack focus = getFocusStack(stack);
        var line = Component.translatable("tc.vis.cost").append(" ");
        if (!focus.isEmpty() && focus.getItem() instanceof FocusItem f) {
            float amt = f.getVisCost(focus);
            if (amt > 0) line.append(Component.literal(FMT.format(amt) + " ").append(Component.translatable("item.Focus.cost1")).withStyle(ChatFormatting.RESET));
        }
        lines.accept(line.withStyle(ChatFormatting.ITALIC, ChatFormatting.AQUA));
        if (getFocus(stack) != null) {
            lines.accept(focus.getHoverName().copy().withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC, ChatFormatting.GREEN));
            getFocus(stack).addFocusInformation(focus, lines);
        }
    }

    @Override
    public void inventoryTick(ItemStack is, ServerLevel w, Entity e, EquipmentSlot slot) {
        if (e.tickCount % 10 == 0 && e instanceof ServerPlayer player) {
            for (ItemStack h : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()})
                if (h.getItem() instanceof CasterItem) { updateAura(w, player); break; }
        }
    }

    private void updateAura(ServerLevel world, ServerPlayer player) {
        float cv = 0, cf = 0; int bv = 0;
        int cx = player.getBlockX() >> 4, cz = player.getBlockZ() >> 4;
        java.util.List<int[]> chunks = new java.util.ArrayList<>();
        switch (area) {
            case 1 -> { chunks.add(new int[] {0, 0}); for (Direction f : Direction.Plane.HORIZONTAL) chunks.add(new int[] {f.getStepX(), f.getStepZ()}); }
            case 2 -> { for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) chunks.add(new int[] {x, z}); }
            default -> chunks.add(new int[] {0, 0});
        }
        for (int[] c : chunks) {
            var p = new BlockPos((cx + c[0]) * 16, 64, (cz + c[1]) * 16);
            if (world.getChunkSource().getChunkNow(cx + c[0], cz + c[1]) == null) continue;
            cv += NativeAura.vis(world, p); cf += NativeAura.flux(world, p); bv += NativeAura.base(world, p);
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new art.arcane.thaumcraft.network.ThaumometerAuraPayload(bv, cv, cf));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos();
        var bs = world.getBlockState(pos);
        if (bs.getBlock() instanceof IInteractWithCaster i && i.onCasterRightClick(world, stack, player, pos, ctx.getClickedFace(), ctx.getHand())) return InteractionResult.PASS;
        if (world.getBlockEntity(pos) instanceof IInteractWithCaster i && i.onCasterRightClick(world, stack, player, pos, ctx.getClickedFace(), ctx.getHand())) return InteractionResult.PASS;
        if (CasterTriggerRegistry.hasTrigger(bs))
            return CasterTriggerRegistry.performTrigger(world, stack, player, pos, ctx.getClickedFace(), bs) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        ItemStack fb = getFocusStack(stack);
        if (!fb.isEmpty()) {
            FocusPackage core = FocusItem.getPackage(fb);
            if (core != null) for (IFocusElement fe : core.nodes) {
                if (fe instanceof IFocusBlockPicker && player.isShiftKeyDown() && world.getBlockEntity(pos) == null) {
                    if (!world.isClientSide()) {
                        ItemStack isout = new ItemStack(bs.getBlock().asItem());
                        if (!bs.is(Blocks.AIR)) {
                            ItemStack is = bs.getCloneItemStack(world, pos, false);
                            if (!is.isEmpty()) isout = is.copyWithCount(1);
                        }
                        storePickedBlock(stack, isout);
                        return InteractionResult.SUCCESS;
                    }
                    player.swing(ctx.getHand());
                    return InteractionResult.PASS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        ItemStack focusStack = getFocusStack(held);
        FocusItem focus = getFocus(held);
        if (focus == null || CasterManager.isOnCooldown(player)) return super.use(world, player, hand);
        CasterManager.setCooldown(player, focus.getActivationTime(focusStack));
        FocusPackage core = FocusItem.getPackage(focusStack);
        if (core == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) for (IFocusElement fe : core.nodes) if (fe instanceof IFocusBlockPicker) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (consumeVis(held, player, focus.getVisCost(focusStack), false, false)) {
            FocusEngine.castFocusPackage(player, core);
            player.swing(hand, true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }

    @Override public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity e) { return 72000; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.BOW; }

    /** ItemCaster as IArchitect: delegates to the first architect node (the PLAN medium) of the mounted focus. */
    public IArchitect getArchitect(ItemStack stack) {
        if (getFocus(stack) == null) return null;
        FocusPackage fp = FocusItem.getPackage(getFocusStack(stack));
        if (fp != null) for (IFocusElement fe : fp.nodes) if (fe instanceof IArchitect a) return a;
        return null;
    }

    public void storePickedBlock(ItemStack stack, ItemStack out) { stack.set(ModComponents.PICKED_BLOCK.get(), net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(out.copy()))); }

    public ItemStack getPickedBlock(ItemStack stack) {
        if (stack == null || stack.isEmpty() || getFocus(stack) == null) return ItemStack.EMPTY;
        var pc = stack.get(ModComponents.PICKED_BLOCK.get());
        if (pc == null) return ItemStack.EMPTY;
        ItemStack picked = pc.copyOne();
        FocusPackage fp = FocusItem.getPackage(getFocusStack(stack));
        if (fp != null) for (IFocusElement fe : fp.nodes) if (fe instanceof IFocusBlockPicker) return picked;
        return ItemStack.EMPTY;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() == this && newStack.getItem() == this) {
            var of = getFocus(oldStack); var nf = getFocus(newStack);
            int s1 = of != null && of.getSortingHelper(getFocusStack(oldStack)) != null ? of.getSortingHelper(getFocusStack(oldStack)).hashCode() : 0;
            int s2 = nf != null && nf.getSortingHelper(getFocusStack(newStack)) != null ? nf.getSortingHelper(getFocusStack(newStack)).hashCode() : 0;
            return s1 != s2;
        }
        return newStack.getItem() != oldStack.getItem();
    }
}
