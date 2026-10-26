package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.CondenserBlock;
import art.arcane.thaumcraft.block.EssentiaLampBlock;
import art.arcane.thaumcraft.block.EssentiaTransfuserBlock;
import art.arcane.thaumcraft.block.VisBatteryBlock;
import art.arcane.thaumcraft.blockentity.BarrierStoneBlockEntity;
import art.arcane.thaumcraft.blockentity.BrainJarBlockEntity;
import art.arcane.thaumcraft.blockentity.CondenserBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaLampBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTransfuserBlockEntity;
import art.arcane.thaumcraft.blockentity.TcBannerBlockEntity;
import art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

/** TC6 blocks and items that were registered but empty before Remaster 1.2. */
final class Tc6GapsVerification {
    private static void check(boolean c, String m) { if (!c) throw new net.minecraft.gametest.framework.GameTestAssertException(net.minecraft.network.chat.Component.literal(m), 0); }
    private static List<ItemEntity> items(ServerLevel w, BlockPos p, double r) { return w.getEntitiesOfClass(ItemEntity.class, new AABB(p).inflate(r)); }
    private static void clearItems(ServerLevel w, BlockPos p) { items(w, p, 6).forEach(ItemEntity::discard); }
    private static void setField(Object o, String name, Object v) {
        try { var f = o.getClass().getDeclaredField(name); f.setAccessible(true); f.set(o, v); } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    /** Urns and crates break into TC6 loot; silk touch is not needed for that. Loot bags open into 8-12 rolls. */
    static void loot(GameTestHelper h) {
        var w = h.getLevel();
        int total = 0;
        for (var b : List.of(ModBlocks.LOOT_URN_COMMON, ModBlocks.LOOT_URN_UNCOMMON, ModBlocks.LOOT_URN_RARE, ModBlocks.LOOT_CRATE_COMMON, ModBlocks.LOOT_CRATE_UNCOMMON, ModBlocks.LOOT_CRATE_RARE)) {
            BlockPos rel = new BlockPos(4, 2, 4), abs = h.absolutePos(rel);
            clearItems(w, abs);
            h.setBlock(rel, b.get());
            w.destroyBlock(abs, true);
            int n = items(w, abs, 2).size();
            check(n >= 1, b.getId() + " dropped nothing");
            check(items(w, abs, 2).stream().noneMatch(e -> e.getItem().is(b.get().asItem())), b.getId() + " dropped itself without silk touch");
            total += n;
        }
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos at = h.absolutePos(new BlockPos(8, 2, 8));
        player.setPos(Vec3.atBottomCenterOf(at));
        clearItems(w, at);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LOOT_BAG_RARE.get()));
        player.getMainHandItem().getItem().use(w, player, InteractionHand.MAIN_HAND);
        int bag = items(w, at, 3).size();
        check(player.getMainHandItem().isEmpty(), "loot bag not used up");
        check(bag >= 4, "loot bag gave " + bag + " stacks");
        System.out.println("Loot passed: urns/crates " + total + " stacks, rare bag " + bag);
        h.succeed();
    }

    /** The vis battery charges from a rich chunk, gives vis back to a poor one and empties into the aura when powered. */
    static void visBattery(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4), pos = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.VIS_BATTERY.get());
        int base = NativeAura.base(w, pos);
        check(base > 0, "chunk has no aura base");
        NativeAura.addVis(w, pos, base * 2.0f);
        float before = NativeAura.vis(w, pos);
        w.getBlockState(pos).randomTick(w, pos, w.getRandom());
        check(w.getBlockState(pos).getValue(VisBatteryBlock.CHARGE) == 1, "battery did not charge from a rich chunk");
        check(NativeAura.vis(w, pos) < before, "charging did not drain vis");
        NativeAura.drainVis(w, pos, NativeAura.vis(w, pos), false);
        w.getBlockState(pos).randomTick(w, pos, w.getRandom());
        check(w.getBlockState(pos).getValue(VisBatteryBlock.CHARGE) == 0, "battery did not discharge into a poor chunk");
        check(NativeAura.vis(w, pos) >= 1.0f, "discharge did not add vis");
        w.setBlock(pos, w.getBlockState(pos).setValue(VisBatteryBlock.CHARGE, 5), 3);
        h.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        for (int i = 0; i < 5; i++) w.getBlockState(pos).tick(w, pos, w.getRandom());
        check(w.getBlockState(pos).getValue(VisBatteryBlock.CHARGE) == 0, "powered battery did not empty: " + w.getBlockState(pos).getValue(VisBatteryBlock.CHARGE));
        check(w.getBlockState(pos).getLightEmission() == 0, "empty battery still glows");
        System.out.println("Vis battery passed: charge, discharge, redstone dump");
        h.succeed();
    }

    /** A void siphon near a visible flux rift fills up with void seeds; hoppers may take them but not insert. */
    static void voidSiphon(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4), pos = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.VOID_SIPHON.get());
        var te = (VoidSiphonBlockEntity) w.getBlockEntity(pos);
        var rift = new FluxRiftEntity(ModEntities.FLUX_RIFT.get(), w);
        rift.setPos(pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5);
        w.addFreshEntity(rift);
        rift.setRiftSize(16);
        check(te.validRifts(w).contains(rift), "siphon does not see the rift");
        te.progress = VoidSiphonBlockEntity.PROGREQ - 1;
        for (int i = 0; i < 20; i++) VoidSiphonBlockEntity.tick(w, pos, w.getBlockState(pos), te);
        check(te.getItem(0).is(ModItems.VOID_SEED.get()), "no void seed: " + te.getItem(0) + " progress " + te.progress);
        check(te.canTakeItemThroughFace(0, te.getItem(0), Direction.DOWN) && !te.canPlaceItemThroughFace(0, new ItemStack(ModItems.VOID_SEED.get()), Direction.UP), "hopper rules");
        // Redstone switches it off.
        h.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        check(!w.getBlockState(pos).getValue(art.arcane.thaumcraft.block.VoidSiphonBlock.ENABLED), "redstone did not disable the siphon");
        rift.discard();
        System.out.println("Void siphon passed: seed " + te.getItem(0));
        h.succeed();
    }

    /** Travel paving speeds up walkers; barrier paving raises a ward that blocks creatures but not players, unless powered. */
    static void paving(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos travel = h.absolutePos(new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.PAVING_STONE_TRAVEL.get());
        var zombie = new Zombie(net.minecraft.world.entity.EntityTypes.ZOMBIE, w);
        zombie.setPos(Vec3.atBottomCenterOf(travel.above()));
        w.getBlockState(travel).getBlock().stepOn(w, travel, w.getBlockState(travel), zombie);
        check(zombie.hasEffect(MobEffects.SPEED) && zombie.hasEffect(MobEffects.JUMP_BOOST), "travel paving gave no speed/jump");

        BlockPos rel = new BlockPos(6, 2, 6), stone = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.PAVING_STONE_BARRIER.get());
        var te = (BarrierStoneBlockEntity) w.getBlockEntity(stone);
        for (int i = 0; i < 200; i++) BarrierStoneBlockEntity.tick(w, stone, w.getBlockState(stone), te);
        check(w.getBlockState(stone.above()).is(ModBlocks.BARRIER.get()) && w.getBlockState(stone.above(2)).is(ModBlocks.BARRIER.get()), "no barrier above the stone");
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        var shapeMob = w.getBlockState(stone.above()).getCollisionShape(w, stone.above(), CollisionContext.of(zombie));
        var shapePlayer = w.getBlockState(stone.above()).getCollisionShape(w, stone.above(), CollisionContext.of(player));
        check(!shapeMob.isEmpty(), "barrier does not stop creatures");
        check(shapePlayer.isEmpty(), "barrier stops players");
        h.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        check(w.getBlockState(stone.above(2)).getCollisionShape(w, stone.above(2), CollisionContext.of(zombie)).isEmpty(), "powered barrier still blocks");
        h.setBlock(rel, Blocks.AIR);
        check(!w.getBlockState(stone.above()).is(ModBlocks.BARRIER.get()), "barrier stayed without its stone");
        System.out.println("Paving passed: travel effects, barrier for mobs only, redstone off, removal");
        h.succeed();
    }

    /** The brain in a jar eats experience orbs and keeps its xp on the dropped item. */
    static void brainJar(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4), pos = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.JAR_BRAIN.get());
        var te = (BrainJarBlockEntity) w.getBlockEntity(pos);
        var orb = new ExperienceOrb(w, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 17);
        w.addFreshEntity(orb);
        BrainJarBlockEntity.tick(w, pos, w.getBlockState(pos), te);
        check(te.xp == 17, "brain xp " + te.xp);
        check(!orb.isAlive(), "orb not eaten");
        clearItems(w, pos);
        w.destroyBlock(pos, true);
        var drop = items(w, pos, 2).stream().filter(e -> e.getItem().is(ModItems.BLOCK_JAR_BRAIN.get())).findFirst();
        check(drop.isPresent(), "brain jar dropped nothing");
        Integer kept = drop.get().getItem().get(ModComponents.BRAIN_XP.get());
        check(kept != null && kept == 17, "xp on dropped jar " + kept);
        System.out.println("Brain jar passed: ate 17 xp, kept on the item");
        h.succeed();
    }

    /** The emptying transfuser offers its tube essentia from a nearby jar and drains it. */
    static void transfuser(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos jarRel = new BlockPos(2, 6, 2), rel = new BlockPos(6, 3, 6), pos = h.absolutePos(rel);
        h.setBlock(jarRel, ModBlocks.JAR_NORMAL.get());
        art.arcane.thaumcraft.essentia.EssentiaHandler.clear();
        var jar = (EssentiaJarBlockEntity) w.getBlockEntity(h.absolutePos(jarRel));
        check(jar.insert("ignis", 10), "jar fill");
        h.setBlock(rel.below(), Blocks.STONE);
        w.setBlock(pos, ModBlocks.ESSENTIA_OUTPUT.get().defaultBlockState().setValue(EssentiaTransfuserBlock.FACING, Direction.UP), 3);
        var te = (EssentiaTransfuserBlockEntity) w.getBlockEntity(pos);
        check("ignis".equals(te.offered("ignis", Direction.DOWN)), "transfuser does not offer the jar's ignis");
        check("".equals(te.offered("aqua", Direction.DOWN)), "transfuser offers essentia it cannot find");
        check(te.extractOne("ignis", Direction.DOWN), "transfuser extract failed");
        check(jar.contents().amount() == 9, "jar amount " + jar.contents().amount());
        System.out.println("Essentia transfuser passed: offered and drained ignis from a jar 5 blocks away");
        h.succeed();
    }

    /** Growth lamp feeds nearby plants; fertility lamp breeds two idle adults. */
    static void lamps(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 6, 4), pos = h.absolutePos(rel);
        h.setBlock(rel.above(), Blocks.STONE);
        w.setBlock(pos, ModBlocks.LAMP_GROWTH.get().defaultBlockState().setValue(EssentiaLampBlock.FACING, Direction.UP), 3);
        h.setBlock(new BlockPos(5, 1, 5), Blocks.FARMLAND);
        h.setBlock(new BlockPos(5, 2, 5), Blocks.WHEAT);
        var te = (EssentiaLampBlockEntity) w.getBlockEntity(pos);
        te.charges = 20;
        for (int i = 0; i < 400 && te.charges == 20; i++) EssentiaLampBlockEntity.tick(w, pos, w.getBlockState(pos), te);
        check(te.charges < 20, "growth lamp never fed the wheat");
        check(!(w.getBlockState(h.absolutePos(new BlockPos(5, 2, 5))).getBlock() instanceof CropBlock c) || c.getAge(w.getBlockState(h.absolutePos(new BlockPos(5, 2, 5)))) >= 0, "wheat");

        BlockPos rel2 = new BlockPos(10, 6, 10), pos2 = h.absolutePos(rel2);
        h.setBlock(rel2.above(), Blocks.STONE);
        w.setBlock(pos2, ModBlocks.LAMP_FERTILITY.get().defaultBlockState().setValue(EssentiaLampBlock.FACING, Direction.UP), 3);
        var cow1 = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(10, 2, 9));
        var cow2 = h.spawn(net.minecraft.world.entity.EntityTypes.COW, new BlockPos(9, 2, 10));
        var te2 = (EssentiaLampBlockEntity) w.getBlockEntity(pos2);
        te2.charges = 10;
        EssentiaLampBlockEntity.tick(w, pos2, w.getBlockState(pos2), te2);
        check(cow1.isInLove() && cow2.isInLove(), "fertility lamp did not breed the cows");
        check(te2.charges == 5, "fertility charges " + te2.charges);
        System.out.println("Lamps passed: growth fed a plant (" + te.charges + " charges left), fertility bred two cows");
        h.succeed();
    }

    /** Lattices above a condenser set its speed, a loose lattice drops, it turns essentia into captured flux, and a filter cleans a clogged lattice. */
    static void condenser(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4), pos = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.CONDENSER.get());
        for (int y = 1; y <= 3; y++) w.setBlock(pos.above(y), ModBlocks.CONDENSER_LATTICE.get().defaultBlockState(), 3);
        var te = (CondenserBlockEntity) w.getBlockEntity(pos);
        te.triggerCheck();
        check(te.latticeCount > 0 && te.interval > 0 && te.interval < 600, "lattice count " + te.latticeCount + " interval " + te.interval);
        BlockPos loose = h.absolutePos(new BlockPos(10, 5, 10));
        w.setBlock(loose, ModBlocks.CONDENSER_LATTICE.get().defaultBlockState(), 3);
        check(!w.getBlockState(loose).is(ModBlocks.CONDENSER_LATTICE.get()), "a lattice with no condenser below stayed");

        NativeAura.addFlux(w, pos, 20.0f);
        float flux0 = NativeAura.flux(w, pos);
        setField(te, "essentia", 50);
        setField(te, "count", te.interval - 1);
        CondenserBlockEntity.tick(w, pos, w.getBlockState(pos), te);
        check(te.flux() == 1, "condenser stored flux " + te.flux());
        check(NativeAura.flux(w, pos) < flux0, "chunk flux not drained");
        check("vitium".equals(te.offered("", Direction.DOWN)) && te.extractOne("vitium", Direction.DOWN) && te.flux() == 0, "stored flux not offered below as vitium");
        h.setBlock(rel.east(), Blocks.REDSTONE_BLOCK);
        check(!w.getBlockState(pos).getValue(CondenserBlock.ENABLED), "redstone did not disable the condenser");

        BlockPos top = pos.above(3);
        w.setBlock(top, ModBlocks.CONDENSER_LATTICE_DIRTY.get().defaultBlockState(), 3);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FILTER.get(), 2));
        w.getBlockState(top).useItemOn(player.getMainHandItem(), w, player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(top), Direction.NORTH, top, false));
        check(w.getBlockState(top).is(ModBlocks.CONDENSER_LATTICE.get()), "filter did not clean the lattice");
        check(player.getMainHandItem().getCount() == 1, "filter not used");
        System.out.println("Flux condenser passed: interval " + te.interval + ", captured and offered flux, lattice rules, cleaning");
        h.succeed();
    }

    /** A dyed banner takes an aspect, keeps it when broken, and the crimson banner can be placed by the portal boss. */
    static void banner(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos rel = new BlockPos(4, 2, 4), pos = h.absolutePos(rel);
        h.setBlock(rel, ModBlocks.BANNER_RED.get());
        var te = (TcBannerBlockEntity) w.getBlockEntity(pos);
        check(te != null, "banner has no block entity");
        te.setAspect("ignis");
        clearItems(w, pos);
        w.destroyBlock(pos, true);
        var drop = items(w, pos, 2).stream().filter(e -> e.getItem().is(ModItems.BLOCK_BANNER_RED.get())).findFirst();
        check(drop.isPresent(), "banner dropped nothing");
        check("ignis".equals(drop.get().getItem().get(ModComponents.ASPECT.get())), "aspect lost on drop");
        h.setBlock(rel, ModBlocks.BANNER_CRIMSON_CULT.get());
        check(w.getBlockEntity(pos) instanceof TcBannerBlockEntity, "cult banner has no block entity");
        System.out.println("Banner passed: aspect kept on the dropped banner");
        h.succeed();
    }
}
