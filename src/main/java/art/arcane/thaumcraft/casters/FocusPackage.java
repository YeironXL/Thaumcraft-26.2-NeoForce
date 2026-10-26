package art.arcane.thaumcraft.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** TC6 FocusPackage: an ordered chain of focus nodes plus the cast's power and caster. */
public final class FocusPackage implements IFocusElement {
    public Level world;
    private LivingEntity caster;
    private UUID casterUUID;
    private float power = 1;
    private int complexity = 0;
    int index;
    UUID uid;
    public List<IFocusElement> nodes = Collections.synchronizedList(new ArrayList<>());

    public FocusPackage() {}
    public FocusPackage(LivingEntity caster) { world = caster.level(); this.caster = caster; casterUUID = caster.getUUID(); }

    @Override public String getResearch() { return null; }
    @Override public String getKey() { return "thaumcraft.PACKAGE"; }
    @Override public EnumUnitType getType() { return EnumUnitType.PACKAGE; }
    public int getComplexity() { return complexity; }
    public void setComplexity(int c) { complexity = c; }
    public UUID getUniqueID() { return uid; }
    public void setUniqueID(UUID id) { uid = id; }
    public int getExecutionIndex() { return index; }
    public void setExecutionIndex(int idx) { index = idx; }
    public void addNode(IFocusElement e) { nodes.add(e); }
    public UUID getCasterUUID() { if (caster != null) casterUUID = caster.getUUID(); return casterUUID; }
    public void setCasterUUID(UUID u) { casterUUID = u; }

    public LivingEntity getCaster() {
        if (caster == null && world instanceof ServerLevel sl && getCasterUUID() != null) {
            var e = sl.getEntity(getCasterUUID());
            if (e instanceof LivingEntity le && le.isAlive()) caster = le;
            if (caster == null) caster = sl.getServer().getPlayerList().getPlayer(getCasterUUID());
        }
        return caster;
    }

    public FocusEffect[] getFocusEffects() { return effects(this).toArray(new FocusEffect[0]); }
    private static List<FocusEffect> effects(FocusPackage fp) {
        List<FocusEffect> out = new ArrayList<>();
        for (IFocusElement el : fp.nodes) {
            if (el instanceof FocusEffect fe) out.add(fe);
            else if (el instanceof FocusPackage p) out.addAll(effects(p));
            else if (el instanceof FocusModSplit s) for (FocusPackage sp : s.getSplitPackages()) out.addAll(effects(sp));
        }
        return out;
    }

    public void deserialize(CompoundTag nbt) {
        uid = nbt.read("uid", UUIDUtil.CODEC).orElse(null);
        index = nbt.getIntOr("index", 0);
        var dim = nbt.getString("dim");
        var server = ServerLifecycleHooks.getCurrentServer();
        if (dim.isPresent() && server != null) world = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim.get())));
        setCasterUUID(nbt.read("casterUUID", UUIDUtil.CODEC).orElse(null));
        power = nbt.getFloatOr("power", 1);
        complexity = nbt.getIntOr("complexity", 0);
        ListTag nodelist = nbt.getListOrEmpty("nodes");
        nodes.clear();
        for (int x = 0; x < nodelist.size(); x++) {
            CompoundTag nodenbt = nodelist.getCompoundOrEmpty(x);
            EnumUnitType ut;
            try { ut = EnumUnitType.valueOf(nodenbt.getStringOr("type", "")); } catch (IllegalArgumentException e) { continue; }
            if (ut == EnumUnitType.PACKAGE) {
                FocusPackage fp = new FocusPackage();
                fp.deserialize(nodenbt.getCompoundOrEmpty("package"));
                nodes.add(fp);
                break;
            }
            IFocusElement fn = FocusEngine.getElement(nodenbt.getStringOr("key", ""));
            if (fn == null) continue;
            if (fn instanceof FocusNode n) {
                for (String ns : n.getSettingList()) n.getSetting(ns).setValue(nodenbt.getIntOr("setting." + ns, 0));
                if (fn instanceof FocusModSplit split) split.deserialize(nodenbt.getCompoundOrEmpty("packages"));
            }
            addNode(fn);
        }
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        if (uid != null) nbt.store("uid", UUIDUtil.CODEC, uid);
        nbt.putInt("index", index);
        if (getCasterUUID() != null) nbt.store("casterUUID", UUIDUtil.CODEC, getCasterUUID());
        if (world != null) nbt.putString("dim", world.dimension().identifier().toString());
        nbt.putFloat("power", power);
        nbt.putInt("complexity", complexity);
        ListTag nodelist = new ListTag();
        synchronized (nodes) {
            for (IFocusElement node : nodes) {
                if (node == null || node.getType() == null) continue;
                CompoundTag nodenbt = new CompoundTag();
                nodenbt.putString("type", node.getType().name());
                nodenbt.putString("key", node.getKey());
                if (node.getType() == EnumUnitType.PACKAGE) {
                    nodenbt.put("package", ((FocusPackage) node).serialize());
                    nodelist.add(nodenbt);
                    break;
                }
                if (node instanceof FocusNode n) for (String ns : n.getSettingList()) nodenbt.putInt("setting." + ns, n.getSettingValue(ns));
                if (node instanceof FocusModSplit split) nodenbt.put("packages", split.serialize());
                nodelist.add(nodenbt);
            }
        }
        nbt.put("nodes", nodelist);
        return nbt;
    }

    public float getPower() { return power; }
    public void multiplyPower(float pow) { power *= pow; }

    public FocusPackage copy(LivingEntity caster) {
        FocusPackage fp = new FocusPackage(caster);
        fp.deserialize(serialize());
        return fp;
    }

    public void initialize(LivingEntity caster) {
        world = caster.level();
        IFocusElement node = nodes.get(0);
        if (node instanceof FocusMediumRoot root && root.supplyTargets() == null) root.setupFromCaster(caster);
    }

    public int getSortingHelper() {
        StringBuilder s = new StringBuilder();
        for (IFocusElement k : nodes) {
            s.append(k.getKey());
            if (k instanceof FocusNode n) for (String ns : n.getSettingList()) s.append(n.getSettingValue(ns));
        }
        return s.toString().hashCode();
    }
}
