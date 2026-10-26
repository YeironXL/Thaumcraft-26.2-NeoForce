package art.arcane.thaumcraft.registry;

import art.arcane.thaumcraft.Thaumcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * ThaumcraftApiHelper.CHAMPION_MOD ("tc.mobmod", -2..100, watched) and ChampionModTainted.TAINTED_MOD ("tc.mobmodtaint", 0..1).
 * TC6 registered both on every EntityCreature as it was constructed; here every living type gets them (and an attack damage
 * attribute, which tainted creatures need to bite).
 */
public final class ModAttributes {
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, Thaumcraft.MOD_ID);
    public static final DeferredHolder<Attribute, Attribute> CHAMPION_MOD = ATTRIBUTES.register("champion_mod",
            () -> new RangedAttribute("attribute.name.thaumcraft.champion_mod", -2.0, -2.0, 100.0).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> TAINTED_MOD = ATTRIBUTES.register("tainted_mod",
            () -> new RangedAttribute("attribute.name.thaumcraft.tainted_mod", 0.0, 0.0, 1.0));

    /** Types that had no attack damage of their own (tainted ones get max(2, (height + width) * 2), as in TC6). */
    public static final java.util.Set<net.minecraft.world.entity.EntityType<?>> ADDED_ATTACK_DAMAGE = new java.util.HashSet<>();

    private static void modify(EntityAttributeModificationEvent e) {
        for (var type : e.getTypes()) {
            if (!e.has(type, CHAMPION_MOD)) e.add(type, CHAMPION_MOD);
            if (!e.has(type, TAINTED_MOD)) e.add(type, TAINTED_MOD);
            if (!e.has(type, Attributes.ATTACK_DAMAGE)) {
                ADDED_ATTACK_DAMAGE.add(type);
                e.add(type, Attributes.ATTACK_DAMAGE, 2.0);
            }
        }
    }

    public static void register(IEventBus bus) { ATTRIBUTES.register(bus); bus.addListener(ModAttributes::modify); }
    private ModAttributes() {}
}
