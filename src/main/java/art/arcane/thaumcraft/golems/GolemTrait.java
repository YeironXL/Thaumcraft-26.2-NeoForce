package art.arcane.thaumcraft.golems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Locale;

/** TC6 EnumGolemTrait: traits granted by parts; opposite pairs cancel each other. */
public enum GolemTrait {
    SMART, DEFT, CLUMSY, FIGHTER, WHEELED, FLYER, CLIMBER, HEAVY, LIGHT, FRAGILE, REPAIR, SCOUT, ARMORED, BRUTAL, FIREPROOF, BREAKER, HAULER, RANGED, BLASTPROOF;

    static {
        CLUMSY.opposite = DEFT; DEFT.opposite = CLUMSY;
        HEAVY.opposite = LIGHT; LIGHT.opposite = HEAVY;
        FRAGILE.opposite = ARMORED; ARMORED.opposite = FRAGILE;
    }
    public GolemTrait opposite;
    public final Identifier icon = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/tag_" + name().toLowerCase(Locale.ROOT) + ".png");
    public Component localizedName() { return Component.translatable("golem.trait." + name().toLowerCase(Locale.ROOT)); }
    public Component localizedDescription() { return Component.translatable("golem.trait.text." + name().toLowerCase(Locale.ROOT)); }
}
