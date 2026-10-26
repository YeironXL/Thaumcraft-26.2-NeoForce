package art.arcane.thaumcraft.compat;

import art.arcane.thaumcraft.util.Baubles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Only loaded when Curios is present. */
public final class CuriosBaubles {
    /** IBauble: worn ticks go to the item's onWornTick. Slot validity comes from the curios item tags. */
    private static final ICurioItem BAUBLE = new ICurioItem() {
        @Override public void curioTick(SlotContext ctx, ItemStack stack) { Baubles.wornTick(stack, ctx.entity()); }
    };
    public static void register() {
        for (var item : Baubles.baubleItems()) CuriosApi.registerCurio(item, BAUBLE);
    }
    public static List<Baubles.Worn> worn(LivingEntity e) {
        List<Baubles.Worn> out = new ArrayList<>();
        CuriosApi.getCuriosInventory(e).ifPresent(inv -> {
            var h = inv.getEquippedCurios();
            for (int i = 0; i < h.getSlots(); i++) {
                final int slot = i;
                var st = h.getStackInSlot(i);
                if (!st.isEmpty()) out.add(new Baubles.Worn(st, s -> h.setStackInSlot(slot, s)));
            }
        });
        return out;
    }
    public static boolean equip(LivingEntity e, String slot, int index, ItemStack stack) {
        var inv = CuriosApi.getCuriosInventory(e);
        if (inv.isEmpty()) return false;
        var handler = inv.get().getStacksHandler(slot);
        if (handler.isEmpty() || index >= handler.get().getSlots()) return false;
        inv.get().setEquippedCurio(slot, index, stack);
        return true;
    }
    private CuriosBaubles() {}
}
