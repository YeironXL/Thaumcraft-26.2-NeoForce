package art.arcane.thaumcraft.casters;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;

/** TC6 FocusElementNode: one cell of the focal manipulator's node tree. */
public final class FocusElementNode {
    public int x, y, id;
    public boolean target = false, trajectory = false;
    public int parent = -1;
    public int[] children = new int[0];
    public float complexityMultiplier = 1.0f;
    public FocusNode node = null;

    public float getPower(Map<Integer, FocusElementNode> data) {
        if (node == null) return 1.0f;
        float pow = node.getPowerMultiplier();
        FocusElementNode p = data.get(parent);
        if (p != null && p.node != null) pow *= p.getPower(data);
        return pow;
    }

    public void deserialize(CompoundTag nbt) {
        x = nbt.getIntOr("x", 0); y = nbt.getIntOr("y", 0); id = nbt.getIntOr("id", 0);
        target = nbt.getBooleanOr("target", false); trajectory = nbt.getBooleanOr("trajectory", false);
        parent = nbt.getIntOr("parent", -1);
        children = nbt.getIntArray("children").orElse(new int[0]);
        complexityMultiplier = nbt.getFloatOr("complexity", 1.0f);
        if (FocusEngine.getElement(nbt.getStringOr("key", "")) instanceof FocusNode fe) {
            node = fe;
            for (String ns : fe.getSettingList()) fe.getSetting(ns).setValue(nbt.getIntOr("setting." + ns, 0));
        }
    }

    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("x", x); nbt.putInt("y", y); nbt.putInt("id", id);
        nbt.putBoolean("target", target); nbt.putBoolean("trajectory", trajectory);
        nbt.putInt("parent", parent); nbt.putIntArray("children", children);
        nbt.putFloat("complexity", complexityMultiplier);
        if (node != null) {
            nbt.putString("key", node.getKey());
            for (String ns : node.getSettingList()) nbt.putInt("setting." + ns, node.getSettingValue(ns));
        }
        return nbt;
    }
}
