package art.arcane.thaumcraft.aspect;

import java.util.Objects;

/** A Thaumcraft aspect: a named magical property with optional parent aspects. */
public record Aspect(String id, Aspect parentA, Aspect parentB) {
    public Aspect {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) throw new IllegalArgumentException("Aspect id cannot be blank");
        if (parentA == this || parentB == this) throw new IllegalArgumentException("Aspect cannot parent itself");
    }

    public boolean isPrimal() { return parentA == null && parentB == null; }
}
