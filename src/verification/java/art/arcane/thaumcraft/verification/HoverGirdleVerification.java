package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.item.gear.HoverGirdleItem;
import art.arcane.thaumcraft.item.gear.HoverHarnessItem;
import art.arcane.thaumcraft.util.Baubles;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Thaumostatic Girdle: speed boost only while harness-flying, extra Potentia burn, no fall damage. */
final class HoverGirdleVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    /** Mock players never join the level, so Curios has not built their inventory yet. */
    private static void init(net.minecraft.world.entity.player.Player p) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(inv -> { if (inv.getCurios().isEmpty()) inv.reset(); });
    }

    static void test(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 4, 1.5)));
        init(player);
        check(Baubles.equip(player, Baubles.BELT, 0, new ItemStack(ModItems.HOVER_GIRDLE.get())), "belt slot");
        check(HoverGirdleItem.isWorn(player), "girdle not detected");
        // --- girdle alone: no speed change
        player.tickCount = 1; HoverHarnessItem.tick(player);
        check(player.getAbilities().getFlyingSpeed() == HoverGirdleItem.BASE_FLY_SPEED, "boost without harness");
        // --- harness allowed but not flying: no boost
        ItemStack harness = new ItemStack(ModItems.HOVER_HARNESS.get());
        HoverHarnessItem.setFuel(harness, 100);
        player.setItemSlot(EquipmentSlot.CHEST, harness);
        ItemStack worn = player.getItemBySlot(EquipmentSlot.CHEST);
        player.tickCount = 2; HoverHarnessItem.tick(player);
        check(player.getAbilities().mayfly, "harness flight");
        check(player.getAbilities().getFlyingSpeed() == HoverGirdleItem.BASE_FLY_SPEED, "boost while not flying");
        // --- flying: boosted, 1 Potentia per 15 ticks (60 ticks -> 4 instead of 3)
        player.getAbilities().flying = true;
        for (int t = 3; t <= 62; t++) { player.tickCount = t; HoverHarnessItem.tick(player); }
        check(player.getAbilities().getFlyingSpeed() == HoverGirdleItem.FLY_SPEED, "no boost while flying");
        check(HoverHarnessItem.getFuel(worn) == 96, "girdle consumption " + HoverHarnessItem.getFuel(worn));
        // --- harness switched off: speed restored
        HoverHarnessItem.setActive(worn, false);
        player.tickCount = 63; HoverHarnessItem.tick(player);
        check(player.getAbilities().getFlyingSpeed() == HoverGirdleItem.BASE_FLY_SPEED, "speed not restored");
        // --- girdle removed: normal speed and rate
        HoverHarnessItem.setActive(worn, true);
        Baubles.equip(player, Baubles.BELT, 0, ItemStack.EMPTY);
        player.getAbilities().flying = true;
        HoverHarnessItem.setFuel(worn, 100);
        for (int t = 64; t <= 123; t++) { player.tickCount = t; HoverHarnessItem.tick(player); }
        check(player.getAbilities().getFlyingSpeed() == HoverGirdleItem.BASE_FLY_SPEED, "boost without girdle");
        check(HoverHarnessItem.getFuel(worn) == 97, "base consumption " + HoverHarnessItem.getFuel(worn));
        // --- fall damage: none with the girdle, normal without
        var level = h.getLevel();
        var faller = h.makeMockPlayer(GameType.SURVIVAL);
        init(faller);
        Baubles.equip(faller, Baubles.BELT, 0, new ItemStack(ModItems.HOVER_GIRDLE.get()));
        float hp = faller.getHealth();
        faller.hurtServer(level, faller.damageSources().fall(), 8.0f);
        check(faller.getHealth() == hp, "fall damage with girdle " + faller.getHealth());
        var control = h.makeMockPlayer(GameType.SURVIVAL);
        float hp2 = control.getHealth();
        control.hurtServer(level, control.damageSources().fall(), 8.0f);
        check(control.getHealth() < hp2, "control took no fall damage");
        h.succeed();
    }
}
