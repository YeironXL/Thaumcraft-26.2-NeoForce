package art.arcane.thaumcraft.golems.parts;

import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.Locale;

/**
 * Common shape of TC6 GolemHead / GolemArm / GolemLeg / GolemAddon: key, research, GUI icon, optional part model,
 * component list (ItemStack suppliers or the strings "base"/"mech" referring to the material), traits and an optional function.
 */
public class GolemPart {
    public byte id;
    public final String kind, key;
    public final String[] research;
    public final Identifier icon;
    public final Object[] components;
    public final GolemTrait[] traits;
    public final PartModel model;
    public final PartFunction function;

    public GolemPart(String kind, String key, String[] research, Identifier icon, PartModel model, Object[] comp, PartFunction function, GolemTrait[] traits) {
        this.kind = kind; this.key = key; this.research = research; this.icon = icon; this.model = model; this.components = comp; this.function = function; this.traits = traits;
    }
    public Component localizedName() { return Component.translatable("golem." + kind + "." + key.toLowerCase(Locale.ROOT)); }
    public Component localizedDescription() { return Component.translatable("golem." + kind + ".text." + key.toLowerCase(Locale.ROOT)); }
}
