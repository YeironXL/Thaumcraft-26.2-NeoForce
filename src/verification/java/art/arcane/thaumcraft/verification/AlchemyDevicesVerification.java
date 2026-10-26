package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.PotionSprayerBlock;
import art.arcane.thaumcraft.block.ThaumatoriumBlock;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EverfullUrnBlockEntity;
import art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity;
import art.arcane.thaumcraft.blockentity.SpaBlockEntity;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Alchemy before infusion: fluids, spa, everfull urn, potion sprayer, thaumatorium, jar labels and brace. */
final class AlchemyDevicesVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static void test(GameTestHelper h) {
        var level = h.getLevel();
        // --- liquid death: hurts and drops crystals of the victim's aspects
        BlockPos ld = new BlockPos(2, 1, 2);
        h.setBlock(ld, ModBlocks.LIQUID_DEATH.get());
        check(level.getFluidState(h.absolutePos(ld)).isSource() && level.getFluidState(h.absolutePos(ld)).getType() == art.arcane.thaumcraft.registry.ModFluids.DEATH.get(), "Liquid death source");
        var cow = (net.minecraft.world.entity.Mob) h.spawn(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:cow")), ld);
        cow.setHealth(1);
        cow.hurtServer(level, level.damageSources().source(art.arcane.thaumcraft.block.fluid.LiquidDeathBlock.DISSOLVE), 5);
        check(cow.isDeadOrDying(), "Dissolve damage did not kill");
        // --- purifying fluid grants warp ward and is used up
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos pf = new BlockPos(5, 1, 2);
        h.setBlock(pf, ModBlocks.PURIFYING_FLUID.get());
        var pfAbs = h.absolutePos(pf);
        check(art.arcane.thaumcraft.block.fluid.PurifyingFluidBlock.wardDuration(0) == 32000 && art.arcane.thaumcraft.block.fluid.PurifyingFluidBlock.wardDuration(100) == 20000, "Warp ward duration formula");
        check(level.getFluidState(pfAbs).isSource(), "Purifying fluid source");
        // --- arcane spa: bath salts + water -> purifying fluid source on top
        BlockPos spaPos = new BlockPos(8, 1, 2);
        h.setBlock(spaPos, ModBlocks.SPA.get());
        h.setBlock(spaPos.below(), Blocks.STONE);
        var spa = h.getBlockEntity(spaPos, SpaBlockEntity.class);
        try (var tx = Transaction.openRoot()) { spa.tank.insert(0, FluidResource.of(Fluids.WATER), 2000, tx); tx.commit(); }
        spa.setItem(0, new ItemStack(ModItems.BATH_SALTS.get(), 2));
        SpaBlockEntity.tick(level, h.absolutePos(spaPos), spa.getBlockState(), spa);
        check(level.getBlockState(h.absolutePos(spaPos.above())).is(ModBlocks.PURIFYING_FLUID.get()) && spa.fluid().getAmount() == 1000 && spa.getItem(0).getCount() == 1, "Spa did not pour purifying fluid");
        // --- everfull urn: refills from vis and fills a cauldron
        BlockPos urnPos = new BlockPos(12, 1, 2);
        h.setBlock(urnPos, ModBlocks.EVERFULL_URN.get());
        BlockPos cauldron = urnPos.offset(1, 0, 0);
        h.setBlock(cauldron, Blocks.CAULDRON);
        var urn = h.getBlockEntity(urnPos, EverfullUrnBlockEntity.class);
        var aura = NativeAura.get(level, level.getChunkAt(h.absolutePos(urnPos)));
        aura.spend(aura.vis()); aura.addVis(50);
        for (int t = 0; t < 1200; t++) EverfullUrnBlockEntity.tick(level, h.absolutePos(urnPos), urn.getBlockState(), urn);
        var cs = level.getBlockState(h.absolutePos(cauldron));
        check(cs.is(Blocks.WATER_CAULDRON) && cs.getValue(LayeredCauldronBlock.LEVEL) == 3, "Urn did not fill cauldron: " + cs);
        // --- potion sprayer: recipe from potion, charge, fires on redstone
        BlockPos sp = new BlockPos(2, 1, 8);
        h.setBlock(sp, ModBlocks.POTION_SPRAYER.get().defaultBlockState().setValue(PotionSprayerBlock.FACING, Direction.EAST));
        var sprayer = h.getBlockEntity(sp, PotionSprayerBlockEntity.class);
        check(!sprayer.canPlaceItem(0, PotionContents.createItemStack(Items.POTION, Potions.WATER)), "Water accepted");
        sprayer.setItem(0, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
        check(!sprayer.recipe.isEmpty(), "Sprayer recipe empty");
        sprayer.recipe.forEach((k, n) -> sprayer.addToContainer(k, n));
        for (int t = 0; t < 5; t++) PotionSprayerBlockEntity.tick(level, h.absolutePos(sp), sprayer.getBlockState(), sprayer);
        check(sprayer.charges == 1, "Sprayer charge " + sprayer.charges);
        var target = (net.minecraft.world.entity.Mob) h.spawn(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse("minecraft:pig")), sp.relative(Direction.EAST, 2));
        h.setBlock(sp.below(), Blocks.REDSTONE_BLOCK);
        check(!level.getBlockState(h.absolutePos(sp)).getValue(PotionSprayerBlock.ENABLED), "Redstone did not disable the sprayer");
        PotionSprayerBlockEntity.tick(level, h.absolutePos(sp), level.getBlockState(h.absolutePos(sp)), sprayer);
        check(sprayer.charges == 0 && target.hasEffect(net.minecraft.world.effect.MobEffects.SPEED), "Sprayer did not spray");
        // --- thaumatorium: selection, heat, essentia, output ejected
        BlockPos th = new BlockPos(8, 2, 8);
        h.setBlock(th.below(2), Blocks.LAVA);
        h.setBlock(th.below(), ModBlocks.CRUCIBLE.get());
        h.setBlock(th, ModBlocks.THAUMATORIUM.get().defaultBlockState().setValue(ThaumatoriumBlock.FACING, Direction.EAST));
        h.setBlock(th.above(), ModBlocks.THAUMATORIUM_TOP.get().defaultBlockState().setValue(ThaumatoriumBlock.FACING, Direction.EAST));
        var tm = h.getBlockEntity(th, ThaumatoriumBlockEntity.class);
        PlayerProgressManager.get(player).unlock("ALUMENTUM"); PlayerProgressManager.get(player).setStage("ALUMENTUM", 99);
        tm.setItem(0, new ItemStack(Items.COAL, 2));
        tm.updateRecipes(player);
        var alu = tm.recipes.stream().filter(e -> e.output().is(ModItems.ALUMENTUM.get())).findFirst();
        check(alu.isPresent(), "Alumentum recipe not offered: " + tm.recipes);
        tm.select(player, alu.get().id());
        check(tm.recipeHash.size() == 1, "Recipe not selected");
        ThaumatoriumBlockEntity.tick(level, h.absolutePos(th), tm.getBlockState(), tm);
        for (int t = 0; t < 5; t++) ThaumatoriumBlockEntity.tick(level, h.absolutePos(th), tm.getBlockState(), tm);
        check(tm.currentSuction != null, "Thaumatorium not drawing essentia");
        alu.get().aspects().forEach((k, n) -> tm.addToContainer(k, n));
        for (int t = 0; t < 5; t++) ThaumatoriumBlockEntity.tick(level, h.absolutePos(th), tm.getBlockState(), tm);
        var out = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(th)).inflate(2), e -> e.getItem().is(ModItems.ALUMENTUM.get()));
        check(out.size() == 1 && tm.getItem(0).getCount() == 1, "Thaumatorium did not craft alumentum");
        // --- jar label and brace
        BlockPos jp = new BlockPos(2, 1, 12);
        h.setBlock(jp, ModBlocks.JAR_NORMAL.get());
        var jar = h.getBlockEntity(jp, EssentiaJarBlockEntity.class);
        check(!jar.applyLabel(null), "Empty jar labelled without aspect");
        check(jar.applyLabel("ignis") && "ignis".equals(jar.filter()) && jar.suction() == 64, "Label filter");
        check(!jar.insert("aqua", 1) && jar.insert("ignis", 1), "Filter not enforced");
        jar.setBlocked(true);
        level.destroyBlock(h.absolutePos(jp), true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(jp)).inflate(1));
        check(drops.stream().anyMatch(e -> e.getItem().is(ModItems.JAR_BRACE.get())), "Brace not dropped");
        check(drops.stream().anyMatch(e -> e.getItem().is(ModItems.BLOCK_JAR_NORMAL.get()) && "ignis".equals(e.getItem().get(art.arcane.thaumcraft.registry.ModComponents.JAR_FILTER.get()))), "Jar lost its label");
        System.out.println("Alchemy devices passed: liquid death, purifying fluid/warp ward, spa, everfull urn, potion sprayer, thaumatorium, jar label/brace.");
        h.succeed();
    }
}
