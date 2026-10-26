package art.arcane.thaumcraft.aura.node;

/**
 * TC4 api.nodes.NodeType, in TC4's NBT order (nodetype 0..5 = normal, unstable, sinister/dark, tainted, hungry, pure).
 * {@code row} is the auranodes.png strip used for the node core (32x32 grid of 64px frames).
 */
public enum NodeType {
    NORMAL(6), UNSTABLE(1), DARK(2), TAINTED(3), HUNGRY(4), PURE(5);
    public final int row;
    NodeType(int row) { this.row = row; }
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static NodeType byId(int id) { return id >= 0 && id < values().length ? values()[id] : NORMAL; }
}
