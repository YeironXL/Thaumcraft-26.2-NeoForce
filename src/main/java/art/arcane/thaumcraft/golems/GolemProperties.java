package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.golems.parts.GolemMaterial;
import art.arcane.thaumcraft.golems.parts.GolemPart;
import art.arcane.thaumcraft.golems.parts.PartFunction;
import art.arcane.thaumcraft.golems.parts.PartModel;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * TC6 GolemProperties: material, head, arms, legs, addon and rank packed one byte each into a long, plus the part
 * registry in TC6 registration order (part ids are their index, so stored golems keep their parts).
 */
public final class GolemProperties {
    public static final List<GolemMaterial> MATERIALS = new ArrayList<>();
    public static final List<GolemPart> HEADS = new ArrayList<>(), ARMS = new ArrayList<>(), LEGS = new ArrayList<>(), ADDONS = new ArrayList<>();

    private long data;
    private Set<GolemTrait> traitCache;

    public GolemProperties() {}
    public static GolemProperties fromLong(long d) { GolemProperties p = new GolemProperties(); p.data = d; return p; }
    public long toLong() { return data; }

    private static byte getByte(long l, int idx) { return (byte) ((l >> (idx * 8)) & 0xFF); }
    private static long setByte(long l, byte b, int idx) { return (l & ~(0xFFL << (idx * 8))) | ((long) (b & 0xFF) << (idx * 8)); }
    private static <T> T pick(List<T> list, int i) { return i >= 0 && i < list.size() ? list.get(i) : list.get(0); }

    public Set<GolemTrait> getTraits() {
        if (traitCache == null) {
            traitCache = EnumSet.noneOf(GolemTrait.class);
            for (GolemTrait t : getMaterial().traits) addTraitSmart(t);
            for (GolemTrait t : getHead().traits) addTraitSmart(t);
            for (GolemTrait t : getArms().traits) addTraitSmart(t);
            for (GolemTrait t : getLegs().traits) addTraitSmart(t);
            for (GolemTrait t : getAddon().traits) addTraitSmart(t);
        }
        return traitCache;
    }
    private void addTraitSmart(GolemTrait trait) {
        if (trait.opposite != null && traitCache.contains(trait.opposite)) traitCache.remove(trait.opposite);
        else traitCache.add(trait);
    }
    public boolean hasTrait(GolemTrait t) { return getTraits().contains(t); }

    public void setMaterial(GolemMaterial m) { data = setByte(data, m.id, 0); traitCache = null; }
    public GolemMaterial getMaterial() { return pick(MATERIALS, getByte(data, 0) & 0xFF); }
    public void setHead(GolemPart p) { data = setByte(data, p.id, 1); traitCache = null; }
    public GolemPart getHead() { return pick(HEADS, getByte(data, 1) & 0xFF); }
    public void setArms(GolemPart p) { data = setByte(data, p.id, 2); traitCache = null; }
    public GolemPart getArms() { return pick(ARMS, getByte(data, 2) & 0xFF); }
    public void setLegs(GolemPart p) { data = setByte(data, p.id, 3); traitCache = null; }
    public GolemPart getLegs() { return pick(LEGS, getByte(data, 3) & 0xFF); }
    public void setAddon(GolemPart p) { data = setByte(data, p.id, 4); traitCache = null; }
    public GolemPart getAddon() { return pick(ADDONS, getByte(data, 4) & 0xFF); }
    public void setRank(int r) { data = setByte(data, (byte) r, 5); }
    public int getRank() { return getByte(data, 5); }

    /** generateComponents: two material bases and one mechanism, then each part's components (merged by item). */
    public ItemStack[] generateComponents() {
        List<ItemStack> comps = new ArrayList<>();
        addToList(comps, getMaterial().componentBase(), 2);
        addToList(comps, getMaterial().componentMechanism(), 1);
        addFromComps(comps, getArms().components, getMaterial());
        addFromComps(comps, getLegs().components, getMaterial());
        addFromComps(comps, getHead().components, getMaterial());
        addFromComps(comps, getAddon().components, getMaterial());
        return comps.toArray(new ItemStack[0]);
    }
    @SuppressWarnings("unchecked")
    private static void addFromComps(List<ItemStack> comps, Object[] objs, GolemMaterial mat) {
        for (Object o : objs) {
            if (o instanceof Supplier<?> s) addToList(comps, ((Supplier<ItemStack>) s).get(), 1);
            else if (o instanceof String str) {
                if (str.equalsIgnoreCase("base")) addToList(comps, mat.componentBase(), 1);
                else if (str.equalsIgnoreCase("mech")) addToList(comps, mat.componentMechanism(), 1);
            }
        }
    }
    private static void addToList(List<ItemStack> comps, ItemStack newItem, int mult) {
        for (ItemStack stack : comps) {
            if (ItemStack.isSameItemSameComponents(stack, newItem)) { stack.grow(newItem.getCount() * mult); return; }
        }
        ItemStack s = newItem.copy();
        s.setCount(s.getCount() * mult);
        comps.add(s);
    }

    // ---------------------------------------------------------------- registry (TC6 static block order)
    private static Identifier tc(String p) { return Identifier.fromNamespaceAndPath("thaumcraft", p); }
    private static Supplier<ItemStack> st(Supplier<? extends net.minecraft.world.level.ItemLike> i) { return () -> new ItemStack(i.get()); }
    private static Supplier<ItemStack> st(Supplier<? extends net.minecraft.world.level.ItemLike> i, int n) { return () -> new ItemStack(i.get(), n); }
    private static Supplier<ItemStack> st(net.minecraft.world.level.ItemLike i) { return () -> new ItemStack(i); }
    private static Supplier<ItemStack> st(net.minecraft.world.level.ItemLike i, int n) { return () -> new ItemStack(i, n); }
    private static void mat(GolemMaterial m) { m.id = (byte) MATERIALS.size(); MATERIALS.add(m); }
    private static void reg(List<GolemPart> l, GolemPart p) { p.id = (byte) l.size(); l.add(p); }
    private static GolemTrait[] t(GolemTrait... t) { return t; }
    private static PartModel pm(String obj, String tex, PartModel.AttachPoint ap) { return new PartModel(tc("models/obj/" + obj + ".obj"), tex == null ? null : tc(tex), ap); }
    private static PartModel pm(String obj, String tex, PartModel.AttachPoint ap, PartModel.Special sp) { return new PartModel(tc("models/obj/" + obj + ".obj"), tex == null ? null : tc(tex), ap, sp); }

    static {
        var simple = st(ModItems.MECHANISM_SIMPLE);
        mat(new GolemMaterial("WOOD", new String[]{"MATSTUDWOOD"}, tc("textures/entity/golems/mat_wood.png"), 5059370, 6, 2, 1, st(ModBlocks.PLANK_GREATWOOD), simple, t(GolemTrait.LIGHT)));
        mat(new GolemMaterial("IRON", new String[]{"MATSTUDIRON"}, tc("textures/entity/golems/mat_iron.png"), 16777215, 20, 8, 3, st(ModItems.PLATE_IRON), simple, t(GolemTrait.HEAVY, GolemTrait.FIREPROOF, GolemTrait.BLASTPROOF)));
        mat(new GolemMaterial("CLAY", new String[]{"MATSTUDCLAY"}, tc("textures/entity/golems/mat_clay.png"), 13071447, 10, 4, 2, st(Blocks.TERRACOTTA), simple, t(GolemTrait.FIREPROOF)));
        mat(new GolemMaterial("BRASS", new String[]{"MATSTUDBRASS"}, tc("textures/entity/golems/mat_brass.png"), 15638812, 16, 6, 3, st(ModItems.PLATE_BRASS), simple, t(GolemTrait.LIGHT)));
        mat(new GolemMaterial("THAUMIUM", new String[]{"MATSTUDTHAUMIUM"}, tc("textures/entity/golems/mat_thaumium.png"), 5257074, 24, 10, 4, st(ModItems.PLATE_THAUMIUM), simple, t(GolemTrait.HEAVY, GolemTrait.FIREPROOF, GolemTrait.BLASTPROOF)));
        mat(new GolemMaterial("VOID", new String[]{"MATSTUDVOID"}, tc("textures/entity/golems/mat_void.png"), 1445161, 20, 6, 4, st(ModItems.PLATE_VOID), simple, t(GolemTrait.REPAIR)));

        var mind0 = st(ModItems.MIND_CLOCKWORK);
        var mind1 = st(ModItems.MIND_BIOTHAUMIC);
        var vision = st(ModItems.MODULE_VISION);
        var aggression = st(ModItems.MODULE_AGGRESSION);
        String other = "textures/entity/golems/golem_head_other.png";
        reg(HEADS, new GolemPart("head", "BASIC", new String[]{"MINDCLOCKWORK"}, tc("textures/misc/golem/head_basic.png"), pm("golem_head_basic", null, PartModel.AttachPoint.HEAD), new Object[]{mind0}, null, t()));
        reg(HEADS, new GolemPart("head", "SMART", new String[]{"MINDBIOTHAUMIC"}, tc("textures/misc/golem/head_smart.png"), pm("golem_head_smart", other, PartModel.AttachPoint.HEAD), new Object[]{mind1}, null, t(GolemTrait.SMART, GolemTrait.FRAGILE)));
        reg(HEADS, new GolemPart("head", "SMART_ARMORED", new String[]{"MINDBIOTHAUMIC", "GOLEMCOMBATADV"}, tc("textures/misc/golem/head_smartarmor.png"), pm("golem_head_smart_armor", null, PartModel.AttachPoint.HEAD), new Object[]{mind1, st(ModItems.PLATE_BRASS), "base", st(Blocks.WOOL.white())}, null, t(GolemTrait.SMART)));
        reg(HEADS, new GolemPart("head", "SCOUT", new String[]{"GOLEMVISION"}, tc("textures/misc/golem/head_scout.png"), pm("golem_head_scout", other, PartModel.AttachPoint.HEAD), new Object[]{mind0, vision}, null, t(GolemTrait.SCOUT, GolemTrait.FRAGILE)));
        reg(HEADS, new GolemPart("head", "SMART_SCOUT", new String[]{"GOLEMVISION", "MINDBIOTHAUMIC"}, tc("textures/misc/golem/head_smartscout.png"), pm("golem_head_scout_smart", other, PartModel.AttachPoint.HEAD), new Object[]{mind1, vision}, null, t(GolemTrait.SCOUT, GolemTrait.SMART, GolemTrait.FRAGILE)));

        reg(ARMS, new GolemPart("arm", "BASIC", new String[]{"MINDCLOCKWORK"}, tc("textures/misc/golem/arms_basic.png"), pm("golem_arms_basic", null, PartModel.AttachPoint.ARMS), new Object[0], null, t()));
        reg(ARMS, new GolemPart("arm", "FINE", new String[]{"MATSTUDBRASS"}, tc("textures/misc/golem/arms_fine.png"), pm("golem_arms_fine", null, PartModel.AttachPoint.ARMS), new Object[]{simple, "base"}, null, t(GolemTrait.DEFT, GolemTrait.FRAGILE)));
        reg(ARMS, new GolemPart("arm", "CLAWS", new String[]{"GOLEMCOMBATADV"}, tc("textures/misc/golem/arms_claws.png"), pm("golem_arms_claws", "textures/entity/golems/golem_arms_claws.png", PartModel.AttachPoint.ARMS, PartModel.Special.CLAWS), new Object[]{aggression, st(Items.SHEARS, 2), "base"}, null, t(GolemTrait.FIGHTER, GolemTrait.CLUMSY, GolemTrait.BRUTAL)));
        reg(ARMS, new GolemPart("arm", "BREAKERS", new String[]{"GOLEMBREAKER"}, tc("textures/misc/golem/arms_breakers.png"), pm("golem_arms_breakers", "textures/entity/golems/golem_arms_breakers.png", PartModel.AttachPoint.ARMS, PartModel.Special.BREAKERS), new Object[]{st(Items.DIAMOND, 2), "base", st(Blocks.PISTON, 2)}, null, t(GolemTrait.BREAKER, GolemTrait.CLUMSY, GolemTrait.BRUTAL)));
        reg(ARMS, new GolemPart("arm", "DARTS", new String[]{"GOLEMCOMBATADV"}, tc("textures/misc/golem/arms_darts.png"), pm("golem_arms_darter", "textures/entity/golems/golem_arms_darter.png", PartModel.AttachPoint.ARMS, PartModel.Special.DARTS), new Object[]{aggression, st(Blocks.DISPENSER, 2), st(Items.ARROW, 32), "mech"}, new GolemParts.ArmDart(), t(GolemTrait.FIGHTER, GolemTrait.CLUMSY, GolemTrait.RANGED, GolemTrait.FRAGILE)));

        reg(LEGS, new GolemPart("leg", "WALKER", new String[]{"MINDCLOCKWORK"}, tc("textures/misc/golem/legs_walker.png"), pm("golem_legs_walker", null, PartModel.AttachPoint.LEGS), new Object[]{"base", "mech"}, null, t()));
        reg(LEGS, new GolemPart("leg", "ROLLER", new String[]{"MINDCLOCKWORK"}, tc("textures/misc/golem/legs_roller.png"), pm("golem_legs_wheel", "textures/entity/golems/golem_legs_wheel.png", PartModel.AttachPoint.BODY, PartModel.Special.WHEEL), new Object[]{st(Items.BOWL, 2), st(Items.LEATHER), "mech"}, new GolemParts.LegWheels(), t(GolemTrait.WHEELED)));
        reg(LEGS, new GolemPart("leg", "CLIMBER", new String[]{"GOLEMCLIMBER"}, tc("textures/misc/golem/legs_climber.png"), pm("golem_legs_climber", "textures/block/base_metal.png", PartModel.AttachPoint.LEGS), new Object[]{st(Items.FLINT, 4), "base", "mech", "mech"}, null, t(GolemTrait.CLIMBER)));
        reg(LEGS, new GolemPart("leg", "FLYER", new String[]{"GOLEMFLYER"}, tc("textures/misc/golem/legs_flyer.png"), pm("golem_legs_floater", "textures/entity/golems/golem_legs_floater.png", PartModel.AttachPoint.BODY), new Object[]{st(ModBlocks.LEVITATOR), st(ModItems.PLATE_BRASS, 4), st(Items.SLIME_BALL), "mech"}, new GolemParts.LegLevitator(), t(GolemTrait.FLYER, GolemTrait.FRAGILE)));

        reg(ADDONS, new GolemPart("addon", "NONE", new String[]{"MINDCLOCKWORK"}, tc("textures/block/blank.png"), null, new Object[0], null, t()));
        reg(ADDONS, new GolemPart("addon", "ARMORED", new String[]{"GOLEMCOMBATADV"}, tc("textures/misc/golem/addon_armored.png"), pm("golem_armor", null, PartModel.AttachPoint.BODY), new Object[]{"base", "base", "base", "base"}, null, t(GolemTrait.ARMORED, GolemTrait.HEAVY)));
        reg(ADDONS, new GolemPart("addon", "FIGHTER", new String[]{"SEALGUARD"}, tc("textures/misc/golem/addon_fighter.png"), null, new Object[]{aggression, "mech"}, null, t(GolemTrait.FIGHTER)));
        reg(ADDONS, new GolemPart("addon", "HAULER", new String[]{"MINDCLOCKWORK"}, tc("textures/misc/golem/addon_hauler.png"), pm("golem_hauler", "textures/entity/golems/golem_hauler.png", PartModel.AttachPoint.BODY, PartModel.Special.HAULER), new Object[]{st(Items.LEATHER), st(Blocks.CHEST)}, null, t(GolemTrait.HAULER)));
    }
    /** Forces the static part registry to load. */
    public static void init() {}
}
