package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ResearchRules;
import art.arcane.thaumcraft.scan.ScanManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** TC6 ScanningManager triggers registered by ConfigResearch.initScannables. */
final class ScanVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var state = PlayerProgressManager.get(player);
        var orePos = new BlockPos(1, 1, 1);
        h.setBlock(orePos, ModBlocks.ORE_AMBER.get());
        ScanManager.scan(player, h.absolutePos(orePos));
        check(ResearchRules.complete(state, "ORE") && state.has("!OREAMBER") && !state.has("!ORECINNABAR"), "Amber ore scan did not complete ORE/!OREAMBER");
        check(state.has("!terra") && state.has("!vinculum") && state.has("!vitreus"), "Amber aspects not discovered: " + state.researched());
        int basics = state.knowledge("OBSERVATION", "BASICS");
        ScanManager.scan(player, h.absolutePos(orePos));
        check(state.knowledge("OBSERVATION", "BASICS") == basics, "Rescan gave knowledge again");
        // Container contents are scanned (ScanningManager.scanTheThing on BlockPos).
        var chestPos = new BlockPos(3, 1, 1);
        h.setBlock(chestPos, Blocks.CHEST);
        var chest = h.getBlockEntity(chestPos, net.minecraft.world.level.block.entity.ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.ARROW)); chest.setItem(1, new ItemStack(Items.DRAGON_BREATH));
        ScanManager.scan(player, h.absolutePos(chestPos));
        check(state.has("f_arrow") && state.has("!DRAGONBREATH"), "Chest contents not scanned");
        // Entities: bats give f_BAT and f_FLY.
        var bat = h.spawn(net.minecraft.world.entity.EntityTypes.BAT, new BlockPos(5, 2, 5));
        ScanManager.scan(player, bat);
        check(state.has("f_BAT") && state.has("f_FLY") && state.scanned("entity:minecraft:bat"), "Bat scan triggers missing");
        // ScanOreDictionary f_MATIRON via an iron ingot.
        ScanManager.scan(player, new ItemStack(Items.IRON_INGOT));
        check(state.has("f_MATIRON"), "Iron material trigger missing");
        System.out.println("Scanning passed: block/entity/item/ore-dictionary triggers, aspect discoveries, single generic reward and container contents.");
        h.succeed();
    }
}
