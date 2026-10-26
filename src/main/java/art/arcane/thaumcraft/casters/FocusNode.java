package art.arcane.thaumcraft.casters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.phys.HitResult;

/** TC6 FocusNode. */
public abstract class FocusNode implements IFocusElement {
    public FocusNode() { initialize(); }

    public String getUnlocalizedName() { return getKey() + ".name"; }
    public String getUnlocalizedText() { return getKey() + ".text"; }
    public abstract int getComplexity();
    /** Aspect tag (lowercase id) or null. */
    public abstract String getAspect();
    public abstract EnumSupplyType[] mustBeSupplied();
    public abstract EnumSupplyType[] willSupply();

    public boolean canSupply(EnumSupplyType type) {
        if (willSupply() != null) for (EnumSupplyType st : willSupply()) if (st == type) return true;
        return false;
    }

    public enum EnumSupplyType { TARGET, TRAJECTORY }

    public HitResult[] supplyTargets() { return null; }
    public Trajectory[] supplyTrajectories() { return null; }

    FocusPackage pack;
    public void setPackage(FocusPackage pack) { this.pack = pack; }
    public FocusPackage getPackage() { return pack; }

    public FocusPackage getRemainingPackage() {
        FocusPackage p = getPackage();
        List<IFocusElement> l = p.nodes.subList(p.index + 1, p.nodes.size());
        List<IFocusElement> l2 = Collections.synchronizedList(new ArrayList<>(l));
        FocusPackage p2 = new FocusPackage();
        p2.setUniqueID(p.getUniqueID());
        p2.world = p.world;
        p2.multiplyPower(p.getPower());
        p2.nodes = l2;
        p2.setCasterUUID(p.getCasterUUID());
        return l2.isEmpty() ? null : p2;
    }

    private FocusNode parent;
    public FocusNode getParent() { return parent; }
    public void setParent(FocusNode parent) { this.parent = parent; }

    final Map<String, NodeSetting> settings = new LinkedHashMap<>();
    public Set<String> getSettingList() { return settings.keySet(); }
    public NodeSetting getSetting(String key) { return settings.get(key); }
    public int getSettingValue(String key) { return settings.containsKey(key) ? settings.get(key).getValue() : 0; }
    public NodeSetting[] createSettings() { return null; }

    public void initialize() {
        NodeSetting[] set = createSettings();
        if (set != null) for (NodeSetting s : set) settings.put(s.key, s);
    }

    public float getPowerMultiplier() { return 1; }
    public boolean isExclusive() { return false; }
}
