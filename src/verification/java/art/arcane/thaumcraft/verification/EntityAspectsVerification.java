package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.infusion.InfusionEnchantment;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.scan.EntityScanRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/** TC6 ConfigAspects.registerEntityTags values via AspectHelper.getEntityAspects, and ToolEvents ESSENCE drops. */
final class EntityAspectsVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }
    private static Map<Aspect, Integer> m(Object... kv) {
        var out = new LinkedHashMap<Aspect, Integer>();
        for (int i = 0; i < kv.length; i += 2) out.put(AspectRegistry.get((String) kv[i]), (Integer) kv[i + 1]);
        return out;
    }
    private static void expect(net.minecraft.world.entity.Entity e, Map<Aspect, Integer> want) {
        var got = EntityScanRegistry.aspects(e);
        check(new java.util.ArrayList<>(got.entrySet()).equals(new java.util.ArrayList<>(want.entrySet())), e.getType() + " aspects " + got + " != TC6 " + want);
    }
    private static <T extends net.minecraft.world.entity.Entity> T make(GameTestHelper h, EntityType<T> t) { return t.create(h.getLevel(), EntitySpawnReason.TRIGGERED); }

    static void test(GameTestHelper h) {
        expect(make(h, EntityTypes.ZOMBIE), m("exanimis", 20, "humanus", 10, "terra", 5));
        expect(make(h, EntityTypes.SKELETON), m("exanimis", 20, "humanus", 5, "terra", 5));
        expect(make(h, EntityTypes.WITHER_SKELETON), m("exanimis", 25, "humanus", 5, "perditio", 10));
        expect(make(h, EntityTypes.MAGMA_CUBE), m("aqua", 5, "ignis", 10, "alkimia", 5));
        expect(make(h, EntityTypes.ZOMBIFIED_PIGLIN), m("exanimis", 15, "ignis", 15, "bestia", 10));
        expect(make(h, EntityTypes.ELDER_GUARDIAN), m("bestia", 10, "alienis", 15, "aqua", 15));
        expect(make(h, ModEntities.ELDRITCH_WARDEN.get()), m("alienis", 40, "mortuus", 40, "exanimis", 40));
        expect(make(h, ModEntities.ELDRITCH_GOLEM.get()), m("alienis", 40, "potentia", 40, "machina", 40));
        expect(make(h, ModEntities.TAINT_SEED.get()), m("herba", 20, "bestia", 20, "vitium", 20));
        expect(make(h, ModEntities.GOLEM.get()), m("machina", 10, "humanus", 10, "motus", 10));
        var pech = make(h, ModEntities.PECH.get());
        pech.setPechType(0); expect(pech, m("humanus", 10, "auram", 5, "permutatio", 10, "desiderium", 5));
        pech.setPechType(1); expect(pech, m("humanus", 10, "auram", 5, "permutatio", 10, "aversio", 5));
        pech.setPechType(2); expect(pech, m("humanus", 10, "auram", 5, "permutatio", 10, "praecantatio", 5));
        var wisp = make(h, ModEntities.WISP.get());
        wisp.setWispType("ignis"); expect(wisp, m("ignis", 5, "auram", 5, "volatus", 5));
        expect(make(h, EntityTypes.WARDEN), m("exanimis", 25, "tenebrae", 25, "sensus", 20, "potentia", 20));
        expect(make(h, EntityTypes.DROWNED), m("exanimis", 20, "humanus", 10, "aqua", 5));
        expect(make(h, EntityTypes.PIGLIN), m("humanus", 10, "ignis", 15, "desiderium", 5));
        for (var t : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE) {
            if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("minecraft")) continue;
            var e = t.create(h.getLevel(), EntitySpawnReason.TRIGGERED);
            if (e instanceof net.minecraft.world.entity.Mob && t.getCategory() != net.minecraft.world.entity.MobCategory.MISC || e instanceof net.minecraft.world.entity.npc.Npc)
                check(!EntityScanRegistry.aspects(e).isEmpty(), "Vanilla mob without aspects: " + t);
        }

        // ESSENCE V: rand.nextInt(5) < 5 always starts the loop, so at least one crystal of the zombie's aspects drops.
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var sword = new ItemStack(Items.IRON_SWORD);
        InfusionEnchantment.addInfusionEnchantment(sword, InfusionEnchantment.ESSENCE, 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        var zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 3));
        zombie.hurtServer(h.getLevel(), h.getLevel().damageSources().playerAttack(player), 1000);
        var crystals = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(zombie.blockPosition()).inflate(3), i -> i.getItem().is(ModItems.CRYSTAL_ESSENCE.get()));
        check(!crystals.isEmpty(), "Essence kill dropped no crystals");
        for (var c : crystals) check(java.util.Set.of("exanimis", "humanus", "terra").contains(CrystalEssenceItem.aspect(c.getItem())), "Essence crystal not from zombie aspects: " + CrystalEssenceItem.aspect(c.getItem()));
        System.out.println("Entity aspects passed: TC6 tags for " + 15 + " entities/variants, modern mobs tagged, essence dropped " + crystals.size() + " zombie crystals.");
        h.succeed();
    }
}
