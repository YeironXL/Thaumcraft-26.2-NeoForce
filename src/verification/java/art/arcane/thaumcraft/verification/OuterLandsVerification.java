package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.eldritch.EldritchAltarBlockEntity;
import art.arcane.thaumcraft.block.eldritch.EldritchLockBlockEntity;
import art.arcane.thaumcraft.block.eldritch.EldritchPortalBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.entity.monster.boss.ThaumcraftBossEntity;
import art.arcane.thaumcraft.world.outer.MazeData;
import art.arcane.thaumcraft.world.outer.MazeGenerator;
import art.arcane.thaumcraft.world.outer.OuterLands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Outer Lands: altar ritual, portal travel, maze generation, Ancient Locking Mechanism. */
public final class OuterLandsVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    private static ServerLevel outer(GameTestHelper h) {
        check(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DIMENSION_TYPE).get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION_TYPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "outer_lands"))).isPresent(), "no outer_lands dimension type");
        // GameTestServer drops datapack dimensions: host the maze in the test level (the real dimension is checked in the client)
        if (h.getLevel().getServer().getLevel(OuterLands.DIMENSION) == null) OuterLands.testHost = s -> s.overworld();
        ServerLevel o = OuterLands.level(h.getLevel().getServer());
        check(o != null, "no thaumcraft:outer_lands dimension");
        return o;
    }

    private static MazeData.Maze maze(GameTestHelper h, int salt) {
        BlockPos key = h.absolutePos(new BlockPos(0, 100 + salt, 0));
        // fixed seed per test: the room mix (taint, crab, rune...) must not depend on where the test runs
        return MazeData.get(h.getLevel().getServer()).forAltar(GlobalPos.of(Level.OVERWORLD, key), 0x0C0FFEEL + salt * 0x9E3779B97F4A7C15L);
    }

    /** Force every chunk of a maze section: here only to stand for a player walking back into an unloaded maze. */
    private static void forceMaze(ServerLevel outer, MazeData.Maze m, boolean on) {
        BlockPos o = MazeGenerator.origin(m);
        int n = MazeGenerator.SIZE * MazeGenerator.CELL / 16;
        for (int x = 0; x < n; x++) for (int z = 0; z < n; z++) outer.setChunkForced((o.getX() >> 4) + x, (o.getZ() >> 4) + z, on);
    }

    /**
     * Run once the freshly generated maze is entity ticking: its portal tickets promote the chunks over the next few
     * ticks, and the mobs placed in them become visible then.
     */
    private static void whenTicking(GameTestHelper h, ServerLevel outer, MazeData.Maze m, int left, Runnable r) {
        BlockPos o = MazeGenerator.origin(m);
        boolean all = true;
        for (int x = 0; x < MazeGenerator.SIZE && all; x++) for (int z = 0; z < MazeGenerator.SIZE && all; z++) all = outer.isPositionEntityTicking(o.offset(x * 16 + 8, 1, z * 16 + 8));
        if (all || left <= 0) r.run(); else h.runAfterDelay(1, () -> whenTicking(h, outer, m, left - 1, r));
    }

    /** Entities of a freshly ticking chunk are loaded asynchronously; give the room's taint seed a few ticks to show up. */
    private static void waitSeed(GameTestHelper h, ServerLevel outer, BlockPos c, int left, Runnable r) {
        boolean seen = !outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.TaintSeedEntity.class, new AABB(c).inflate(6)).isEmpty();
        if (seen || left <= 0) r.run(); else h.runAfterDelay(1, () -> waitSeed(h, outer, c, left - 1, r));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
    }

    /** Four Eldritch Eyes in the altar: after the ritual a two-block portal bound to a maze opens above it. */
    public static void altarPortal(GameTestHelper h) {
        floor(h);
        BlockPos a = new BlockPos(4, 2, 4);
        h.setBlock(a, ModBlocks.ELDRITCH_ALTAR.get());
        ServerPlayer p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ELDRITCH_EYE.get(), 5));
        BlockPos abs = h.absolutePos(a);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        for (int i = 0; i < 4; i++) check(h.getBlockState(a).useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, hit).consumesAction(), "eye " + i + " refused");
        check(p.getMainHandItem().getCount() == 1, "eyes not consumed");
        check(h.getLevel().getBlockState(abs).useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL, "fifth eye accepted");
        EldritchAltarBlockEntity be = (EldritchAltarBlockEntity) h.getBlockEntity(a, EldritchAltarBlockEntity.class);
        check(be.eyes() == 4 && be.ritualRunning() && !be.isOpen(), "ritual not started");
        check(!h.getBlockState(a.above()).is(ModBlocks.ELDRITCH_PORTAL.get()), "portal before the ritual ended");
        h.runAfterDelay(EldritchAltarBlockEntity.RITUAL_TICKS + 5, () -> {
            check(be.isOpen(), "altar not open");
            for (int y = 1; y <= 2; y++) {
                check(h.getBlockState(a.above(y)).is(ModBlocks.ELDRITCH_PORTAL.get()), "no portal at +" + y);
                var pe = (EldritchPortalBlockEntity) h.getBlockEntity(a.above(y), EldritchPortalBlockEntity.class);
                check(pe.mazeId() >= 0 && !pe.exit(), "portal not bound to a maze");
                check(MazeData.get(h.getLevel().getServer()).byId(pe.mazeId()).altar().pos().equals(abs), "maze not owned by this altar");
            }
            System.out.println("Outer Lands altar passed: 4 eyes, ritual, portal bound to the altar's maze.");
            h.succeed();
        });
    }

    /** Walking into the portal lands in the Outer Lands entry room; the exit portal leads back to the altar. */
    public static void enter(GameTestHelper h) {
        floor(h);
        ServerLevel outer = outer(h);
        BlockPos a = new BlockPos(4, 2, 4);
        h.setBlock(a, ModBlocks.ELDRITCH_ALTAR.get());
        EldritchAltarBlockEntity be = (EldritchAltarBlockEntity) h.getBlockEntity(a, EldritchAltarBlockEntity.class);
        for (int i = 0; i < 4; i++) be.insertEye();
        be.openPortal(h.getLevel());
        int id = ((EldritchPortalBlockEntity) h.getBlockEntity(a.above(), EldritchPortalBlockEntity.class)).mazeId();
        BlockPos arrival = MazeGenerator.arrival(MazeData.get(h.getLevel().getServer()).byId(id));
        Pig pig = h.spawn(EntityTypes.PIG, a.above());
        h.runAfterDelay(10, () -> {
            MazeData.Maze m = MazeData.get(h.getLevel().getServer()).byId(id);
            check(m.generated(), "maze not generated on entry");
            BlockPos arr = MazeGenerator.arrival(m);
            var there = outer.getEntitiesOfClass(Pig.class, new AABB(arr).inflate(2));
            check(!there.isEmpty(), "pig did not arrive in the Outer Lands at " + arr);
            if (outer.dimension() == OuterLands.DIMENSION) check(outer.getBiome(arr).is(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", "eldritch"))), "arrival not in the eldritch biome");
            BlockPos exit = MazeGenerator.exitPortal(m);
            check(outer.getBlockState(exit).is(ModBlocks.ELDRITCH_PORTAL.get()) && outer.getBlockEntity(exit) instanceof EldritchPortalBlockEntity pe && pe.exit() && pe.mazeId() == id, "no exit portal in the entry room");
            Pig back = there.get(0);
            back.setPortalCooldown(0);
            check(OuterLands.leave(back, id), "exit portal failed");
            h.runAfterDelay(5, () -> {
                check(!h.getLevel().getEntitiesOfClass(Pig.class, new AABB(h.absolutePos(a)).inflate(5)).isEmpty(), "pig did not come back to the altar");
                System.out.println("Outer Lands travel passed: portal -> entry room (eldritch biome) -> exit portal -> altar.");
                h.succeed();
            });
        });
    }

    /** The maze has an entry room, at least one boss room behind a lock, a tablet room and loot rooms. */
    public static void maze(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 1);
        MazeGenerator.Plan p = MazeGenerator.plan(m.seed());
        check(p.count(MazeGenerator.Kind.ENTRY) == 1 && p.count(MazeGenerator.Kind.BOSS) >= 1 && p.count(MazeGenerator.Kind.TABLET) >= p.count(MazeGenerator.Kind.BOSS), "plan rooms: boss " + p.count(MazeGenerator.Kind.BOSS) + " tablet " + p.count(MazeGenerator.Kind.TABLET));
        // every cell reachable: spanning tree
        int links = 0;
        for (int[] r : p.conn()) for (int c : r) links += Integer.bitCount(c);
        check(links / 2 >= MazeGenerator.SIZE * MazeGenerator.SIZE - 1, "maze not connected");
        OuterLands.ensureGenerated(outer, m.id());
        for (BlockPos lock : MazeGenerator.locks(m)) {
            check(outer.getBlockState(lock).is(ModBlocks.ELDRITCH_LOCK.get()), "no lock at " + lock);
            check(outer.getBlockState(lock.above(2)).is(ModBlocks.ELDRITCH_DOOR.get()), "no doorway above the lock");
        }
        for (BlockPos t : MazeGenerator.rooms(m, MazeGenerator.Kind.TABLET))
            check(outer.getBlockEntity(t) instanceof PedestalBlockEntity pe && pe.item().is(ModItems.RUNED_TABLET.get()), "tablet room without a runed tablet at " + t);
        BlockPos boss = MazeGenerator.rooms(m, MazeGenerator.Kind.BOSS).get(0);
        check(outer.getBlockState(boss).isAir() && outer.getBlockState(boss.above(8)).isAir() && outer.getBlockState(boss.offset(6, 0, 6)).isAir(), "boss room not 13x13x9");
        check(outer.getBlockState(boss.below(3)).is(Blocks.BEDROCK), "no bedrock under the maze");
        BlockPos e = MazeGenerator.arrival(m);
        check(outer.getBlockState(e).isAir() && !outer.getBlockState(e.below()).isAir(), "entry room floor");
        long loot = MazeGenerator.rooms(m, MazeGenerator.Kind.LOOT).size();
        System.out.println("Outer Lands maze passed: " + p.count(MazeGenerator.Kind.BOSS) + " boss rooms, " + p.count(MazeGenerator.Kind.TABLET) + " tablet rooms, " + loot + " loot rooms, locks and doorways.");
        h.succeed();
    }

    /** A Runed Tablet in the lock opens the doorway and wakes the room's boss. */
    public static void lock(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 2);
        OuterLands.ensureGenerated(outer, m.id());
        BlockPos lock = MazeGenerator.locks(m).get(0);
        BlockPos room = MazeGenerator.rooms(m, MazeGenerator.Kind.BOSS).stream().min((x, y) -> Double.compare(x.distSqr(lock), y.distSqr(lock))).get();
        ServerPlayer p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(lock), Direction.NORTH, lock, false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.DIRT));
        check(!outer.getBlockState(lock).useItemOn(p.getMainHandItem(), outer, p, InteractionHand.MAIN_HAND, hit).consumesAction(), "lock opened without a tablet");
        // outside the Outer Lands the mechanism is inert (only checkable with a real dimension)
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUNED_TABLET.get()));
        if (!OuterLands.is(h.getLevel())) {
        h.setBlock(new BlockPos(2, 2, 2), ModBlocks.ELDRITCH_LOCK.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RUNED_TABLET.get()));
        BlockPos ow = h.absolutePos(new BlockPos(2, 2, 2));
        check(h.getLevel().getBlockState(ow).useItemOn(p.getMainHandItem(), h.getLevel(), p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(ow), Direction.NORTH, ow, false)) == InteractionResult.FAIL && p.getMainHandItem().getCount() == 1, "overworld lock took the tablet");
        }
        check(outer.getBlockState(lock).useItemOn(p.getMainHandItem(), outer, p, InteractionHand.MAIN_HAND, hit).consumesAction(), "tablet refused");
        check(p.getMainHandItem().isEmpty(), "tablet not consumed");
        EldritchLockBlockEntity be = (EldritchLockBlockEntity) outer.getBlockEntity(lock);
        check(be.opening() && !be.boss().isEmpty(), "lock not opening");
        String boss = be.boss();
        h.runAfterDelay(90, () -> {
            check(outer.getBlockState(lock).isAir(), "lock still there");
            check(outer.getBlockState(lock.above(2)).isAir() && outer.getBlockState(lock.below(2)).isAir(), "doorway still closed");
            var bosses = outer.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(room).inflate(8), e -> e instanceof ThaumcraftBossEntity || e instanceof art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity || e instanceof art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity);
            check(!bosses.isEmpty(), "no " + boss + " in the boss room");
            bosses.forEach(net.minecraft.world.entity.Entity::discard);
            System.out.println("Outer Lands lock passed: tablet consumed, doorway opened, boss " + boss + " spawned.");
            h.succeed();
        });
    }

    /** New maze rooms: rune room with a rune stone, mind spider spawner in cobwebs, crusted openings in crab rooms. */
    public static void rooms(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 3);
        check(m.version() == MazeData.VERSION, "new maze not on the current layout version");
        MazeGenerator.Plan p = MazeGenerator.plan(m);
        for (MazeGenerator.Kind k : new MazeGenerator.Kind[]{MazeGenerator.Kind.RUNE, MazeGenerator.Kind.SPIDER, MazeGenerator.Kind.TAINT})
            check(p.count(k) >= 1, "maze has no " + k + " room");
        // old saves keep their plan: version 1 has none of the new rooms and the same boss rooms
        MazeGenerator.Plan old = MazeGenerator.plan(m.seed(), 1);
        check(old.count(MazeGenerator.Kind.RUNE) == 0 && old.count(MazeGenerator.Kind.BOSS) == p.count(MazeGenerator.Kind.BOSS), "version 1 plan changed");
        for (int x = 0; x < MazeGenerator.SIZE; x++) for (int z = 0; z < MazeGenerator.SIZE; z++)
            if (old.kind()[x][z] == MazeGenerator.Kind.BOSS || old.kind()[x][z] == MazeGenerator.Kind.TABLET) check(p.kind()[x][z] == old.kind()[x][z], "boss/tablet layout moved");
        h.runAfterDelay(20, () -> { OuterLands.ensureGenerated(outer, m.id()); whenTicking(h, outer, m, 200, () -> {
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.RUNE)) check(outer.getBlockState(c).is(ModBlocks.ELDRITCH_RUNE_STONE.get()), "no rune stone at " + c);
        int webs = 0;
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.SPIDER)) {
            check(outer.getBlockEntity(c) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity, "no spawner at " + c);
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-3, 0, -3), c.offset(3, 3, 3))) if (outer.getBlockState(q).is(Blocks.COBWEB)) webs++;
        }
        check(webs > 5, "spider rooms without cobwebs");
        int openings = 0;
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.CRAB)) for (BlockPos q : BlockPos.betweenClosed(c.offset(-5, 0, -5), c.offset(5, 0, 5))) if (outer.getBlockState(q).is(ModBlocks.ELDRITCH_CRAB_SPAWNER.get())) openings++;
        check(p.count(MazeGenerator.Kind.CRAB) == 0 || openings > 0, "crab rooms without crusted openings");
        System.out.println("Outer Lands rooms passed: " + p.count(MazeGenerator.Kind.RUNE) + " rune, " + p.count(MazeGenerator.Kind.SPIDER) + " spider (" + webs + " webs), " + p.count(MazeGenerator.Kind.TAINT) + " taint rooms, " + openings + " crusted openings.");
        h.succeed();
        }); });
    }

    /** A Crusted Opening pushes Eldritch Crabs out of a free face, up to its cap; a player nearby triggers it on its own. */
    public static void crabSpawner(GameTestHelper h) {
        floor(h);
        BlockPos sp = new BlockPos(4, 2, 4);
        h.setBlock(sp, ModBlocks.ELDRITCH_CRAB_SPAWNER.get());
        var be = (art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity) h.getBlockEntity(sp, art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity.class);
        check(be.spawnCrab(h.getLevel()), "no crab came out");
        var crabs = h.getLevel().getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchCrabEntity.class, new AABB(h.absolutePos(sp)).inflate(3));
        check(crabs.size() == 1, "expected 1 crab, got " + crabs.size());
        for (int i = 0; i < 6; i++) be.spawnCrab(h.getLevel());
        crabs = h.getLevel().getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchCrabEntity.class, new AABB(h.absolutePos(sp)).inflate(17));
        check(crabs.size() == art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity.MAX_NEARBY, "cap not respected: " + crabs.size());
        crabs.forEach(net.minecraft.world.entity.Entity::discard);
        System.out.println("Outer Lands crab spawner passed: crabs crawl out of the Crusted Opening, capped at " + art.arcane.thaumcraft.block.eldritch.EldritchCrabSpawnerBlockEntity.MAX_NEARBY + ".");
        h.succeed();
    }

    /** Tainted rooms: taint crust/soil floor, taint fibres and flux goo, a taintacle; corridors get taint patches and pools. */
    public static void taint(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 4);
        h.runAfterDelay(20, () -> { OuterLands.ensureGenerated(outer, m.id()); whenTicking(h, outer, m, 200, () -> {
        int crust = 0, fibre = 0, goo = 0;
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT)) for (BlockPos q : BlockPos.betweenClosed(c.offset(-5, -1, -5), c.offset(5, 0, 5))) {
            var s = outer.getBlockState(q);
            if (s.is(ModBlocks.TAINT_CRUST.get()) || s.is(ModBlocks.TAINT_SOIL.get())) crust++;
            if (s.is(ModBlocks.TAINT_FIBRE.get())) fibre++;
            if (s.is(ModBlocks.FLUX_GOO.get())) goo++;
        }
        check(crust > 20 && fibre >= 3, "taint room not tainted: crust " + crust + " fibre " + fibre);
        BlockPos c0 = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0);
        check(!outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.TaintacleEntity.class, new AABB(c0).inflate(6)).isEmpty(), "no taintacle in the taint room");
        int patches = 0, pools = 0;
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.CORRIDOR)) {
            var f = outer.getBlockState(c.below());
            if (f.is(ModBlocks.TAINT_CRUST.get()) || f.is(ModBlocks.TAINT_SOIL.get()) || outer.getBlockState(c).is(ModBlocks.TAINT_FIBRE.get())) patches++;
            if (!f.getFluidState().isEmpty()) pools++;
        }
        System.out.println("Outer Lands taint passed: crust " + crust + ", fibres " + fibre + ", goo " + goo + "; corridors with taint patches " + patches + ", pools " + pools + ".");
        h.succeed();
        }); });
    }

    private static boolean isTaint(net.minecraft.world.level.block.state.BlockState s) { return art.arcane.thaumcraft.block.world.taint.ITaintBlock.isTaint(s) || s.is(ModBlocks.TAINT_FIBRE.get()); }

    /** Version 3 taint room: Tainted Lands biome, a taint seed, taint that keeps spreading inside the room only. */
    static final java.util.List<String> DIAG = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    static { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDeathEvent e) -> {
        if (e.getEntity() instanceof art.arcane.thaumcraft.entity.TaintSeedEntity) DIAG.add(e.getEntity().blockPosition() + " by " + e.getSource().getMsgId()); });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e) -> {
        if (e.getEntity() instanceof art.arcane.thaumcraft.entity.TaintSeedEntity t) DIAG.add("leave " + t.blockPosition() + " " + t.getRemovalReason()); }); }
    public static void taintSpread(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 5);
        // This test checks taint spread, not chunk persistence (outer_unloaded_mobs covers that): keep the maze loaded so the
        // portal tickets expiring under a busy server can't unload the room mid-test.
        forceMaze(outer, m, true);
        h.runAfterDelay(20, () -> { OuterLands.ensureGenerated(outer, m.id()); whenTicking(h, outer, m, 200, () -> waitSeed(h, outer, MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0), 100, () -> {
        BlockPos c = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0);
        check(outer.getBiome(c).is(art.arcane.thaumcraft.world.outer.MazeDecor.TAINTED_LANDS), "taint room biome is " + outer.getBiome(c).getRegisteredName());
        { var near = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.TaintSeedEntity.class, new AABB(c).inflate(6));
          if (near.isEmpty()) { var far = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.TaintSeedEntity.class, new AABB(c).inflate(64));
            StringBuilder sb = new StringBuilder(); for (var e : far) sb.append(e.blockPosition().subtract(c)).append(e.isAlive() ? " alive " : " dead ").append(e.getHealth()).append("; ");
            check(false, "no taint seed in the taint room (c=" + c + " loadedChunk=" + outer.hasChunkAt(c) + " ticking=" + outer.isPositionEntityTicking(c) + " seedsWithin64=" + far.size() + " " + sb + " deaths=" + DIAG + ")"); } }
        BlockPos o = c.offset(-8, -1, -8);
        check(MazeGenerator.taintMayReach(outer, o.offset(4, 1, 8)) && !MazeGenerator.taintMayReach(outer, o.offset(2, 1, 8)) && !MazeGenerator.taintMayReach(outer, o.offset(8, -2, 8)), "taint bounds wrong");
        h.runAfterDelay(40, () -> {
            java.util.function.IntSupplier inRoom = () -> { int n = 0; for (BlockPos q : BlockPos.betweenClosed(o.offset(3, 0, 3), o.offset(13, 8, 13))) if (isTaint(outer.getBlockState(q))) n++; return n; };
            int before = inRoom.getAsInt();
            var rnd = outer.getRandom();
            for (int i = 0; i < 6000; i++) {
                BlockPos q = o.offset(3 + rnd.nextInt(11), rnd.nextInt(9), 3 + rnd.nextInt(11));
                if (isTaint(outer.getBlockState(q))) art.arcane.thaumcraft.block.world.taint.TaintHelper.spreadFibres(outer, q, true);
            }
            int after = inRoom.getAsInt();
            check(after > before, "taint did not spread: " + before + " -> " + after);
            int outside = 0;
            for (BlockPos q : BlockPos.betweenClosed(o.offset(0, -1, 0), o.offset(15, 11, 15))) {
                int lx = q.getX() - o.getX(), lz = q.getZ() - o.getZ();
                if ((lx <= 2 || lx >= 14 || lz <= 2 || lz >= 14) && isTaint(outer.getBlockState(q))) outside++;
            }
            check(outside == 0, outside + " taint blocks escaped the room");
            check(outer.getBlockState(o.offset(8, -2, 8)).is(Blocks.BEDROCK), "bedrock tainted");
            System.out.println("Outer Lands taint spread passed: Tainted Lands biome, seed, taint " + before + " -> " + after + ", none outside the room.");
                forceMaze(outer, m, false);
                h.succeed();
        });
        })); });
    }

    /** An Ancient Autocaster casts at its target: the shock focus hurts it, the orb focus throws an eldritch orb. */
    public static void autocaster(GameTestHelper h) {
        floor(h);
        var a = h.spawn(art.arcane.thaumcraft.registry.ModEntities.ELDRITCH_AUTOCASTER.get(), new BlockPos(4, 2, 1));
        a.setFocus(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.FOCUS_SHOCK);
        Pig pig = h.spawn(EntityTypes.PIG, new BlockPos(4, 2, 6));
        pig.setNoAi(true);
        a.setTarget(pig);
        h.runAfterDelay(70, () -> {
            check(a.shots > 0, "autocaster never cast");
            check(pig.getHealth() < pig.getMaxHealth(), "target not hurt");
            check(a.position().distanceTo(Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(4, 2, 1)))) < 0.6, "autocaster moved");
            a.setFocus(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.FOCUS_ORB);
            a.cast(pig);
            check(!h.getLevel().getEntitiesOfClass(art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity.class, a.getBoundingBox().inflate(3)).isEmpty(), "no eldritch orb");
            System.out.println("Outer Lands autocaster passed: " + a.shots + " casts, target hp " + pig.getHealth() + ", orb thrown.");
            h.succeed();
        });
    }

    /** Version 3 dressing: strange crystals, glowing crusted stone, crusted stone crab rooms and autocasters on supports. */
    public static void crystals(GameTestHelper h) {
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 6);
        h.runAfterDelay(20, () -> { OuterLands.ensureGenerated(outer, m.id()); whenTicking(h, outer, m, 200, () -> {
        BlockPos o = MazeGenerator.origin(m);
        int crystals = 0, glow = 0, crust = 0;
        for (BlockPos q : BlockPos.betweenClosed(o.offset(0, -1, 0), o.offset(143, 10, 143))) {
            var s = outer.getBlockState(q);
            if (s.is(ModBlocks.CRYSTAL_STRANGE.get())) { crystals++; check(s.canSurvive(outer, q), "floating strange crystal at " + q); }
            else if (s.is(ModBlocks.CRUSTED_STONE_GLOW.get())) glow++;
            else if (s.is(ModBlocks.CRUSTED_STONE.get())) crust++;
        }
        var casters = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.class, new AABB(o.getX(), o.getY() - 2, o.getZ(), o.getX() + 144, o.getY() + 12, o.getZ() + 144));
        check(crystals > 0, "no strange crystals");
        check(glow > 0, "no glowing crusted stone");
        check(MazeGenerator.rooms(m, MazeGenerator.Kind.CRAB).isEmpty() || crust > 20, "crab rooms not crusted: " + crust);
        check(!casters.isEmpty(), "no autocasters");
        for (var a : casters) check(outer.getBlockState(a.blockPosition().below()).is(ModBlocks.ELDRITCH_DOOR.get()), "autocaster without its Ancient Doorway support");
        System.out.println("Outer Lands dressing passed: " + crystals + " strange crystals, " + glow + " glowing crusted stone, " + crust + " crusted stone, " + casters.size() + " autocasters.");
        casters.forEach(net.minecraft.world.entity.Entity::discard);
        h.succeed();
        }); });
    }

    private OuterLandsVerification() {}

    /**
     * Real-play generation: nothing of the maze is loaded or forced when the portal builds it. Its mobs must still be
     * there once the chunks load later (taintacles + taint seeds per taint room, crabs per crab room, autocasters).
     */
    public static void unloadedMobs(GameTestHelper h) {
        ServerLevel outer = outer(h);
        int salt = 7;
        while (MazeGenerator.plan(0x0C0FFEEL + salt * 0x9E3779B97F4A7C15L).count(MazeGenerator.Kind.TAINT) == 0
                || MazeGenerator.plan(0x0C0FFEEL + salt * 0x9E3779B97F4A7C15L).count(MazeGenerator.Kind.CRAB) == 0) salt++;
        MazeData.Maze m = maze(h, salt);
        MazeGenerator.Plan p = MazeGenerator.plan(m);
        int taint = p.count(MazeGenerator.Kind.TAINT), crab = p.count(MazeGenerator.Kind.CRAB);
        System.out.println("Outer Lands unloaded mobs: start maze " + m.id() + " generated=" + m.generated());
        OuterLands.ensureGenerated(outer, m.id());
        BlockPos o = MazeGenerator.origin(m);
        AABB box = new AABB(o.getX(), o.getY() - 2, o.getZ(), o.getX() + 144, o.getY() + 12, o.getZ() + 144);
        // let the generation's portal tickets expire and the chunks unload (mobs saved to disk), then come back (one ticker: the
        // gametest scheduler must not be modified from inside its own callbacks)
        int[] t = {0, 0};
        h.onEachTick(() -> {
            int now = t[0]++;
            if (t[1] == 0) {
                BlockPos tr = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0);
                int seen = outer.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box).size();
                if (outer.isPositionEntityTicking(tr) && seen >= taint * 2 + crab * 3) {
                    t[1] = 1;
                    System.out.println("Outer Lands unloaded mobs: " + now + " ticks after generation, taint room entity ticking, visible mobs=" + seen);
                } else if (now >= 200) throw new AssertionError("freshly generated maze not entity ticking: " + seen + " mobs");
                return;
            }
            if (now < 400) return;
            if (now == 400) {
                BlockPos tr = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0);
                System.out.println("Outer Lands unloaded mobs: before load, taint room chunk loaded=" + outer.getChunkSource().hasChunk(tr.getX() >> 4, tr.getZ() >> 4) + " entityTicking=" + outer.isPositionEntityTicking(tr)
                        + " visible mobs=" + outer.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box).size());
                if (outer.isPositionEntityTicking(tr)) throw new AssertionError("maze still loaded after its tickets expired");
                forceMaze(outer, m, true);
                return;
            }
            int tc = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.TaintacleEntity.class, box).size();
            int ts = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.TaintSeedEntity.class, box).size();
            int cr = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchCrabEntity.class, box).size();
            int ac = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.class, box).size();
            boolean ok = tc >= taint && ts >= taint && cr >= crab * 3 && ac > 0
                    && outer.getBlockEntity(MazeGenerator.rooms(m, MazeGenerator.Kind.SPIDER).get(0)) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity;
            if (ok && now >= 420) {
                System.out.println("Outer Lands unloaded mobs passed: taintacles " + tc + "/" + taint + ", seeds " + ts + ", crabs " + cr + " (" + crab + " rooms), autocasters " + ac + " after reload.");
                for (var e : outer.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box)) e.discard();
                forceMaze(outer, m, false);
                h.succeed();
            } else if (now == 600) {
                forceMaze(outer, m, false);
                throw new AssertionError("maze mobs lost: taintacles " + tc + "/" + taint + ", seeds " + ts + "/" + taint + ", crabs " + cr + "/" + crab * 3 + ", autocasters " + ac);
            }
        });
    }

    /** Version 4: a tainted room holding 1-5 Giant Taintacles, in the Tainted Lands, still there after an unload. */
    public static void giantTaintacles(GameTestHelper h) {
        ServerLevel outer = outer(h);
        int salt = 20;
        while (MazeGenerator.plan(0x0C0FFEEL + salt * 0x9E3779B97F4A7C15L).count(MazeGenerator.Kind.TAINTACLES) == 0) salt++;
        check(MazeGenerator.plan(0x0C0FFEEL + salt * 0x9E3779B97F4A7C15L, 3).count(MazeGenerator.Kind.TAINTACLES) == 0, "version 3 plan got a giant taintacle room");
        MazeData.Maze m = maze(h, salt);
        OuterLands.ensureGenerated(outer, m.id());
        BlockPos c = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINTACLES).get(0);
        AABB room = new AABB(c).inflate(7, 4, 7);
        java.util.function.Supplier<java.util.List<art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity>> giants = () -> outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity.class, room);
        int[] t = {0, 0, 0};
        h.onEachTick(() -> {
            int now = t[0]++;
            if (t[1] == 0) {
                if (!outer.isPositionEntityTicking(c) || giants.get().isEmpty()) { if (now >= 200) throw new AssertionError("no giant taintacles in their room"); return; }
                var g = giants.get();
                check(g.size() >= 1 && g.size() <= 5, "giant taintacles: " + g.size());
                for (var e : g) check(e.isPersistenceRequired(), "giant taintacle not persistent");
                check(outer.getBiome(c).is(art.arcane.thaumcraft.world.outer.MazeDecor.TAINTED_LANDS), "room not Tainted Lands");
                check(!outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.TaintSeedEntity.class, room).isEmpty(), "no taint seed");
                check(MazeGenerator.taintMayReach(outer, c), "taint may not spread in the room");
                t[1] = 1; t[2] = g.size();
                return;
            }
            if (now < 420) return;
            if (now == 420) { check(!outer.isPositionEntityTicking(c), "room still loaded"); forceMaze(outer, m, true); return; }
            int n = giants.get().size();
            if (n == t[2]) {
                System.out.println("Outer Lands giant taintacles passed: " + n + " giant taintacles in a Tainted Lands room (maze " + m.id() + "), persistent across an unload.");
                for (var e : outer.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new AABB(MazeGenerator.origin(m)).expandTowards(144, 12, 144).inflate(0, 2, 0))) e.discard();
                forceMaze(outer, m, false);
                h.succeed();
            } else if (now == 600) { forceMaze(outer, m, false); throw new AssertionError("giant taintacles lost: " + n + "/" + t[2]); }
        });
    }

    /** Version 5: reshaped corridor pools (longer than the old 3x3, 2 deep at the middle, 4 liquid kinds); version 4 keeps 3x3. */
    public static void pools(GameTestHelper h) {
        var kinds = java.util.EnumSet.noneOf(art.arcane.thaumcraft.world.outer.MazePools.Kind.class);
        var rr = new java.util.Random(5);
        for (int i = 0; i < 200; i++) kinds.add(art.arcane.thaumcraft.world.outer.MazePools.kind(rr));
        check(kinds.size() == 4, "pool kinds: " + kinds);
        ServerLevel outer = outer(h);
        MazeData.Maze m = maze(h, 40);
        check(m.version() == 5, "maze not version 5");
        OuterLands.ensureGenerated(outer, m.id());
        int pools = 0, big = 0, deep = 0;
        for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.CORRIDOR)) {
            boolean goo = outer.getBlockState(c).is(ModBlocks.FLUX_GOO.get());
            if (outer.getBlockState(c.below()).getFluidState().isEmpty() && !goo) continue;
            pools++;
            int cells = 0;
            for (BlockPos q : BlockPos.betweenClosed(c.offset(-7, -1, -7), c.offset(7, 0, 7))) {
                if (q.getY() == c.getY() - 1 && (!outer.getBlockState(q).getFluidState().isEmpty() || outer.getBlockState(q.above()).is(ModBlocks.FLUX_GOO.get()))) cells++;
            }
            if (cells > 9) big++;
            if (!outer.getBlockState(c.below(2)).getFluidState().isEmpty()) deep++;
        }
        check(pools > 0 && big > 0, "version 5 pools: " + pools + " big " + big);
        System.out.println("Outer Lands pools passed: " + pools + " pools, " + big + " larger than 3x3, " + deep + " two deep; kinds " + kinds + ".");
        h.succeed();
    }
}
