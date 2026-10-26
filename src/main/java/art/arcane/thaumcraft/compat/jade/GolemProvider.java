package art.arcane.thaumcraft.compat.jade;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Golem material, rank and traits; all of it is in the synced entity data, so no server half is needed. */
enum GolemProvider implements IEntityComponentProvider {
    INSTANCE;
    static final Identifier UID = Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID, "golem");

    @Override public Identifier getUid() { return UID; }

    @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof GolemEntity g)) return;
        var p = g.getProperties();
        tooltip.add(p.getMaterial().localizedName().copy().withStyle(ChatFormatting.GREEN));
        if (p.hasTrait(GolemTrait.SMART)) tooltip.add(Component.translatable("golem.rank").append(" " + p.getRank()).withStyle(ChatFormatting.GOLD));
        var traits = Component.empty();
        boolean first = true;
        for (GolemTrait t : p.getTraits()) { if (!first) traits.append(", "); traits.append(t.localizedName()); first = false; }
        if (!first) tooltip.add(traits.withStyle(ChatFormatting.BLUE));
    }
}
