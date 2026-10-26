package art.arcane.thaumcraft.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** TC6 api.items.IGogglesDisplayExtended: lines floated over a block entity while wearing goggles of revealing. */
public interface IGogglesDisplayExtended {
    List<Component> getIGogglesText();
    default Vec3 getIGogglesTextOffset() { return Vec3.ZERO; }
}
