package art.arcane.thaumcraft.world.outer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.Set;

/**
 * TC4 WorldProviderOuter / "The Outer Lands": a void dimension (datapack dimension thaumcraft:outer_lands)
 * holding one maze per opened Eldritch Portal.
 */
public final class OuterLands {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("thaumcraft", "outer_lands"));

    /**
     * The game test server bakes its levels without datapack dimensions; verification sets this so the maze can be
     * hosted in the test level instead. Always null in normal play.
     */
    public static volatile java.util.function.Function<MinecraftServer, ServerLevel> testHost;

    public static boolean is(Level level) {
        if (level == null) return false;
        if (level.dimension() == DIMENSION) return true;
        var host = testHost;
        return host != null && level instanceof ServerLevel sl && host.apply(sl.getServer()) == level;
    }

    public static ServerLevel level(MinecraftServer server) {
        ServerLevel l = server.getLevel(DIMENSION);
        var host = testHost;
        return l == null && host != null ? host.apply(server) : l;
    }

    /** Send an entity through an Eldritch Portal into the maze it belongs to, generating the maze on first use. */
    public static boolean enter(Entity e, int mazeId) {
        if (!(e.level() instanceof ServerLevel sl)) return false;
        ServerLevel outer = level(sl.getServer());
        if (outer == null) return false;
        MazeData data = MazeData.get(sl.getServer());
        MazeData.Maze m = data.byId(mazeId);
        if (m == null) return false;
        ensureGenerated(outer, mazeId);
        BlockPos p = MazeGenerator.arrival(m);
        e.setPortalCooldown(60);
        return e.teleportTo(outer, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), 180.0f, 0.0f, true);
    }

    /**
     * Build a maze section the first time anyone travels into it. The whole section is ticketed first (portal tickets,
     * entity-ticking, 15 s): otherwise every setBlock loads its chunk with a one-tick ticket, the chunk unloads again
     * mid-generation and the mobs placed in it (taintacles, seeds, crabs, autocasters) are written to disk straight
     * away and only come back asynchronously, after a reload, when someone walks in. With the ticket the maze is built
     * on loaded chunks, its mobs join a ticking world, and it is saved and unloaded normally once the ticket expires.
     */
    public static void ensureGenerated(ServerLevel outer, int mazeId) {
        MazeData data = MazeData.get(outer.getServer());
        MazeData.Maze m = data.byId(mazeId);
        if (m != null && !m.generated()) {
            ticket(outer, m);
            MazeGenerator.generate(outer, m);
            data.markGenerated(mazeId);
        }
    }

    /** Portal tickets over every chunk of a maze section (radius 2 = entity ticking); they expire on their own. */
    static void ticket(ServerLevel outer, MazeData.Maze m) {
        BlockPos o = MazeGenerator.origin(m);
        int n = MazeGenerator.SIZE * MazeGenerator.CELL / 16;
        for (int x = 0; x < n; x++) for (int z = 0; z < n; z++)
            outer.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL, new net.minecraft.world.level.ChunkPos((o.getX() >> 4) + x, (o.getZ() >> 4) + z), 2);
    }

    /** The exit portal: back beside the Eldritch Altar that opened this maze. */
    public static boolean leave(Entity e, int mazeId) {
        if (!(e.level() instanceof ServerLevel sl)) return false;
        MazeData.Maze m = MazeData.get(sl.getServer()).byId(mazeId);
        ServerLevel back = m == null ? null : sl.getServer().getLevel(m.altar().dimension());
        if (back == null) back = sl.getServer().overworld();
        BlockPos a = m == null ? back.getRespawnData().pos() : m.altar().pos();
        BlockPos p = a.offset(0, 0, 3);
        e.setPortalCooldown(60);
        return e.teleportTo(back, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, Set.of(), 0.0f, 0.0f, true);
    }

    private OuterLands() {}
}
