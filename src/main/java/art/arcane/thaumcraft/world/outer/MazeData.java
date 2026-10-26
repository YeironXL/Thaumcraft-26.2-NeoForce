package art.arcane.thaumcraft.world.outer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * TC4 MazeHandler persistence: every Eldritch Altar that opens a portal owns one maze section of the Outer Lands
 * ("each obelisk opens its own segment"). Stored in the overworld's data storage.
 */
public final class MazeData extends SavedData {
    /** id = index; the maze occupies cells starting at chunk (id * SPACING, 0) of the Outer Lands. */
    /** version 5: reshaped corridor pools (MazePools: elongated, 1-2 deep, lava/water/Liquid Death/flux goo with rims). version 4: rooms of 1-5 Giant Taintacles. version 3: crusted stone, glowing crusted stone, strange crystals, autocasters, living taint rooms (seed + biome). version 2: rune/spider/taint rooms and corridor hazards. version 1: mazes saved before the rune, spider, taint rooms and corridor hazards (kept on their old layout). */
    public static final int VERSION = 5;
    public record Maze(int id, GlobalPos altar, long seed, boolean generated, int version) {
        public Maze(int id, GlobalPos altar, long seed, boolean generated) { this(id, altar, seed, generated, VERSION); }
        static final Codec<Maze> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(Maze::id),
                GlobalPos.CODEC.fieldOf("altar").forGetter(Maze::altar),
                Codec.LONG.fieldOf("seed").forGetter(Maze::seed),
                Codec.BOOL.fieldOf("generated").forGetter(Maze::generated),
                Codec.INT.optionalFieldOf("version", 1).forGetter(Maze::version)).apply(i, Maze::new));
    }

    private final List<Maze> mazes;

    public MazeData() { this(new ArrayList<>()); }
    private MazeData(List<Maze> list) { this.mazes = new ArrayList<>(list); }

    public static final Codec<MazeData> CODEC = Maze.CODEC.listOf().fieldOf("mazes").codec().xmap(MazeData::new, d -> d.mazes);
    public static final SavedDataType<MazeData> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("thaumcraft", "outer_lands_mazes"), MazeData::new, CODEC);

    public static MazeData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }

    public Maze byId(int id) { return id >= 0 && id < mazes.size() ? mazes.get(id) : null; }
    public int size() { return mazes.size(); }

    /** The maze opened by this altar, allocating a new section the first time. */
    public Maze forAltar(GlobalPos altar, long seed) {
        for (Maze m : mazes) if (m.altar().equals(altar)) return m;
        Maze m = new Maze(mazes.size(), altar, seed, false);
        mazes.add(m);
        setDirty();
        return m;
    }

    void markGenerated(int id) {
        Maze m = mazes.get(id);
        mazes.set(id, new Maze(m.id(), m.altar(), m.seed(), true, m.version()));
        setDirty();
    }
}
