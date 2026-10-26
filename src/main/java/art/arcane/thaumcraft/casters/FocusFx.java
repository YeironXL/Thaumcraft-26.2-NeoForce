package art.arcane.thaumcraft.casters;

import art.arcane.thaumcraft.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server senders for PacketFXFocusPartImpact / PacketFXFocusEffect / PacketFXZap. */
public final class FocusFx {
    public static void impact(Level level, double x, double y, double z, String... keys) {
        if (level instanceof ServerLevel sl) Fx.send(sl, "focus_impact|" + String.join("%", keys), x, y, z);
    }
    public static void effect(Level level, Vec3 pos, Vec3 motion, String... keys) {
        if (level instanceof ServerLevel sl) Fx.send(sl, "focus_effect|" + String.join("%", keys), pos.x, pos.y, pos.z, (float) motion.x, (float) motion.y, (float) motion.z);
    }
    public static void zap(Level level, Vec3 source, Vec3 target, int color, float width) {
        if (level instanceof ServerLevel sl) Fx.send(sl, "zap", source.x, source.y, source.z, (float) target.x, (float) target.y, (float) target.z, color, width);
    }
    public static String[] keys(FocusEffect[] fe) {
        String[] k = new String[fe.length];
        for (int i = 0; i < fe.length; i++) k[i] = fe[i].getKey();
        return k;
    }
    private FocusFx() {}
}
