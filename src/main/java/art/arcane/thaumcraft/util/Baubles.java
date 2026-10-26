package art.arcane.thaumcraft.util;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.item.BaubleItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.fml.ModList;

/**
 * TC6 BaublesApi equivalent. The bauble slots (amulet = curios "necklace", ring x2, belt, head, body, charm) come from
 * Curios when it is installed (optional dependency); without it, worn-bauble lookups are simply empty.
 */
public final class Baubles {
    public record Worn(ItemStack stack, Consumer<ItemStack> setter) {}
    /** Curios slot ids used for TC6's BaubleType values. */
    public static final String AMULET = "necklace", RING = "ring", BELT = "belt", HEAD = "head", BODY = "body", CHARM = "charm";
    private static Boolean loaded;
    public static boolean available() {
        if (loaded == null) loaded = ModList.get() != null && ModList.get().isLoaded("curios");
        return loaded;
    }
    public static List<Worn> worn(LivingEntity e) {
        return available() ? art.arcane.thaumcraft.compat.CuriosBaubles.worn(e) : List.of();
    }
    /** BaublesApi.isBaubleEquipped: the first worn stack of this item. */
    public static Optional<Worn> find(LivingEntity e, Item item) {
        for (Worn w : worn(e)) if (w.stack().is(item)) return Optional.of(w);
        return Optional.empty();
    }
    public static boolean isEquipped(LivingEntity e, Item item) { return find(e, item).isPresent(); }
    /** Puts a stack into a bauble slot (Curios slot id, index); false when the slot does not exist. */
    public static boolean equip(LivingEntity e, String slot, int index, ItemStack stack) {
        return available() && art.arcane.thaumcraft.compat.CuriosBaubles.equip(e, slot, index, stack);
    }
    /** IBauble.onWornTick dispatch, called by the accessory backend. */
    public static void wornTick(ItemStack stack, LivingEntity wearer) {
        if (stack.getItem() instanceof BaubleItem b) b.onWornTick(stack, wearer);
    }

    /** IRevealer.showNodes: goggles, void robe hood, fortress helm with goggles. */
    private static boolean reveals(ItemStack s) {
        if (s.isEmpty()) return false;
        if (s.is(ModItems.GOGGLES.get()) || s.is(ModItems.VOID_ROBE_HELM.get())) return true;
        return s.is(ModItems.FORTRESS_HELM.get()) && s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("goggles");
    }
    /** EntityUtils.hasRevealer: either hand, armour, or worn baubles. */
    public static boolean hasRevealer(LivingEntity viewer) {
        if (viewer == null) return false;
        if (reveals(viewer.getMainHandItem()) || reveals(viewer.getOffhandItem())) return true;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD })
            if (reveals(viewer.getItemBySlot(slot))) return true;
        for (Worn w : worn(viewer)) if (reveals(w.stack())) return true;
        return false;
    }
    /** EntityUtils.hasGoggles (IGoggles.showIngamePopups): main hand, armour, worn baubles. */
    public static boolean hasGoggles(LivingEntity viewer) {
        if (viewer == null) return false;
        if (reveals(viewer.getMainHandItem())) return true;
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD })
            if (reveals(viewer.getItemBySlot(slot))) return true;
        for (Worn w : worn(viewer)) if (reveals(w.stack())) return true;
        return false;
    }
    /** Items Thaumcraft registers as baubles. */
    public static List<Item> baubleItems() {
        List<Item> l = new ArrayList<>();
        for (var h : List.of(ModItems.AMULET_VIS, ModItems.BAUBLES_AMULET_MUNDANE, ModItems.BAUBLES_AMULET_FANCY, ModItems.BAUBLES_RING_MUNDANE,
                ModItems.BAUBLES_RING_APPRENTICE, ModItems.BAUBLES_RING_FANCY, ModItems.CLOUD_RING, ModItems.BAUBLES_GIRDLE_MUNDANE,
                ModItems.BAUBLES_GIRDLE_FANCY, ModItems.HOVER_GIRDLE, ModItems.FOCUS_POUCH, ModItems.GOGGLES, ModItems.CURIOSITY_BAND, ModItems.CHARM_UNDYING,
                ModItems.VERDANT_CHARM, ModItems.VOIDSEER_CHARM)) l.add(h.get());
        return l;
    }
    private Baubles() {}
}
