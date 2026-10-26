package art.arcane.thaumcraft.util;

import net.minecraft.server.level.ServerPlayer;

/** TC6 EntityUtils.resetFloatCounter: keeps a magically lifted player from being kicked for flying. */
public final class Flight {
    public static void resetFloatCounter(ServerPlayer player) {
        if (player.connection != null) player.connection.aboveGroundTickCount = 0;
    }
    private Flight() {}
}
