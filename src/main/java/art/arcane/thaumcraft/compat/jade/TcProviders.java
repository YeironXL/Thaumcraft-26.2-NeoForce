package art.arcane.thaumcraft.compat.jade;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Server half of the Thaumcraft Jade providers: fills the data tag on the server (block entities sync little of this
 * to clients). Must not reference client classes: Jade loads it on dedicated servers too. The client half that turns
 * the tag into tooltip lines is {@link TcClientProviders}.
 */
final class TcProviders {
    /** Jade requires the data and component providers to be separate objects. */
    record ServerPart(Identifier uid, BiConsumer<CompoundTag, BlockAccessor> fill) implements IServerDataProvider<BlockAccessor> {
        @Override public Identifier getUid() { return uid; }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) { fill.accept(data, accessor); }
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(Thaumcraft.MOD_ID, path); }

    static void putAspects(CompoundTag data, String key, Map<String, Integer> aspects) {
        var tag = new CompoundTag();
        aspects.forEach((a, n) -> { if (n != null && n > 0) tag.putInt(a, n); });
        data.put(key, tag);
    }

    static final ServerPart JAR = new ServerPart(id("jar"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof EssentiaJarBlockEntity jar)) return;
        data.putString("aspect", jar.aspect());
        data.putInt("amount", jar.contents().amount());
        data.putInt("capacity", jar.capacity());
        data.putString("filter", jar.filter());
    });

    static final ServerPart CRUCIBLE = new ServerPart(id("crucible"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof CrucibleBlockEntity c)) return;
        var m = c.mixture();
        data.putInt("water", m.water());
        data.putInt("heat", m.heat());
        data.putBoolean("boiling", m.boiling());
        putAspects(data, "aspects", m.aspects());
    });

    static final ServerPart INFUSION = new ServerPart(id("infusion"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof InfusionMatrixBlockEntity m)) return;
        data.putBoolean("active", m.active);
        data.putBoolean("crafting", m.crafting);
        data.putFloat("stability", m.stability);
        data.putString("level", m.getStability().name());
        data.putInt("instability", m.recipeInstability());
        data.putInt("items", m.recipeIngredients().size());
        putAspects(data, "essentia", m.recipeEssentia());
    });

    static final ServerPart TUBE = new ServerPart(id("tube"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof EssentiaTubeBlockEntity t)) return;
        putAspects(data, "aspects", t.contents());
        data.putString("filter", t.filter());
        data.putInt("suction", t.suction());
        data.putBoolean("open", t.open());
        data.putString("kind", t.kind().name());
    });

    static final ServerPart CENTRIFUGE = new ServerPart(id("centrifuge"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof EssentiaCentrifugeBlockEntity c)) return;
        data.putString("input", c.input());
        data.putString("output", c.aspect());
        data.putInt("remaining", c.remaining());
        data.putBoolean("paused", c.paused());
    });

    static final ServerPart SMELTER = new ServerPart(id("smelter"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof EssentiaSmelterBlockEntity s)) return;
        data.putInt("total", s.total());
        data.putInt("efficiency", s.efficiencyPercent());
        int dur = s.cookDuration();
        data.putInt("progress", dur > 0 ? Math.min(100, s.cook() * 100 / dur) : 0);
    });

    static final ServerPart THAUMATORIUM = new ServerPart(id("thaumatorium"), (data, acc) -> {
        if (!(acc.getBlockEntity() instanceof ThaumatoriumBlockEntity t)) return;
        putAspects(data, "essentia", t.essentia);
    });

    static String cap(String a) { return a.isEmpty() ? a : Character.toUpperCase(a.charAt(0)) + a.substring(1); }

    private TcProviders() {}
}
