package art.arcane.thaumcraft.fx;

import art.arcane.thaumcraft.network.FxPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/** Server-side senders for TC6 effects; the client runs the matching FXDispatcher routine. */
public final class Fx {
    public static void send(ServerLevel level, String kind, double x, double y, double z, float... args) {
        var list = new ArrayList<Float>(args.length);
        for (float a : args) list.add(a);
        PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(BlockPos.containing(x, y, z)), new FxPayload(kind, x, y, z, List.copyOf(list)));
    }
    /** FXDispatcher.drawBamf(x,y,z,r,g,b,sound,flair,side) — PacketFXBlockBamf. side=-1 for none. */
    public static void bamf(ServerLevel level, double x, double y, double z, int color, boolean sound, boolean flair, Direction side) {
        send(level, "bamf", x, y, z, ((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, sound ? 1 : 0, flair ? 1 : 0, side == null ? -1 : side.get3DDataValue());
    }
    /** Default purple bamf (0.5, 0.1, 0.6). */
    public static void bamf(ServerLevel level, double x, double y, double z, boolean sound, boolean flair, Direction side) {
        send(level, "bamf", x, y, z, 0.5f, 0.1f, 0.6f, sound ? 1 : 0, flair ? 1 : 0, side == null ? -1 : side.get3DDataValue());
    }
    /** ItemMagicDust.doSparkles: dust from the player's hand plus sparkles on the affected blocks. */
    public static void dust(ServerLevel level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, BlockPos pos, net.minecraft.world.phys.Vec3 hit, java.util.List<BlockPos> sparkles) {
        var args = new float[5 + sparkles.size() * 3];
        args[0] = player.getId(); args[1] = hand == net.minecraft.world.InteractionHand.OFF_HAND ? 1 : 0;
        args[2] = (float) hit.x; args[3] = (float) hit.y; args[4] = (float) hit.z;
        for (int i = 0; i < sparkles.size(); i++) { var p = sparkles.get(i); args[5 + i * 3] = p.getX() + 0.5f; args[6 + i * 3] = p.getY() + 0.5f; args[7 + i * 3] = p.getZ() + 0.5f; }
        send(level, "dust", pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, args);
    }
    private Fx() {}
}
