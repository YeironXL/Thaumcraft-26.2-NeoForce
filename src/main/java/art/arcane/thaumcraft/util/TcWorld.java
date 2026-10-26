package art.arcane.thaumcraft.util;

import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Small world helpers replacing 1.12 accessors. */
public final class TcWorld {
    /** World.getMoonPhase() (0 = full moon). */
    public static int moonPhase(Level level, Vec3 pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index();
    }
    private TcWorld() {}
}
