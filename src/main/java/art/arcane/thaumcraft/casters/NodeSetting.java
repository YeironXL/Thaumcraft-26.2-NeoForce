package art.arcane.thaumcraft.casters;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** TC6 NodeSetting. */
public final class NodeSetting {
    int value;
    public final String key;
    final String description;
    final INodeSettingType type;
    final String research;

    public NodeSetting(String key, String description, INodeSettingType setting, String research) {
        this.key = key; this.type = setting; this.value = setting.getDefault(); this.description = description; this.research = research;
    }
    public NodeSetting(String key, String description, INodeSettingType setting) { this(key, description, setting, null); }

    public int getValue() { return type.getValue(value); }
    public void setValue(int truevalue) {
        int lv = -1; value = 0;
        while (getValue() != truevalue && lv != value) { lv = value; increment(); }
    }
    public String getResearch() { return research; }
    public Component getValueText() { return Component.translatable(type.getValueText(value)); }
    public void increment() { value++; value = type.clamp(value); }
    public void decrement() { value--; value = type.clamp(value); }
    public INodeSettingType getType() { return type; }
    public Component getLocalizedName() { return Component.translatable(description); }
    public int rawIndex() { return value; }

    public interface INodeSettingType {
        int getDefault();
        int clamp(int i);
        int getValue(int value);
        String getValueText(int value);
    }

    public static final class NodeSettingIntList implements INodeSettingType {
        final int[] values; final String[] descriptions;
        public NodeSettingIntList(int[] values, String[] descriptions) { this.values = values; this.descriptions = descriptions; }
        public int getDefault() { return 0; }
        public int clamp(int old) { return Mth.clamp(old, 0, values.length - 1); }
        public int getValue(int value) { return values[clamp(value)]; }
        public String getValueText(int value) { return descriptions[value]; }
    }

    public static final class NodeSettingIntRange implements INodeSettingType {
        final int min, max;
        public NodeSettingIntRange(int min, int max) { this.min = min; this.max = max; }
        public int getDefault() { return min; }
        public int clamp(int old) { return Mth.clamp(old, min, max); }
        public int getValue(int value) { return clamp(value); }
        public String getValueText(int value) { return "" + getValue(value); }
    }
}
