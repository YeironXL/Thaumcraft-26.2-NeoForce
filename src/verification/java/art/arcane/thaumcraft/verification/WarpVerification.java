package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.events.WarpEvents;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchRules;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Warp gain/decay, warp event selection with seeded randomness, warp potions and mind spider targeting. */
public final class WarpVerification {
    private static void check(boolean ok, String m) { if (!ok) throw new AssertionError(m); }

    private static float exhaustion(ServerPlayer p) { try { var f = net.minecraft.world.food.FoodData.class.getDeclaredField("exhaustionLevel"); f.setAccessible(true); return f.getFloat(p.getFoodData()); } catch (ReflectiveOperationException e) { throw new AssertionError(e); } }

    public static void test(GameTestHelper h) {
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        PlayerResearch r = PlayerProgressManager.get(player);
        // gain: counter reset to total, clamp to 500, TC6 over-reduction quirk
        PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.PERMANENT, 20);
        PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.NORMAL, 10);
        PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.TEMPORARY, 5);
        check(r.warpCounter() == 35, "counter not reset to total warp: " + r.warpCounter());
        PlayerProgressManager.addWarp(player, PlayerProgressManager.WarpType.PERMANENT, 1000);
        check(r.permanentWarp() == 500, "permanent warp not clamped to 500");
        r.setPermanentWarp(20);
        // event chain
        check(WarpEvents.eventFor(0).equals("none") && WarpEvents.eventFor(4).equals("creeper_hiss") && WarpEvents.eventFor(20).equals("thaumarhia")
                && WarpEvents.eventFor(74).equals("unnatural_hunger_long") && WarpEvents.eventFor(76).equals("clarity")
                && WarpEvents.eventFor(85).equals("portal") && WarpEvents.eventFor(99).equals("mist_many_guardians"), "warp event chain mismatch");
        // decay + seeded check: two runs with same seed agree
        int temp = r.temporaryWarp();
        r.setWarpCounter(10000);
        long seed = 0; for (long s = 0; s < 1000; s++) { var rr = RandomSource.create(s); rr.nextInt(100); String ev = WarpEvents.eventFor(rr.nextInt(100)); if (ev.startsWith("text_") || ev.endsWith("_sound") || ev.equals("creeper_hiss")) { seed = s; break; } }
        var res1 = WarpEvents.checkWarpEvent(player, RandomSource.create(seed));
        check(r.temporaryWarp() == temp - 1, "temporary warp did not decay by one");
        check(res1.eff() >= 0 && res1.event().equals(WarpEvents.eventFor(res1.eff())), "warp event did not fire with a full counter");
        check(r.warpCounter() < 10000, "counter not drained");
        // research unlocks at actual warp > 10 / 25
        check(ResearchRules.knows(r, "!BATHSALTS") && ResearchRules.knows(r, "ELDRITCHMINOR") && !ResearchRules.knows(r, "ELDRITCHMAJOR"), "warp research unlocks wrong");
        r.setWarpCounter(0);
        check(WarpEvents.checkWarpEvent(player, RandomSource.create(1)).eff() == -1, "warp event fired with empty counter");
        // potions
        float ex = exhaustion(player);
        ModEffects.UNNATURAL_HUNGER.value().applyEffectTick(h.getLevel(), player, 1);
        check(exhaustion(player) > ex, "unnatural hunger added no exhaustion");
        h.runAfterDelay(5, () -> {
            // mind spiders: hallucinations are harmless, bound to the viewer and target the player
            for (int x = 0; x < 48; x++) for (int z = 0; z < 48; z++) { h.setBlock(new BlockPos(x, 1, z), Blocks.STONE); h.setBlock(new BlockPos(x, 2, z), Blocks.AIR); h.setBlock(new BlockPos(x, 3, z), Blocks.AIR); }
            player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(24.5, 2, 24.5)));
            var spiders = WarpEvents.suddenlySpiders(player, 3, false, RandomSource.create(7));
            for (var s : spiders) check(s.isHarmless() && s.getViewer().equals(player.getName().getString()) && s.getTarget() == player, "fake mind spider wrong");
            var real = WarpEvents.suddenlySpiders(player, 2, true, RandomSource.create(9));
            for (var s : real) check(!s.isHarmless() && s.getViewer().isEmpty() && s.getTarget() == player, "real mind spider wrong");
            spiders.forEach(e -> e.discard()); real.forEach(e -> e.discard());
            h.succeed();
        });
    }
}
