package art.arcane.thaumcraft.golems.seals;

public interface ISealConfigToggles {
    SealToggle[] getToggles();
    void setToggle(int indx, boolean value);
    final class SealToggle {
        public boolean value;
        public final String key, name;
        public SealToggle(boolean value, String key, String name) { this.value = value; this.key = key; this.name = name; }
        public boolean getValue() { return value; }
        public void setValue(boolean v) { value = v; }
        public String getKey() { return key; }
        public String getName() { return name; }
    }
}
