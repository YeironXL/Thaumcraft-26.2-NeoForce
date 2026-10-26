package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.item.gear.HoverHarnessItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Thaumostatic Harness: fuelling from a jar, flight granted/revoked, Potentia burnt while flying. */
final class HoverHarnessVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    static void test(GameTestHelper h) {
        // --- fuelling from a warded jar of Potentia
        BlockPos jp = new BlockPos(2, 1, 2);
        h.setBlock(jp, ModBlocks.JAR_NORMAL.get());
        var jar = h.getBlockEntity(jp, EssentiaJarBlockEntity.class);
        check(jar.insert("ignis", 10), "jar ignis");
        ItemStack harness = new ItemStack(ModItems.HOVER_HARNESS.get());
        check(HoverHarnessItem.fillFromJar(harness, jar) == 0 && HoverHarnessItem.getFuel(harness) == 0, "ignis accepted as fuel");
        jar.extract(10);
        check(jar.insert("potentia", 200), "jar potentia");
        check(HoverHarnessItem.fillFromJar(harness, jar) == 200 && HoverHarnessItem.getFuel(harness) == 200, "fill amount " + HoverHarnessItem.getFuel(harness));
        check(jar.contents().amount() == 0, "jar not drained");
        jar.insert("potentia", 250); jar.extract(0);
        HoverHarnessItem.setFuel(harness, 400);
        check(HoverHarnessItem.fillFromJar(harness, jar) == 100 && jar.contents().amount() == 150, "fill capped at max");
        check(HoverHarnessItem.isActive(harness), "inactive by default");

        // --- flight granted while worn and fuelled
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 4, 1.5)));
        HoverHarnessItem.setFuel(harness, 3);
        player.setItemSlot(EquipmentSlot.CHEST, harness);
        player.tickCount = 1;
        HoverHarnessItem.tick(player);
        check(player.getAbilities().mayfly, "flight not granted");
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        // --- consumption: not while merely allowed, 1 per second while flying
        for (int t = 2; t <= 40; t++) { player.tickCount = t; HoverHarnessItem.tick(player); }
        check(HoverHarnessItem.getFuel(worn) == 3, "fuel used without flying");
        player.getAbilities().flying = true;
        for (int t = 41; t <= 80; t++) { player.tickCount = t; HoverHarnessItem.tick(player); }
        check(HoverHarnessItem.getFuel(worn) == 1, "consumption " + HoverHarnessItem.getFuel(worn));
        // --- toggle off revokes flight, with a safe landing
        HoverHarnessItem.setActive(worn, false);
        player.tickCount = 81; HoverHarnessItem.tick(player);
        check(!player.getAbilities().mayfly && !player.getAbilities().flying, "toggle did not revoke");
        check(player.hasEffect(MobEffects.SLOW_FALLING), "no safe landing");
        HoverHarnessItem.setActive(worn, true);
        player.tickCount = 82; HoverHarnessItem.tick(player);
        check(player.getAbilities().mayfly, "re-enable");
        // --- running dry revokes flight
        player.getAbilities().flying = true;
        for (int t = 83; t <= 101; t++) { player.tickCount = t; HoverHarnessItem.tick(player); }
        check(HoverHarnessItem.getFuel(worn) == 0, "fuel left " + HoverHarnessItem.getFuel(worn));
        player.tickCount = 102; HoverHarnessItem.tick(player);
        check(!player.getAbilities().mayfly, "empty harness still flies");
        // --- removing the harness revokes; creative flight is never touched
        HoverHarnessItem.setFuel(worn, 50);
        HoverHarnessItem.tick(player);
        check(player.getAbilities().mayfly, "refuelled");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        HoverHarnessItem.tick(player);
        check(!player.getAbilities().mayfly, "unequipped still flies");
        var creative = h.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().mayfly = true;
        HoverHarnessItem.tick(creative);
        check(creative.getAbilities().mayfly, "creative flight removed");
        h.succeed();
    }
}
