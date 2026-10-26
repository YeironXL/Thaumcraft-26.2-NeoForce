package art.arcane.thaumcraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.network.chat.Component;

@Mod(Thaumcraft.MOD_ID)
public final class Thaumcraft {
    public static final String MOD_ID = "thaumcraft";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("thaumcraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thaumcraft"))
            .icon(() -> ModItems.AMBER.get().getDefaultInstance())
            .displayItems((params, output) -> CreativeInventory.populate(output)).build());

    public Thaumcraft(IEventBus bus) {
        art.arcane.thaumcraft.registry.ModComponents.register(bus);
        art.arcane.thaumcraft.registry.ModRecipes.register(bus);
        art.arcane.thaumcraft.registry.ModMenus.register(bus);
        art.arcane.thaumcraft.registry.ModWorldgen.register(bus);
        art.arcane.thaumcraft.registry.ModSounds.register(bus);
        art.arcane.thaumcraft.registry.ModEffects.register(bus);
        art.arcane.thaumcraft.registry.ModEntities.register(bus);
        art.arcane.thaumcraft.registry.ModAttributes.register(bus);
        art.arcane.thaumcraft.events.ChampionEvents.register();
        art.arcane.thaumcraft.registry.ModParticles.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(art.arcane.thaumcraft.aura.NativeAura::tick);
        art.arcane.thaumcraft.events.ServerEvents.register();
        art.arcane.thaumcraft.events.EntityEvents.register();
        art.arcane.thaumcraft.events.WarpEvents.register();
        art.arcane.thaumcraft.events.BaubleEvents.register();
        art.arcane.thaumcraft.events.InfusionEnchantmentEvents.register();
        art.arcane.thaumcraft.events.GearEvents.register();
        art.arcane.thaumcraft.item.CurioItems.PrimordialPearl.register();
        art.arcane.thaumcraft.events.OuterLandsEvents.register();
        art.arcane.thaumcraft.golems.Golemancy.init();
        art.arcane.thaumcraft.golems.GolemDebugScene.register();
        art.arcane.thaumcraft.golems.trunk.TrunkDebugScene.register();
        art.arcane.thaumcraft.events.ManaDebugScene.register();
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e) -> { if (art.arcane.thaumcraft.util.Baubles.available()) art.arcane.thaumcraft.compat.CuriosBaubles.register(); });
        art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity.register();
        art.arcane.thaumcraft.events.DebugSceneServer.register();
        // Recipe viewers (JEI) read the thaumcraft recipe types from the client recipe map, which only holds what the server sends.
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) -> e.sendRecipes(
                art.arcane.thaumcraft.registry.ModRecipes.CRUCIBLE_TYPE.get(), art.arcane.thaumcraft.registry.ModRecipes.ARCANE_TYPE.get(), art.arcane.thaumcraft.registry.ModRecipes.INFUSION_TYPE.get()));
        art.arcane.thaumcraft.casters.foci.FociRegistry.init();
        art.arcane.thaumcraft.casters.ward.WardManager.register();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(art.arcane.thaumcraft.aura.NativeAura::onChunkLoad);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(art.arcane.thaumcraft.aura.NativeAura::onChunkUnload);
        art.arcane.thaumcraft.progression.ProgressionEvents.register();
        art.arcane.thaumcraft.scan.DerivedAspectEvents.register();
        art.arcane.thaumcraft.registry.ModAttachments.register(bus);
        bus.addListener(art.arcane.thaumcraft.network.ResearchNetworking::register);
        bus.addListener(art.arcane.thaumcraft.registry.ModCapabilities::register);
        ModBlocks.register(bus);
        art.arcane.thaumcraft.registry.ModFluids.register(bus);
        ModItems.register(bus);
        art.arcane.thaumcraft.registry.ModBlockEntities.register(bus);
        TABS.register(bus);
    }
}
