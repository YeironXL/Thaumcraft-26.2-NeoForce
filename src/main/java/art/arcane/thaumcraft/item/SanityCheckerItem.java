package art.arcane.thaumcraft.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** TC6 ItemSanityChecker: while held, the client HUD shows the three warp pools (see client.WarpClient). */
public final class SanityCheckerItem extends Item {
    public SanityCheckerItem(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }
}
