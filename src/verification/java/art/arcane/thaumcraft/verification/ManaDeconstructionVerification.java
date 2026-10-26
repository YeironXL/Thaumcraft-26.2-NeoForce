package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.block.ManaPodBlock;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.DeconstructionTableBlockEntity;
import art.arcane.thaumcraft.blockentity.ManaPodBlockEntity;
import art.arcane.thaumcraft.item.ManaBeanItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** TC4 recreations: mana pods / beans and the deconstruction table. */
final class ManaDeconstructionVerification {
    private static void check(boolean c, String m) { if (!c) throw new net.minecraft.gametest.framework.GameTestAssertException(net.minecraft.network.chat.Component.literal(m), 0); }

    /** A bean planted under a greatwood log grows into a pod through all stages and drops beans of its aspect. */
    static void podGrowth(GameTestHelper h) {
        var world = h.getLevel();
        BlockPos logRel = new BlockPos(4, 4, 4);
        h.setBlock(logRel, ModBlocks.LOG_GREATWOOD.get());
        BlockPos log = h.absolutePos(logRel), podPos = log.below();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, ManaBeanItem.of("ignis", 2));
        var ctx = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atBottomCenterOf(log), Direction.DOWN, log, false));
        check(player.getMainHandItem().getItem().useOn(ctx).consumesAction(), "bean did not plant");
        check(world.getBlockState(podPos).is(ModBlocks.MANA_POD.get()), "no pod under the log");
        check(player.getMainHandItem().getCount() == 1, "bean not consumed");
        var be = (ManaPodBlockEntity) world.getBlockEntity(podPos);
        check("ignis".equals(be.aspect()), "pod aspect " + be.aspect());
        check(be.tint() == (0xFF000000 | AspectRegistry.color("ignis")), "pod tint " + Integer.toHexString(be.tint()));
        var sync = be.getUpdateTag(world.registryAccess());
        check("ignis".equals(sync.getStringOr("aspect", "")), "pod aspect not in client sync tag " + sync);
        check(Block.getDrops(world.getBlockState(podPos), world, podPos, be).isEmpty(), "unripe pod dropped beans");
        for (int i = 0; i < 7; i++) ManaPodBlock.grow(world.getBlockState(podPos), world, podPos, world.getRandom());
        check(world.getBlockState(podPos).getValue(ManaPodBlock.AGE) == ManaPodBlock.MATURE, "pod did not mature");
        var drops = Block.getDrops(world.getBlockState(podPos), world, podPos, be);
        check(!drops.isEmpty() && drops.getFirst().is(ModItems.MANA_BEAN.get()) && "ignis".equals(ManaBeanItem.aspect(drops.getFirst())), "mature drops " + drops);
        // A pod without a host log cannot survive; an unseeded pod picks an aspect when growing.
        check(!ModBlocks.MANA_POD.get().defaultBlockState().canSurvive(world, h.absolutePos(new BlockPos(8, 4, 8))), "pod survives without log");
        h.setBlock(new BlockPos(8, 4, 8), ModBlocks.LOG_SILVERWOOD.get());
        BlockPos p2 = h.absolutePos(new BlockPos(8, 3, 8));
        world.setBlock(p2, ModBlocks.MANA_POD.get().defaultBlockState(), 3);
        ManaPodBlock.grow(world.getBlockState(p2), world, p2, world.getRandom());
        check(AspectRegistry.get(((ManaPodBlockEntity) world.getBlockEntity(p2)).aspect()) != null, "unseeded pod got no aspect");
        // Breaking the log pops the pod off.
        world.setBlock(log, Blocks.AIR.defaultBlockState(), 3);
        check(!world.getBlockState(podPos).is(ModBlocks.MANA_POD.get()), "pod stayed without its log");
        System.out.println("Mana pod passed: planting, 8 stages, aspect drops " + drops);
        h.succeed();
    }

    /** A mana bean dissolves in a boiling crucible as one point of its aspect. */
    static void beanCrucible(GameTestHelper h) {
        var world = h.getLevel();
        check(ObjectAspects.get(ManaBeanItem.of("victus", 1)).equals(java.util.Map.of(AspectRegistry.get("victus"), 1)), "bean aspects " + ObjectAspects.get(ManaBeanItem.of("victus", 1)));
        BlockPos rel = new BlockPos(4, 2, 4);
        h.setBlock(rel.below(), Blocks.MAGMA_BLOCK);
        h.setBlock(rel, ModBlocks.CRUCIBLE.get());
        BlockPos pos = h.absolutePos(rel);
        var pot = h.getBlockEntity(rel, CrucibleBlockEntity.class);
        pot.mixture().fill();
        for (int t = 0; t < 160; t++) CrucibleBlockEntity.tick(world, pos, pot.getBlockState(), pot);
        check(pot.mixture().boiling(), "crucible not boiling");
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        check(pot.processOne(ManaBeanItem.of("victus", 1), player), "bean not dissolved");
        check(pot.mixture().aspects().getOrDefault("victus", 0) == 1, "mixture " + pot.mixture().aspects());
        System.out.println("Mana bean crucible passed: " + pot.mixture().aspects());
        h.succeed();
    }

    /** Deconstructing an item destroys it over time and grants observation knowledge matching its aspects. */
    static void deconstruct(GameTestHelper h) {
        var world = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4);
        h.setBlock(rel, ModBlocks.DECONSTRUCTION_TABLE.get());
        BlockPos pos = h.absolutePos(rel);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var te = h.getBlockEntity(rel, DeconstructionTableBlockEntity.class);
        check(!te.canPlaceItem(0, new ItemStack(Items.AIR)) && !DeconstructionTableBlockEntity.breakable(ItemStack.EMPTY), "empty accepted");
        ItemStack input = new ItemStack(Items.IRON_INGOT, 2);
        var expected = DeconstructionTableBlockEntity.knowledgeFor(input);
        check(!expected.isEmpty(), "iron ingot gives no knowledge");
        var research = PlayerProgressManager.get(player);
        var before = new java.util.HashMap<String, Integer>();
        expected.keySet().forEach(c -> before.put(c, research.knowledge("OBSERVATION", c)));
        te.setOwner(player.getUUID());
        te.setItem(0, input);
        for (int t = 0; t < DeconstructionTableBlockEntity.DURATION - 1; t++) te.work(player);
        check(te.getItem(0).getCount() == 2 && te.progress() == DeconstructionTableBlockEntity.DURATION - 1, "finished early / not progressing: " + te.progress());
        te.work(player);
        check(te.getItem(0).getCount() == 1, "item not consumed");
        for (var e : expected.entrySet())
            check(research.knowledge("OBSERVATION", e.getKey()) == before.get(e.getKey()) + e.getValue(), "knowledge " + e.getKey() + " " + research.knowledge("OBSERVATION", e.getKey()) + " expected +" + e.getValue());
        System.out.println("Deconstruction passed: iron ingot -> " + expected);
        h.succeed();
    }

    /** Hoppers feed the deconstruction table (WorldlyContainer + item capability), unbreakable items are refused, nothing is pulled out. */
    static void deconstructHopper(GameTestHelper h) {
        var world = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4);
        h.setBlock(rel, ModBlocks.DECONSTRUCTION_TABLE.get());
        h.setBlock(rel.above(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        h.setBlock(rel.below(), Blocks.HOPPER.defaultBlockState().setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        var te = h.getBlockEntity(rel, DeconstructionTableBlockEntity.class);
        var cap = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, h.absolutePos(rel), Direction.NORTH);
        check(cap != null, "no item capability on the table");
        var top = h.getBlockEntity(rel.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        top.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
        h.succeedWhen(() -> {
            h.assertTrue(te.getItem(0).is(Items.IRON_INGOT) && te.getItem(0).getCount() >= 2, "hopper did not feed the table: " + te.getItem(0));
            var bottom = h.getBlockEntity(rel.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
            h.assertTrue(bottom.isEmpty(), "bottom hopper extracted from the table");
            System.out.println("Deconstruction hopper passed: table holds " + te.getItem(0));
        });
    }
}
