package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.entity.ai.AltarFocusGoal;
import art.arcane.thaumcraft.entity.ai.RestrictOpenDoorGoal;
import art.arcane.thaumcraft.entity.monster.CultistClericEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;

/** TC6 EntityCultistCleric ritual: AIAltarFocus (priority 1), EntityAIRestrictOpenDoor (4), ritual exits and sync. */
public final class CultistRitualVerification {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static boolean has(Mob m, int prio, Class<? extends Goal> c) {
        return m.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getPriority() == prio && c.isInstance(w.getGoal()));
    }

    public static void test(GameTestHelper h) {
        var world = h.getLevel();
        for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        CultistClericEntity keeper = h.spawn(ModEntities.CULTIST_CLERIC.get(), new BlockPos(2, 2, 2));
        CultistClericEntity stray = h.spawn(ModEntities.CULTIST_CLERIC.get(), new BlockPos(6, 2, 6));
        CultistClericEntity raged = h.spawn(ModEntities.CULTIST_CLERIC.get(), new BlockPos(6, 2, 2));
        var knight = h.spawn(ModEntities.CULTIST_KNIGHT.get(), new BlockPos(2, 2, 6));
        check(has(keeper, 1, AltarFocusGoal.class) && has(keeper, 4, RestrictOpenDoorGoal.class), "Cleric lacks AIAltarFocus(1)/RestrictOpenDoor(4)");
        check(has(knight, 4, RestrictOpenDoorGoal.class), "Knight lacks RestrictOpenDoor(4)");
        check(!keeper.getIsRitualist() && keeper.removeWhenFarAway(1000), "Clerics start as non-ritualists and may despawn");
        // altar focus: home 1 block below-ahead; a stand-in altar block for the never-assigned BlocksTC.eldritch
        BlockPos altar = h.absolutePos(new BlockPos(3, 2, 2));
        world.setBlockAndUpdate(altar, Blocks.OBSIDIAN.defaultBlockState());
        AltarFocusGoal.ALTAR = Blocks.OBSIDIAN;
        keeper.setHomeTo(altar, 8);
        keeper.setIsRitualist(true);
        check(!keeper.removeWhenFarAway(1000), "Ritualist clerics must not despawn");
        // stray: its home is not the altar block -> dropped at the next 40-tick check
        stray.setHomeTo(h.absolutePos(new BlockPos(6, 2, 7)), 8);
        stray.setIsRitualist(true);
        raged.setHomeTo(altar, 8);
        raged.setIsRitualist(true);
        raged.rage = 5;
        var keeperStart = keeper.position();
        h.runAfterDelay(45, () -> {
            check(keeper.getIsRitualist(), "Cleric left a valid altar ritual");
            check(keeper.position().distanceToSqr(keeperStart) < 0.25, "Ritualist moved while focusing the altar");
            check(keeper.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.isRunning() && w.getGoal() instanceof AltarFocusGoal), "AIAltarFocus is not running");
            check(!stray.getIsRitualist(), "Ritual not dropped when the home block is not the altar");
            check(!raged.getIsRitualist(), "Ritual not dropped at rage >= 5");
            // TC6 behaviour: BlocksTC.eldritch is null, so without the stand-in every ritual ends
            AltarFocusGoal.ALTAR = null;
            h.runAfterDelay(41, () -> {
                check(!keeper.getIsRitualist(), "Ritual must end when the home block is not BlocksTC.eldritch (null in TC6)");
                keeper.setIsRitualist(true);
                check(keeper.hurtServer(world, world.damageSources().generic(), 1) && !keeper.getIsRitualist(), "Damage did not break the ritual");
                System.out.println("Cultist ritual passed: goals, altar focus hold, stray/rage/damage exits, despawn guard.");
                h.succeed();
            });
        });
    }
}
