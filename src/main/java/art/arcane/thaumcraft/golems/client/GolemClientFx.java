package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.client.fx.FXDispatcher;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.golems.GolemEntity;
import net.minecraft.util.RandomSource;

/** Client particles of golems: TC6 EntityThaumcraftGolem.handleStatusUpdate emotes, levitator exhaust and seal sparks. */
public final class GolemClientFx {
    public static void emote(GolemEntity g, byte id) {
        RandomSource r = g.getRandom();
        double x = g.getX(), y = g.getY() + g.getBbHeight() + 0.1, z = g.getZ();
        switch (id) {
            case 5 -> FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 1, 1, 1, 0.5f, false, 704 + (r.nextBoolean() ? 0 : 3), 3, 1, 6, 0, 2.0f, 0, 1);
            case 6 -> FXDispatcher.drawGenericParticles(x, y, z, 0, 0.025, 0, 0.1f, 1, 1, 0.5f, false, 15, 1, 1, 10, 0, 2.0f, 0, 1);
            case 7 -> FXDispatcher.drawGenericParticles(x, y, z, 0, 0.05, 0, 1, 1, 1, 0.5f, false, 640, 10, 1, 10, 0, 2.0f, 0, 1);
            case 8 -> FXDispatcher.drawGenericParticles(x, y, z, 0, 0.01, 0, 1, 1, 0.1f, 0.5f, false, 14, 1, 1, 20, 0, 2.0f, 0, 1);
            case 9 -> {
                for (int a = 0; a < 5; a++)
                    FXDispatcher.drawGenericParticles(x, g.getY() + g.getBbHeight(), z, r.nextGaussian() * 0.01, r.nextFloat() * 0.02, r.nextGaussian() * 0.01, 1, 1, 1, 0.5f, false, 13, 1, 1, 20 + r.nextInt(20), 0, 0.3f + r.nextFloat() * 0.4f, 0, 1);
            }
            default -> {}
        }
    }
    /** FXDispatcher.drawGolemFlyParticles. */
    public static void fly(double x, double y, double z, double x2, double y2, double z2) {
        RandomSource r = RandomSource.create();
        FXDispatcher.generic(x, y, z, x2, y2, z2, GenericParticleOptions.builder().maxAge(20 + r.nextInt(5)).alpha(0.3f, 0.0f).grid(16).particles(56, 1, 1)
                .scale(1.5f, 3.0f, 8.0f).layer(0).slowDown(1.0).wind(0.001, 0.001).rotation(r.nextFloat(), r.nextBoolean() ? -1.0f : 1.0f).build(), 0);
    }
    /** FXDispatcher.spark: red flicker on a redstone-stopped seal. */
    public static void spark(double x, double y, double z, float size, float rr, float g, float b, float a) {
        RandomSource r = RandomSource.create();
        FXDispatcher.generic(x, y, z, 0, 0, 0, GenericParticleOptions.builder().maxAge(5 + r.nextInt(5)).alpha(a).color(rr, g, b).grid(16).particles(8 + r.nextInt(3) * 16, 8, 1)
                .scale(size).flipped(r.nextBoolean()).build(), 0);
    }
    private GolemClientFx() {}
}
