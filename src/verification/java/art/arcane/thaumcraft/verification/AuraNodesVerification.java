package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.aura.node.AuraNodeGen;
import art.arcane.thaumcraft.aura.node.NodeModifier;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.block.AuraNodeBlock;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.item.JarredNodeItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.scan.ScanManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Aura nodes (TC4 nodes adapted to TC6): generation, regeneration, chunk aura, jarring, tapping, scanning, type behaviour. */
public final class AuraNodesVerification {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static AuraNodeBlockEntity node(GameTestHelper h, BlockPos rel, NodeType type, NodeModifier mod) {
        BlockPos abs = h.absolutePos(rel);
        check(AuraNodeGen.place(h.getLevel(), abs, type, mod, RandomSource.create(7), false), "node not placed at " + rel);
        return (AuraNodeBlockEntity) h.getLevel().getBlockEntity(abs);
    }

    /** Generation rolls, silverwood pure node, regeneration and modifier rates. */
    public static void generation(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var n = node(h, new BlockPos(3, 3, 3), null, null);
        check(n.initialized && !n.base.isEmpty() && n.base.size() >= 2 && n.base.size() <= 5, "random node has 2..5 aspects: " + n.base);
        n.base.forEach((k, v) -> check(v > 0 && v <= 255 && n.aspects.get(k).equals(v), "generated node starts full"));
        // roll statistics: special types ~1/18, modifiers ~1/9 (TC4 config defaults)
        int special = 0, modded = 0, runs = 4000;
        var r = RandomSource.create(1234);
        var biome = level.getBiome(n.getBlockPos());
        for (int i = 0; i < runs; i++) {
            AuraNodeGen.roll(n, biome, r, null, null, false);
            if (n.type != NodeType.NORMAL) special++;
            if (n.modifier != NodeModifier.NONE) modded++;
        }
        check(special > runs / 30 && special < runs / 10, "special node rate off: " + special + "/" + runs);
        check(modded > runs / 15 && modded < runs / 5, "modifier rate off: " + modded + "/" + runs);
        check(AuraNodeGen.NODE_RARITY == 36 && AuraNodeGen.SPECIAL_RARITY == 18, "TC4 rarity constants changed");
        // modifiers: TC4 regen intervals
        check(NodeModifier.NONE.regen == 600 && NodeModifier.BRIGHT.regen == 400 && NodeModifier.PALE.regen == 900 && NodeModifier.FADING.regen == 0, "regen intervals differ from TC4");
        var regen = node(h, new BlockPos(6, 3, 3), NodeType.NORMAL, NodeModifier.NONE);
        String id = regen.aspects.keySet().iterator().next();
        int max = regen.base.get(id);
        regen.aspects.put(id, max - 2);
        check(regen.regenStep() && regen.aspects.get(id) == max - 1, "regeneration did not add one point");
        regen.regenStep();
        check(!regen.regenStep() && regen.aspects.get(id) == max, "regeneration exceeded the maximum");
        // draining an aspect dry shrinks the node
        regen.drain(id, 999);
        check(regen.base.getOrDefault(id, 0) == max - 1, "fully drained aspect did not shrink the node");
        // silverwood: a pure node inside the trunk
        BlockPos ground = new BlockPos(10, 1, 10);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) { h.setBlock(ground.offset(x, 0, z), Blocks.GRASS_BLOCK); h.setBlock(ground.offset(x, -1, z), Blocks.DIRT); }
        boolean grown = new art.arcane.thaumcraft.worldgen.SilverwoodTree(false, 7, 4).generate(level, new java.util.Random(5), h.absolutePos(ground.above()));
        check(grown, "silverwood did not generate");
        AuraNodeBlockEntity pure = null;
        for (int y = 2; y < 14; y++) if (level.getBlockEntity(h.absolutePos(new BlockPos(10, y, 10))) instanceof AuraNodeBlockEntity a) pure = a;
        check(pure != null && pure.type == NodeType.PURE && pure.base.containsKey(AspectRegistry.AURAM.id()), "silverwood trunk lacks a pure node");
        System.out.println("Aura node generation passed: rolls (special " + special + ", modifiers " + modded + " of " + runs + "), regen, shrink, silverwood.");
        h.succeed();
    }

    /** Nodes feed vis into their chunk up to base + half their size; flux by type. */
    public static void aura(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var n = node(h, new BlockPos(4, 3, 4), NodeType.NORMAL, NodeModifier.NONE);
        BlockPos p = n.getBlockPos();
        var aura = NativeAura.get(level, level.getChunkAt(p));
        aura.setVis(0); aura.setFlux(0);
        int before = n.total();
        for (int i = 0; i < 5; i++) n.feedAura(level);
        check(aura.vis() >= 5 && n.total() == before - 5, "node did not breathe vis into its chunk: vis=" + aura.vis() + " total " + before + "->" + n.total());
        aura.setVis(n.auraCap(level));
        int t2 = n.total();
        n.feedAura(level);
        check(n.total() == t2, "node fed vis above its cap");
        check(n.auraCap(level) == aura.base() + n.totalBase() / 2, "aura cap formula changed");
        // tainted adds flux, pure removes it
        var t = node(h, new BlockPos(4, 3, 12), NodeType.TAINTED, NodeModifier.NONE);
        var tAura = NativeAura.get(level, level.getChunkAt(t.getBlockPos()));
        float f0 = tAura.flux();
        t.typeEffects(level);
        check(tAura.flux() > f0, "tainted node added no flux");
        level.removeBlock(t.getBlockPos(), false);
        var pn = node(h, new BlockPos(4, 3, 12), NodeType.PURE, NodeModifier.NONE);
        tAura.setFlux(5);
        pn.typeEffects(level);
        check(tAura.flux() < 5, "pure node did not remove flux");
        // pure nodes cleanse taint
        BlockPos taint = h.absolutePos(new BlockPos(5, 2, 12));
        level.setBlock(taint, ModBlocks.TAINT_SOIL.get().defaultBlockState(), 3);
        AuraNodeBlockEntity.purify(level, taint);
        check(level.getBlockState(taint).is(Blocks.DIRT), "purify left taint soil");
        // unstable nodes keep their total while shifting aspects
        var u = node(h, new BlockPos(10, 3, 4), NodeType.UNSTABLE, NodeModifier.NONE);
        for (int i = 0; i < 400; i++) u.typeEffects(level);
        check(u.total() > 0, "unstable node vanished");
        // bullying: the bigger node drains the smaller one within 4 blocks
        var big = node(h, new BlockPos(12, 3, 12), NodeType.NORMAL, NodeModifier.NONE);
        var small = node(h, new BlockPos(14, 3, 12), NodeType.NORMAL, NodeModifier.NONE);
        big.set(AspectRegistry.AER, 100); small.aspects.clear(); small.base.clear(); small.set(AspectRegistry.IGNIS, 5);
        int bt = big.total();
        big.bully(level);
        check(small.amount(AspectRegistry.IGNIS) == 4 && big.total() == bt + 1, "bullying did not move a point");
        System.out.println("Aura node aura passed: vis feeding/cap, tainted flux, pure cleansing, unstable, bullying.");
        h.succeed();
    }

    /** Node jar round trip, caster tapping and thaumometer scanning. */
    public static void jar(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var n = node(h, new BlockPos(4, 3, 4), NodeType.HUNGRY, NodeModifier.BRIGHT);
        n.set(AspectRegistry.AQUA, 60);
        BlockPos p = n.getBlockPos();
        // scanning: node discovery and per-node knowledge
        ScanManager.scan(player, p);
        var research = PlayerProgressManager.get(player);
        check(research.has("!AuraNode"), "scanning a node did not unlock !AuraNode");
        check(research.has("!aqua") || research.scanned("node:" + level.dimension().identifier() + "@" + p.toShortString().replace(" ", "")), "node scan gave no aspect knowledge");
        // tapping with the caster gauntlet
        int aqua = n.amount(AspectRegistry.AQUA);
        ItemStack crystal = AuraNodeBlock.tap(level, n, player);
        check(!crystal.isEmpty() && CrystalEssenceItem.aspect(crystal).equals(n.largest() == null ? "" : CrystalEssenceItem.aspect(crystal)), "tap gave no crystal");
        check(n.amount(AspectRegistry.AQUA) == aqua - AuraNodeBlock.TAP_COST && CrystalEssenceItem.aspect(crystal).equals("aqua"), "tap did not drain the largest aspect");
        // jar: needs vis, may damage, keeps the node
        var aura = NativeAura.get(level, level.getChunkAt(p));
        aura.setVis(5);
        ItemStack jar = new ItemStack(ModItems.NODE_JAR.get(), 2);
        check(AuraNodeBlock.jar(level, p, n, player, jar).isEmpty() && level.getBlockState(p).is(ModBlocks.AURA_NODE.get()), "jarring without vis succeeded");
        aura.setVis(100);
        var before = new java.util.LinkedHashMap<>(n.aspects);
        ItemStack jarred = AuraNodeBlock.jar(level, p, n, player, jar);
        check(jarred.is(ModItems.JARRED_NODE.get()) && level.getBlockState(p).isAir() && jar.getCount() == 1, "jarring failed");
        check(Math.abs(aura.vis() - (100 - AuraNodeBlock.JAR_VIS)) < 0.01, "jarring did not cost " + AuraNodeBlock.JAR_VIS + " vis");
        var data = JarredNodeItem.data(jarred);
        check(data.getIntOr("nodetype", -1) == NodeType.HUNGRY.ordinal(), "jar lost the node type");
        int mod = data.getIntOr("nodemod", -1);
        check(mod == NodeModifier.BRIGHT.ordinal() || mod == NodeModifier.NONE.ordinal(), "jar damage went too far: " + mod);
        // release it elsewhere
        BlockPos floor = h.absolutePos(new BlockPos(8, 1, 8));
        level.setBlock(floor, Blocks.STONE.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, jarred);
        var ctx = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false));
        jarred.useOn(ctx);
        check(level.getBlockEntity(floor.above()) instanceof AuraNodeBlockEntity back && back.type == NodeType.HUNGRY && back.aspects.equals(before), "released node differs");
        check(player.getMainHandItem().isEmpty(), "jarred node not consumed");
        System.out.println("Aura node jar passed: scan, tap, jar cost/damage, release round trip.");
        h.succeed();
    }

    /** Hungry nodes pull and eat items, gaining their aspects; obsidian is spared. */
    public static void hungry(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var n = node(h, new BlockPos(8, 6, 8), NodeType.HUNGRY, NodeModifier.NONE);
        n.aspects.clear(); n.base.clear(); n.set(AspectRegistry.VACUOS, 10);
        BlockPos c = n.getBlockPos();
        var item = new ItemEntity(level, c.getX() + 4.5, c.getY() + 0.5, c.getZ() + 0.5, new ItemStack(Items.IRON_INGOT, 8));
        item.setNoGravity(true);
        level.addFreshEntity(item);
        BlockPos obs = h.absolutePos(new BlockPos(8, 4, 8));
        level.setBlock(obs, Blocks.OBSIDIAN.defaultBlockState(), 3);
        h.runAfterDelay(80, () -> {
            check(item.isRemoved(), "hungry node did not swallow the item (at " + item.position() + ")");
            check(n.amount(AspectRegistry.METALLUM) > 0, "hungry node gained no metallum: " + n.aspects);
            for (int i = 0; i < 300; i++) n.eatBlock(level);
            check(level.getBlockState(obs).is(Blocks.OBSIDIAN), "hungry node ate obsidian");
            System.out.println("Aura node hungry passed: pull, swallow, aspect gain, obsidian immune.");
            h.succeed();
        });
    }
}
