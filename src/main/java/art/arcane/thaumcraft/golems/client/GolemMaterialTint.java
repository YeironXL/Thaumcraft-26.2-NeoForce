package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.golems.GolemPlacerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** ColorHandler itemGolemColourHandler: the golem placer tinted by its material colour. */
public record GolemMaterialTint() implements ItemTintSource {
    public static final MapCodec<GolemMaterialTint> MAP_CODEC = MapCodec.unit(new GolemMaterialTint());
    @Override public int calculate(ItemStack s, @Nullable ClientLevel l, @Nullable LivingEntity e) {
        var p = GolemPlacerItem.props(s);
        return ARGB.opaque(p == null ? 0xFFFFFF : p.getMaterial().itemColor);
    }
    @Override public MapCodec<GolemMaterialTint> type() { return MAP_CODEC; }
}
