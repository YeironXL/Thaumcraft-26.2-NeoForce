package art.arcane.thaumcraft.golems;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TC6 CropUtils as configured by ModConfig.postInitMisc: melons and pumpkins are standard crops at any state, sugar
 * cane and cactus are stacked crops (only the upper blocks), nether wart is grown at age 3; any growable block that can
 * no longer grow (except stems) and fully grown crop blocks count as grown.
 */
public final class CropUtils {
    public static final Set<Block> clickableCrops = new HashSet<>();

    public static boolean isClickable(BlockState bs) { return clickableCrops.contains(bs.getBlock()); }

    public static boolean isGrownCrop(Level world, BlockPos pos) {
        if (world.isEmptyBlock(pos)) return false;
        BlockState bs = world.getBlockState(pos);
        Block bi = bs.getBlock();
        boolean standard = bi == Blocks.MELON || bi == Blocks.PUMPKIN || (bi instanceof NetherWartBlock && bs.getValue(NetherWartBlock.AGE) == 3);
        boolean stacked = bi == Blocks.SUGAR_CANE || bi == Blocks.CACTUS;
        boolean found = standard || stacked || isClickable(bs);
        Block below = world.getBlockState(pos.below()).getBlock();
        return (bi instanceof BonemealableBlock g && !g.isValidBonemealTarget(world, pos, bs) && !(bi instanceof StemBlock))
                || (bi instanceof CropBlock c && c.isMaxAge(bs) && !found)
                || standard || isClickable(bs) || (stacked && below == bi);
    }

    /** ThaumcraftApi.getSeed: the item that plants this block (cocoa uses cocoa beans). */
    public static ItemStack getSeed(Block block) {
        if (block == Blocks.COCOA) return new ItemStack(Items.COCOA_BEANS);
        var item = block.asItem();
        if (item instanceof BlockItem bi && bi.getBlock() == block && (block instanceof CropBlock || block instanceof NetherWartBlock || block instanceof net.minecraft.world.level.block.SaplingBlock))
            return new ItemStack(item);
        return ItemStack.EMPTY;
    }
    private CropUtils() {}
}
