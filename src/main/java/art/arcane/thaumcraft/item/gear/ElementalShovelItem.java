package art.arcane.thaumcraft.item.gear;

import art.arcane.thaumcraft.casters.IArchitect;
import art.arcane.thaumcraft.fx.Fx;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemElementalShovel (Shovel of the Earthmover): THAUMIUM_ELEMENTAL shovel, rare. Right clicking a block face places a
 * 3x3 panel of that block from the inventory (grass falls back to dirt), one durability per block. The panel lies on the
 * clicked face (orientation 0) or stands out of it vertically / horizontally (1, 2); G cycles the orientation ("or" byte).
 * While sneaking the panel is previewed through IArchitect. 3x3 digging is the DESTRUCTIVE infusion enchantment.
 */
public class ElementalShovelItem extends Item implements IArchitect {
    public ElementalShovelItem(Properties p) { super(p.shovel(ElementalGear.TOOL, 1.5F, -3.0F)); }

    public static byte getOrientation(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("or") ? tag.getByteOr("or", (byte) 0) : 0;
    }
    public static void setOrientation(ItemStack stack, byte o) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putByte("or", (byte) (o % 3)));
    }

    /** The 3x3 offsets used by both onItemUse and getArchitectBlocks. */
    private static ArrayList<BlockPos> panel(byte o, BlockPos pos, Direction side, float yaw) {
        ArrayList<BlockPos> out = new ArrayList<>(9);
        int s = side.ordinal();
        for (int aa = -1; aa <= 1; ++aa) for (int bb = -1; bb <= 1; ++bb) {
            int xx = 0, yy = 0, zz = 0;
            if (o == 1) {
                yy = bb;
                if (s <= 1) { int l = Mth.floor(yaw * 4.0f / 360.0f + 0.5) & 3; if (l == 0 || l == 2) xx = aa; else zz = aa; }
                else if (s <= 3) zz = aa;
                else xx = aa;
            } else if (o == 2) {
                if (s <= 1) { int l = Mth.floor(yaw * 4.0f / 360.0f + 0.5) & 3; yy = bb; if (l == 0 || l == 2) xx = aa; else zz = aa; }
                else { zz = bb; xx = aa; }
            } else if (s <= 1) { xx = aa; zz = bb; }
            else if (s <= 3) { xx = aa; yy = bb; }
            else { zz = aa; yy = bb; }
            out.add(pos.relative(side).offset(xx, yy, zz));
        }
        return out;
    }

    private static boolean canPlaceAt(Level level, BlockState bs, BlockPos p) {
        return level.getBlockState(p).canBeReplaced() && bs.canSurvive(level, p);
    }

    /** InventoryUtils.consumePlayerItem: take one of the item from anywhere in the inventory. */
    private static boolean consume(Player player, Item item) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(item)) { s.shrink(1); inv.setChanged(); return true; }
        }
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos();
        Direction side = ctx.getClickedFace();
        InteractionHand hand = ctx.getHand();
        BlockState bs = world.getBlockState(pos);
        if (world.getBlockEntity(pos) != null) return InteractionResult.FAIL;
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        boolean did = false;
        for (BlockPos p2 : panel(getOrientation(player.getItemInHand(hand)), pos, side, player.getYRot())) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) break;
            if (!canPlaceAt(world, bs, p2)) continue;
            BlockState placed = null;
            if (player.getAbilities().instabuild || consume(player, bs.getBlock().asItem())) placed = bs;
            else if (bs.is(Blocks.GRASS_BLOCK) && consume(player, Blocks.DIRT.asItem())) placed = Blocks.DIRT.defaultBlockState();
            if (placed == null) continue;
            world.playSound(null, p2, bs.getSoundType(world, pos, player).getBreakSound(), SoundSource.BLOCKS, 0.6f, 0.9f + world.getRandom().nextFloat() * 0.2f);
            world.setBlock(p2, placed, 3);
            held.hurtAndBreak(1, player, hand.asEquipmentSlot());
            Fx.bamf((ServerLevel) world, p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, 8401408, false, false, side);
            did = true;
        }
        if (did) player.swing(hand, true);
        return did ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    // ---------------- IArchitect ----------------
    @Override
    public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
        ArrayList<BlockPos> b = new ArrayList<>();
        if (!player.isShiftKeyDown()) return b;
        BlockState bs = world.getBlockState(pos);
        for (BlockPos p2 : panel(getOrientation(stack), pos, side, player.getYRot())) if (canPlaceAt(world, bs, p2)) b.add(p2);
        return b;
    }
    @Override public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) { return false; }
    @Override public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
        Vec3 eye = player.getEyePosition();
        double reach = player instanceof Player pl ? pl.blockInteractionRange() : 4.5;
        return world.clip(new ClipContext(eye, eye.add(player.getViewVector(1.0f).scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }
    @Override public boolean useBlockHighlight(ItemStack stack) { return true; }

}
