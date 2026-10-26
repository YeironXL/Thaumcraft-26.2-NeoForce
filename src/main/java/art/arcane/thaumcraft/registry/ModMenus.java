package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.menu.ArcaneWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> TYPES = DeferredRegister.create(Registries.MENU,Thaumcraft.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = TYPES.register("arcane_workbench", () -> new MenuType<>(ArcaneWorkbenchMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.VoidSiphonMenu>> VOID_SIPHON=TYPES.register("void_siphon",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.VoidSiphonMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.DeconstructionTableMenu>> DECONSTRUCTION_TABLE=TYPES.register("deconstruction_table",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.DeconstructionTableMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.SpaMenu>> SPA=TYPES.register("spa",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.SpaMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.PotionSprayerMenu>> POTION_SPRAYER=TYPES.register("potion_sprayer",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.PotionSprayerMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.ThaumatoriumMenu>> THAUMATORIUM=TYPES.register("thaumatorium",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.ThaumatoriumMenu::new));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    public static final DeferredHolder<MenuType<?>, MenuType<art.arcane.thaumcraft.menu.EssentiaSmelterMenu>> ESSENTIA_SMELTER = TYPES.register("essentia_smelter", () -> new MenuType<>(art.arcane.thaumcraft.menu.EssentiaSmelterMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.ResearchTableMenu>> RESEARCH_TABLE=TYPES.register("research_table",()->new MenuType<>(art.arcane.thaumcraft.menu.ResearchTableMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.FocalManipulatorMenu>> FOCAL_MANIPULATOR=TYPES.register("focal_manipulator",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.FocalManipulatorMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.PechMenu>> PECH=TYPES.register("pech",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.menu.PechMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.HandMirrorMenu>> HAND_MIRROR=TYPES.register("hand_mirror",()->new MenuType<>(art.arcane.thaumcraft.menu.HandMirrorMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.menu.FocusPouchMenu>> FOCUS_POUCH=TYPES.register("focus_pouch",()->new MenuType<>(art.arcane.thaumcraft.menu.FocusPouchMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.golems.seals.SealMenu>> SEAL=TYPES.register("seal",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.golems.seals.SealMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.golems.builder.GolemBuilderMenu>> GOLEM_BUILDER=TYPES.register("golem_builder",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.golems.builder.GolemBuilderMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.entity.construct.TurretMenu>> TURRET=TYPES.register("turret",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.entity.construct.TurretMenu::new));
    public static final DeferredHolder<MenuType<?>,MenuType<art.arcane.thaumcraft.golems.logistics.LogisticsMenu>> LOGISTICS=TYPES.register("logistics",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(art.arcane.thaumcraft.golems.logistics.LogisticsMenu::new));
    private ModMenus() {}
}
