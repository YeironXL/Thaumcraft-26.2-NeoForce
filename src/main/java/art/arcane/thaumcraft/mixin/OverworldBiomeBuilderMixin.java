package art.arcane.thaumcraft.mixin;

import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TC6 Registrar.registerBiomes: {@code BiomeManager.addBiome(WARM|COOL, MAGICAL_FOREST, biomeMagicalForestWeight=5)}.
 * In those 1.12 lists the magical forest (weight 5) sat beside the vanilla forest (weight 10), so here every forest
 * cell of the multi-noise overworld gives its most humid third to the magical forest (5 : 10).
 */
@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {
    private static final ResourceKey<Biome> THAUMCRAFT$MAGICAL_FOREST = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "magical_forest"));

    /** The built-in (code-only) registries used at bootstrap/datagen have no data-driven biomes; leave those untouched. */
    private static boolean thaumcraft$builtin() {
        return StackWalker.getInstance().walk(f -> f.anyMatch(x -> x.getClassName().endsWith("VanillaRegistries")));
    }

    @Inject(method = "addSurfaceBiome", at = @At("HEAD"), cancellable = true)
    private void thaumcraft$magicalForest(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion, Climate.Parameter weirdness, float offset, ResourceKey<Biome> biome, CallbackInfo ci) {
        if (biome != Biomes.FOREST || humidity.max() <= humidity.min() || thaumcraft$builtin()) return;
        long split = humidity.max() - (humidity.max() - humidity.min()) / 3;
        ci.cancel();
        Climate.Parameter lo = new Climate.Parameter(humidity.min(), split), hi = new Climate.Parameter(split, humidity.max());
        for (float depth : new float[] {0.0F, 1.0F}) {
            biomes.accept(Pair.of(Climate.parameters(temperature, lo, continentalness, erosion, Climate.Parameter.point(depth), weirdness, offset), Biomes.FOREST));
            biomes.accept(Pair.of(Climate.parameters(temperature, hi, continentalness, erosion, Climate.Parameter.point(depth), weirdness, offset), THAUMCRAFT$MAGICAL_FOREST));
        }
    }
}
