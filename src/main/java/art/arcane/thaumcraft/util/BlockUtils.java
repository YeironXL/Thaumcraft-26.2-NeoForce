package art.arcane.thaumcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;

/** TC6 common.lib.utils.BlockUtils (the parts used by ported systems). */
public final class BlockUtils {
    public static boolean harvestBlock(Level world, Player player, BlockPos pos) { return harvestBlock(world, player, pos, false, false, 0, false); }
    public static boolean harvestBlockSkipCheck(Level world, Player player, BlockPos pos) { return harvestBlock(world, player, pos, false, false, 0, true); }

    /** Breaks a block as if mined by the player, optionally forcing drops, silk touch or a fortune level. */
    public static boolean harvestBlock(Level world, Player p, BlockPos pos, boolean alwaysDrop, boolean silkOverride, int fortuneOverride, boolean skipEvent) {
        if (!(world instanceof ServerLevel level) || !(p instanceof ServerPlayer player)) return false;
        BlockState state = level.getBlockState(pos);
        if (!skipEvent && CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled()) return false;
        var be = level.getBlockEntity(pos);
        Block block = state.getBlock();
        if (block instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) { level.sendBlockUpdated(pos, state, state, 3); return false; }
        level.levelEvent(null, 2001, pos, Block.getId(state));
        boolean removed;
        if (player.isCreative()) {
            removed = remove(level, player, pos, state, false, ItemStack.EMPTY);
        } else {
            ItemStack held = player.getMainHandItem();
            boolean canHarvest = alwaysDrop || state.canHarvestBlock(level, pos, player);
            removed = remove(level, player, pos, state, canHarvest, held);
            if (removed && canHarvest) {
                ItemStack fake = held.copy();
                var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var fortune = enchants.getOrThrow(Enchantments.FORTUNE);
                if (silkOverride || fortuneOverride > EnchantmentHelper.getEnchantmentLevel(fortune, player)) {
                    if (alwaysDrop || fake.isEmpty()) fake = new ItemStack(Items.STICK); // ItemsTC.enchantedPlaceholder
                    if (silkOverride) fake.enchant(enchants.getOrThrow(Enchantments.SILK_TOUCH), 1);
                    int fort = Math.max(fortuneOverride, EnchantmentHelper.getItemEnchantmentLevel(fortune, fake));
                    if (fort > 0) fake.enchant(fortune, fort);
                }
                block.playerDestroy(level, player, pos, state, be, fake);
            }
        }
        return removed;
    }

    private static boolean remove(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, boolean canHarvest, ItemStack tool) {
        boolean flag = state.onDestroyedByPlayer(level, pos, player, tool, canHarvest, level.getFluidState(pos));
        if (flag) state.getBlock().destroy(level, pos, state);
        return flag;
    }

    public static boolean isBlockExposed(LevelReader world, BlockPos pos) {
        for (Direction face : Direction.values()) if (!world.getBlockState(pos.relative(face)).isSolidRender()) return true;
        return false;
    }

    public static boolean isBlockTouching(LevelReader world, BlockPos pos, Block block) {
        for (Direction face : Direction.values()) if (world.getBlockState(pos.relative(face)).is(block)) return true;
        return false;
    }

    /** BlockUtils.hasLOS: nothing solid between the two block centres (the source block itself is ignored). */
    public static boolean hasLOS(net.minecraft.world.level.Level world, BlockPos source, BlockPos target) {
        var from = net.minecraft.world.phys.Vec3.atCenterOf(source);
        var to = net.minecraft.world.phys.Vec3.atCenterOf(target);
        var dir = to.subtract(from).normalize().scale(0.5);
        var hit = world.clip(new net.minecraft.world.level.ClipContext(from.add(dir), to, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(target);
    }

    private BlockUtils() {}
}
