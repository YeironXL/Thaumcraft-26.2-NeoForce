package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.PillarBlock;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 infusion altar: salis mundus multiblock (research gate, pillars/air), pedestal item handling, matrix activation and stability. */
final class InfusionAltarVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos ped = new BlockPos(5, 1, 5), matrix = ped.above(2);
        h.setBlock(ped, ModBlocks.PEDESTAL_ARCANE.get());
        h.setBlock(matrix, ModBlocks.INFUSION_MATRIX.get());
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) { h.setBlock(ped.offset(c[0], 0, c[1]), ModBlocks.STONE_ARCANE.get()); h.setBlock(ped.offset(c[0], 1, c[1]), ModBlocks.STONE_ARCANE.get()); }
        var abs = h.absolutePos(ped.offset(1, 1, 1));
        var salis = new ItemStack(ModItems.SALIS_MUNDUS.get(), 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, salis);
        player.setPos(Vec3.atCenterOf(abs).add(2, 0, 2));
        var ctx = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        check(!ModItems.SALIS_MUNDUS.get().useOn(ctx).consumesAction(), "Altar formed without INFUSION research");
        PlayerProgressManager.get(player).unlock("INFUSION");
        check(ModItems.SALIS_MUNDUS.get().useOn(ctx).consumesAction(), "Salis did not recognise the altar");
        // pedestal item
        var pe = h.getBlockEntity(ped, PedestalBlockEntity.class);
        pe.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        check(pe.item().getCount() == 1, "Pedestal item");
        h.runAfterDelay(60, () -> {
            for (int[] c : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
                check(h.getBlockState(ped.offset(c[0], 0, c[1])).getBlock() instanceof PillarBlock, "Lower stone not turned into pillar at " + c[0] + "," + c[1]);
                check(h.getBlockState(ped.offset(c[0], 1, c[1])).isAir(), "Upper stone not removed");
            }
            var te = h.getBlockEntity(matrix, InfusionMatrixBlockEntity.class);
            check(te.validLocation(), "Formed altar invalid");
            te.onCasterRightClick(level, new ItemStack(ModItems.CASTER_BASIC.get()), player, h.absolutePos(matrix), Direction.UP, InteractionHand.MAIN_HAND);
            check(te.active, "Matrix did not activate");
            // break pillar returns two arcane stone
            level.destroyBlock(h.absolutePos(ped.offset(1, 0, 1)), false);
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(ped.offset(1, 0, 1))).inflate(1), e -> e.getItem().is(ModItems.BLOCK_STONE_ARCANE.get()));
            check(drops.stream().mapToInt(e -> e.getItem().getCount()).sum() == 2, "Pillar did not return two arcane stone");
            check(!te.validLocation(), "Broken altar still valid");
            System.out.println("Infusion altar passed: research gate, salis multiblock to pillars/air, pedestal, matrix activation, pillar drops.");
            h.succeed();
        });
    }
}
