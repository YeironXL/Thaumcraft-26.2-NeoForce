package art.arcane.thaumcraft.entity.ai;

import art.arcane.thaumcraft.entity.monster.CultistClericEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;

/**
 * TC6 AIAltarFocus: while a cleric is a ritualist it holds every control flag (mutex 7 = move|look|jump) so it stays
 * at its altar; every 40 ticks it drops the ritual if it strayed more than 4 blocks from its home or the home block is
 * no longer the altar. TC6 compares against BlocksTC.eldritch, a TC4 leftover field that is never assigned in 6.1
 * (it stays null), so any real block fails that check exactly as in the original; {@link #ALTAR} mirrors that field.
 */
public class AltarFocusGoal extends Goal {
    /** BlocksTC.eldritch: never registered in TC6.1.BETA26, therefore null. */
    public static Block ALTAR = null;
    private final CultistClericEntity entity;

    public AltarFocusGoal(CultistClericEntity entity) {
        this.entity = entity;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** 1.12 getHomePosition() is never null (defaults to the origin), so only the ritualist flag matters. */
    private boolean active() { return entity.getIsRitualist(); }

    @Override public boolean canUse() { return active(); }
    @Override public boolean canContinueToUse() { return active(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        BlockPos h = entity.getHomePosition();
        if (entity.tickCount % 40 == 0
                && (h.distToCenterSqr(entity.getX() + 0.5, entity.getY() + 0.5, entity.getZ() + 0.5) > 16.0
                    || entity.level().getBlockState(h).getBlock() != ALTAR)) {
            entity.setIsRitualist(false);
        }
    }
}
