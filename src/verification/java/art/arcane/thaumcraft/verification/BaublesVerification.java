package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.casters.CasterManager;
import art.arcane.thaumcraft.events.WarpEvents;
import art.arcane.thaumcraft.item.BaubleItem;
import art.arcane.thaumcraft.item.RechargeHelper;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.util.Baubles;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import top.theillusivec4.curios.api.CuriosApi;

/** Bauble slots through Curios: equip, worn effects, vis amulet recharge, voidseer warp/discount, revealing, ring and charm events. */
public final class BaublesVerification {
    private static void check(boolean ok, String m) { if (!ok) throw new AssertionError(m); }

    public static void test(GameTestHelper h) {
        check(Baubles.available(), "Curios not loaded");
        // a plain mock player for effects and damage (a mock ServerPlayer has no connection to send them on)
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var level = h.getLevel();
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5)));
        // mock players never join the level, so Curios has not built their default inventory yet
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> { if (inv.getCurios().isEmpty()) inv.reset(); });
        // slot layout: amulet, two rings, belt, head, body, charm; tags gate the slots
        for (String s : new String[] { Baubles.AMULET, Baubles.RING, Baubles.BELT, Baubles.HEAD, Baubles.BODY, Baubles.CHARM })
            check(Baubles.equip(player, s, 0, ItemStack.EMPTY), "missing bauble slot " + s);
        check(Baubles.equip(player, Baubles.RING, 1, ItemStack.EMPTY), "second ring slot missing");
        check(CuriosApi.getCurio(new ItemStack(ModItems.VERDANT_CHARM.get())).isPresent(), "verdant charm is not a curio");
        check(new ItemStack(ModItems.CLOUD_RING.get()).is(top.theillusivec4.curios.api.CuriosTags.RING), "ring tag missing");

        // verdant charm: cures poison for 10 charge
        ItemStack charm = new ItemStack(ModItems.VERDANT_CHARM.get());
        charm.set(ModComponents.CHARGE.get(), 200);
        Baubles.equip(player, Baubles.CHARM, 0, charm);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
        player.tickCount = 20;
        for (var w : Baubles.worn(player)) Baubles.wornTick(w.stack(), player);
        var worn = Baubles.find(player, ModItems.VERDANT_CHARM.get()).orElseThrow();
        check(!player.hasEffect(MobEffects.POISON), "verdant charm did not cure poison");
        check(RechargeHelper.getCharge(worn.stack()) == 190, "verdant charm charge not consumed: " + RechargeHelper.getCharge(worn.stack()));
        // lifegiver heals 1 for 5
        BaubleItem.withType(worn.stack(), 1);
        player.setHealth(10);
        Baubles.wornTick(worn.stack(), player);
        check(player.getHealth() == 11 && RechargeHelper.getCharge(worn.stack()) == 185, "lifegiver did not heal");

        // vis amulet (crafted, every 5 ticks) recharges the worn charm from the aura
        worn.stack().set(ModComponents.CHARGE.get(), 0);
        Baubles.equip(player, Baubles.AMULET, 0, BaubleItem.withType(new ItemStack(ModItems.AMULET_VIS.get()), 1));
        NativeAura.addVis(level, player.blockPosition(), 50);
        player.tickCount = 25;
        Baubles.wornTick(Baubles.find(player, ModItems.AMULET_VIS.get()).orElseThrow().stack(), player);
        check(RechargeHelper.getCharge(Baubles.find(player, ModItems.VERDANT_CHARM.get()).orElseThrow().stack()) == 1, "vis amulet did not recharge the worn charm");

        // voidseer charm: permanent warp 100 -> 25% discount, 5 warp; apprentice ring adds 5%
        var sp = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        CuriosApi.getCuriosInventory(sp).ifPresent(inv -> { if (inv.getCurios().isEmpty()) inv.reset(); });
        PlayerProgressManager.get(sp).setPermanentWarp(100);
        Baubles.equip(sp, Baubles.CHARM, 0, new ItemStack(ModItems.VOIDSEER_CHARM.get()));
        Baubles.equip(sp, Baubles.RING, 1, new ItemStack(ModItems.BAUBLES_RING_APPRENTICE.get()));
        check(WarpEvents.getWarpFromGear(sp) == 5, "voidseer warp wrong: " + WarpEvents.getWarpFromGear(sp));
        check(Math.abs(CasterManager.getTotalVisDiscount(sp) - 0.30f) < 1e-4, "vis discount wrong: " + CasterManager.getTotalVisDiscount(sp));
        Baubles.equip(player, Baubles.CHARM, 0, ItemStack.EMPTY);

        // goggles worn as head bauble reveal
        check(!Baubles.hasRevealer(player), "revealer without goggles");
        Baubles.equip(player, Baubles.HEAD, 0, new ItemStack(ModItems.GOGGLES.get()));
        check(Baubles.hasRevealer(player) && Baubles.hasGoggles(player), "goggles bauble does not reveal");

        // ring of clouds: fall damage amount/3 - 2
        Baubles.equip(player, Baubles.RING, 0, new ItemStack(ModItems.CLOUD_RING.get()));
        player.setHealth(20);
        player.hurtServer(level, level.damageSources().fall(), 6);
        check(player.getHealth() == 20, "small fall not negated by ring of clouds");
        player.invulnerableTime = 0;
        player.hurtServer(level, level.damageSources().fall(), 15);
        check(Math.abs(player.getHealth() - 17) < 1e-3, "ring of clouds fall reduction wrong: " + player.getHealth());

        // charm of undying consumed on death
        Baubles.equip(player, Baubles.CHARM, 0, new ItemStack(ModItems.CHARM_UNDYING.get()));
        player.invulnerableTime = 0;
        player.hurtServer(level, level.damageSources().generic(), 1000);
        check(player.isAlive() && player.getHealth() >= 1 && player.hasEffect(MobEffects.REGENERATION), "charm of undying did not save the player");
        check(!Baubles.isEquipped(player, ModItems.CHARM_UNDYING.get()), "charm of undying not consumed");
        // variants: thaumcraft:variant drives the item model; vis amulet found/crafted names and rarity
        ItemStack sustain = BaubleItem.withType(new ItemStack(ModItems.VERDANT_CHARM.get()), 2);
        check(Integer.valueOf(2).equals(sustain.get(ModComponents.VARIANT.get())) && BaubleItem.type(sustain) == 2, "verdant variant component missing");
        check(BaubleItem.type(new ItemStack(ModItems.VERDANT_CHARM.get())) == 0, "default verdant variant not 0");
        ItemStack found = new ItemStack(ModItems.AMULET_VIS.get()), crafted = BaubleItem.withType(new ItemStack(ModItems.AMULET_VIS.get()), 1);
        check(found.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t1 && t1.getKey().equals("item.amulet_vis.found.name"), "found amulet name");
        check(crafted.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t2 && t2.getKey().equals("item.amulet_vis.crafted.name"), "crafted amulet name");
        check(crafted.getRarity() == net.minecraft.world.item.Rarity.RARE && found.getRarity() == net.minecraft.world.item.Rarity.UNCOMMON, "vis amulet rarity");

        // crimson cult armour: ThaumcraftMaterials stats, equipment assets, iron repair, robe warp
        var plateChest = new ItemStack(ModItems.CRIMSON_PLATE_CHEST.get());
        var eq = plateChest.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        check(eq != null && eq.slot() == net.minecraft.world.entity.EquipmentSlot.CHEST && eq.assetId().orElseThrow().identifier().toString().equals("thaumcraft:cultist_plate"), "plate chest equippable");
        check(plateChest.getMaxDamage() == 16 * 18, "plate chest durability " + plateChest.getMaxDamage());
        double[] armour = new double[1];
        new ItemStack(ModItems.CRIMSON_PRAETOR_CHEST.get()).forEachModifier(net.minecraft.world.entity.EquipmentSlot.CHEST, (a, m) -> { if (a.is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) armour[0] += m.amount(); });
        check(armour[0] == 7, "praetor chest armour " + armour[0]);
        check(plateChest.isValidRepairItem(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT)), "plate not repairable with iron");
        sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.CRIMSON_ROBE_HELM.get()));
        sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.CRIMSON_BOOTS.get()));
        check(WarpEvents.getWarpFromGear(sp) == 7, "robe/boots warp wrong: " + WarpEvents.getWarpFromGear(sp));
        // cultist knight and cleric wear their crimson gear after finalizeSpawn
        var knight = art.arcane.thaumcraft.registry.ModEntities.CULTIST_KNIGHT.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        knight.finalizeSpawn(level, level.getCurrentDifficultyAt(knight.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        check(knight.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.CRIMSON_PLATE_CHEST.get()), "knight without plate");
        var cleric = art.arcane.thaumcraft.registry.ModEntities.CULTIST_CLERIC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        cleric.finalizeSpawn(level, level.getCurrentDifficultyAt(cleric.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        check(cleric.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(ModItems.CRIMSON_ROBE_CHEST.get()), "cleric without robe");
        h.succeed();
    }
}
