package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.entity.FireBatEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.scan.ScanManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class FireBatVerification {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    public static void test(GameTestHelper h) {
        var world = h.getLevel();
        var type = ModEntities.FIRE_BAT.get();
        // registration, spawning rules and egg
        check(type.getCategory() == MobCategory.MONSTER && type.fireImmune(), "Fire bat must be a fire immune monster");
        check(Math.abs(type.getWidth() - 0.5f) < 1e-4 && Math.abs(type.getHeight() - 0.9f) < 1e-4, "Fire bat size changed");
        check(SpawnPlacements.getPlacementType(type) == SpawnPlacementTypes.ON_GROUND, "Fire bat spawn placement not registered");
        var nether = world.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES).value();
        check(nether.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(w -> w.value().type() == type && w.weight() == 10
                && w.value().minCount() == 1 && w.value().maxCount() == 2), "Nether biomes lack the 10/1/2 fire bat spawn");
        check(ModItems.FIREBAT_SPAWN_EGG.get() instanceof SpawnEggItem && SpawnEggItem.getType(ModItems.FIREBAT_SPAWN_EGG.get().getDefaultInstance()) == type, "Spawn egg does not hatch fire bats");
        // attributes, defaults and scan aspects
        h.setBlock(new BlockPos(2, 5, 2), Blocks.STONE);
        FireBatEntity hanging = h.spawn(type, new BlockPos(2, 4, 2));
        FireBatEntity flyer = h.spawn(type, new BlockPos(6, 3, 6));
        check(hanging.getAttributeBaseValue(Attributes.MAX_HEALTH) == 5.0 && hanging.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 1.0, "Fire bat attributes changed");
        check(hanging.getIsBatHanging() && flyer.getIsBatHanging(), "Fire bats are created hanging");
        var aspects = ScanManager.entityAspects(hanging);
        check(aspects.getOrDefault(AspectRegistry.IGNIS, 0) == 10 && aspects.getOrDefault(AspectRegistry.BESTIA, 0) == 5 && aspects.getOrDefault(AspectRegistry.VOLATUS, 0) == 5, "Fire bat aspects differ from ConfigAspects");
        // fire and explosions are ignored, other damage wakes it
        check(!hanging.hurtServer(world, world.damageSources().onFire(), 2) && !hanging.hurtServer(world, world.damageSources().explosion(null, null), 2), "Fire bat took fire/explosion damage");
        check(hanging.getHealth() == 5.0f, "Fire bat lost health to fire");
        // pig target placed in-reach: the bat must attack it
        var pig = h.spawn(EntityTypes.PIG, new BlockPos(6, 2, 7));
        pig.setNoAi(true);
        float pigHealth = pig.getHealth();
        flyer.setIsBatHanging(false);
        flyer.setTarget(pig);
        h.runAfterDelay(30, () -> {
            check(hanging.isAlive() && hanging.getIsBatHanging(), "Bat under a solid ceiling should keep hanging");
            check(flyer.isRemoved() || !flyer.getIsBatHanging(), "Bat without ceiling should fly");
            check(pig.getHealth() < pigHealth, "Fire bat did not attack its target");
            check(hanging.hurtServer(world, world.damageSources().generic(), 1) && !hanging.getIsBatHanging(), "Damage did not wake the hanging bat");
            // drops: gunpowder 0-2 per bat
            var origin = h.absolutePos(new BlockPos(2, 2, 2));
            for (int i = 0; i < 20; i++) {
                var bat = type.create(world, EntitySpawnReason.COMMAND);
                bat.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
                world.addFreshEntity(bat);
                bat.kill(world);
            }
            var drops = world.getEntitiesOfClass(ItemEntity.class, new AABB(origin).inflate(3));
            check(!drops.isEmpty() && drops.stream().allMatch(e -> e.getItem().is(Items.GUNPOWDER) && e.getItem().getCount() <= 2), "Fire bat loot is not 0-2 gunpowder");
            System.out.println("Fire bat passed: registration, nether spawns, egg, attributes, aspects, immunity, hanging, attack and drops.");
            h.succeed();
        });
    }
}
