package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusMediumRoot;
import net.minecraft.resources.Identifier;

/** ConfigItems: focus elements with their icons and colours. */
public final class FociRegistry {
    private static Identifier tex(String n) { return Identifier.fromNamespaceAndPath("thaumcraft", "textures/foci/" + n + ".png"); }
    public static void init() {
        FocusEngine.registerElement(FocusMediumRoot::new, tex("root"), 10066329);
        FocusEngine.registerElement(FocusMediumTouch::new, tex("touch"), 11371909);
        FocusEngine.registerElement(FocusMediumBolt::new, tex("bolt"), 11377029);
        FocusEngine.registerElement(FocusMediumProjectile::new, tex("projectile"), 11382149);
        FocusEngine.registerElement(FocusMediumCloud::new, tex("cloud"), 10071429);
        FocusEngine.registerElement(FocusMediumMine::new, tex("mine"), 8760709);
        FocusEngine.registerElement(FocusMediumPlan::new, tex("plan"), 8760728);
        FocusEngine.registerElement(FocusMediumSpellBat::new, tex("spellbat"), 8760748);
        FocusEngine.registerElement(FocusEffectFire::new, tex("fire"), 16734721);
        FocusEngine.registerElement(FocusEffectFrost::new, tex("frost"), 14811135);
        FocusEngine.registerElement(FocusEffectAir::new, tex("air"), 16777086);
        FocusEngine.registerElement(FocusEffectEarth::new, tex("earth"), 5685248);
        FocusEngine.registerElement(FocusEffectFlux::new, tex("flux"), 8388736);
        FocusEngine.registerElement(FocusEffectBreak::new, tex("break"), 9063176);
        FocusEngine.registerElement(FocusEffectRift::new, tex("rift"), 3084645);
        FocusEngine.registerElement(FocusEffectExchange::new, tex("exchange"), 5735255);
        FocusEngine.registerElement(FocusEffectCurse::new, tex("curse"), 6946821);
        FocusEngine.registerElement(FocusEffectHeal::new, tex("heal"), 14548997);
        FocusEngine.registerElement(FocusEffectWard::new, tex("ward"), 4210943);
        FocusEngine.registerElement(FocusEffectShock::new, tex("shock"), 10475775);
        FocusEngine.registerElement(FocusEffectPrimal::new, tex("primal"), 16777215);
        FocusEngine.registerElement(FocusModScatter::new, tex("scatter"), 10066329);
        FocusEngine.registerElement(FocusModSplitTarget::new, tex("split_target"), 10066329);
        FocusEngine.registerElement(FocusModSplitTrajectory::new, tex("split_trajectory"), 10066329);
    }
    private FociRegistry() {}
}
