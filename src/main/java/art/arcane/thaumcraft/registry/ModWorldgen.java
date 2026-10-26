package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE,Thaumcraft.MOD_ID);
    static { FEATURES.register("thaumcraft_world",art.arcane.thaumcraft.worldgen.ThaumcraftWorldFeature::new); FEATURES.register("magical_forest",art.arcane.thaumcraft.worldgen.MagicalForestFeature::new); }
    public static void register(IEventBus bus) { FEATURES.register(bus); }
    private ModWorldgen() {}
}
