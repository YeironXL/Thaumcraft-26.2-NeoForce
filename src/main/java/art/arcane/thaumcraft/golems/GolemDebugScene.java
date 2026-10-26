package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.golems.builder.GolemBuilderBlock;
import art.arcane.thaumcraft.golems.builder.GolemBuilderBlockEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.seals.SealMenu;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Development aid (-Dthaumcraft.debugGolems): golems, seals, press, crossbows and bore in front of the player for screenshots. */
public final class GolemDebugScene {
    public static void register() {
        if (System.getProperty("thaumcraft.debugGolems") == null) return;
        System.out.println("[GOLEMSCENE] registered");
        final int[] t = {0};
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            var pl = e.getServer().getPlayerList().getPlayers();
            if (pl.isEmpty()) return;
            t[0]++;
            ServerPlayer p = pl.get(0);
            if (t[0] == 100) { try { build(p); System.out.println("[GOLEMSCENE] built"); } catch (Throwable ex) { ex.printStackTrace(); } return; }
            if (t[0] != 200) return;
            String gui = System.getProperty("thaumcraft.debugGolemsGui", "");
            BlockPos base = BlockPos.containing(p.getX(), p.getY() - 1, p.getZ());
            if (gui.startsWith("seal")) {
                var se = SealHandler.getSealEntity(p.level(), new art.arcane.thaumcraft.golems.seals.SealPos(base.offset(0, 0, 4), Direction.UP));
                if (se instanceof art.arcane.thaumcraft.golems.seals.SealEntity s2 && gui.length() > 4) for (int i = 0; i < Integer.parseInt(gui.substring(4)); i++) {}
                if (se != null) SealMenu.open(p, se);
            } else if (gui.equals("press") && p.level().getBlockEntity(base.offset(-5, 1, 6)) instanceof GolemBuilderBlockEntity te) p.openMenu(te, buf -> buf.writeBlockPos(te.getBlockPos()));
            else if (gui.equals("bore")) {
                var b = p.level().getEntitiesOfClass(art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity.class, p.getBoundingBox().inflate(20));
                if (!b.isEmpty()) b.get(0).interact(p, InteractionHand.MAIN_HAND, b.get(0).position());
            }
        });
    }
    private static GolemProperties pr(int mat, int head, int arm, int leg, int addon) {
        GolemProperties g = new GolemProperties();
        g.setMaterial(GolemProperties.MATERIALS.get(mat)); g.setHead(GolemProperties.HEADS.get(head)); g.setArms(GolemProperties.ARMS.get(arm));
        g.setLegs(GolemProperties.LEGS.get(leg)); g.setAddon(GolemProperties.ADDONS.get(addon));
        return g;
    }
    private static void build(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        var r = art.arcane.thaumcraft.progression.PlayerProgressManager.get(p);
        for (var en : art.arcane.thaumcraft.research.ResearchCatalog.entries().values()) { r.unlock(en.key()); r.setStage(en.key(), en.stages().size() + 1); }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(p, false));
        p.getAbilities().flying = false; p.onUpdateAbilities();
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon"); // daylight screenshots
        BlockPos base = BlockPos.containing(p.getX() + 40, 230, p.getZ());
        for (int x = -12; x <= 12; x++) for (int y = 1; y <= 6; y++) for (int z = -6; z <= 15; z++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        for (int x = -9; x <= 9; x++) for (int z = -3; z <= 12; z++) level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);
        p.teleportTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5);
        GolemProperties[] combos = {pr(0, 0, 0, 0, 0), pr(1, 1, 2, 0, 1), pr(3, 3, 1, 1, 3), pr(4, 2, 3, 2, 0), pr(5, 4, 4, 3, 0), pr(2, 1, 0, 0, 3)};
        String mode = System.getProperty("thaumcraft.debugGolems");
        for (int i = 0; i < combos.length; i++) {
            GolemEntity g = ModEntities.GOLEM.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            BlockPos at = base.offset(-5 + i * 2, 1, 3 + (mode.equals("close") ? 0 : 2));
            g.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
            g.setProperties(combos[i]);
            g.setOwned(true); g.setValidSpawn(); g.setOwnerId(p.getUUID());
            g.setGolemColor((byte) (i * 3));
            if (i == 2) g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT, 5));
            g.initialSpawn();
            level.addFreshEntity(g);
        }
        // seals: collect seal on the floor, fill seal on a chest, guard seal on a wall
        SealHandler.addSealEntity(level, base.offset(0, 0, 4), Direction.UP, SealHandler.newSeal("thaumcraft:pickup"), p);
        level.setBlock(base.offset(2, 1, 9), Blocks.CHEST.defaultBlockState(), 3);
        SealHandler.addSealEntity(level, base.offset(2, 1, 9), Direction.UP, SealHandler.newSeal("thaumcraft:fill_advanced"), p);
        level.setBlock(base.offset(-2, 1, 10), Blocks.STONE.defaultBlockState(), 3);
        SealHandler.addSealEntity(level, base.offset(-2, 1, 10), Direction.NORTH, SealHandler.newSeal("thaumcraft:guard"), p);
        // golem press
        BlockPos core = base.offset(-5, 1, 6);
        level.setBlock(core, ModBlocks.GOLEM_BUILDER.get().defaultBlockState().setValue(GolemBuilderBlock.FACING, Direction.NORTH), 3);
        level.setBlock(core.east(), ModBlocks.PLACEHOLDER_TABLE.get().defaultBlockState(), 3);
        level.setBlock(core.south(), ModBlocks.PLACEHOLDER_CAULDRON.get().defaultBlockState(), 3);
        level.setBlock(core.south().east(), ModBlocks.PLACEHOLDER_ANVIL.get().defaultBlockState(), 3);
        level.setBlock(core.above().south(), ModBlocks.PLACEHOLDER_BARS.get().defaultBlockState(), 3);
        // crossbows and bore
        var t1 = ModEntities.TURRET_BASIC.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        var t2 = ModEntities.TURRET_ADVANCED.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        var t3 = ModEntities.ARCANE_BORE.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        int k = 0;
        for (var t : new art.arcane.thaumcraft.entity.construct.OwnedConstruct[]{t1, t2, t3}) {
            BlockPos at = base.offset(4 + k * 2, 1, mode.equals("close") ? 4 : 7);
            t.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180, 0);
            t.setYHeadRot(160 + k * 20);
            t.setOwned(true); t.setValidSpawn(); t.setOwnerId(p.getUUID());
            level.addFreshEntity(t);
            k++;
        }
        t3.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(art.arcane.thaumcraft.ModItems.GOLEM_BELL.get()));
        if (mode.equals("tc4seals")) buildTc4Seals(level, p, base);
        // xray: a wall between the player and the golems (the client sneaks, see DebugScene)
        if (mode.equals("xray")) for (int x = -8; x <= 8; x++) for (int y = 1; y <= 5; y++) level.setBlock(base.offset(x, y, 2), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        // bore: switch the arcane bore on, charged, digging the floor/blocks it faces
        if (mode.equals("bore") && t3 instanceof art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity bore) {
            bore.setFacing(Direction.SOUTH); bore.setCharge(10.0f); bore.setActive(true);
            level.setBlock(bore.blockPosition().below(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            for (int x = 1; x <= 6; x++) for (int z = -3; z <= 0; z++) level.setBlock(bore.blockPosition().offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            p.teleportTo(bore.getX() + 5, bore.getY(), bore.getZ() - 2);
            for (int x = -3; x <= 3; x++) for (int y = 0; y <= 5; y++) for (int z = 1; z <= 9; z++) level.setBlock(BlockPos.containing(bore.getX(), bore.getY(), bore.getZ()).offset(x, y - 1, z), (z + x) % 2 == 0 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState(), 3);
        }
    }
    /** The TC4-core seals: fishing at a pond, decanting into a cauldron, alchemy between jars, each with a golem. */
    private static void buildTc4Seals(ServerLevel level, ServerPlayer p, BlockPos base) {
        for (int x = 3; x <= 5; x++) for (int z = 8; z <= 10; z++) { level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3); level.setBlock(base.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 3); }
        SealHandler.addSealEntity(level, base.offset(2, 0, 9), Direction.UP, SealHandler.newSeal("thaumcraft:fishing"), p);
        level.setBlock(base.offset(-4, 1, 9), Blocks.CAULDRON.defaultBlockState(), 3);
        level.setBlock(base.offset(-7, -1, 9), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(base.offset(-7, 0, 9), Blocks.WATER.defaultBlockState(), 3);
        SealHandler.addSealEntity(level, base.offset(-4, 1, 9), Direction.UP, SealHandler.newSeal("thaumcraft:fluid"), p);
        level.setBlock(base.offset(-1, 1, 7), ModBlocks.JAR_NORMAL.get().defaultBlockState(), 3);
        level.setBlock(base.offset(1, 1, 7), ModBlocks.JAR_NORMAL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(base.offset(1, 1, 7)) instanceof art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity j) j.insert("ignis", 200);
        SealHandler.addSealEntity(level, base.offset(-1, 1, 7), Direction.UP, SealHandler.newSeal("thaumcraft:essentia"), p);
        p.getInventory().setItem(1, new ItemStack(art.arcane.thaumcraft.ModItems.SEAL_FISHING.get()));
        p.getInventory().setItem(2, new ItemStack(art.arcane.thaumcraft.ModItems.SEAL_FLUID.get()));
        p.getInventory().setItem(3, new ItemStack(art.arcane.thaumcraft.ModItems.SEAL_ESSENTIA.get()));
    }
    private GolemDebugScene() {}
}
