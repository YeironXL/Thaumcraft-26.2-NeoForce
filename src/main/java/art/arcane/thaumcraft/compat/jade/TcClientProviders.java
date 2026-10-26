package art.arcane.thaumcraft.compat.jade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;

/** Client half of the Thaumcraft Jade providers: turns the server data from {@link TcProviders} into lines and aspect icons. */
final class TcClientProviders {
    record ClientPart(Identifier uid, Client lines) implements IBlockComponentProvider {
        @Override public Identifier getUid() { return uid; }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) { lines.append(tooltip, accessor.getServerData()); }
    }
    interface Client { void append(ITooltip tooltip, CompoundTag data); }

    static Map<String, Integer> aspects(CompoundTag data, String key) {
        var out = new LinkedHashMap<String, Integer>();
        var tag = data.getCompoundOrEmpty(key);
        for (String k : tag.keySet()) out.put(k, tag.getIntOr(k, 0));
        return out;
    }

    /** Aspect icons, eight per line. */
    static void addAspects(ITooltip tooltip, Map<String, Integer> aspects) {
        List<Element> row = new ArrayList<>();
        for (var e : aspects.entrySet()) {
            row.add(new AspectElement(e.getKey(), e.getValue()));
            if (row.size() == 8) { tooltip.add(new ArrayList<>(row)); row.clear(); }
        }
        if (!row.isEmpty()) tooltip.add(row);
    }

    private static Component aspectName(String aspect) {
        return Component.literal(TcProviders.cap(aspect)).withStyle(s -> s.withColor(art.arcane.thaumcraft.aspect.AspectRegistry.color(aspect)));
    }

    static final ClientPart JAR = new ClientPart(TcProviders.JAR.uid(), (tooltip, data) -> {
        String aspect = data.getStringOr("aspect", "");
        int amount = data.getIntOr("amount", 0), cap = data.getIntOr("capacity", 0);
        String filter = data.getStringOr("filter", "");
        if (!aspect.isEmpty() && amount > 0) {
            tooltip.add(new AspectElement(aspect, amount));
            tooltip.append(Component.literal(" ").append(aspectName(aspect)).append(Component.literal("  " + amount + " / " + cap).withStyle(ChatFormatting.GRAY)));
        } else if (data.contains("amount")) tooltip.add(Component.translatable("jade.thaumcraft.empty").withStyle(ChatFormatting.GRAY));
        if (!filter.isEmpty()) tooltip.add(Component.translatable("jade.thaumcraft.label", aspectName(filter)));
    });

    static final ClientPart CRUCIBLE = new ClientPart(TcProviders.CRUCIBLE.uid(), (tooltip, data) -> {
        if (!data.contains("heat")) return;
        int water = data.getIntOr("water", 0), heat = data.getIntOr("heat", 0);
        if (water <= 0) tooltip.add(Component.translatable("jade.thaumcraft.crucible.dry").withStyle(ChatFormatting.GRAY));
        else if (data.getBooleanOr("boiling", false)) tooltip.add(Component.translatable("jade.thaumcraft.crucible.boiling").withStyle(ChatFormatting.GOLD));
        else tooltip.add(Component.translatable("jade.thaumcraft.crucible.heating", Math.min(100, heat * 100 / 151)).withStyle(ChatFormatting.GRAY));
        var aspects = aspects(data, "aspects");
        if (!aspects.isEmpty()) {
            int total = aspects.values().stream().mapToInt(Integer::intValue).sum();
            tooltip.add(Component.translatable("jade.thaumcraft.crucible.essentia", total).withStyle(total > 500 ? ChatFormatting.RED : ChatFormatting.GRAY));
            addAspects(tooltip, aspects);
        }
    });

    static final ClientPart INFUSION = new ClientPart(TcProviders.INFUSION.uid(), (tooltip, data) -> {
        if (!data.contains("active")) return;
        if (!data.getBooleanOr("active", false)) { tooltip.add(Component.translatable("jade.thaumcraft.infusion.inactive").withStyle(ChatFormatting.GRAY)); return; }
        String level = data.getStringOr("level", "STABLE");
        ChatFormatting color = switch (level) { case "VERY_STABLE" -> ChatFormatting.GREEN; case "STABLE" -> ChatFormatting.DARK_GREEN; case "UNSTABLE" -> ChatFormatting.GOLD; default -> ChatFormatting.RED; };
        tooltip.add(Component.translatable("jade.thaumcraft.infusion.stability", Component.translatable("jade.thaumcraft.stability." + level.toLowerCase(java.util.Locale.ROOT)).withStyle(color),
                String.format(java.util.Locale.ROOT, "%.1f", data.getFloatOr("stability", 0))));
        if (data.getBooleanOr("crafting", false)) {
            int inst = Math.min(5, data.getIntOr("instability", 0) / 2);
            // TC6's tc.inst.N strings carry colour codes meant for the light book page (e.g. §1 dark blue); use readable colours on Jade's dark panel.
            ChatFormatting ic = switch (inst) { case 0 -> ChatFormatting.AQUA; case 1 -> ChatFormatting.BLUE; case 2 -> ChatFormatting.LIGHT_PURPLE; case 3 -> ChatFormatting.YELLOW; case 4 -> ChatFormatting.GOLD; default -> ChatFormatting.RED; };
            String instText = ChatFormatting.stripFormatting(net.minecraft.client.resources.language.I18n.get("tc.inst." + inst));
            tooltip.add(Component.translatable("tc.inst").append(Component.literal(instText).withStyle(ic)));
            var essentia = aspects(data, "essentia");
            int items = data.getIntOr("items", 0);
            if (essentia.isEmpty() && items == 0) tooltip.add(Component.translatable("jade.thaumcraft.infusion.finishing").withStyle(ChatFormatting.LIGHT_PURPLE));
            else {
                tooltip.add(Component.translatable("jade.thaumcraft.infusion.remaining", items).withStyle(ChatFormatting.GRAY));
                addAspects(tooltip, essentia);
            }
        }
    });

    static final ClientPart TUBE = new ClientPart(TcProviders.TUBE.uid(), (tooltip, data) -> {
        if (!data.contains("kind")) return;
        var aspects = aspects(data, "aspects");
        if (aspects.isEmpty()) tooltip.add(Component.translatable("jade.thaumcraft.empty").withStyle(ChatFormatting.GRAY));
        else addAspects(tooltip, aspects);
        String filter = data.getStringOr("filter", "");
        if (!filter.isEmpty()) tooltip.add(Component.translatable("jade.thaumcraft.filter", aspectName(filter)));
        if ("VALVE".equals(data.getStringOr("kind", "")) && !data.getBooleanOr("open", true)) tooltip.add(Component.translatable("jade.thaumcraft.tube.closed").withStyle(ChatFormatting.RED));
        int suction = data.getIntOr("suction", 0);
        if (suction > 0) tooltip.add(Component.translatable("jade.thaumcraft.suction", suction).withStyle(ChatFormatting.DARK_GRAY));
    });

    static final ClientPart CENTRIFUGE = new ClientPart(TcProviders.CENTRIFUGE.uid(), (tooltip, data) -> {
        if (!data.contains("input")) return;
        String in = data.getStringOr("input", ""), out = data.getStringOr("output", "");
        if (!in.isEmpty()) {
            tooltip.add(new AspectElement(in, 1));
            tooltip.append(Component.literal(" ").append(Component.translatable("jade.thaumcraft.centrifuge.processing", aspectName(in), 100 - data.getIntOr("remaining", 0) * 100 / 39)));
        }
        if (!out.isEmpty()) {
            tooltip.add(new AspectElement(out, 1));
            tooltip.append(Component.literal(" ").append(Component.translatable("jade.thaumcraft.centrifuge.output", aspectName(out))));
        }
        if (in.isEmpty() && out.isEmpty()) tooltip.add(Component.translatable("jade.thaumcraft.empty").withStyle(ChatFormatting.GRAY));
        if (data.getBooleanOr("paused", false)) tooltip.add(Component.translatable("jade.thaumcraft.paused").withStyle(ChatFormatting.RED));
    });

    static final ClientPart SMELTER = new ClientPart(TcProviders.SMELTER.uid(), (tooltip, data) -> {
        if (!data.contains("total")) return;
        tooltip.add(Component.translatable("jade.thaumcraft.smelter.essentia", data.getIntOr("total", 0)).withStyle(ChatFormatting.GRAY));
        int p = data.getIntOr("progress", 0);
        if (p > 0) tooltip.add(Component.translatable("jade.thaumcraft.progress", p).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("jade.thaumcraft.smelter.efficiency", data.getIntOr("efficiency", 0)).withStyle(ChatFormatting.DARK_GRAY));
    });

    static final ClientPart THAUMATORIUM = new ClientPart(TcProviders.THAUMATORIUM.uid(), (tooltip, data) -> {
        var essentia = aspects(data, "essentia");
        if (!essentia.isEmpty()) addAspects(tooltip, essentia);
    });

    private TcClientProviders() {}
}
