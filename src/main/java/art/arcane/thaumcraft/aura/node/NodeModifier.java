package art.arcane.thaumcraft.aura.node;

/**
 * TC4 api.nodes.NodeModifier (bright, pale, fading) plus NONE for an ordinary node. {@code regen} is the number of
 * ticks between +1 regenerations of every aspect below its maximum (TC4 TileNode: 600 normal, 400 bright,
 * 900 pale, never when fading). {@code power} is the energised-node factor the forum describes (1.2 / 0.8).
 */
public enum NodeModifier {
    NONE(600, 1.0f), BRIGHT(400, 1.2f), PALE(900, 0.8f), FADING(0, 0.8f);
    public final int regen;
    public final float power;
    NodeModifier(int regen, float power) { this.regen = regen; this.power = power; }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static NodeModifier byId(int id) { return id >= 0 && id < values().length ? values()[id] : NONE; }
    /** Damage from a rough capture: bright -> normal -> pale -> fading. */
    public NodeModifier worse() { return switch (this) { case BRIGHT -> NONE; case NONE -> PALE; default -> FADING; }; }
}
