package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Travelling Trunk (TC4 recreation): following, stay toggle, pickup keeping contents, upgrades. */
final class TrunkVerification {
    private static void floor(GameTestHelper h) { for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE); }
    private static void check(boolean c, String m) { if (!c) throw new net.minecraft.gametest.framework.GameTestAssertException(net.minecraft.network.chat.Component.literal(m), 0); }
    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        var p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        p.snapTo(v.x, v.y, v.z, 0, 0);
        return p;
    }
    private static TravellingTrunkEntity trunk(GameTestHelper h, ServerPlayer owner, double x, double z) {
        Vec3 v = h.absoluteVec(new Vec3(x, 1, z));
        TravellingTrunkEntity t = TravellingTrunkEntity.spawn(h.getLevel(), v.x, v.y, v.z, 0, owner, null);
        check(t != null, "trunk not spawned");
        return t;
    }

    /** The trunk walks to its far-away owner. */
    static void follow(GameTestHelper h) {
        floor(h);
        var p = player(h, 16.5, 16.5);
        var t = trunk(h, p, 3.5, 3.5);
        double d0 = t.distanceTo(p);
        h.succeedWhen(() -> {
            h.assertTrue(t.distanceTo(p) < 4.0, "Trunk did not follow: " + t.distanceTo(p) + " (start " + d0 + ")");
            System.out.println("Trunk follow passed: distance " + d0 + " -> " + t.distanceTo(p));
        });
    }

    /** Sneak-right-click toggles staying; a staying trunk does not move towards its owner. */
    static void stay(GameTestHelper h) {
        floor(h);
        var p = player(h, 16.5, 16.5);
        var t = trunk(h, p, 3.5, 3.5);
        p.setShiftKeyDown(true);
        t.interact(p, InteractionHand.MAIN_HAND, t.position());
        p.setShiftKeyDown(false);
        check(t.isStaying(), "sneak-use did not make it stay");
        Vec3 start = t.position();
        h.runAfterDelay(80, () -> {
            check(t.position().distanceTo(start) < 0.6, "staying trunk moved " + t.position().distanceTo(start));
            p.setShiftKeyDown(true);
            t.interact(p, InteractionHand.MAIN_HAND, t.position());
            p.setShiftKeyDown(false);
            check(!t.isStaying(), "second sneak-use did not release it");
            h.succeedWhen(() -> h.assertTrue(t.distanceTo(p) < 4.0, "released trunk did not follow: " + t.distanceTo(p)));
        });
    }

    /** Contents survive picking the trunk up (owner sneak-hit) and placing it again; strangers cannot hurt it. */
    static void pickup(GameTestHelper h) {
        floor(h);
        var p = player(h, 5.5, 5.5);
        var stranger = player(h, 7.5, 5.5);
        var t = trunk(h, p, 6.5, 6.5);
        t.setStaying(true);
        t.inventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        t.inventory().setItem(26, new ItemStack(Items.APPLE, 3));
        float hp = t.getHealth();
        check(!t.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(stranger), 10f) && t.getHealth() == hp, "stranger hurt the trunk");
        check(t.isAlive(), "stranger removed the trunk");
        p.setShiftKeyDown(true);
        t.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 1f);
        p.setShiftKeyDown(false);
        check(t.isRemoved(), "owner sneak-hit did not pick it up");
        ItemStack item = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(ModItems.TRAVELLING_TRUNK.get())) item = p.getInventory().getItem(i);
        if (item.isEmpty()) for (ItemEntity ie : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(t.blockPosition()).inflate(3))) if (ie.getItem().is(ModItems.TRAVELLING_TRUNK.get())) item = ie.getItem();
        check(!item.isEmpty(), "no trunk item");
        check(item.has(DataComponents.CONTAINER), "trunk item lost its contents");
        p.setItemInHand(InteractionHand.MAIN_HAND, item.copy());
        BlockPos floorPos = h.absolutePos(new BlockPos(10, 0, 10));
        item.getItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floorPos), Direction.UP, floorPos, false)));
        var placed = h.getLevel().getEntitiesOfClass(TravellingTrunkEntity.class, new AABB(floorPos).inflate(2));
        check(placed.size() == 1, "trunk not placed again");
        var t2 = placed.get(0);
        check(t2.inventory().getItem(0).is(Items.DIAMOND) && t2.inventory().getItem(0).getCount() == 7, "diamonds lost: " + t2.inventory().getItem(0));
        check(t2.inventory().getItem(26).is(Items.APPLE) && t2.inventory().getItem(26).getCount() == 3, "apples lost: " + t2.inventory().getItem(26));
        check(t2.isOwner(p), "placed trunk not owned");
        // plain (non-sneaking) owner hit: the trunk pops off as an item on the ground, contents kept, and can be placed again
        p.getInventory().clearContent();
        BlockPos at2 = t2.blockPosition();
        t2.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(p), 1f);
        check(t2.isRemoved(), "owner plain hit did not knock the trunk loose");
        ItemEntity dropped = null;
        for (ItemEntity ie : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at2).inflate(3))) if (ie.getItem().is(ModItems.TRAVELLING_TRUNK.get())) dropped = ie;
        check(dropped != null, "no trunk item on the ground after a plain hit");
        check(dropped.getItem().has(DataComponents.CONTAINER), "dropped trunk lost its contents");
        ItemStack again = dropped.getItem().copy();
        dropped.discard();
        p.setItemInHand(InteractionHand.MAIN_HAND, again.copy());
        BlockPos floor3 = h.absolutePos(new BlockPos(14, 0, 14));
        again.getItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor3), Direction.UP, floor3, false)));
        var placed3 = h.getLevel().getEntitiesOfClass(TravellingTrunkEntity.class, new AABB(floor3).inflate(2));
        check(placed3.size() == 1 && placed3.get(0).inventory().getItem(0).getCount() == 7, "trunk from the ground not placed again with its contents");
        System.out.println("Trunk pickup passed: contents kept across pickup/placement.");
        h.succeed();
    }

    /** Vis crystals upgrade the trunk: aer speeds it up, terra doubles the slots, perditio swallows loose items. */
    static void upgrade(GameTestHelper h) {
        floor(h);
        var p = player(h, 2.5, 2.5);
        var a = trunk(h, p, 4.5, 4.5); a.setStaying(true);
        var e = trunk(h, p, 8.5, 4.5); e.setStaying(true);
        var v = trunk(h, p, 12.5, 12.5); v.setStaying(true);
        double base = a.getAttributeValue(Attributes.MOVEMENT_SPEED);
        p.setItemInHand(InteractionHand.MAIN_HAND, CrystalEssenceItem.of("aer", 2));
        a.interact(p, InteractionHand.MAIN_HAND, a.position());
        check(a.getUpgrade() == TravellingTrunkEntity.AIR && a.getAttributeValue(Attributes.MOVEMENT_SPEED) > base, "aer upgrade: " + a.getUpgrade() + " speed " + a.getAttributeValue(Attributes.MOVEMENT_SPEED));
        check(p.getMainHandItem().getCount() == 1, "crystal not consumed");
        a.interact(p, InteractionHand.MAIN_HAND, a.position());
        check(a.getUpgrade() == TravellingTrunkEntity.AIR && p.getMainHandItem().getCount() == 1, "second upgrade was accepted");
        check(e.slotCount() == 27, "base slots " + e.slotCount());
        p.setItemInHand(InteractionHand.MAIN_HAND, CrystalEssenceItem.of("terra", 1));
        e.interact(p, InteractionHand.MAIN_HAND, e.position());
        check(e.slotCount() == 54, "terra slots " + e.slotCount());
        p.setItemInHand(InteractionHand.MAIN_HAND, CrystalEssenceItem.of("perditio", 1));
        v.interact(p, InteractionHand.MAIN_HAND, v.position());
        check(v.getUpgrade() == TravellingTrunkEntity.ENTROPY, "perditio upgrade");
        Vec3 ip = h.absoluteVec(new Vec3(14.0, 1.2, 12.5));
        var loose = new ItemEntity(h.getLevel(), ip.x, ip.y, ip.z, new ItemStack(Items.EMERALD, 5));
        h.getLevel().addFreshEntity(loose);
        h.succeedWhen(() -> {
            h.assertTrue(v.inventory().getItem(0).is(Items.EMERALD) && v.inventory().getItem(0).getCount() == 5, "perditio trunk did not swallow emeralds: " + v.inventory().getItem(0));
            System.out.println("Trunk upgrade passed: aer speed " + a.getAttributeValue(Attributes.MOVEMENT_SPEED) + ", terra 54 slots, perditio swallowed items.");
        });
    }
    private TrunkVerification() {}

    /** TC4: a following trunk near its owner goes with it to another dimension, keeping contents, upgrade and owner. */
    static void dimension(GameTestHelper h) {
        floor(h);
        var p = player(h, 6.5, 6.5);
        var t = trunk(h, p, 4.5, 4.5);
        var stayer = trunk(h, p, 8.5, 8.5);
        stayer.setStaying(true);
        t.inventory().setItem(3, new ItemStack(Items.DIAMOND, 5));
        t.setUpgrade(TravellingTrunkEntity.EARTH);
        t.inventory().setItem(40, new ItemStack(Items.EMERALD, 2));
        var followers = TravellingTrunkEntity.followers(p);
        check(followers.contains(t) && !followers.contains(stayer), "followers " + followers);
        var nether = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        check(nether != null, "no nether level in the test server");
        Vec3 dest = new Vec3(0.5, 128, 0.5);
        var moved = TravellingTrunkEntity.bringTo(t, nether, dest, 0);
        check(moved != null && moved.level() == nether, "trunk not moved to the nether: " + moved);
        check(t.isRemoved() && stayer.isAlive() && stayer.level() == h.getLevel(), "old trunk still present / stayer moved");
        check(moved.inventory().getItem(3).is(Items.DIAMOND) && moved.inventory().getItem(3).getCount() == 5, "contents lost " + moved.inventory().getItem(3));
        check(moved.getUpgrade() == TravellingTrunkEntity.EARTH && moved.inventory().getItem(40).is(Items.EMERALD), "upgrade/earth slots lost");
        check(p.getUUID().equals(moved.getOwnerId()) && !moved.isStaying(), "owner lost");
        System.out.println("Trunk dimension follow passed: " + moved.level().dimension() + " " + moved.position());
        moved.discard();
        h.succeed();
    }
}
