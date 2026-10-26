package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC4 BlockAiry (node metadata): an intangible, unbreakable light source holding an {@link AuraNodeBlockEntity}.
 * Its small selection box lets the thaumometer scan it and items be used on it (node jar, caster gauntlet).
 */
public final class AuraNodeBlock extends BaseEntityBlock {
    public static final MapCodec<AuraNodeBlock> CODEC = simpleCodec(AuraNodeBlock::new);
    private static final VoxelShape SHAPE = Block.box(5, 5, 5, 11, 11, 11);
    /** Points drained by a caster gauntlet per crystal (TC6 adaptation of TC4 wand tapping). */
    public static final int TAP_COST = 5;
    /** Chunk vis consumed by sealing a node in a jar (TC4 jarring cost wand vis). */
    public static final int JAR_VIS = 20;

    public AuraNodeBlock(Properties p) {
        super(p.strength(-1.0f, 3600000.0f).sound(SoundType.WOOL).lightLevel(s -> 8).noCollision().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
    @Override protected boolean canBeReplaced(BlockState s, net.minecraft.world.item.context.BlockPlaceContext c) { return false; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AuraNodeBlockEntity(pos, state); }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.AURA_NODE.get(), AuraNodeBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity node)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.is(ModItems.NODE_JAR.get())) {
            if (level instanceof ServerLevel sl) jar(sl, pos, node, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof CasterItem) {
            if (level instanceof ServerLevel sl) tap(sl, node, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void say(Player player, Component c) {
        if (player instanceof net.minecraft.server.level.ServerPlayer sp && sp.connection != null) sp.sendOverlayMessage(c);
    }

    /** Draw {@value #TAP_COST} points of the node's largest aspect into one vis crystal of that aspect. */
    public static ItemStack tap(ServerLevel level, AuraNodeBlockEntity node, Player player) {
        String id = node.largest();
        if (id == null || node.aspects.get(id) < TAP_COST) {
            say(player, Component.translatable("tc.node.tap.empty").withStyle(ChatFormatting.DARK_PURPLE));
            return ItemStack.EMPTY;
        }
        node.drain(id, TAP_COST);
        ItemStack crystal = CrystalEssenceItem.of(id, 1);
        if (!player.getInventory().add(crystal.copy())) player.drop(crystal.copy(), false);
        level.playSound(null, node.getBlockPos(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, node.getBlockPos().getX() + 0.5, node.getBlockPos().getY() + 0.5, node.getBlockPos().getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
        return crystal;
    }

    /** Seal the node in a jar: costs chunk vis and may damage the node's modifier (TC4: jarring "can damage nodes"). */
    public static ItemStack jar(ServerLevel level, BlockPos pos, AuraNodeBlockEntity node, Player player, ItemStack jar) {
        if (!player.isCreative() && NativeAura.vis(level, pos) < JAR_VIS) {
            say(player, Component.translatable("tc.node.jar.novis").withStyle(ChatFormatting.DARK_PURPLE));
            return ItemStack.EMPTY;
        }
        if (!player.isCreative()) NativeAura.drainVis(level, pos, JAR_VIS, false);
        if (level.getRandom().nextInt(4) == 0) node.modifier = node.modifier.worse();
        ItemStack out = new ItemStack(ModItems.JARRED_NODE.get());
        out.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(node.toTag()));
        level.removeBlock(pos, false);
        jar.consume(1, player);
        if (!player.getInventory().add(out.copy())) player.drop(out.copy(), false);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 0.8f);
        return out;
    }
}
