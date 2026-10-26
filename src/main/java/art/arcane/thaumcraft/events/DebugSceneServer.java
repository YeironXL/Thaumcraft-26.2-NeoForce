package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Development aid (-Dthaumcraft.debugHold): thaumometer in hand and a row of mod blocks on stone for render checks. */
public final class DebugSceneServer {
    public static void register() {
        if (System.getProperty("thaumcraft.debugHold") == null) return;
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p)) return;
            p.setGameMode(GameType.CREATIVE);
            String outer = System.getProperty("thaumcraft.debugOuter");
            if (outer != null) { art.arcane.thaumcraft.world.outer.OuterLandsDebug.run(p, outer); return; }
            if (System.getProperty("thaumcraft.debugNodes") != null) {
                // -PdebugNodes=1: one node of every type (plus bright/pale/fading normals) in a row in front of the player
                var level = (net.minecraft.server.level.ServerLevel) p.level();
                BlockPos base = BlockPos.containing(p.getX(), 200, p.getZ());
                for (BlockPos q : BlockPos.betweenClosed(base.offset(-12, 1, -2), base.offset(12, 10, 12))) if (level.getBlockState(q).is(ModBlocks.AURA_NODE.get())) level.removeBlock(q, false);
                var types = art.arcane.thaumcraft.aura.node.NodeType.values();
                var rnd = net.minecraft.util.RandomSource.create(42);
                for (int i = 0; i < types.length; i++)
                    art.arcane.thaumcraft.aura.node.AuraNodeGen.place(level, base.offset(-5 + i * 2, 4, 4), types[i], art.arcane.thaumcraft.aura.node.NodeModifier.NONE, rnd, false);
                var mods = art.arcane.thaumcraft.aura.node.NodeModifier.values();
                for (int i = 1; i < mods.length; i++)
                    art.arcane.thaumcraft.aura.node.AuraNodeGen.place(level, base.offset(-4 + i * 2, 6, 5), art.arcane.thaumcraft.aura.node.NodeType.NORMAL, mods[i], rnd, false);
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set midnight");
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "gamerule doDaylightCycle false");
            }
            if (System.getProperty("thaumcraft.debugShrines", "").startsWith("barrow")) {
                // -PdebugShrines=barrow|barrow_in: a Barrow Den seen from the south-west, or from its ledge.
                var level = (net.minecraft.server.level.ServerLevel) p.level();
                BlockPos base = BlockPos.containing(p.getX(), 200, p.getZ());
                BlockPos g = base.offset(0, -1, 16);
                for (BlockPos q : BlockPos.betweenClosed(g.offset(-24, -9, -24), g.offset(24, 12, 24))) level.setBlock(q, q.getY() < g.getY() ? Blocks.DIRT.defaultBlockState() : q.getY() == g.getY() ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                art.arcane.thaumcraft.worldgen.BarrowDenGen.build(level, new java.util.Random(11), g);
                boolean in = System.getProperty("thaumcraft.debugShrines").equals("barrow_in");
                double px = in ? g.getX() - 4.5 : g.getX() - 17.5, py = in ? g.getY() + 2 : g.getY() + 7, pz = in ? g.getZ() + 0.5 : g.getZ() - 9.5;
                float yaw = (float) Math.toDegrees(Math.atan2(-(g.getX() + 0.5 - px), g.getZ() + 0.5 - pz));
                p.teleportTo(level, px, py, pz, java.util.Set.of(), yaw, in ? 35f : 20f, false);
                p.getAbilities().flying = true; p.onUpdateAbilities();
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set 6000");
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "gamerule doDaylightCycle false");
                return;
            }
            if (System.getProperty("thaumcraft.debugShrines") != null) {
                // -PdebugShrines=1: wisp altar, sinister obelisk and a warded wall in front of the player, goggles on.
                var level = (net.minecraft.server.level.ServerLevel) p.level();
                BlockPos base = BlockPos.containing(p.getX(), 200, p.getZ());
                for (BlockPos q : BlockPos.betweenClosed(base.offset(-10, -3, 2), base.offset(10, 14, 20))) level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                for (BlockPos q : BlockPos.betweenClosed(base.offset(-10, -1, 0), base.offset(10, -1, 20))) level.setBlock(q, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                var r = new java.util.Random(5);
                art.arcane.thaumcraft.worldgen.SinisterShrinesGen.wispAltar(level, r, base.offset(-5, -1, 12));
                art.arcane.thaumcraft.worldgen.SinisterShrinesGen.obelisk(level, r, base.offset(5, -1, 13));
                for (int x = -2; x <= 2; x++) for (int y = 0; y <= 1; y++) {
                    BlockPos w = base.offset(x, y, 5);
                    level.setBlock(w, Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    if (x != 0) art.arcane.thaumcraft.casters.ward.WardManager.ward(level, w, p.getUUID());
                }
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.GOGGLES.get()));
                p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
                p.getAbilities().flying = true; p.onUpdateAbilities();
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set 6000");
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "gamerule doDaylightCycle false");
                return;
            }
            String biome = System.getProperty("thaumcraft.debugBiome");
            if (biome != null) {
                // Teleport to the nearest biome (e.g. thaumcraft:magical_forest) for screenshots.
                var level = (net.minecraft.server.level.ServerLevel) p.level();
                var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.Identifier.parse(biome));
                var found = level.findClosestBiome3d(h -> h.is(key), p.blockPosition(), 12800, 32, 64);
                System.out.println("[DEBUGBIOME] " + biome + " -> " + (found == null ? "none" : found.getFirst()));
                if (found != null) {
                    var bp = found.getFirst();
                    level.getChunk(bp.getX() >> 4, bp.getZ() >> 4);
                    int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bp.getX(), bp.getZ());
                    p.teleportTo(bp.getX() + 0.5, y + 22, bp.getZ() + 0.5);
                    level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon");
                    p.getAbilities().flying = true; p.onUpdateAbilities();
                }
                return;
            }
            if (System.getProperty("thaumcraft.debugPlay") != null) {
                // Scripted early game: dust bookshelf -> pick up book -> open -> (client clicks FIRSTSTEPS) -> dust crafting table -> open arcane workbench
                p.setGameMode(GameType.SURVIVAL);
                p.setData(art.arcane.thaumcraft.registry.ModAttachments.RESEARCH.get(), new art.arcane.thaumcraft.research.PlayerResearch());
                var level = (net.minecraft.server.level.ServerLevel) p.level();
                BlockPos base = BlockPos.containing(p.getX(), 200, p.getZ());
                p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) p.level().setBlock(base.offset(x, 0, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
                BlockPos shelf = base.offset(-1, 1, 3), table = base.offset(1, 1, 3);
                final int[] st = {0, -1};
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() != p) return;
                    int t = ++st[0];
                    var r = art.arcane.thaumcraft.progression.PlayerProgressManager.get(p);
                    java.util.function.Consumer<BlockPos> dust = pos -> {
                        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(art.arcane.thaumcraft.ModItems.SALIS_MUNDUS.get(), 4));
                        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.SOUTH, pos, false);
                        // Real right-click path (ServerPlayerGameMode.useItemOn), first without sneaking, then with Shift held.
                        p.setShiftKeyDown(false);
                        var plain = p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                        System.out.println("[PLAY] plain click on " + level.getBlockState(pos).getBlock() + " -> " + plain + " menu=" + p.containerMenu.getClass().getSimpleName() + " dust=" + p.getMainHandItem().getCount());
                        p.closeContainer();
                        p.setShiftKeyDown(true);
                        var sneak = p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
                        p.setShiftKeyDown(false);
                        System.out.println("[PLAY] shift click on " + level.getBlockState(pos).getBlock() + " -> " + sneak + " menu=" + p.containerMenu.getClass().getSimpleName() + " dust=" + p.getMainHandItem().getCount());
                    };
                    if (t == 40) {
                        r.unlock("!gotcrystals"); r.unlock("!gotdream");
                        level.setBlock(shelf, Blocks.BOOKSHELF.defaultBlockState(), 3);
                        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
                        dust.accept(shelf);
                    }
                    if (t == 110) {
                        var books = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(shelf).inflate(1), ie -> ie.getItem().is(art.arcane.thaumcraft.ModItems.THAUMONOMICON.get()));
                        System.out.println("[PLAY] shelf now " + level.getBlockState(shelf).getBlock() + ", floating books " + books.size());
                        books.forEach(b -> b.playerTouch(p));
                        System.out.println("[PLAY] books in inventory " + p.getInventory().countItem(art.arcane.thaumcraft.ModItems.THAUMONOMICON.get()) + " gotthaumonomicon=" + r.has("!gotthaumonomicon"));
                    }
                    if (t == 130) {
                        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(art.arcane.thaumcraft.ModItems.THAUMONOMICON.get()));
                        System.out.println("[PLAY] open book -> " + art.arcane.thaumcraft.ModItems.THAUMONOMICON.get().use(level, p, InteractionHand.MAIN_HAND));
                    }
                    if (t > 130 && st[1] < 0 && r.has("FIRSTSTEPS@1")) { st[1] = t; System.out.println("[PLAY] FIRSTSTEPS started at t=" + t + " stage=" + r.stage("FIRSTSTEPS")); }
                    if (st[1] > 0 && t == st[1] + 60) dust.accept(table);
                    if (st[1] > 0 && t == st[1] + 130) {
                        System.out.println("[PLAY] table now " + level.getBlockState(table).getBlock());
                        if (level.getBlockEntity(table) instanceof net.minecraft.world.MenuProvider mp) p.openMenu(mp);
                    }
                    if (t == 600 && st[1] < 0) System.out.println("[PLAY] FIRSTSTEPS never started");
                });
            }
            String unlockOnly = System.getProperty("thaumcraft.debugUnlock");
            if (unlockOnly != null) {
                p.setData(art.arcane.thaumcraft.registry.ModAttachments.RESEARCH.get(), new art.arcane.thaumcraft.research.PlayerResearch());
                var r0 = art.arcane.thaumcraft.progression.PlayerProgressManager.get(p);
                for (String k : unlockOnly.split(",")) r0.unlock(k);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(p, false));
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() == p && p.tickCount % 40 == 0) {
                        var rr = art.arcane.thaumcraft.progression.PlayerProgressManager.get(p);
                        System.out.println("[DEBUGRESEARCH] t=" + p.tickCount + " FIRSTSTEPS stage=" + rr.stage("FIRSTSTEPS") + " has@1=" + rr.has("FIRSTSTEPS@1") + " gotthaum=" + rr.has("!gotthaumonomicon"));
                    }
                });
            }
            if (System.getProperty("thaumcraft.debugBook") != null && unlockOnly == null) {
                var r = art.arcane.thaumcraft.progression.PlayerProgressManager.get(p);
                String only = System.getProperty("thaumcraft.debugBookPartial");
                for (var en : art.arcane.thaumcraft.research.ResearchCatalog.entries().values()) {
                    // -PdebugBookPartial=KEY,KEY2:1 unlocks only those; ":n" leaves the research at stage n (incomplete)
                    var list = only == null ? null : java.util.Arrays.asList(only.split(","));
                    var staged = list == null ? null : list.stream().filter(k -> k.startsWith(en.key() + ":")).findFirst().orElse(null);
                    if (list != null && !list.contains(en.key()) && staged == null) continue;
                    r.unlock(en.key()); r.setStage(en.key(), staged != null ? Integer.parseInt(staged.substring(en.key().length() + 1)) : en.stages().size() + 1);
                }
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(p, false));
            }
            p.level().getServer().setDifficulty(net.minecraft.world.Difficulty.EASY, true);
            p.getAbilities().flying = true; p.onUpdateAbilities();
            var level = p.level();
            BlockPos base = BlockPos.containing(p.getX(), 200, p.getZ());
            p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
            String item = System.getProperty("thaumcraft.debugItem", "thaumometer");
            var it = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", item));
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(it, System.getProperty("thaumcraft.debugUse") != null ? it.getDefaultMaxStackSize() : 1));
            // -PdebugWear=head:crimson_robe_helm,curio_head:goggles: armour slots or Curios bauble slots
            String wear = System.getProperty("thaumcraft.debugWear");
            if (wear != null) for (String w : wear.split(",")) {
                String[] sw = w.split(":");
                var wi = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", sw[1])));
                // extra ":key=value" parts become custom_data ints (e.g. head:fortress_helm:mask=1:goggles=1)
                for (int k = 2; k < sw.length; k++) { String[] kv = sw[k].split("="); net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, wi, t -> t.putInt(kv[0], Integer.parseInt(kv[1]))); }
                if (sw[0].startsWith("curio_")) art.arcane.thaumcraft.util.Baubles.equip(p, sw[0].substring(6), 0, wi);
                else p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.byName(sw[0]), wi);
            }
            // -PdebugHarness=1: survival, airborne flight granted by a worn Thaumostatic Harness
            if (System.getProperty("thaumcraft.debugHarness") != null) {
                p.setGameMode(GameType.SURVIVAL);
                p.teleportTo(base.getX() + 0.5, base.getY() + 3, base.getZ() + 0.5);
                p.getAbilities().mayfly = true; p.getAbilities().flying = true; p.onUpdateAbilities();
                p.getPersistentData().putBoolean(art.arcane.thaumcraft.item.gear.HoverHarnessItem.GRANTED, true);
                level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon");
            }
            // -PdebugHotbar=amulet_vis#0,mc.deepslate,...: hotbar items ("mc." = vanilla), "#n" sets the thaumcraft:variant component
            String hot = System.getProperty("thaumcraft.debugHotbar");
            if (hot != null) { int slot = 1; for (String h : hot.split(",")) {
                String[] hv = h.split("#");
                var hs = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(hv[0].startsWith("mc.") ? net.minecraft.resources.Identifier.withDefaultNamespace(hv[0].substring(3)) : net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", hv[0])));
                if (hv.length > 1) hs.set(art.arcane.thaumcraft.registry.ModComponents.VARIANT.get(), Integer.parseInt(hv[1]));
                p.getInventory().setItem(slot++, hs);
            } }
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            // -PdebugDrops=n: loose items scattered 4-9 blocks around the player (axe pull checks).
            if (System.getProperty("thaumcraft.debugDrops") != null) for (int i = 0; i < Integer.parseInt(System.getProperty("thaumcraft.debugDrops")); i++) {
                double a = i * Math.PI * 2 / 8, d = 4 + i % 6;
                var drop = new net.minecraft.world.entity.item.ItemEntity(level, base.getX() + 0.5 + Math.cos(a) * d, base.getY() + 1.2, base.getZ() + 0.5 + Math.sin(a) * d, new ItemStack(Blocks.OAK_LOG));
                drop.setNeverPickUp();
                level.addFreshEntity(drop);
            }
            if (System.getProperty("thaumcraft.debugGrapple") != null) {
                // a wall 8 blocks south and a grapple fired at it 6 seconds after login
                for (int x = -3; x <= 3; x++) for (int y = 1; y <= 6; y++) level.setBlock(base.offset(x, y, 11), Blocks.STONE.defaultBlockState(), 3);
                level.getServer().execute(() -> {});
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() == p && p.tickCount > 100 && p.tickCount % 25 == 0) {
                        level.getEntitiesOfClass(art.arcane.thaumcraft.entity.GrappleEntity.class, p.getBoundingBox().inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
                        p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
                        var g = new art.arcane.thaumcraft.entity.GrappleEntity(level, p, net.minecraft.world.InteractionHand.MAIN_HAND);
                        g.shootFromRotation(p, 0, 0, 0, 1.5f, 0);
                        level.addFreshEntity(g);
                    }
                });
            }
            // -PdebugFx=collector|shield|shield_all|slash|pedestal: repeat an infusion enchantment / shield effect in front of the player
            String fx = System.getProperty("thaumcraft.debugFx");
            if (fx != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
                for (int x = -6; x <= 6; x++) for (int z = 1; z <= 9; z++) for (int y = 1; y <= 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                java.util.function.Function<BlockPos, net.minecraft.world.entity.monster.zombie.Zombie> zombie = at -> {
                    var zb = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(sl, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    zb.setNoAi(true); zb.setInvulnerable(true); zb.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET)); zb.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0); sl.addFreshEntity(zb); return zb;
                };
                var z1 = fx.equals("shield") || fx.equals("slash") ? zombie.apply(fx.equals("slash") ? base.offset(-2, 1, 4) : base.offset(3, 1, 1)) : null;
                var z2 = fx.equals("slash") ? zombie.apply(base.offset(2, 1, 5)) : null;
                BlockPos ped = base.offset(0, 1, 3);
                if (fx.equals("pedestal")) level.setBlock(ped, art.arcane.thaumcraft.ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState(), 3);
                NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() != p || p.tickCount < 80) return;
                    p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
                    switch (fx) {
                        case "collector" -> { if (p.tickCount % 8 == 0) {
                            var fi = new art.arcane.thaumcraft.entity.FollowingItemEntity(sl, base.getX() + 0.5 + (p.tickCount % 16 == 0 ? 5.0 : -5.0), base.getY() + 2.5, base.getZ() + 1.5, new ItemStack(Blocks.OAK_LOG), p, 10);
                            fi.setPickUpDelay(32767); sl.addFreshEntity(fi);
                            sl.getEntitiesOfClass(art.arcane.thaumcraft.entity.FollowingItemEntity.class, p.getBoundingBox().inflate(16), fe -> !fe.isFollowing()).forEach(net.minecraft.world.entity.Entity::discard);
                        } }
                        case "shield" -> { if (p.tickCount % 14 == 0) InfusionEnchantmentEvents.shieldFx(sl, p, z1.getId()); }
                        case "shield_all" -> { if (p.tickCount % 14 == 0) InfusionEnchantmentEvents.shieldFx(sl, p, -1); }
                        case "slash" -> { if (p.tickCount % 6 == 0) {
                            art.arcane.thaumcraft.fx.Fx.send(sl, "slash", p.getX(), p.getY(), p.getZ(), p.getId(), z1.getId());
                            art.arcane.thaumcraft.fx.Fx.send(sl, "slash", p.getX(), p.getY(), p.getZ(), z1.getId(), z2.getId());
                        } }
                        case "pedestal" -> { if (p.tickCount % 14 == 0) sl.blockEvent(ped, sl.getBlockState(ped).getBlock(), 5, 0); }
                        default -> {}
                    }
                });
                return;
            }
            // -PdebugInfusion=cloudring|cloudring_unstable: formed altar 5 blocks south, craft started after 5 s
            String inf = System.getProperty("thaumcraft.debugInfusion");
            if (inf != null && level instanceof net.minecraft.server.level.ServerLevel sl) {
                BlockPos ped = base.offset(0, 1, 6);
                boolean unstable = inf.contains("_unstable");
                double near = inf.contains(":") ? Double.parseDouble(inf.substring(inf.indexOf(':') + 1)) : 0;
                Runnable build = () -> {
                art.arcane.thaumcraft.essentia.EssentiaHandler.clear();
                // leftovers of earlier sessions (flux rifts eat blocks)
                level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(base).inflate(32), en -> !(en instanceof ServerPlayer)).forEach(net.minecraft.world.entity.Entity::discard);
                for (int x = -14; x <= 14; x++) for (int z = -6; z <= 18; z++) for (int y = -2; y <= 10; y++) level.setBlock(base.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : y == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                level.setBlock(ped, ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState(), 3);
                for (int[] c : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) level.setBlock(ped.offset(c[0], 0, c[1]), ModBlocks.PILLAR_ARCANE.get().defaultBlockState(), 3);
                level.setBlock(ped.above(2), ModBlocks.INFUSION_MATRIX.get().defaultBlockState(), 3);
                java.util.function.BiConsumer<BlockPos, ItemStack> pedestal = (bp, st) -> {
                    level.setBlock(bp, ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState(), 3);
                    ((art.arcane.thaumcraft.blockentity.PedestalBlockEntity) level.getBlockEntity(bp)).setItem(0, st);
                };
                ((art.arcane.thaumcraft.blockentity.PedestalBlockEntity) level.getBlockEntity(ped)).setItem(0, new ItemStack(ModItems.BAUBLES_RING_MUNDANE.get()));
                pedestal.accept(ped.offset(-3, 0, 0), art.arcane.thaumcraft.item.CrystalEssenceItem.of("aer", 1));
                pedestal.accept(ped.offset(3, 0, 0), new ItemStack(net.minecraft.world.item.Items.FEATHER));
                level.setBlock(ped.offset(-2, 0, 2), ModBlocks.STABILIZER.get().defaultBlockState(), 3);
                if (!unstable) level.setBlock(ped.offset(2, 0, -2), ModBlocks.STABILIZER.get().defaultBlockState(), 3);
                for (int[] c : new int[][] {{-3, -2}, {3, -2}}) {
                    BlockPos jp = ped.offset(c[0], 0, c[1]);
                    level.setBlock(jp, ModBlocks.JAR_NORMAL.get().defaultBlockState(), 3);
                    ((art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity) level.getBlockEntity(jp)).insert("aer", 40);
                }
                };
                art.arcane.thaumcraft.progression.PlayerProgressManager.get(p).unlock("CLOUDRING");
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.GOGGLES.get()));
                final int[] tk = {0};
                NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() != p) return;
                    int t = ++tk[0];
                    var dir = net.minecraft.core.Direction.UP;
                    if (t == 20) build.run();
                    if (t < 20) return;
                    if (t == 20) { sl.getServer().getCommands().performPrefixedCommand(sl.getServer().createCommandSourceStack(), "time set noon"); p.teleportTo(base.getX() + 0.5, base.getY() + 2.5, base.getZ() + 0.5 + near); }
                    var te = (art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity) level.getBlockEntity(ped.above(2));
                    if (te == null) return;
                    if (t == 80) te.onCasterRightClick(level, new ItemStack(ModItems.CASTER_BASIC.get()), p, te.getBlockPos(), dir, InteractionHand.MAIN_HAND);
                    if (t == 120) { if (!unstable) te.stability = te.stabilityCap; te.onCasterRightClick(level, new ItemStack(ModItems.CASTER_BASIC.get()), p, te.getBlockPos(), dir, InteractionHand.MAIN_HAND);
                        System.out.println("[INFVIS] crafting=" + te.crafting + " essentia=" + te.recipeEssentia() + " repl=" + te.stabilityReplenish); }
                    if (inf.contains("_loop") && t > 160 && t % 60 == 0 && !te.crafting) { // keep an infusion running for slow screenshot runs
                        ((art.arcane.thaumcraft.blockentity.PedestalBlockEntity) level.getBlockEntity(ped)).setItem(0, new ItemStack(ModItems.BAUBLES_RING_MUNDANE.get()));
                        ((art.arcane.thaumcraft.blockentity.PedestalBlockEntity) level.getBlockEntity(ped.offset(-3, 0, 0))).setItem(0, art.arcane.thaumcraft.item.CrystalEssenceItem.of("aer", 1));
                        ((art.arcane.thaumcraft.blockentity.PedestalBlockEntity) level.getBlockEntity(ped.offset(3, 0, 0))).setItem(0, new ItemStack(net.minecraft.world.item.Items.FEATHER));
                        for (int[] c : new int[][] {{-3, -2}, {3, -2}}) ((art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity) level.getBlockEntity(ped.offset(c[0], 0, c[1]))).insert("aer", 40);
                        te.stability = te.stabilityCap;
                        te.onCasterRightClick(level, new ItemStack(ModItems.CASTER_BASIC.get()), p, te.getBlockPos(), dir, InteractionHand.MAIN_HAND);
                    }
                    if (inf.contains("_zap") && t == 100) { // a target for the zap arc: a pig 2 blocks east of the matrix
                        var pig = net.minecraft.world.entity.EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        pig.snapTo(ped.getX() - 2.5, ped.getY(), ped.getZ() - 1.5, 90, 0); pig.setNoAi(true); pig.setInvulnerable(true); level.addFreshEntity(pig);
                    }
                    if (inf.contains("_zap") && t > 130 && t % 3 == 0) { te.debugZap(true); if (t % 120 == 0) System.out.println("[INFVIS] zap targets=" + level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(te.getBlockPos()).inflate(10))); p.setHealth(p.getMaxHealth()); }
                    if (unstable && !inf.contains("_zap") && t > 120 && te.crafting) te.stability = -100; // forces frequent instability events
                    if (t % 20 == 0) System.out.println("[INFVIS] t=" + t + " active=" + te.active + " crafting=" + te.crafting + " stab=" + te.stability + " count=" + te.craftCount + " center=" + level.getBlockState(ped) + " " + (level.getBlockEntity(ped) instanceof art.arcane.thaumcraft.blockentity.PedestalBlockEntity pe ? pe.item() : null));
                });
                return;
            }
            // Row of every mod block, each between two stone blocks, 4 blocks in front (south) of the player.
            String only = System.getProperty("thaumcraft.debugBlocks");
            if (only != null) {
                for (int x = -10; x <= 10; x++) for (int z = -4; z <= 14; z++) { for (int y = 1; y <= 14; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3); level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3); }
                for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(base).inflate(12), en -> !(en instanceof ServerPlayer))) old.discard();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) sl.getServer().getCommands().performPrefixedCommand(sl.getServer().createCommandSourceStack(), "time set noon");
                // entities of the saved world load a little later: clear the stage again once
                final BlockPos stage = base;
                NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() == p && p.tickCount == 40) for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(stage).inflate(12),
                            en -> !(en instanceof ServerPlayer) && !en.entityTags().contains("tcdebug") && !(en instanceof net.minecraft.world.entity.item.ItemEntity))) old.discard();
                });
                String gui = System.getProperty("thaumcraft.debugGui");
                if (gui != null) NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                    if (ev.getEntity() != p || p.tickCount != 100) return;
                    if (gui.equals("pech")) for (var pe : level.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.PechEntity.class, new net.minecraft.world.phys.AABB(stage).inflate(12))) {
                        pe.setTamed(true); p.interactOn(pe, InteractionHand.MAIN_HAND, p.position()); break; }
                    if (gui.equals("handmirror")) {
                        BlockPos mpos = null;
                        for (BlockPos q : BlockPos.betweenClosed(stage.offset(-10, 0, -4), stage.offset(10, 4, 14))) if (level.getBlockEntity(q) instanceof art.arcane.thaumcraft.blockentity.MirrorBlockEntity) { mpos = q.immutable(); break; }
                        ItemStack hm = new ItemStack(ModItems.HAND_MIRROR.get());
                        if (mpos != null) hm.set(art.arcane.thaumcraft.registry.ModComponents.MIRROR_LINK.get(), net.minecraft.core.GlobalPos.of(level.dimension(), mpos));
                        p.setItemInHand(InteractionHand.MAIN_HAND, hm);
                        hm.use(level, p, InteractionHand.MAIN_HAND);
                    }
                });
                int k = 0;
                for (String id : only.split(",")) {
                    if (id.startsWith("e:")) {
                        // "e:cultist_cleric+ritual": ritualist cleric focusing a point 3 blocks east (AI off, so the ritual persists)
                        // extra flags: +pech0/1/2 (forager/thaumaturge/stalker), +helm / +nohelm (crab), +angerNN (giant zombie anger NN/10)
                        int pechT = -1, helm = -1; float anger = -1;
                        var fm = java.util.regex.Pattern.compile("\\+(pech\\d|helm|nohelm|anger\\d+)").matcher(id);
                        while (fm.find()) { String fl = fm.group(1); if (fl.startsWith("pech")) pechT = fl.charAt(4) - '0'; else if (fl.equals("helm")) helm = 1; else if (fl.equals("nohelm")) helm = 0; else anger = Integer.parseInt(fl.substring(5)) / 10f; }
                        id = id.replaceAll("\\+(pech\\d|helm|nohelm|anger\\d+)", "");
                        // "e:spell_bat+fxFIRE+fxFROST": a spell bat carrying a real focus package with those effects
                        java.util.List<String> batFx = new java.util.ArrayList<>();
                        var bm = java.util.regex.Pattern.compile("\\+fx([A-Z]+)").matcher(id);
                        while (bm.find()) batFx.add("thaumcraft." + bm.group(1));
                        id = id.replaceAll("\\+fx[A-Z]+", "");
                        boolean ritual = id.endsWith("+ritual");
                        if (ritual) id = id.substring(0, id.length() - 7);
                        // "e:zombie+champion8": a champion with that modifier (minecraft entities allowed with a "mc." prefix)
                        int champion = -1;
                        if (id.contains("+champion")) { champion = Integer.parseInt(id.substring(id.indexOf("+champion") + 9)); id = id.substring(0, id.indexOf("+champion")); }
                        boolean headless = id.endsWith("+headless");
                        if (headless) id = id.substring(0, id.length() - 9);
                        boolean ai = id.endsWith("+ai");
                        if (ai) id = id.substring(0, id.length() - 3);
                        String eid = id.substring(2);
                        var t = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(eid.startsWith("mc.") ? net.minecraft.resources.Identifier.withDefaultNamespace(eid.substring(3)) : net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", eid));
                        var en = t.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.CultistEntity c) c.finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.PechEntity || en instanceof art.arcane.thaumcraft.entity.monster.EldritchCrabEntity || en instanceof art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity)
                            ((net.minecraft.world.entity.Mob) en).finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.PechEntity pe && pechT >= 0) {
                            pe.setPechType(pechT);
                            pe.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, switch (pechT) { case 1 -> new ItemStack(ModItems.PECH_WAND.get()); case 2 -> new ItemStack(net.minecraft.world.item.Items.BOW); default -> new ItemStack(net.minecraft.world.item.Items.IRON_SWORD); });
                        }
                        if (en instanceof art.arcane.thaumcraft.entity.monster.EldritchCrabEntity cr && helm >= 0) cr.setHelm(helm == 1);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity gz && anger >= 0) gz.setAnger(anger);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.boss.ThaumcraftBossEntity || en instanceof art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity || en instanceof art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity)
                            ((net.minecraft.world.entity.Mob) en).finalizeSpawn(level, level.getCurrentDifficultyAt(base), net.minecraft.world.entity.EntitySpawnReason.COMMAND, null);
                        if (en instanceof art.arcane.thaumcraft.entity.monster.boss.ThaumcraftBossEntity b && System.getProperty("thaumcraft.debugRisen") != null) b.spawnTimer = 0;
                        if (headless && en instanceof art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity g) g.setHeadless(true);
                        if (en instanceof net.minecraft.world.entity.monster.Monster mo && champion >= 0) art.arcane.thaumcraft.entity.champion.ChampionModifier.makeChampion(mo, true, champion);
                        else if (en instanceof net.minecraft.world.entity.monster.Monster mo) { var a = mo.getAttribute(art.arcane.thaumcraft.registry.ModAttributes.CHAMPION_MOD); if (a != null) a.addPermanentModifier(art.arcane.thaumcraft.entity.champion.ChampionModifier.ATTRIBUTE_MOD_NONE); }
                        if (en instanceof net.minecraft.world.entity.Mob m && !ai) m.setNoAi(true);
                        if (en instanceof net.minecraft.world.entity.monster.cubemob.Slime sl) sl.setSize(3, true);
                        if (en instanceof art.arcane.thaumcraft.entity.WispEntity w) w.setWispType("praecantatio");
                        if (en instanceof art.arcane.thaumcraft.entity.TaintSeedEntity ts) ts.boost = 4000;
                        if (en instanceof art.arcane.thaumcraft.entity.FluxRiftEntity fr) { fr.setRiftSeed(42); fr.setRiftSize(30); }
                        double gap = Double.parseDouble(System.getProperty("thaumcraft.debugEntGap", "2")), dist = Double.parseDouble(System.getProperty("thaumcraft.debugEntDist", "6"));
                        long ne = java.util.Arrays.stream(only.split(",")).filter(q -> q.startsWith("e:")).count();
                        en.snapTo(base.getX() + 0.5 + (k - (ne - 1) / 2.0) * gap, base.getY() + 1, base.getZ() + 0.5 + dist, 180, 0);
                        if (en instanceof net.minecraft.world.entity.LivingEntity le) { le.setYHeadRot(180); le.yBodyRot = 180; }
                        if (ritual && en instanceof art.arcane.thaumcraft.entity.monster.CultistClericEntity cc) {
                            BlockPos home = base.offset(k * 2 - 3, 0, 6);
                            level.setBlock(home, Blocks.OBSIDIAN.defaultBlockState(), 3);
                            cc.setHomeTo(home, 8);
                            cc.setIsRitualist(true);
                        }
                        if (en instanceof art.arcane.thaumcraft.entity.monster.TaintacleEntity) { // rooted taint mobs starve off taint: give them a patch of taint soil
                            BlockPos under = BlockPos.containing(en.getX(), base.getY(), en.getZ());
                            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlock(under.offset(dx, 0, dz), art.arcane.thaumcraft.ModBlocks.TAINT_SOIL.get().defaultBlockState(), 3);
                        }
                        { // stone roof high above + invisible light blocks: undead don't burn in the noon sun
                            BlockPos at = BlockPos.containing(en.getX(), base.getY() + 14, en.getZ());
                            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) level.setBlock(at.offset(dx, 0, dz), Blocks.STONE.defaultBlockState(), 3);
                            for (int dx = -2; dx <= 2; dx += 2) for (int dz = -2; dz <= 2; dz += 2) level.setBlock(at.offset(dx, -10, dz), Blocks.LIGHT.defaultBlockState(), 3);
                        }
                        if (en instanceof art.arcane.thaumcraft.entity.SpellBatEntity sb) { // a focus summon: needs an owner and lives 600 ticks
                            sb.setOwner(p);
                            var pkg = new art.arcane.thaumcraft.casters.FocusPackage(p);
                            pkg.addNode(new art.arcane.thaumcraft.casters.FocusMediumRoot());
                            for (String fk : batFx.isEmpty() ? java.util.List.of("thaumcraft.FIRE") : batFx) pkg.addNode(art.arcane.thaumcraft.casters.FocusEngine.getElement(fk));
                            sb.setFocusPackage(pkg);
                            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.EntityTickEvent.Post ev) -> { if (ev.getEntity() == sb) sb.tickCount = 1; });
                        }
                        if (en instanceof art.arcane.thaumcraft.entity.monster.TaintacleSmallEntity tsm) tsm.lifetime = 100000;
                        en.addTag("tcdebug");
                        level.addFreshEntity(en);
                        k++;
                        continue;
                    }
                    if (id.equals("mirror+linked") || id.equals("mirror_essentia+linked")) {
                        Block mb = id.startsWith("mirror_essentia") ? art.arcane.thaumcraft.ModBlocks.MIRROR_ESSENTIA.get() : art.arcane.thaumcraft.ModBlocks.MIRROR.get();
                        // a linked pair of magic mirrors on a stone wall, facing the player
                        for (int i = 0; i < 2; i++) {
                            BlockPos wall = base.offset(-3 + k * 2 + i * 2, 1, 4), mp = wall.north();
                            level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
                            level.setBlock(mp, mb.defaultBlockState().setValue(art.arcane.thaumcraft.block.MirrorBlock.FACING, net.minecraft.core.Direction.NORTH), 3);
                        }
                        var m1 = (art.arcane.thaumcraft.blockentity.MirrorBlockEntity) level.getBlockEntity(base.offset(-3 + k * 2, 1, 3));
                        m1.setLink(net.minecraft.core.GlobalPos.of(level.dimension(), base.offset(-1 + k * 2, 1, 3)));
                        m1.restoreLink();
                        k += 2;
                        continue;
                    }
                    boolean filled = id.endsWith("+filled");
                    String bid = filled ? id.substring(0, id.length() - 7) : id;
                    Block b = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(bid.startsWith("mc.") ? net.minecraft.resources.Identifier.withDefaultNamespace(bid.substring(3)) : net.minecraft.resources.Identifier.fromNamespaceAndPath("thaumcraft", bid));
                    level.setBlock(base.offset(-3 + k * 2, 1, 3), b.defaultBlockState(), 3);
                    if (filled) { // goggles screenshots: a jar of ignis, a boiling crucible with essentia
                        var fbe = level.getBlockEntity(base.offset(-3 + k * 2, 1, 3));
                        if (fbe instanceof art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity jar) jar.insert("ignis", 48);
                        if (fbe instanceof art.arcane.thaumcraft.blockentity.CrucibleBlockEntity cru) {
                            // refilled every 40 ticks: an unheated crucible spills one essentia per 100 ticks (TC6 spillRandom)
                            NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post ev) -> {
                                if (ev.getEntity() != p || p.tickCount % 40 != 1 || cru.isRemoved()) return;
                                var tags = new java.util.LinkedHashMap<String, Integer>();
                                tags.put("aqua", 12); tags.put("terra", 5); tags.put("herba", 3); tags.put("ordo", 7); tags.put("victus", 2); tags.put("metallum", 9);
                                cru.mixture().empty(); cru.mixture().fill(); for (int q = 0; q < 200; q++) cru.mixture().heatTick(true);
                                cru.mixture().dissolve(tags); cru.changed();
                            });
                        }
                        if (fbe instanceof art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity ear) ear.note = 9;
                    }
                    k++;
                }
                return;
            }
            int i = 0;
            for (var holder : ModBlocks.BLOCKS.getEntries()) {
                Block b = holder.get();
                int x = -11 + (i % 12) * 2, z = 4 + (i / 12) * 2;
                if (z > 12) break;
                level.setBlock(base.offset(x, 1, z), b.defaultBlockState(), 3);
                level.setBlock(base.offset(x, 1, z + 1), Blocks.STONE.defaultBlockState(), 3);
                i++;
            }
        });
    }
    private DebugSceneServer() {}
}
