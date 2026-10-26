package art.arcane.thaumcraft.golems.parts;

import art.arcane.thaumcraft.golems.GolemTrait;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** TC6 GolemMaterial. */
public final class GolemMaterial {
    public byte id;
    public final String key;
    public final String[] research;
    public final Identifier texture;
    public final int itemColor, healthMod, armor, damage;
    private final Supplier<ItemStack> base, mech;
    public final GolemTrait[] traits;
    public GolemMaterial(String key, String[] research, Identifier texture, int itemColor, int hp, int armor, int damage, Supplier<ItemStack> compb, Supplier<ItemStack> compm, GolemTrait[] traits) {
        this.key = key; this.research = research; this.texture = texture; this.itemColor = itemColor; this.healthMod = hp; this.armor = armor; this.damage = damage;
        this.base = compb; this.mech = compm; this.traits = traits;
    }
    public ItemStack componentBase() { return base.get(); }
    public ItemStack componentMechanism() { return mech.get(); }
    public Component localizedName() { return Component.translatable("golem.material." + key.toLowerCase(Locale.ROOT)); }
    public Component localizedDescription() { return Component.translatable("golem.material.text." + key.toLowerCase(Locale.ROOT)); }
}
