package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.TcBannerBlockEntity;
import art.arcane.thaumcraft.item.EssentiaPhialItem;
import art.arcane.thaumcraft.registry.ModComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * TC6 BlockBannerTC (dyed banners and the crimson cult banner), drawn by its tile renderer. Placed on top of a block it
 * stands on a pole facing the player (16 rotations); placed on a side it hangs from the wall. A dyed banner takes an
 * aspect from a filled phial (sneak-click clears it); the aspect stays on the dropped banner. No collision.
 */
public final class TcBannerBlock extends BaseEntityBlock {
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 15);
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    private static final VoxelShape STANDING = Block.box(5.28, 0, 5.28, 10.56, 32, 10.56);
    @Nullable public final DyeColor dye;

    public TcBannerBlock(Properties p, @Nullable DyeColor dye) {
        super(p.strength(1.0F).sound(SoundType.WOOD).noOcclusion().noCollision());
        this.dye = dye;
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0).setValue(WALL, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(p -> new TcBannerBlock(p, dye)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ROTATION, WALL); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TcBannerBlockEntity(pos, state); }

    /** BlockBannerTCItem.onItemUse: never from below. */
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        if (side == Direction.DOWN) return null;
        if (side == Direction.UP) {
            float yaw = ctx.getPlayer() == null ? 0 : ctx.getPlayer().getYRot();
            return defaultBlockState().setValue(ROTATION, Mth.floor((yaw + 180.0f) * 16.0f / 360.0f + 0.5) & 15);
        }
        int i = switch (side) { case NORTH -> 8; case WEST -> 4; case EAST -> 12; default -> 0; };
        return defaultBlockState().setValue(WALL, true).setValue(ROTATION, i);
    }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        if (!s.getValue(WALL)) return STANDING;
        return switch (s.getValue(ROTATION)) {
            case 0 -> Block.box(0, -16, 0, 16, 16, 4);
            case 8 -> Block.box(0, -16, 12, 16, 16, 16);
            case 12 -> Block.box(0, -16, 0, 4, 16, 16);
            case 4 -> Block.box(12, -16, 0, 16, 16, 16);
            default -> Shapes.block();
        };
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level w, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!p.isShiftKeyDown() && !(stack.getItem() instanceof EssentiaPhialItem)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!w.isClientSide() && dye != null && w.getBlockEntity(pos) instanceof TcBannerBlockEntity te) {
            if (p.isShiftKeyDown()) te.setAspect("");
            else {
                String a = stack.get(ModComponents.ASPECT.get());
                if (a == null || a.isEmpty()) return InteractionResult.PASS;
                te.setAspect(a);
                stack.shrink(1);
            }
            w.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level w, BlockPos pos, Player p, BlockHitResult hit) {
        if (p.isShiftKeyDown() && !w.isClientSide() && dye != null && w.getBlockEntity(pos) instanceof TcBannerBlockEntity te) {
            te.setAspect("");
            w.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
