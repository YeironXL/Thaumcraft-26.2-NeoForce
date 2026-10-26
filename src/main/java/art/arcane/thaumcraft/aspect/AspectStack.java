package art.arcane.thaumcraft.aspect;

import java.util.Objects;

/** Mutable quantity of one aspect, used by scans, recipes and machines. */
public final class AspectStack {
    private final Aspect aspect;
    private int amount;

    public AspectStack(Aspect aspect, int amount) {
        this.aspect = Objects.requireNonNull(aspect);
        this.amount = Math.max(0, amount);
    }

    public Aspect aspect() { return aspect; }
    public int amount() { return amount; }
    public void grow(int value) { amount = Math.max(0, amount + value); }
    public boolean shrink(int value) {
        if (value < 0 || amount < value) return false;
        amount -= value;
        return true;
    }
}
