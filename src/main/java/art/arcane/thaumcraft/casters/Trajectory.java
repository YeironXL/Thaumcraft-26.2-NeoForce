package art.arcane.thaumcraft.casters;

import net.minecraft.world.phys.Vec3;

/** TC6 Trajectory. */
public final class Trajectory {
    public Vec3 source;
    public Vec3 direction;
    public Trajectory(Vec3 source, Vec3 direction) { this.source = source; this.direction = direction; }
}
