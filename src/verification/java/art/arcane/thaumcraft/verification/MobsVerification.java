package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.MirrorBlock;
import art.arcane.thaumcraft.blockentity.MirrorBlockEntity;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.*;
import art.arcane.thaumcraft.menu.PechMenu;
import art.arcane.thaumcraft.registry.ModAttributes;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class MobsVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    public static void mirror(GameTestHelper h) {
        var w = h.getLevel();
        BlockPos wallA = new BlockPos(2, 2, 2), wallB = new BlockPos(8, 2, 2);
        h.setBlock(wallA, Blocks.STONE); h.setBlock(wallB, Blocks.STONE);
        var st = ModBlocks.MIRROR.get().defaultBlockState().setValue(MirrorBlock.FACING, Direction.SOUTH);
        h.setBlock(wallA.south(), st); h.setBlock(wallB.south(), st);
        var a = (MirrorBlockEntity) w.getBlockEntity(h.absolutePos(wallA.south()));
        var b = (MirrorBlockEntity) w.getBlockEntity(h.absolutePos(wallB.south()));
        a.setLink(GlobalPos.of(w.dimension(), b.getBlockPos()));
        a.restoreLink();
        check(a.linked && b.linked && a.isLinkValid() && b.isLinkValid(), "mirrors did not link");
        var item = new ItemEntity(w, a.getBlockPos().getX() + 0.5, a.getBlockPos().getY() + 0.3, a.getBlockPos().getZ() + 0.3, new ItemStack(Items.DIAMOND, 3));
        item.setDeltaMovement(0, 0, 0);
        w.addFreshEntity(item);
        // hand mirror
        var hm = new ItemStack(ModItems.HAND_MIRROR.get());
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        hm.set(art.arcane.thaumcraft.registry.ModComponents.MIRROR_LINK.get(), GlobalPos.of(w.dimension(), b.getBlockPos()));
        check(art.arcane.thaumcraft.item.HandMirrorItem.transport(hm, new ItemStack(Items.EMERALD, 2), player, w), "hand mirror transport failed");
        h.runAfterDelay(80, () -> {
            var out = w.getEntitiesOfClass(ItemEntity.class, new AABB(b.getBlockPos()).inflate(4));
            int d = out.stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum();
            int em = out.stream().filter(e -> e.getItem().is(Items.EMERALD)).mapToInt(e -> e.getItem().getCount()).sum();
            check(item.isRemoved(), "item not swallowed by mirror");
            check(d == 3 && em == 2, "partner ejected " + d + " diamonds, " + em + " emeralds");
            check(a.instability > 0, "no instability");
            h.succeed();
        });
    }

    public static void champion(GameTestHelper h) {
        var w = h.getLevel();
        var z = h.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 3));
        var a = z.getAttribute(ModAttributes.CHAMPION_MOD);
        check(a != null, "no champion attribute");
        a.removeModifiers(); z.getAttribute(Attributes.MAX_HEALTH).removeModifiers(); z.getAttribute(Attributes.ATTACK_DAMAGE).removeModifiers(); if (z.getAttribute(Attributes.ARMOR) != null) z.getAttribute(Attributes.ARMOR).removeModifiers(); z.setCustomName(null);
        double base = z.getAttributeValue(Attributes.MAX_HEALTH), dmg = z.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ChampionModifier.makeChampion(z, true, 5);
        check(ChampionModifier.championValue(z) == 5, "champion value " + ChampionModifier.championValue(z));
        check(z.getAttributeValue(Attributes.MAX_HEALTH) == base + 100, "no +100 health");
        check(Math.abs(z.getAttributeValue(Attributes.ATTACK_DAMAGE) - dmg * 3) < 1e-6, "no x3 damage");
        check(z.getAbsorptionAmount() > 0, "warded has no absorption");
        check(z.getCustomName() != null, "no champion name");
        // ordinary roll marks -1
        var z2 = h.spawn(EntityTypes.PIG, new BlockPos(6, 2, 6));
        check(ChampionModifier.championValue(z2) == -2, "pig was considered");
        var crab = h.spawn(ModEntities.ELDRITCH_CRAB.get(), new BlockPos(5, 2, 3));
        check(ChampionModifier.championValue(crab) >= -1, "monster not rolled");
        h.succeed();
    }

    public static void taint(GameTestHelper h) {
        var w = h.getLevel();
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        var crawler = h.spawn(ModEntities.TAINT_CRAWLER.get(), new BlockPos(4, 2, 4));
        check(crawler.getAttributeBaseValue(Attributes.MAX_HEALTH) == 8.0, "crawler hp");
        var tent = h.spawn(ModEntities.TAINTACLE.get(), new BlockPos(1, 2, 1));
        var small = h.spawn(ModEntities.TAINTACLE_SMALL.get(), new BlockPos(7, 2, 7));
        check(tent.getAttributeBaseValue(Attributes.MAX_HEALTH) == 50.0 && small.getAttributeBaseValue(Attributes.MAX_HEALTH) == 5.0, "taintacle hp");
        float hp = tent.getHealth();
        h.runAfterDelay(45, () -> check(tent.getHealth() < hp, "taintacle off taint did not starve"));
        // fibre laying is random per tick; poll until it happens (test max_ticks bounds it)
        h.succeedWhen(() -> { if (!h.getLevel().getBlockStates(new AABB(h.absolutePos(new BlockPos(0, 2, 0))).expandTowards(9, 0, 9)).anyMatch(s -> s.is(ModBlocks.TAINT_FIBRE.get()))) throw h.assertionException(net.minecraft.network.chat.Component.literal("crawler laid no fibre")); });
    }

    public static void pech(GameTestHelper h) {
        var w = h.getLevel();
        var pech = h.spawn(ModEntities.PECH.get(), new BlockPos(3, 2, 3));
        pech.setNoAi(true);
        check(PechEntity.getValueOf(new ItemStack(Items.ENDER_PEARL)) == 15, "pearl value");
        pech.setTamed(true);
        var player = h.makeMockServerPlayer(GameType.SURVIVAL);
        var menu = new PechMenu(1, player.getInventory(), pech);
        menu.inventory.setItem(0, new ItemStack(Items.ENDER_PEARL));
        check(menu.canRoll(), "cannot roll");
        // a single roll can legitimately pay nothing (random trade table); retry a few pearls
        int n = 0;
        for (int tries = 0; tries < 20 && n == 0; tries++) {
            if (tries > 0) menu.inventory.setItem(0, new ItemStack(Items.ENDER_PEARL));
            menu.clickMenuButton(player, 0);
            check(menu.inventory.getItem(0).isEmpty(), "pearl not consumed");
            for (int i = 1; i < 5; i++) n += menu.inventory.getItem(i).getCount();
        }
        check(n > 0, "trade paid nothing");
        // anger spreads
        var other = h.spawn(ModEntities.PECH.get(), new BlockPos(6, 2, 6));
        pech.hurtServer(w, w.damageSources().playerAttack(player), 1.0f);
        check(other.getAnger() > 0 && !pech.isTamed(), "anger did not spread");
        h.succeed();
    }
}
