package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.ai.LongRangeAttackGoal;
import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.entity.monster.boss.CultistLeaderEntity;
import art.arcane.thaumcraft.entity.monster.boss.CultistPortalGreaterEntity;
import art.arcane.thaumcraft.entity.monster.boss.EldritchGolemEntity;
import art.arcane.thaumcraft.entity.monster.boss.EldritchWardenEntity;
import art.arcane.thaumcraft.entity.monster.boss.TaintacleGiantEntity;
import art.arcane.thaumcraft.entity.projectile.EldritchOrbEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** TC6 bosses: Eldritch Warden, Eldritch Construct, Greater Crimson Portal + Crimson Praetor, Giant Taintacle. */
public final class BossesVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x < 20; x++) for (int z = 0; z < 20; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
    }

    /** A boss as it arrives from a spawn egg: created, finalizeSpawn'd and added. */
    private static <E extends Mob> E summon(GameTestHelper h, EntityType<E> type, BlockPos rel) {
        ServerLevel w = h.getLevel();
        E e = type.create(w, EntitySpawnReason.SPAWN_ITEM_USE);
        BlockPos p = h.absolutePos(rel);
        e.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0f, 0.0f);
        e.finalizeSpawn(w, w.getCurrentDifficultyAt(p), EntitySpawnReason.SPAWN_ITEM_USE, null);
        w.addFreshEntity(e);
        return e;
    }

    private static int count(GameTestHelper h, net.minecraft.world.level.block.Block b) {
        return (int) h.getLevel().getBlockStates(new AABB(h.absolutePos(new BlockPos(0, 2, 0))).expandTowards(19, 0, 19)).filter(s -> s.is(b)).count();
    }

    public static void warden(GameTestHelper h) {
        floor(h);
        ServerLevel w = h.getLevel();
        EldritchWardenEntity wd = summon(h, ModEntities.ELDRITCH_WARDEN.get(), new BlockPos(10, 2, 10));
        check(wd.getAttributeBaseValue(Attributes.MAX_HEALTH) == 400.0 && wd.getAttributeValue(Attributes.ARMOR) == 4.0, "warden stats");
        check(Math.abs(wd.getAbsorptionAmount() - 264.0f) < 0.01f, "ward " + wd.getAbsorptionAmount());
        check(wd.getSpawnTimer() == 150 && wd.getHomeRadius() == 24, "spawn timer/home");
        check(wd.bossEvent().getColor() == BossEvent.BossBarColor.PURPLE && wd.bossEvent().getOverlay() == BossEvent.BossBarOverlay.PROGRESS && wd.bossEvent().shouldDarkenScreen(), "purple boss bar");
        check(wd.wardEvent().getColor() == BossEvent.BossBarColor.BLUE && wd.wardEvent().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_10, "blue ward bar");
        check(!wd.hurtServer(w, w.damageSources().generic(), 5.0f), "warden hurt while rising");
        check(EldritchWardenEntity.TITLES.length == 15 && wd.getCustomName() == null, "warden title/name");
        // Outer Lands: the eldritch biome and (recreated from TC4) the thaumcraft:outer_lands dimension
        check(w.registryAccess().lookupOrThrow(Registries.BIOME).get(ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "eldritch"))).isPresent(), "Outer Lands biome missing");
        // (the game test server bakes no datapack dimensions, so check the registered dimension type)
        check(w.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE).get(ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath("thaumcraft", "outer_lands"))).isPresent(), "no Outer Lands dimension type");
        // ranged: orbs (80%) or the screech (wither + weakness)
        var zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(10, 2, 2));
        zombie.setNoAi(true);
        for (int i = 0; i < 12; i++) wd.performRangedAttack(zombie, 1.0f);
        boolean orbs = !w.getEntitiesOfClass(EldritchOrbEntity.class, wd.getBoundingBox().inflate(4)).isEmpty();
        check(orbs, "no eldritch orbs fired");
        h.runAfterDelay(155, () -> {
            check(wd.getSpawnTimer() == 0, "warden never finished rising");
            check(count(h, ModBlocks.EFFECT_SAP.get()) > 0, "warden leaves no sap");
            // break the ward: field frenzy
            wd.setAbsorptionAmount(0.0f);
            check(wd.hurtServer(w, w.damageSources().mobAttack(zombie), 4.0f), "warden not hurt");
            check(wd.fieldFrenzyCounter == 150 && wd.isInvulnerableTo(w, w.damageSources().generic()), "no field frenzy");
            int before = count(h, ModBlocks.EFFECT_SAP.get());
            h.runAfterDelay(45, () -> {
                check(count(h, ModBlocks.EFFECT_SAP.get()) > before + 8, "field frenzy laid no sap rings: " + before + " -> " + count(h, ModBlocks.EFFECT_SAP.get()));
                System.out.println("Warden passed: stats, ward bar, rising, sap, orbs, field frenzy.");
                h.succeed();
            });
        });
    }

    public static void golem(GameTestHelper h) {
        floor(h);
        ServerLevel w = h.getLevel();
        EldritchGolemEntity g = summon(h, ModEntities.ELDRITCH_GOLEM.get(), new BlockPos(10, 2, 10));
        check(g.getAttributeBaseValue(Attributes.MAX_HEALTH) == 400.0 && g.getAttributeValue(Attributes.ARMOR) == 6.0 && g.getType().fireImmune(), "golem stats");
        check(g.getSpawnTimer() == 100 && g.bossEvent().getColor() == BossEvent.BossBarColor.PURPLE, "golem spawn timer/bar");
        check(!g.hurtServer(w, w.damageSources().generic(), 5.0f), "golem hurt while waking");
        var zombie = h.spawn(EntityTypes.PIG, new BlockPos(10, 2, 12));
        zombie.setNoAi(true);
        h.runAfterDelay(102, () -> {
            check(g.getSpawnTimer() == 0, "golem still waking");
            float zh = zombie.getHealth();
            check(g.doHurtTarget(w, zombie) && Math.abs(zh - zombie.getHealth() - 7.5f) < 0.01f, "smash dealt " + (zh - zombie.getHealth()));
            // the killing blow only blows its head off
            check(!g.hurtServer(w, w.damageSources().mobAttack(zombie), 1000.0f), "lethal hit accepted");
            check(g.isAlive() && g.isHeadless() && g.getSpawnTimer() == 100, "golem did not lose its head");
            check(g.goalSelector.getAvailableGoals().stream().anyMatch(x -> x.getPriority() == 2 && x.getGoal() instanceof LongRangeAttackGoal), "headless golem has no beam attack");
            check(Math.abs(g.getEyeHeight() - 3.33f) < 0.01f, "headless eye height " + g.getEyeHeight());
            h.runAfterDelay(20, () -> {
                check(g.chargingBeam && g.beamCharge >= 15, "beam not charging: " + g.beamCharge);
                System.out.println("Eldritch Construct passed: stats, waking, smash, headless phase, beam charge.");
                h.succeed();
            });
        });
    }

    public static void crimsonPortal(GameTestHelper h) {
        floor(h);
        ServerLevel w = h.getLevel();
        CultistPortalGreaterEntity portal = summon(h, ModEntities.CULTIST_PORTAL_GREATER.get(), new BlockPos(10, 2, 10));
        check(portal.getAttributeBaseValue(Attributes.MAX_HEALTH) == 500.0 && portal.bossEvent().getColor() == BossEvent.BossBarColor.RED
                && portal.bossEvent().getOverlay() == BossEvent.BossBarOverlay.NOTCHED_6, "greater portal stats/bar");
        int px = (int) portal.getX(), py = (int) portal.getY(), pz = (int) portal.getZ();
        h.runAfterDelay(45, () -> {
            for (BlockPos b : new BlockPos[]{new BlockPos(px, py, pz + 6), new BlockPos(px + 6, py, pz), new BlockPos(px, py, pz - 6), new BlockPos(px - 6, py, pz)})
                check(w.getBlockState(b).is(ModBlocks.BANNER_CRIMSON_CULT.get()), "no cult banner at " + b);
            // no player within 48: the waves wait
            portal.stage = 12;
            portal.stagecounter = 0;
            h.runAfterDelay(2, () -> {
                check(portal.stage == 12 && portal.stagecounter >= 28, "portal advanced without a player near: stage " + portal.stage);
                portal.spawnBoss(w); // the stage-12 summon
                var leaders = w.getEntitiesOfClass(CultistLeaderEntity.class, portal.getBoundingBox().inflate(4));
                check(leaders.size() == 1, "praetor not summoned: " + leaders.size());
                CultistLeaderEntity l = leaders.get(0);
                check(l.getAttributeBaseValue(Attributes.MAX_HEALTH) == 150.0 && l.getHomeRadius() == 32, "praetor stats/home");
                check(l.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CRIMSON_PRAETOR_HELM.get()) && l.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.CRIMSON_PRAETOR_CHEST.get())
                        && (l.getMainHandItem().is(ModItems.CRIMSON_BLADE.get()) || l.getMainHandItem().is(ModItems.VOID_SWORD.get())), "praetor equipment");
                var knight = h.spawn(ModEntities.CULTIST_KNIGHT.get(), new BlockPos(12, 2, 12));
                knight.setNoAi(true);
                h.runAfterDelay(2, () -> {
                    check(knight.hasEffect(MobEffects.REGENERATION), "praetor did not heal its cultists");
                    System.out.println("Crimson portal passed: bar, banners, player gate, praetor summon/equipment, cultist regeneration.");
                    h.succeed();
                });
            });
        });
    }

    public static void taintacleGiant(GameTestHelper h) {
        floor(h);
        ServerLevel w = h.getLevel();
        TaintacleGiantEntity t = summon(h, ModEntities.TAINTACLE_GIANT.get(), new BlockPos(10, 2, 10));
        check(ChampionModifier.championValue(t) >= 0 && t.isPersistenceRequired(), "giant taintacle is not a persistent champion");
        check(t.getAttributeBaseValue(Attributes.MAX_HEALTH) == 175.0 && t.getMaxHealth() == 275.0f, "giant hp " + t.getMaxHealth());
        check(t.bossEvent().getColor() == BossEvent.BossBarColor.PURPLE && t.getBbHeight() == 6.0f, "giant bar/size");
        var zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setNoAi(true);
        t.setAbsorptionAmount(0.0f);
        float before = t.getHealth();
        t.hurtServer(w, w.damageSources().mobAttack(zombie), 60.0f);
        check(t.getAnger() > 0 && t.hasEffect(MobEffects.REGENERATION), "giant not enraged");
        check(before - t.getHealth() <= 35.0f + 1e-3f && before > t.getHealth(), "damage not capped at 35: " + (before - t.getHealth()));
        System.out.println("Giant taintacle passed: champion, hp, bar, enrage cap.");
        h.succeed();
    }
}
