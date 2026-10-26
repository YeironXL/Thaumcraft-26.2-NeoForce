package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.essentia.EssentiaHandler;
import art.arcane.thaumcraft.infusion.InfusionEnchantment;
import art.arcane.thaumcraft.infusion.InfusionEnchantmentRecipe;
import art.arcane.thaumcraft.infusion.InfusionRecipe;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModRecipes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import art.arcane.thaumcraft.entity.FollowingItemEntity;

/** TC6 infusion crafting: a full stabilised craft, an instability event, and an infusion enchantment with its effect. */
final class InfusionCraftVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    /** Altar: central pedestal at (5,1,5), arcane pillars on the corners, runic matrix two blocks above. */
    private static InfusionMatrixBlockEntity altar(GameTestHelper h) {
        BlockPos ped = new BlockPos(5, 1, 5);
        h.setBlock(ped, ModBlocks.PEDESTAL_ARCANE.get());
        for (int[] c : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) h.setBlock(ped.offset(c[0], 0, c[1]), ModBlocks.PILLAR_ARCANE.get());
        h.setBlock(ped.above(2), ModBlocks.INFUSION_MATRIX.get());
        return h.getBlockEntity(ped.above(2), InfusionMatrixBlockEntity.class);
    }

    private static PedestalBlockEntity pedestal(GameTestHelper h, BlockPos p, ItemStack item) {
        h.setBlock(p, ModBlocks.PEDESTAL_ARCANE.get());
        var pe = h.getBlockEntity(p, PedestalBlockEntity.class);
        pe.setItem(0, item);
        return pe;
    }

    private static EssentiaJarBlockEntity jar(GameTestHelper h, BlockPos p, String aspect, int amount) {
        h.setBlock(p, ModBlocks.JAR_NORMAL.get());
        var j = h.getBlockEntity(p, EssentiaJarBlockEntity.class);
        check(j.insert(aspect, amount), "Jar fill");
        return j;
    }

    private static void click(GameTestHelper h, InfusionMatrixBlockEntity te, ServerPlayer player) {
        te.onCasterRightClick(h.getLevel(), new ItemStack(ModItems.CASTER_BASIC.get()), player, te.getBlockPos(), Direction.UP, InteractionHand.MAIN_HAND);
    }

    /** Ring of Clouds (CLOUDRING): symmetric pedestals and candles, 50 aer from a nearby jar, both components drained. */
    static void craft(GameTestHelper h) {
        EssentiaHandler.clear();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        PlayerProgressManager.get(player).unlock("CLOUDRING");
        var te = altar(h);
        h.getBlockEntity(new BlockPos(5, 1, 5), PedestalBlockEntity.class).setItem(0, new ItemStack(ModItems.BAUBLES_RING_MUNDANE.get()));
        var p1 = pedestal(h, new BlockPos(5, 1, 2), CrystalEssenceItem.of("aer", 1));
        var p2 = pedestal(h, new BlockPos(5, 1, 8), new ItemStack(Items.FEATHER));
        h.setBlock(new BlockPos(2, 1, 5), ModBlocks.CANDLE_WHITE.get());
        h.setBlock(new BlockPos(8, 1, 5), ModBlocks.CANDLE_WHITE.get());
        var j = jar(h, new BlockPos(2, 1, 2), "aer", 60);
        click(h, te, player);
        check(te.active, "Matrix did not activate");
        te.stability = te.stabilityCap; // an altar left active for a while charges up to its cap
        click(h, te, player);
        check(te.crafting, "Infusion did not start (recipe not found)");
        check(te.stabilityReplenish > 0, "Symmetric candles should stabilise, got " + te.stabilityReplenish);
        check(te.recipeEssentia().getOrDefault("aer", 0) == 50, "Essentia cost " + te.recipeEssentia());
        check(te.recipeInstability() == 1, "Instability");
        h.succeedWhen(() -> {
            h.assertTrue(!te.crafting, "still crafting, essentia left " + te.recipeEssentia() + ", items left " + te.recipeIngredients().size());
            var out = h.getBlockEntity(new BlockPos(5, 1, 5), PedestalBlockEntity.class).item();
            h.assertTrue(out.is(ModItems.CLOUD_RING.get()), "Central item is " + out);
            h.assertTrue(p1.item().isEmpty() && p2.item().isEmpty(), "Components not consumed");
            h.assertTrue(j.contents().amount() == 10, "Jar should keep 10 aer, has " + j.contents().amount());
            h.assertTrue(PlayerProgressManager.get(player).has("CLOUDRING"), "research");
            System.out.println("Infusion craft passed: activation, recipe lookup, stability gain, 50 aer drained from the jar, components consumed, Ring of Clouds made.");
        });
    }

    /** An unbalanced stabilizer makes the altar unstable; pulling the central item mid-craft forces an instability event (mock players cannot join the level, so the nearby-player effects are not asserted). */
    static void instability(GameTestHelper h) {
        EssentiaHandler.clear();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        PlayerProgressManager.get(player).unlock("CLOUDRING");
        var te = altar(h);
        var center = h.getBlockEntity(new BlockPos(5, 1, 5), PedestalBlockEntity.class);
        center.setItem(0, new ItemStack(ModItems.BAUBLES_RING_MUNDANE.get()));
        pedestal(h, new BlockPos(5, 1, 2), CrystalEssenceItem.of("aer", 1));
        pedestal(h, new BlockPos(5, 1, 8), new ItemStack(Items.FEATHER));
        h.setBlock(new BlockPos(2, 1, 5), ModBlocks.STABILIZER.get());
        jar(h, new BlockPos(2, 1, 2), "aer", 60);
        click(h, te, player);
        click(h, te, player);
        check(te.crafting, "Infusion did not start");
        check(te.stabilityReplenish < 0, "Lone stabilizer should destabilise, got " + te.stabilityReplenish);
        check(!te.problemBlocks().isEmpty(), "Asymmetric stabilizer not flagged as a problem block");
        h.runAfterDelay(20, () -> {
            float before = te.stability;
            center.setItem(0, ItemStack.EMPTY);
            te.craftCycle();
            check(!te.crafting, "Craft should stop when the central item is gone");
            check(te.stability > before, "Instability event should release stability (" + before + " -> " + te.stability + ")");
            System.out.println("Infusion instability passed: asymmetric stabilizer penalty, forced instability event releasing stability and failing the craft.");
            h.succeed();
        });
    }

    /** Collector: the enchantment recipe matches a stone axe, applies level 1, and the axe's block drops land on the harvester. */
    static void enchantment(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var research = PlayerProgressManager.get(player);
        research.unlock("INFUSIONENCHANTMENT");
        research.setStage("INFUSIONENCHANTMENT", 99);
        int loaded = level.getServer().getRecipeManager().recipeMap().byType(ModRecipes.INFUSION_TYPE.get()).size();
        check(loaded == 62, "Expected all 56 TC6 infusion recipes + the Outer Lands eldritch eye + the TC4 Thaumostatic Harness and Girdle + the TC4 travelling trunk + the TC4 fishing and alchemy seals to load, got " + loaded + ": "
                + level.getServer().getRecipeManager().recipeMap().byType(ModRecipes.INFUSION_TYPE.get()).stream().map(r -> r.id().identifier().getPath()).sorted().toList());
        var holder = level.getServer().getRecipeManager().recipeMap().byType(ModRecipes.INFUSION_TYPE.get()).stream()
                .filter(r -> r.value() instanceof InfusionEnchantmentRecipe ie && ie.enchantment() == InfusionEnchantment.COLLECTOR).findFirst().orElseThrow();
        InfusionRecipe recipe = holder.value();
        ItemStack axe = new ItemStack(Items.STONE_AXE);
        List<ItemStack> comps = List.of(new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Items.LEAD));
        check(recipe.matches(comps, axe, level, player), "Collector recipe should accept a stone axe");
        check(!recipe.matches(comps, new ItemStack(Items.STICK), level, player), "Collector recipe accepted a stick");
        check(recipe.aspects(player, axe, comps).equals(java.util.Map.of("desiderium", 80, "aqua", 100)), "Collector cost " + recipe.aspects(player, axe, comps));
        ItemStack out = recipe.output(player, axe, comps).stack();
        check(InfusionEnchantment.getInfusionEnchantmentLevel(out, InfusionEnchantment.COLLECTOR) == 1, "Collector not applied");
        check(!recipe.matches(comps, out, level, player), "Max level collector axe still accepted");
        // Effect: the log's drop is delivered to the harvester instead of the block.
        player.setItemInHand(InteractionHand.MAIN_HAND, out);
        BlockPos log = new BlockPos(3, 1, 3);
        h.setBlock(log, Blocks.OAK_LOG);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1, 9.5)));
        var abs = h.absolutePos(log);
        var state = level.getBlockState(abs);
        level.removeBlock(abs, false);
        Block.dropResources(state, level, abs, null, player, out);
        var following = level.getEntitiesOfClass(FollowingItemEntity.class, new AABB(abs).inflate(1.5), e -> e.getItem().is(Items.OAK_LOG));
        check(following.size() == 1 && following.get(0).target == player && following.get(0).isFollowing(), "Collector drop did not become a following item targeting the harvester: " + following.size());
        System.out.println("Infusion enchantment passed: collector recipe match/cost/output, max level gate, drop became a following item.");
        h.succeed();
    }

    /** Collector (EntityFollowingItem): a harvested log flies from the block to the harvester and stops following there. */
    static void collectorFollow(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.STONE_AXE);
        InfusionEnchantment.addInfusionEnchantment(axe, InfusionEnchantment.COLLECTOR, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        BlockPos log = new BlockPos(2, 1, 2);
        h.setBlock(log, Blocks.OAK_LOG);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 1, 9.5)));
        var abs = h.absolutePos(log);
        var state = level.getBlockState(abs);
        level.removeBlock(abs, false);
        Block.dropResources(state, level, abs, null, player, axe);
        var spawned = level.getEntitiesOfClass(FollowingItemEntity.class, new AABB(abs).inflate(1.5));
        check(spawned.size() == 1, "No following item spawned at the block: " + spawned.size());
        var fi = spawned.get(0);
        check(fi.target == player, "Following item does not target the harvester");
        double start = fi.distanceTo(player);
        h.succeedWhen(() -> {
            h.assertTrue(!fi.isFollowing(), "Following item still flying, " + String.format("%.2f", fi.position().distanceTo(player.position().add(0, player.getBbHeight() / 2.0, 0))) + " from the player (started " + String.format("%.2f", start) + ")");
            h.assertTrue(fi.isRemoved() || fi.position().distanceTo(player.position().add(0, player.getBbHeight() / 2.0, 0)) < 1.0, "Following item stopped away from the player");
            System.out.println("Collector following item passed: flew " + String.format("%.2f", start) + " blocks to the harvester.");
        });
    }
}
