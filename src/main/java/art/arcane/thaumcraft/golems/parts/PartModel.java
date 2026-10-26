package art.arcane.thaumcraft.golems.parts;

import net.minecraft.resources.Identifier;

/**
 * TC6 PartModel: an OBJ model, its texture (null = the material texture) and where it attaches. The TC6 subclasses
 * (PartModelClaws, Breakers, Darts, Hauler, Wheel) only change rendering; that behaviour lives in the client renderer,
 * selected by {@link #special}.
 */
public final class PartModel {
    public enum AttachPoint { ARMS, LEGS, BODY, HEAD }
    public enum LimbSide { LEFT, RIGHT, MIDDLE }
    public enum Special { NONE, CLAWS, BREAKERS, DARTS, HAULER, WHEEL }
    public final Identifier objModel, texture;
    public final AttachPoint attachPoint;
    public final Special special;
    public PartModel(Identifier objModel, Identifier texture, AttachPoint attachPoint) { this(objModel, texture, attachPoint, Special.NONE); }
    public PartModel(Identifier objModel, Identifier texture, AttachPoint attachPoint, Special special) {
        this.objModel = objModel; this.texture = texture; this.attachPoint = attachPoint; this.special = special;
    }
    /** Object parts named "bm..." use the golem material texture. */
    public static boolean useMaterialTextureForObjectPart(String partName) { return partName.startsWith("bm"); }
}
