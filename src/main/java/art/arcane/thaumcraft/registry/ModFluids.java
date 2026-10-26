package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** ConfigBlocks.FluidDeath / FluidPure as NeoForge fluids. */
public final class ModFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, "thaumcraft");
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, "thaumcraft");

    /** FluidDeath: viscosity 1500, rare; four quanta per block. */
    public static final DeferredHolder<FluidType, FluidType> DEATH_TYPE = TYPES.register("liquid_death", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.thaumcraft.liquid_death").viscosity(1500).rarity(Rarity.RARE).canConvertToSource(false).canSwim(true).canDrown(true)));
    /** FluidPure: light 5, rare; eight quanta per block. */
    public static final DeferredHolder<FluidType, FluidType> PURE_TYPE = TYPES.register("purifying_fluid", () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.thaumcraft.purifying_fluid").lightLevel(5).rarity(Rarity.RARE).canConvertToSource(false).canSwim(true).canDrown(true)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> DEATH = FLUIDS.register("liquid_death", () -> new BaseFlowingFluid.Source(deathProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> DEATH_FLOWING = FLUIDS.register("flowing_liquid_death", () -> new BaseFlowingFluid.Flowing(deathProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> PURE = FLUIDS.register("purifying_fluid", () -> new BaseFlowingFluid.Source(pureProps()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> PURE_FLOWING = FLUIDS.register("flowing_purifying_fluid", () -> new BaseFlowingFluid.Flowing(pureProps()));

    @SuppressWarnings("unchecked")
    private static BaseFlowingFluid.Properties deathProps() {
        return new BaseFlowingFluid.Properties(DEATH_TYPE, DEATH, DEATH_FLOWING).levelDecreasePerBlock(2).tickRate(8)
                .block(() -> (net.minecraft.world.level.block.LiquidBlock) ModBlocks.LIQUID_DEATH.get()).bucket(ModItems.BUCKET_DEATH);
    }
    @SuppressWarnings("unchecked")
    private static BaseFlowingFluid.Properties pureProps() {
        return new BaseFlowingFluid.Properties(PURE_TYPE, PURE, PURE_FLOWING).levelDecreasePerBlock(1).tickRate(5)
                .block(() -> (net.minecraft.world.level.block.LiquidBlock) ModBlocks.PURIFYING_FLUID.get()).bucket(ModItems.BUCKET_PURE);
    }

    public static void register(IEventBus bus) { TYPES.register(bus); FLUIDS.register(bus); }
    private ModFluids() {}
}
