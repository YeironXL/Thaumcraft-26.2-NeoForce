package art.arcane.thaumcraft.casters;

import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.item.FocusPouchItem;
import art.arcane.thaumcraft.item.VisDiscountGear;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.util.Baubles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** TC6 CasterManager: vis discounts, focus switching (inventory and pouches) and casting cooldowns. */
public final class CasterManager {
    private static final Map<Integer, Long> cooldownServer = new HashMap<>();
    private static final Map<Integer, Long> cooldownClient = new HashMap<>();

    private static int effectLevel(LivingEntity p, String id) {
        var h = BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("thaumcraft", id));
        if (h.isEmpty()) return -1;
        var inst = p.getEffect(h.get());
        return inst == null ? -1 : inst.getAmplifier();
    }

    public static float getTotalVisDiscount(Player player) {
        if (player == null) return 0.0f;
        int total = 0;
        for (var w : Baubles.worn(player)) if (w.stack().getItem() instanceof VisDiscountGear g) total += g.getVisDiscount(w.stack(), player);
        for (var slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD})
            if (player.getItemBySlot(slot).getItem() instanceof VisDiscountGear g) total += g.getVisDiscount(player.getItemBySlot(slot), player);
        int l1 = effectLevel(player, "vis_exhaust"), l2 = effectLevel(player, "infectious_vis_exhaust");
        if (l1 >= 0 || l2 >= 0) total -= (Math.max(Math.max(l1, 0), Math.max(l2, 0)) + 1) * 10;
        return total / 100.0f;
    }

    public static boolean consumeVisFromInventory(Player player, float cost) {
        var inv = player.getInventory();
        for (int a = inv.getNonEquipmentItems().size() - 1; a >= 0; --a) {
            ItemStack item = inv.getNonEquipmentItems().get(a);
            if (item.getItem() instanceof CasterItem c && c.consumeVis(item, player, cost, true, false)) return true;
        }
        return false;
    }

    /** Pouch locations: >=0 main inventory slot, <0 worn bauble index -(i+1). */
    private record Pouch(int slot, Baubles.Worn worn) {}

    public static void changeFocus(ItemStack is, Player player, String focus) {
        CasterItem wand = (CasterItem) is.getItem();
        TreeMap<String, Integer> foci = new TreeMap<>();
        Map<Integer, Pouch> pouches = new HashMap<>();
        int pouchcount = 0;
        for (var w : Baubles.worn(player)) {
            if (w.stack().getItem() instanceof FocusPouchItem fp) {
                ++pouchcount;
                pouches.put(pouchcount, new Pouch(-1, w));
                var inv = fp.getInventory(w.stack());
                for (int q = 0; q < inv.size(); ++q)
                    if (inv.get(q).getItem() instanceof FocusItem f) { String sh = f.getSortingHelper(inv.get(q)); if (sh != null) foci.put(sh, q + pouchcount * 1000); }
            }
        }
        var main = player.getInventory().getNonEquipmentItems();
        for (int a = 0; a < 36; ++a) {
            ItemStack item = main.get(a);
            if (item.getItem() instanceof FocusItem f) {
                String sh2 = f.getSortingHelper(item);
                if (sh2 != null) foci.put(sh2, a);
            }
            if (item.getItem() instanceof FocusPouchItem fp) {
                ++pouchcount;
                pouches.put(pouchcount, new Pouch(a, null));
                var inv = fp.getInventory(item);
                for (int q = 0; q < inv.size(); ++q)
                    if (inv.get(q).getItem() instanceof FocusItem f) { String sh = f.getSortingHelper(inv.get(q)); if (sh != null) foci.put(sh, q + pouchcount * 1000); }
            }
        }
        if (focus.equals("REMOVE") || foci.isEmpty()) {
            ItemStack cur = wand.getFocusStack(is);
            if (!cur.isEmpty() && (addFocusToPouch(player, cur.copy(), pouches) || player.getInventory().add(cur.copy()))) {
                wand.setFocus(is, ItemStack.EMPTY);
                player.level().playSound(null, player.blockPosition(), ModSounds.get("ticks"), SoundSource.PLAYERS, 0.3f, 0.9f);
            }
            return;
        }
        String newkey = focus;
        if (foci.get(newkey) == null) newkey = foci.higherKey(newkey);
        if (newkey == null || foci.get(newkey) == null) newkey = foci.firstKey();
        int loc = foci.get(newkey);
        ItemStack item = ItemStack.EMPTY;
        if (loc < 1000 && loc >= 0) item = main.get(loc).copy();
        else {
            int pid = loc / 1000;
            Pouch p = pouches.get(pid);
            if (p != null) item = fetchFocusFromPouch(player, loc - pid * 1000, p);
        }
        if (item.isEmpty()) return;
        if (loc < 1000 && loc >= 0) main.set(loc, ItemStack.EMPTY);
        player.level().playSound(null, player.blockPosition(), ModSounds.get("ticks"), SoundSource.PLAYERS, 0.3f, 1.0f);
        ItemStack cur = wand.getFocusStack(is);
        if (!cur.isEmpty() && (addFocusToPouch(player, cur.copy(), pouches) || player.getInventory().add(cur.copy()))) wand.setFocus(is, ItemStack.EMPTY);
        if (wand.getFocusStack(is).isEmpty()) wand.setFocus(is, item);
        else if (!addFocusToPouch(player, item, pouches)) player.getInventory().add(item);
    }

    private static ItemStack pouchStack(Player player, Pouch p) { return p.slot >= 0 ? player.getInventory().getNonEquipmentItems().get(p.slot) : p.worn.stack(); }

    private static void storePouch(Player player, Pouch p, ItemStack pouch) {
        if (p.slot >= 0) { player.getInventory().setItem(p.slot, pouch); player.getInventory().setChanged(); }
        else p.worn.setter().accept(pouch);
    }

    private static ItemStack fetchFocusFromPouch(Player player, int focusid, Pouch p) {
        ItemStack pouch = pouchStack(player, p).copy();
        if (!(pouch.getItem() instanceof FocusPouchItem fp)) return ItemStack.EMPTY;
        var inv = fp.getInventory(pouch);
        ItemStack contents = inv.get(focusid);
        if (!(contents.getItem() instanceof FocusItem)) return ItemStack.EMPTY;
        ItemStack focus = contents.copy();
        inv.set(focusid, ItemStack.EMPTY);
        fp.setInventory(pouch, inv);
        storePouch(player, p, pouch);
        return focus;
    }

    private static boolean addFocusToPouch(Player player, ItemStack focus, Map<Integer, Pouch> pouches) {
        for (Pouch p : pouches.values()) {
            ItemStack pouch = pouchStack(player, p).copy();
            if (!(pouch.getItem() instanceof FocusPouchItem fp)) continue;
            var inv = fp.getInventory(pouch);
            for (int q = 0; q < inv.size(); ++q) {
                if (inv.get(q).isEmpty()) {
                    inv.set(q, focus.copy());
                    fp.setInventory(pouch, inv);
                    storePouch(player, p, pouch);
                    return true;
                }
            }
        }
        return false;
    }

    /** Every focus the player could switch to (used by the radial selector). */
    public static List<ItemStack> availableFoci(Player player) {
        var out = new java.util.ArrayList<ItemStack>();
        for (var w : Baubles.worn(player)) if (w.stack().getItem() instanceof FocusPouchItem fp) for (var s : fp.getInventory(w.stack())) if (s.getItem() instanceof FocusItem) out.add(s);
        for (int a = 0; a < 36; ++a) {
            ItemStack item = player.getInventory().getNonEquipmentItems().get(a);
            if (item.getItem() instanceof FocusItem) out.add(item);
            if (item.getItem() instanceof FocusPouchItem fp) for (var s : fp.getInventory(item)) if (s.getItem() instanceof FocusItem) out.add(s);
        }
        return out;
    }

    // ---- architect area (PLAN medium): stored as [areax, areay, areaz, aread] ----
    private static java.util.List<Integer> area(ItemStack s) { return s.getOrDefault(art.arcane.thaumcraft.registry.ModComponents.CASTER_AREA.get(), java.util.List.of()); }
    private static int areaSize(ItemStack s) { return 3; }
    private static int areaValue(ItemStack s, int i) { var a = area(s); return a.size() > i ? Math.min(a.get(i), areaSize(s)) : areaSize(s); }
    public static int getAreaX(ItemStack s) { return areaValue(s, 0); }
    public static int getAreaY(ItemStack s) { return areaValue(s, 1); }
    public static int getAreaZ(ItemStack s) { return areaValue(s, 2); }
    public static int getAreaDim(ItemStack s) { var a = area(s); return a.size() > 3 ? a.get(3) : 0; }
    private static void setArea(ItemStack s, int x, int y, int z, int d) { s.set(art.arcane.thaumcraft.registry.ModComponents.CASTER_AREA.get(), java.util.List.of(x, y, z, d)); }

    /** KeyHandler G: mod 0 grows the area along the selected dimension(s), mod 1 cycles the dimension. */
    public static void toggleMisc(ItemStack itemstack, Player player, int mod) {
        if (!(itemstack.getItem() instanceof CasterItem caster)) return;
        FocusPackage fp = FocusItem.getPackage(caster.getFocusStack(itemstack));
        if (fp == null || !FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")) return;
        int dim = getAreaDim(itemstack), ax = getAreaX(itemstack), ay = getAreaY(itemstack), az = getAreaZ(itemstack), max = areaSize(itemstack);
        if (mod == 0) {
            if (dim == 0) { ++ax; ++az; ++ay; } else if (dim == 1) ++ax; else if (dim == 2) ++az; else if (dim == 3) ++ay;
            if (ax > max) ax = 0;
            if (az > max) az = 0;
            if (ay > max) ay = 0;
        }
        if (mod == 1 && ++dim > 3) dim = 0;
        setArea(itemstack, ax, ay, az, dim);
    }

    public static boolean isOnCooldown(LivingEntity e) {
        if (e.level().isClientSide()) return cooldownClient.containsKey(e.getId()) && cooldownClient.get(e.getId()) > System.currentTimeMillis();
        return cooldownServer.containsKey(e.getId()) && cooldownServer.get(e.getId()) > System.currentTimeMillis();
    }

    public static float getCooldown(LivingEntity e) {
        if (e.level().isClientSide() && cooldownClient.containsKey(e.getId())) return (cooldownClient.get(e.getId()) - System.currentTimeMillis()) / 1000.0f;
        return 0.0f;
    }

    public static void setCooldown(LivingEntity e, int cd) {
        if (cd == 0) { cooldownClient.remove(e.getId()); cooldownServer.remove(e.getId()); }
        else if (e.level().isClientSide()) cooldownClient.put(e.getId(), System.currentTimeMillis() + cd * 50L);
        else cooldownServer.put(e.getId(), System.currentTimeMillis() + cd * 50L);
    }

    private CasterManager() {}
}
