package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record ResearchSnapshotPayload(String data) implements CustomPacketPayload {
    public static final Type<ResearchSnapshotPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "research_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchSnapshotPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, ResearchSnapshotPayload::data, ResearchSnapshotPayload::new);

    public static ResearchSnapshotPayload of(ServerPlayer player) {
        return of(player, true);
    }
    public static ResearchSnapshotPayload of(ServerPlayer player, boolean open) {
        var research = PlayerProgressManager.get(player);
        JsonObject result = new JsonObject();
        result.addProperty("open", open);
        JsonArray keys = new JsonArray();
        research.researched().stream().sorted().forEach(keys::add);
        result.add("researched", keys);
        JsonObject stages = new JsonObject();
        research.stages().forEach(stages::addProperty);
        result.add("stages", stages);
        JsonObject flags = new JsonObject();
        research.flags().forEach((key, list) -> { JsonArray a = new JsonArray(); list.forEach(a::add); flags.add(key, a); });
        result.add("flags", flags);
        result.addProperty("scanned_count", research.scannedObjects().size());
        result.addProperty("knowledge", research.knowledge());
        result.addProperty("permanent_warp", research.permanentWarp());
        result.addProperty("normal_warp", research.normalWarp());
        result.addProperty("temporary_warp", research.temporaryWarp());
        // Research page (GuiResearchPage.checkRequisites/drawKnowledges) needs crafted references and raw category knowledge.
        JsonObject page = new JsonObject();
        JsonArray crafted = new JsonArray();
        research.craftedItems().stream().sorted().forEach(crafted::add);
        page.add("crafted", crafted);
        JsonObject knowledge = new JsonObject();
        research.categoryKnowledge().forEach(knowledge::addProperty);
        page.add("knowledge", knowledge);
        result.add("page", page);
        return new ResearchSnapshotPayload(result.toString());
    }

    @Override public Type<ResearchSnapshotPayload> type() { return TYPE; }
}
