package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.client.fx.FXDispatcher;
import art.arcane.thaumcraft.fx.GenericParticleOptions;

/** Client-only particle helpers for focus effects (FXGeneric / GenPart). Only ever called on the client. */
final class FociFx {
    static void gen(double x, double y, double z, double mx, double my, double mz, GenericParticleOptions.Builder b, int delay) {
        FXDispatcher.generic(x, y, z, mx, my, mz, b.build(), delay);
    }
    private FociFx() {}
}
