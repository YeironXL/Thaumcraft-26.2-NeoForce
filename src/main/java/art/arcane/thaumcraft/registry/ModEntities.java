package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.entity.AlumentumProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,Thaumcraft.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<AlumentumProjectile>> ALUMENTUM=TYPES.register("alumentum",()->
        EntityType.Builder.<AlumentumProjectile>of(AlumentumProjectile::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"alumentum"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.CausalityCollapserProjectile>> CAUSALITY_COLLAPSER=TYPES.register("causality_collapser",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.CausalityCollapserProjectile>of(art.arcane.thaumcraft.entity.CausalityCollapserProjectile::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"causality_collapser"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.SpecialItemEntity>> SPECIAL_ITEM=TYPES.register("special_item",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.SpecialItemEntity>of(art.arcane.thaumcraft.entity.SpecialItemEntity::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(6).updateInterval(20)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"special_item"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FollowingItemEntity>> FOLLOWING_ITEM=TYPES.register("follow_item",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FollowingItemEntity>of(art.arcane.thaumcraft.entity.FollowingItemEntity::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(20)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"follow_item"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FocusProjectileEntity>> FOCUS_PROJECTILE=TYPES.register("focus_projectile",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FocusProjectileEntity>of(art.arcane.thaumcraft.entity.FocusProjectileEntity::new,MobCategory.MISC)
            .sized(0.15F,0.15F).clientTrackingRange(4).updateInterval(5)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"focus_projectile"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FocusCloudEntity>> FOCUS_CLOUD=TYPES.register("focus_cloud",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FocusCloudEntity>of(art.arcane.thaumcraft.entity.FocusCloudEntity::new,MobCategory.MISC)
            .sized(0.15F,0.15F).clientTrackingRange(4).updateInterval(5).noSave()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"focus_cloud"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FocusMineEntity>> FOCUS_MINE=TYPES.register("focus_mine",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FocusMineEntity>of(art.arcane.thaumcraft.entity.FocusMineEntity::new,MobCategory.MISC)
            .sized(0.15F,0.15F).clientTrackingRange(4).updateInterval(5)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"focus_mine"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.SpellBatEntity>> SPELL_BAT=TYPES.register("spellbat",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.SpellBatEntity>of(art.arcane.thaumcraft.entity.SpellBatEntity::new,MobCategory.MISC)
            .sized(0.5F,0.9F).clientTrackingRange(8).updateInterval(3)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"spellbat"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.GrappleEntity>> GRAPPLE=TYPES.register("grapple",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.GrappleEntity>of(art.arcane.thaumcraft.entity.GrappleEntity::new,MobCategory.MISC)
            .sized(0.1F,0.1F).clientTrackingRange(4).updateInterval(1).noSave()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"grapple"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.BottleTaintEntity>> BOTTLE_TAINT=TYPES.register("bottle_taint",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.BottleTaintEntity>of(art.arcane.thaumcraft.entity.BottleTaintEntity::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"bottle_taint"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.ThaumicSlimeEntity>> THAUMIC_SLIME=TYPES.register("thaumic_slime",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.ThaumicSlimeEntity>of(art.arcane.thaumcraft.entity.ThaumicSlimeEntity::new,MobCategory.MONSTER)
            .sized(0.52F,0.52F).eyeHeight(0.325F).spawnDimensionsScale(4.0F).clientTrackingRange(10).notInPeaceful()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"thaumic_slime"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FluxRiftEntity>> FLUX_RIFT=TYPES.register("flux_rift",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FluxRiftEntity>of(art.arcane.thaumcraft.entity.FluxRiftEntity::new,MobCategory.MISC)
            .sized(2.0F,2.0F).clientTrackingRange(4).updateInterval(20).fireImmune()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"flux_rift"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.FireBatEntity>> FIRE_BAT=TYPES.register("firebat",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.FireBatEntity>of(art.arcane.thaumcraft.entity.FireBatEntity::new,MobCategory.MONSTER)
            .sized(0.5F,0.9F).fireImmune().clientTrackingRange(4).updateInterval(3)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"firebat"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.MindSpiderEntity>> MIND_SPIDER=TYPES.register("mind_spider",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.MindSpiderEntity>of(art.arcane.thaumcraft.entity.monster.MindSpiderEntity::new,MobCategory.MONSTER)
            .sized(0.7F,0.5F).eyeHeight(0.45F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"mind_spider"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity>> ELDRITCH_GUARDIAN=TYPES.register("eldritch_guardian",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity>of(art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity::new,MobCategory.MONSTER)
            .sized(0.8F,2.25F).eyeHeight(2.1F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_guardian"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.CultistKnightEntity>> CULTIST_KNIGHT=TYPES.register("cultist_knight",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.CultistKnightEntity>of(art.arcane.thaumcraft.entity.monster.CultistKnightEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.8F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"cultist_knight"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.CultistClericEntity>> CULTIST_CLERIC=TYPES.register("cultist_cleric",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.CultistClericEntity>of(art.arcane.thaumcraft.entity.monster.CultistClericEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.8F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"cultist_cleric"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity>> CULTIST_PORTAL_LESSER=TYPES.register("cultist_portal_lesser",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity>of(art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity::new,MobCategory.MONSTER)
            .sized(1.5F,3.0F).fireImmune().clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"cultist_portal_lesser"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity>> ELDRITCH_ORB=TYPES.register("eldritch_orb",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity>of(art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_orb"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.projectile.GolemOrbEntity>> GOLEM_ORB=TYPES.register("golem_orb",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.projectile.GolemOrbEntity>of(art.arcane.thaumcraft.entity.projectile.GolemOrbEntity::new,MobCategory.MISC)
            .sized(0.25F,0.25F).clientTrackingRange(4).updateInterval(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"golem_orb"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.WispEntity>> WISP=TYPES.register("wisp",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.WispEntity>of(art.arcane.thaumcraft.entity.WispEntity::new,MobCategory.MONSTER)
            .sized(0.9F,0.9F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"wisp"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.TaintSeedEntity>> TAINT_SEED=TYPES.register("taint_seed",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.TaintSeedEntity>of(art.arcane.thaumcraft.entity.TaintSeedEntity::new,MobCategory.MONSTER)
            .sized(1.5F,1.25F).clientTrackingRange(4).updateInterval(20).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taint_seed"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.TaintSeedPrimeEntity>> TAINT_SEED_PRIME=TYPES.register("taint_seed_prime",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.TaintSeedPrimeEntity>of(art.arcane.thaumcraft.entity.TaintSeedPrimeEntity::new,MobCategory.MONSTER)
            .sized(2.0F,2.0F).clientTrackingRange(4).updateInterval(20).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taint_seed_prime"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.golems.GolemEntity>> GOLEM=TYPES.register("golem",()->
        EntityType.Builder.<art.arcane.thaumcraft.golems.GolemEntity>of(art.arcane.thaumcraft.golems.GolemEntity::new,MobCategory.MISC)
            .sized(0.4F,0.9F).eyeHeight(0.7F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"golem"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity>> TRAVELLING_TRUNK=TYPES.register("travelling_trunk",()->
        EntityType.Builder.<art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity>of(art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity::new,MobCategory.MISC)
            .sized(0.7F,0.85F).eyeHeight(0.6F).clientTrackingRange(8).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"travelling_trunk"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.projectile.GolemDartEntity>> GOLEM_DART=TYPES.register("golem_dart",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.projectile.GolemDartEntity>of(art.arcane.thaumcraft.entity.projectile.GolemDartEntity::new,MobCategory.MISC)
            .sized(0.2F,0.2F).clientTrackingRange(4).updateInterval(20)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"golem_dart"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity>> TURRET_BASIC=TYPES.register("turret_basic",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity>of(art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity::new,MobCategory.MISC)
            .sized(0.95F,1.25F).eyeHeight(0.825F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"turret_basic"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.construct.TurretCrossbowAdvancedEntity>> TURRET_ADVANCED=TYPES.register("turret_advanced",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.construct.TurretCrossbowAdvancedEntity>of(art.arcane.thaumcraft.entity.construct.TurretCrossbowAdvancedEntity::new,MobCategory.MISC)
            .sized(0.95F,1.5F).eyeHeight(1.0F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"turret_advanced"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity>> ELDRITCH_AUTOCASTER=TYPES.register("eldritch_autocaster",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity>of(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity::new,MobCategory.MONSTER)
            .sized(0.8F,1.0F).eyeHeight(0.8F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_autocaster"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity>> ARCANE_BORE=TYPES.register("arcane_bore",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity>of(art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity::new,MobCategory.MISC)
            .sized(0.9F,0.9F).eyeHeight(0.8125F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"arcane_bore"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.TaintCrawlerEntity>> TAINT_CRAWLER=TYPES.register("taint_crawler",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.TaintCrawlerEntity>of(art.arcane.thaumcraft.entity.monster.TaintCrawlerEntity::new,MobCategory.MONSTER)
            .sized(0.5F,0.4F).eyeHeight(0.1F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taint_crawler"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.TaintacleEntity>> TAINTACLE=TYPES.register("taintacle",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.TaintacleEntity>of(art.arcane.thaumcraft.entity.monster.TaintacleEntity::new,MobCategory.MONSTER)
            .sized(0.8F,3.0F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taintacle"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.TaintacleSmallEntity>> TAINTACLE_SMALL=TYPES.register("taintacle_small",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.TaintacleSmallEntity>of(art.arcane.thaumcraft.entity.monster.TaintacleSmallEntity::new,MobCategory.MONSTER)
            .sized(0.22F,1.0F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taintacle_small"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.TaintSwarmEntity>> TAINT_SWARM=TYPES.register("taint_swarm",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.TaintSwarmEntity>of(art.arcane.thaumcraft.entity.monster.TaintSwarmEntity::new,MobCategory.MONSTER)
            .sized(2.0F,2.0F).clientTrackingRange(8).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taint_swarm"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.BrainyZombieEntity>> BRAINY_ZOMBIE=TYPES.register("brainy_zombie",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.BrainyZombieEntity>of(art.arcane.thaumcraft.entity.monster.BrainyZombieEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"brainy_zombie"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity>> GIANT_BRAINY_ZOMBIE=TYPES.register("giant_brainy_zombie",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity>of(art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"giant_brainy_zombie"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity>> INHABITED_ZOMBIE=TYPES.register("inhabited_zombie",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity>of(art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.95F).eyeHeight(1.74F).passengerAttachments(2.0125F).ridingOffset(-0.7F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"inhabited_zombie"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.EldritchCrabEntity>> ELDRITCH_CRAB=TYPES.register("eldritch_crab",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.EldritchCrabEntity>of(art.arcane.thaumcraft.entity.monster.EldritchCrabEntity::new,MobCategory.MONSTER)
            .sized(0.8F,0.6F).clientTrackingRange(8).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_crab"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.PechEntity>> PECH=TYPES.register("pech",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.PechEntity>of(art.arcane.thaumcraft.entity.monster.PechEntity::new,MobCategory.MONSTER)
            .sized(0.6F,1.8F).eyeHeight(1.188F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"pech"))));
    /** TC6 entities.monster.boss. */
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity>> ELDRITCH_WARDEN=TYPES.register("eldritch_warden",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity>of(art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity::new,MobCategory.MONSTER)
            .sized(1.5F,3.5F).eyeHeight(3.1F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_warden"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity>> ELDRITCH_GOLEM=TYPES.register("eldritch_golem",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity>of(art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity::new,MobCategory.MONSTER)
            .sized(1.75F,3.5F).eyeHeight(3.0F).fireImmune().clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"eldritch_golem"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity>> CULTIST_LEADER=TYPES.register("cultist_leader",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity>of(art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity::new,MobCategory.MONSTER)
            .sized(0.75F,2.25F).clientTrackingRange(4).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"cultist_leader"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity>> CULTIST_PORTAL_GREATER=TYPES.register("cultist_portal_greater",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity>of(art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity::new,MobCategory.MONSTER)
            .sized(1.5F,3.0F).fireImmune().clientTrackingRange(4).updateInterval(20).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"cultist_portal_greater"))));
    public static final DeferredHolder<EntityType<?>,EntityType<art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity>> TAINTACLE_GIANT=TYPES.register("taintacle_giant",()->
        EntityType.Builder.<art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity>of(art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity::new,MobCategory.MONSTER)
            .sized(1.1F,6.0F).clientTrackingRange(6).updateInterval(3).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID,"taintacle_giant"))));
    public static void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) {
        e.put(TURRET_BASIC.get(), art.arcane.thaumcraft.entity.construct.TurretCrossbowEntity.createAttributes().build());
        e.put(TURRET_ADVANCED.get(), art.arcane.thaumcraft.entity.construct.TurretCrossbowAdvancedEntity.createAttributes().build());
        e.put(ARCANE_BORE.get(), art.arcane.thaumcraft.entity.construct.ArcaneBoreEntity.createAttributes().build());
        e.put(ELDRITCH_AUTOCASTER.get(), art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.createAttributes().build());
        e.put(GOLEM.get(), art.arcane.thaumcraft.golems.GolemEntity.createAttributes().build());
        e.put(TRAVELLING_TRUNK.get(), art.arcane.thaumcraft.golems.trunk.TravellingTrunkEntity.createAttributes().build());
        e.put(THAUMIC_SLIME.get(), net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build());
        e.put(SPELL_BAT.get(), art.arcane.thaumcraft.entity.SpellBatEntity.createAttributes().build());
        e.put(WISP.get(), art.arcane.thaumcraft.entity.WispEntity.createAttributes().build());
        e.put(TAINT_SEED.get(), art.arcane.thaumcraft.entity.TaintSeedEntity.createAttributes().build());
        e.put(TAINT_SEED_PRIME.get(), art.arcane.thaumcraft.entity.TaintSeedPrimeEntity.createAttributes().build());
        e.put(FIRE_BAT.get(), art.arcane.thaumcraft.entity.FireBatEntity.createAttributes().build());
        e.put(MIND_SPIDER.get(), art.arcane.thaumcraft.entity.monster.MindSpiderEntity.createAttributes().build());
        e.put(ELDRITCH_GUARDIAN.get(), art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity.createAttributes().build());
        e.put(CULTIST_KNIGHT.get(), art.arcane.thaumcraft.entity.monster.CultistKnightEntity.createAttributes().build());
        e.put(CULTIST_CLERIC.get(), art.arcane.thaumcraft.entity.monster.CultistClericEntity.createAttributes().build());
        e.put(CULTIST_PORTAL_LESSER.get(), art.arcane.thaumcraft.entity.monster.CultistPortalLesserEntity.createAttributes().build());
        e.put(TAINT_CRAWLER.get(), art.arcane.thaumcraft.entity.monster.TaintCrawlerEntity.createAttributes().build());
        e.put(TAINTACLE.get(), art.arcane.thaumcraft.entity.monster.TaintacleEntity.createAttributes().build());
        e.put(TAINTACLE_SMALL.get(), art.arcane.thaumcraft.entity.monster.TaintacleSmallEntity.createAttributes().build());
        e.put(TAINT_SWARM.get(), art.arcane.thaumcraft.entity.monster.TaintSwarmEntity.createAttributes().build());
        e.put(BRAINY_ZOMBIE.get(), art.arcane.thaumcraft.entity.monster.BrainyZombieEntity.createAttributes().build());
        e.put(GIANT_BRAINY_ZOMBIE.get(), art.arcane.thaumcraft.entity.monster.GiantBrainyZombieEntity.createAttributes().build());
        e.put(INHABITED_ZOMBIE.get(), art.arcane.thaumcraft.entity.monster.InhabitedZombieEntity.createAttributes().build());
        e.put(ELDRITCH_CRAB.get(), art.arcane.thaumcraft.entity.monster.EldritchCrabEntity.createAttributes().build());
        e.put(PECH.get(), art.arcane.thaumcraft.entity.monster.PechEntity.createAttributes().build());
        e.put(ELDRITCH_WARDEN.get(), art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity.createAttributes().build());
        e.put(ELDRITCH_GOLEM.get(), art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity.createAttributes().build());
        e.put(CULTIST_LEADER.get(), art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity.createAttributes().build());
        e.put(CULTIST_PORTAL_GREATER.get(), art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity.createAttributes().build());
        e.put(TAINTACLE_GIANT.get(), art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity.createAttributes().build());
    }
    public static void register(IEventBus bus) { TYPES.register(bus); art.arcane.thaumcraft.worldgen.HalloweenSpawnsBiomeModifier.register(bus); art.arcane.thaumcraft.worldgen.MonsterBiomeSpawnsBiomeModifier.register(bus); bus.addListener(ModEntities::registerAttributes); bus.addListener(ModEntities::registerSpawnPlacements); bus.addListener(ModEntities::addSpawnEggs); }
    public static void registerSpawnPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e) {
        e.register(FIRE_BAT.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            art.arcane.thaumcraft.entity.FireBatEntity::checkFireBatSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(WISP.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            art.arcane.thaumcraft.entity.WispEntity::checkWispSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(TAINTACLE.get(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            art.arcane.thaumcraft.entity.monster.TaintacleEntity::checkTaintacleSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        e.register(TAINT_SWARM.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            art.arcane.thaumcraft.entity.monster.TaintSwarmEntity::checkSwarmSpawnRules, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        for (var t : java.util.List.of(BRAINY_ZOMBIE.get(), GIANT_BRAINY_ZOMBIE.get(), INHABITED_ZOMBIE.get(), ELDRITCH_CRAB.get(), ELDRITCH_GUARDIAN.get(), PECH.get(), TAINT_CRAWLER.get()))
            e.register((EntityType) t, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (net.minecraft.world.entity.SpawnPlacements.SpawnPredicate) (net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<net.minecraft.world.entity.monster.Monster>) net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules,
                net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
    /** TC6 registered its mobs with spawn eggs; those live in the vanilla spawn egg tab. */
    public static void addSpawnEggs(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS) {
            e.accept(art.arcane.thaumcraft.ModItems.FIREBAT_SPAWN_EGG.get());
            for (var egg : art.arcane.thaumcraft.ModItems.MOB_EGGS) e.accept(egg.get());
        }
    }
    private ModEntities() {}
}
