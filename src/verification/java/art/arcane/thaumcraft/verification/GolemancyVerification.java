package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemPlacerItem;
import art.arcane.thaumcraft.golems.GolemProperties;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Golemancy: press crafting and golem stats, item collection to a chest, guard seal combat, crossbow turret. */
final class GolemancyVerification {
    private static void floor(GameTestHelper h) { for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE); }
    private static void check(boolean c, String m) { if (!c) throw new net.minecraft.gametest.framework.GameTestAssertException(net.minecraft.network.chat.Component.literal(m), 0); }
    private static GolemEntity golem(GameTestHelper h, ServerPlayer owner, GolemProperties props, BlockPos at) {
        GolemEntity g = ModEntities.GOLEM.get().create(h.getLevel(), EntitySpawnReason.SPAWN_ITEM_USE);
        Vec3 p = h.absoluteVec(Vec3.atBottomCenterOf(at));
        g.snapTo(p.x, p.y, p.z, 0, 0);
        g.setProperties(props);
        g.setOwned(true); g.setValidSpawn(); g.setOwnerId(owner.getUUID());
        g.initialSpawn();
        h.getLevel().addFreshEntity(g);
        return g;
    }

    /** Press: thaumium + smart head + claws; components taken from an adjacent chest, a placer comes out, the golem has the TC6 stats. */
    static void press(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos core = new BlockPos(5, 1, 5);
        h.setBlock(core, ModBlocks.GOLEM_BUILDER.get());
        h.setBlock(core.east(), Blocks.CHEST);
        GolemProperties props = new GolemProperties();
        props.setMaterial(GolemProperties.MATERIALS.get(4)); props.setHead(GolemProperties.HEADS.get(1)); props.setArms(GolemProperties.ARMS.get(2));
        var chest = (ChestBlockEntity) h.getBlockEntity(core.east(), ChestBlockEntity.class);
        ItemStack[] comps = props.generateComponents();
        int slot = 0;
        for (ItemStack c : comps) for (int n = c.getCount(); n > 0; ) { int k = Math.min(n, c.getMaxStackSize()); chest.setItem(slot++, c.copyWithCount(k)); n -= k; }
        var te = (GolemBuilderBlockEntity) h.getBlockEntity(core, GolemBuilderBlockEntity.class);
        check(te.checkCraft(props.toLong()).length == comps.length, "checkCraft size");
        boolean[] has = te.checkCraft(props.toLong());
        for (int i = 0; i < has.length; i++) check(has[i], "Component " + comps[i] + " not found in chest; handler=" + art.arcane.thaumcraft.golems.InvHelper.getItemHandlerAt(h.getLevel(), h.absolutePos(core.east()), Direction.WEST) + " count=" + art.arcane.thaumcraft.golems.InvHelper.countTotalItemsIn(h.getLevel(), h.absolutePos(core.east()), Direction.WEST, comps[i], art.arcane.thaumcraft.golems.InvHelper.InvFilter.STRICT));
        check(te.startCraft(props.toLong(), player), "Craft did not start");
        int expected = props.getTraits().size() * 2;
        for (ItemStack s : comps) expected += s.getCount();
        check(te.cost == expected && te.maxCost == expected, "Cost " + te.cost + " expected " + expected);
        check(chest.isEmpty(), "Components should be consumed");
        // feed machina essentia one unit at a time
        h.onEachTick(() -> te.addEssentia("machina", 1));
        h.succeedWhen(() -> {
            ItemStack out = te.getItem(0);
            h.assertTrue(!out.isEmpty(), "No golem yet, cost " + te.cost);
            var p2 = GolemPlacerItem.props(out);
            h.assertTrue(p2 != null && p2.toLong() == props.toLong(), "Wrong golem");
            player.setItemInHand(InteractionHand.MAIN_HAND, out.copy());
            BlockPos floorPos = h.absolutePos(new BlockPos(9, 0, 9));
            out.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floorPos), Direction.UP, floorPos, false)));
            var golems = h.getLevel().getEntitiesOfClass(GolemEntity.class, new AABB(floorPos).inflate(2));
            h.assertTrue(golems.size() == 1, "Golem not placed");
            GolemEntity g = golems.get(0);
            // thaumium hp 24 (+10), smart -> fragile? smart head adds FRAGILE, claws add FIGHTER, CLUMSY, BRUTAL; material HEAVY FIREPROOF BLASTPROOF
            var t = g.getProperties().getTraits();
            h.assertTrue(t.contains(GolemTrait.SMART) && t.contains(GolemTrait.FIGHTER) && t.contains(GolemTrait.BRUTAL) && t.contains(GolemTrait.FRAGILE) && t.contains(GolemTrait.HEAVY), "Traits " + t);
            h.assertTrue(g.getMaxHealth() == (int) ((10 + 24) * 0.75), "Health " + g.getMaxHealth());
            h.assertTrue(g.getArmorValue() == (int) (10 * 0.75), "Armor " + g.getArmorValue());
            h.assertTrue(Math.abs(g.getAttributeValue(Attributes.ATTACK_DAMAGE) - 6.0) < 1e-6, "Damage " + g.getAttributeValue(Attributes.ATTACK_DAMAGE));
            h.assertTrue(g.fireImmune(), "Thaumium golems are fireproof");
            System.out.println("Golem press passed: cost " + te.maxCost + ", golem hp " + g.getMaxHealth() + " armor " + g.getArmorValue() + " dmg 6 traits " + t);
        });
    }

    /** Collect seal on the floor and fill seal on a chest: a basic golem carries a dropped item into the chest. */
    static void pickup(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        BlockPos chestPos = new BlockPos(3, 1, 3);
        h.setBlock(chestPos, Blocks.CHEST);
        BlockPos sealFloor = new BlockPos(10, 0, 10);
        check(SealHandler.addSealEntity(level, h.absolutePos(sealFloor), Direction.UP, SealHandler.newSeal("thaumcraft:pickup"), player), "pickup seal");
        check(SealHandler.addSealEntity(level, h.absolutePos(chestPos), Direction.UP, SealHandler.newSeal("thaumcraft:fill"), player), "fill seal");
        GolemEntity g = golem(h, player, new GolemProperties(), new BlockPos(6, 1, 6));
        var item = new ItemEntity(level, h.absoluteVec(new Vec3(10.5, 1.1, 10.5)).x, h.absoluteVec(new Vec3(10.5, 1.1, 10.5)).y, h.absoluteVec(new Vec3(10.5, 1.1, 10.5)).z, new ItemStack(Items.DIAMOND, 3));
        level.addFreshEntity(item);
        h.succeedWhen(() -> {
            var chest = (ChestBlockEntity) h.getBlockEntity(chestPos, ChestBlockEntity.class);
            int n = 0;
            for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.DIAMOND)) n += chest.getItem(i).getCount();
            h.assertTrue(n == 3, "Chest has " + n + " diamonds, golem holds " + g.getMainHandItem());
            System.out.println("Pickup seal passed: golem moved 3 diamonds into the chest.");
        });
    }

    /** Fill seal on a Thaumcraft pedestal: the golem deposits only through the NeoForge item capability. */
    static void capability(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        BlockPos ped = new BlockPos(3, 1, 3);
        h.setBlock(ped, ModBlocks.PEDESTAL_ARCANE.get());
        check(SealHandler.addSealEntity(level, h.absolutePos(new BlockPos(10, 0, 10)), Direction.UP, SealHandler.newSeal("thaumcraft:pickup"), player), "pickup seal");
        check(SealHandler.addSealEntity(level, h.absolutePos(ped), Direction.UP, SealHandler.newSeal("thaumcraft:fill"), player), "fill seal");
        GolemEntity g = golem(h, player, new GolemProperties(), new BlockPos(6, 1, 6));
        var v = h.absoluteVec(new Vec3(10.5, 1.1, 10.5));
        level.addFreshEntity(new ItemEntity(level, v.x, v.y, v.z, new ItemStack(Items.EMERALD, 1)));
        h.succeedWhen(() -> {
            var be = h.getBlockEntity(ped, art.arcane.thaumcraft.blockentity.PedestalBlockEntity.class);
            h.assertTrue(be.getItem(0).is(Items.EMERALD), "Pedestal holds " + be.getItem(0) + ", golem holds " + g.getMainHandItem());
            System.out.println("Golem capability passed: golem filled a pedestal through the item capability.");
        });
    }

    /** Guard seal: a fighter golem attacks a zombie in the guarded area. */
    static void guard(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        check(SealHandler.addSealEntity(level, h.absolutePos(new BlockPos(10, 0, 10)), Direction.UP, SealHandler.newSeal("thaumcraft:guard"), player), "guard seal");
        GolemProperties props = new GolemProperties();
        props.setMaterial(GolemProperties.MATERIALS.get(1)); props.setAddon(GolemProperties.ADDONS.get(2));
        GolemEntity g = golem(h, player, props, new BlockPos(6, 1, 6));
        check(g.getProperties().hasTrait(GolemTrait.FIGHTER), "fighter");
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(10, 1, 10));
        zombie.setNoAi(true);
        float hp = zombie.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(zombie.getHealth() < hp || !zombie.isAlive(), "Zombie unharmed, golem target " + g.getTarget());
            h.assertTrue(zombie.getLastHurtByMob() == g, "Zombie hurt by " + zombie.getLastHurtByMob());
            System.out.println("Guard seal passed: golem attacked the zombie (hp " + zombie.getHealth() + ").");
        });
    }

    /** Automated crossbow: loaded with arrows it shoots a zombie in range. */
    static void turret(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        TurretCrossbowEntity t = ModEntities.TURRET_BASIC.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        Vec3 p = h.absoluteVec(new Vec3(5.5, 1, 5.5));
        t.snapTo(p.x, p.y, p.z, 0, 0);
        t.setOwned(true); t.setValidSpawn(); t.setOwnerId(player.getUUID());
        t.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARROW, 16));
        level.addFreshEntity(t);
        var zombie = h.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new BlockPos(5, 1, 14));
        zombie.setNoAi(true);
        float hp = zombie.getHealth();
        h.succeedWhen(() -> {
            h.assertTrue(t.getMainHandItem().getCount() < 16, "No arrow fired, target " + t.getTarget());
            h.assertTrue(zombie.getHealth() < hp, "Zombie not hit");
            System.out.println("Turret passed: arrows left " + t.getMainHandItem().getCount() + ", zombie hp " + zombie.getHealth());
        });
    }
    /** Fishing seal (TC4 fishing core): a golem fishes in a pond inside the seal area and holds the catch. */
    static void fishing(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        for (int x = 8; x <= 10; x++) for (int z = 8; z <= 10; z++) {
            level.setBlock(h.absolutePos(new BlockPos(x, -1, z)), Blocks.STONE.defaultBlockState(), 3);
            h.setBlock(new BlockPos(x, 0, z), Blocks.WATER);
        }
        check(SealHandler.addSealEntity(level, h.absolutePos(new BlockPos(7, 0, 9)), Direction.UP, SealHandler.newSeal("thaumcraft:fishing"), player), "fishing seal");
        GolemEntity g = golem(h, player, new GolemProperties(), new BlockPos(4, 1, 9));
        h.succeedWhen(() -> {
            h.assertTrue(!g.getMainHandItem().isEmpty(), "Golem caught nothing yet, task " + g.getTask());
            System.out.println("Fishing seal passed: golem caught " + g.getMainHandItem());
        });
    }

    /** Decant seal (TC4 liquid core): a golem scoops a water source block and pours it into the cauldron the seal is on. */
    static void fluid(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        BlockPos cauldron = new BlockPos(5, 1, 5), water = new BlockPos(8, 0, 5);
        h.setBlock(cauldron, Blocks.CAULDRON);
        level.setBlock(h.absolutePos(water.below()), Blocks.STONE.defaultBlockState(), 3);
        h.setBlock(water, Blocks.WATER);
        check(SealHandler.addSealEntity(level, h.absolutePos(cauldron), Direction.UP, SealHandler.newSeal("thaumcraft:fluid"), player), "fluid seal");
        GolemEntity g = golem(h, player, new GolemProperties(), new BlockPos(5, 1, 9));
        h.succeedWhen(() -> {
            var st = h.getBlockState(cauldron);
            h.assertTrue(st.is(Blocks.WATER_CAULDRON) && st.getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) == 3, "Cauldron " + st + ", golem carries " + art.arcane.thaumcraft.golems.seals.SealFluid.carriedAmount(g) + " task " + g.getTask());
            h.assertTrue(!h.getBlockState(water).getFluidState().isSource(), "Water source should have been picked up");
            System.out.println("Fluid seal passed: golem filled the cauldron from a water source.");
        });
    }

    /** Alchemy seal (TC4 alchemy core): a smart golem carries ignis from a source jar into the jar the seal is on. */
    static void essentia(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.CREATIVE);
        var level = h.getLevel();
        BlockPos target = new BlockPos(5, 1, 5), source = new BlockPos(8, 1, 5);
        h.setBlock(target, ModBlocks.JAR_NORMAL.get());
        h.setBlock(source, ModBlocks.JAR_NORMAL.get());
        var src = (art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity) h.getBlockEntity(source, art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
        var dst = (art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity) h.getBlockEntity(target, art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity.class);
        check(src.insert("ignis", 25), "fill source jar");
        check(SealHandler.addSealEntity(level, h.absolutePos(target), Direction.UP, SealHandler.newSeal("thaumcraft:essentia"), player), "essentia seal");
        GolemProperties props = new GolemProperties();
        props.setHead(GolemProperties.HEADS.get(1));
        GolemEntity g = golem(h, player, props, new BlockPos(5, 1, 9));
        check(g.getProperties().hasTrait(GolemTrait.SMART), "smart golem");
        h.succeedWhen(() -> {
            h.assertTrue(dst.contents().amount() >= 20 && "ignis".equals(dst.aspect()), "Target jar " + dst.contents() + ", source " + src.contents() + ", golem carries " + art.arcane.thaumcraft.golems.seals.SealEssentia.carriedAmount(g) + " task " + g.getTask());
            h.assertTrue(src.contents().amount() + dst.contents().amount() + art.arcane.thaumcraft.golems.seals.SealEssentia.carriedAmount(g) == 25, "Essentia not conserved");
            System.out.println("Essentia seal passed: golem moved " + dst.contents().amount() + " ignis into the target jar.");
        });
    }
    /** Golem bell raytrace: looking at a golem the bell's trace hits the golem entity, and the bell does not open the logistics screen. */
    static void bell(GameTestHelper h) {
        floor(h);
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 pp = h.absoluteVec(new Vec3(5.5, 1, 3.5));
        player.snapTo(pp.x, pp.y, pp.z, 0.0f, 20.0f);
        GolemEntity g = golem(h, player, new GolemProperties(), new BlockPos(5, 1, 6));
        h.runAfterDelay(2, () -> {
            player.snapTo(pp.x, pp.y, pp.z, 0.0f, 20.0f);
            player.setYHeadRot(0.0f);
            var hit = art.arcane.thaumcraft.golems.GolemBellItem.retrace(player);
            check(hit instanceof net.minecraft.world.phys.EntityHitResult ehr && ehr.getEntity() == g, "Bell trace should hit the golem, got " + hit.getType() + " " + (hit instanceof net.minecraft.world.phys.EntityHitResult e2 ? e2.getEntity() : ""));
            player.setShiftKeyDown(true);
            var bell = new net.minecraft.world.item.ItemStack(art.arcane.thaumcraft.ModItems.GOLEM_BELL.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bell);
            var res = bell.getItem().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            check(res == net.minecraft.world.InteractionResult.FAIL, "Bell aimed at a golem should FAIL (no logistics), got " + res);
            System.out.println("Golem bell passed: trace hit golem " + g.getId());
            h.succeed();
        });
    }
    private GolemancyVerification() {}
}
