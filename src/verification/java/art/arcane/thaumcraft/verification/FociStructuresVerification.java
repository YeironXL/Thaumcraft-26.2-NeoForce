package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.foci.FocusEffectPrimal;
import art.arcane.thaumcraft.casters.foci.FocusEffectWard;
import art.arcane.thaumcraft.casters.ward.WardManager;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.worldgen.SinisterShrinesGen;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.Vec3;

final class FociStructuresVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    static void ward(GameTestHelper h) {
        var level = h.getLevel();
        check(FocusEngine.getElement("thaumcraft.WARD") != null && FocusEngine.getElement("thaumcraft.PRIMAL") != null, "Ward/Primal focus effects not registered");
        var owner = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var other = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var rel = new BlockPos(3, 1, 3);
        h.setBlock(rel, Blocks.STONE);
        var pos = h.absolutePos(rel);
        check(FocusEffectWard.toggle(level, pos, owner.getUUID()), "Warding a stone block failed");
        check(owner.getUUID().equals(WardManager.owner(level, pos)), "Ward owner not stored in chunk");
        check(!FocusEffectWard.toggle(level, pos, other.getUUID()) && WardManager.isWarded(level, pos), "Another player removed the ward");
        // other player can't break
        other.gameMode.destroyBlock(pos);
        check(level.getBlockState(pos).is(Blocks.STONE), "Other player broke a warded block");
        // explosion can't destroy it
        level.explode(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 3.0f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        check(level.getBlockState(pos).is(Blocks.STONE), "Explosion destroyed a warded block");
        // piston can't push it
        var pistonRel = rel.west();
        h.setBlock(pistonRel, Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, net.minecraft.core.Direction.EAST));
        h.setBlock(pistonRel.west(), Blocks.REDSTONE_BLOCK);
        h.runAfterDelay(6, () -> {
            check(level.getBlockState(pos).is(Blocks.STONE) && WardManager.isWarded(level, pos), "Piston moved a warded block");
            h.setBlock(pistonRel.west(), Blocks.AIR); h.setBlock(pistonRel, Blocks.AIR);
            // owner unwards by casting again, rewards, then breaks it
            check(FocusEffectWard.toggle(level, pos, owner.getUUID()) && !WardManager.isWarded(level, pos), "Owner could not unward by casting again");
            check(FocusEffectWard.toggle(level, pos, owner.getUUID()), "Re-ward failed");
            owner.gameMode.destroyBlock(pos);
            check(level.getBlockState(pos).isAir() && !WardManager.isWarded(level, pos), "Owner could not break own warded block");
            System.out.println("Ward passed: owner store, other break/unward denied, explosion and piston proof, owner unward and break.");
            h.succeed();
        });
    }

    static void primal(GameTestHelper h) {
        var level = h.getLevel();
        var caster = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var r = RandomSource.create(7);
        String[] keys = FocusEffectPrimal.PRIMALS;
        for (int i = 0; i < keys.length; i++) {
            var rel = new BlockPos(2 + i * 2, 1, 6);
            var mob = h.spawn(EntityTypes.HUSK, rel);
            mob.setNoAi(true);
            var hv = Vec3.atBottomCenterOf(h.absolutePos(rel));
            mob.invulnerableTime = 0; // the previous blast (radius 2.5) may have just hit this husk
            float hp = mob.getHealth();
            FocusEffectPrimal.blast(level, hv, h.absolutePos(rel), caster, 2.0f, i, false, false, r);
            check(mob.getHealth() < hp || mob.isRemoved(), "Primal blast " + keys[i] + " did no damage");
            switch (keys[i]) {
                case "terra" -> check(mob.hasEffect(MobEffects.MINING_FATIGUE), "Terra blast did not apply fatigue");
                case "aqua" -> check(mob.hasEffect(MobEffects.SLOWNESS), "Aqua blast did not slow");
                case "ordo" -> check(mob.hasEffect(MobEffects.WEAKNESS), "Ordo blast did not weaken");
                case "ignis" -> check(mob.getRemainingFireTicks() > 0, "Ignis blast did not ignite");
                default -> {}
            }
        }
        var tRel = new BlockPos(4, 1, 12);
        h.setBlock(tRel.below(), Blocks.STONE); h.setBlock(tRel, Blocks.AIR); h.setBlock(tRel.above(), Blocks.AIR);
        var t = h.absolutePos(tRel);
        FocusEffectPrimal.blast(level, Vec3.atBottomCenterOf(t), t, caster, 0f, 4, true, true, r);
        check(level.getBlockState(t).is(ModBlocks.TAINT_FIBRE.get()), "Forced primal blast did not seed taint fibre");
        check(level.getBlockEntity(t.above()) instanceof AuraNodeBlockEntity n && n.type == NodeType.UNSTABLE, "Forced primal blast did not create an unstable node");
        System.out.println("Primal passed: six primal blasts, taint fibre and unstable node.");
        h.succeed();
    }

    static void structures(GameTestHelper h) {
        var level = h.getLevel();
        var rnd = new java.util.Random(3);
        var g = h.absolutePos(new BlockPos(5, 3, 5));
        var spawner = SinisterShrinesGen.wispAltar(level, rnd, g);
        check(level.getBlockEntity(spawner) instanceof SpawnerBlockEntity, "Wisp altar has no spawner");
        var sp = (SpawnerBlockEntity) level.getBlockEntity(spawner);
        var shown = sp.getSpawner().getOrCreateDisplayEntity(level, spawner);
        check(shown instanceof art.arcane.thaumcraft.entity.WispEntity, "Wisp altar spawner does not spawn wisps: " + shown);
        check(level.getBlockEntity(spawner.above(3)) instanceof AuraNodeBlockEntity n && n.type == NodeType.DARK, "Wisp altar has no sinister node");
        var g2 = h.absolutePos(new BlockPos(14, 3, 14));
        var node = SinisterShrinesGen.obelisk(level, rnd, g2);
        check(level.getBlockState(g2.above()).is(Blocks.OBSIDIAN), "Obelisk base is not obsidian");
        check(level.getBlockEntity(node) instanceof AuraNodeBlockEntity n2 && n2.type == NodeType.DARK, "Obelisk has no sinister node");
        check(ModEntities.WISP.get() != null, "Wisp entity missing");
        System.out.println("Structures passed: wisp altar spawner+sinister node, sinister obelisk+node.");
        h.succeed();
    }

    static void shock(GameTestHelper h) {
        var level = h.getLevel();
        check(FocusEngine.getElement("thaumcraft.SHOCK") != null, "Shock focus effect not registered");
        var caster = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        var mobs = new java.util.ArrayList<net.minecraft.world.entity.Mob>();
        for (int i = 0; i < 4; i++) { var m = h.spawn(EntityTypes.PIG, new BlockPos(3 + i * 3, 1, 5)); m.setNoAi(true); mobs.add(m); }
        var far = h.spawn(EntityTypes.CREEPER, new BlockPos(3, 1, 18)); far.setNoAi(true);
        float[] hp = new float[4];
        for (int i = 0; i < 4; i++) hp[i] = mobs.get(i).getHealth();
        var hit = art.arcane.thaumcraft.casters.foci.FocusEffectShock.shock(level, mobs.get(0), caster, 4.0f, 2);
        check(hit.size() == 3, "Shock chain 2 should hit 3 entities, hit " + hit.size());
        for (int i = 0; i < 3; i++) check(mobs.get(i).getHealth() < hp[i] || mobs.get(i).isRemoved(), "Chained pig " + i + " not damaged");
        check(mobs.get(3).getHealth() == hp[3], "Shock chained further than its setting");
        check(!hit.contains(caster), "Shock hit its caster");
        var p0 = mobs.get(0);
        check(!p0.isRemoved() || level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.ZombifiedPiglin.class, p0.getBoundingBox().inflate(3)).isEmpty(), "Shock transformed a pig");
        var lone = art.arcane.thaumcraft.casters.foci.FocusEffectShock.shock(level, far, caster, 2.0f, 3);
        check(lone.size() == 1 && !((net.minecraft.world.entity.monster.Creeper) far).isPowered(), "Lone shock chained or charged the creeper");
        System.out.println("Shock passed: chain of 3, setting respected, no transformation.");
        h.succeed();
    }

    static void barrow(GameTestHelper h) {
        var level = h.getLevel();
        var g = h.absolutePos(new BlockPos(10, 9, 10));
        var node = art.arcane.thaumcraft.worldgen.BarrowDenGen.build(level, new java.util.Random(11), g);
        check(level.getBlockEntity(node) instanceof AuraNodeBlockEntity n && n.type == NodeType.DARK, "Barrow has no sinister node");
        int vis = ((AuraNodeBlockEntity) level.getBlockEntity(node)).base.values().stream().mapToInt(Integer::intValue).sum();
        check(vis >= 40, "Barrow node too weak: " + vis);
        int chests = 0, zombie = 0, skeleton = 0;
        for (BlockPos q : BlockPos.betweenClosed(g.offset(-6, -7, -6), g.offset(6, 4, 6))) {
            var be = level.getBlockEntity(q);
            if (be instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) { chests++; check(q.getY() == g.getY() - 5, "chest not at the pit bottom"); }
            if (be instanceof SpawnerBlockEntity sp) {
                check(q.getX() < g.getX(), "spawner not on the west side");
                var e = sp.getSpawner().getOrCreateDisplayEntity(level, q);
                if (e != null && e.getType() == EntityTypes.ZOMBIE) zombie++;
                if (e != null && e.getType() == EntityTypes.SKELETON) skeleton++;
            }
        }
        check(chests == 3 && zombie == 1 && skeleton == 1, "Barrow contents: chests " + chests + " zombie " + zombie + " skeleton " + skeleton);
        check(level.getBlockState(g.offset(-8, 1, 0)).isAir() && level.getBlockState(g.offset(-8, 2, 0)).isAir(), "West entrance not open");
        check(level.getBlockState(g.offset(0, 4, 0)).isSolid(), "Barrow has no roof");
        check(art.arcane.thaumcraft.worldgen.BarrowDenGen.biome(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.SAVANNA))
                && !art.arcane.thaumcraft.worldgen.BarrowDenGen.biome(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.DARK_FOREST)), "Barrow biome filter");
        System.out.println("Barrow passed: dark node vis " + vis + ", 3 pit chests, zombie+skeleton spawners west, entrance and roof.");
        h.succeed();
    }
}
