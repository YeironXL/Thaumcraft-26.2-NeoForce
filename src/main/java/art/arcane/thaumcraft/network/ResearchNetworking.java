package art.arcane.thaumcraft.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ResearchNetworking {
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ResearchSnapshotPayload.TYPE, ResearchSnapshotPayload.CODEC);
        event.registrar("1").playToClient(AspectSnapshotPayload.TYPE, AspectSnapshotPayload.CODEC);
        event.registrar("1").playToClient(ThaumometerAuraPayload.TYPE, ThaumometerAuraPayload.CODEC);
        event.registrar("1").playToClient(ResearchToastPayload.TYPE, ResearchToastPayload.CODEC);
        event.registrar("1").playToClient(FxPayload.TYPE, FxPayload.CODEC);
        event.registrar("1").playToClient(SealSyncPayload.TYPE, SealSyncPayload.CODEC);
        event.registrar("1").playToClient(GolemBuilderPayload.Stuff.TYPE, GolemBuilderPayload.Stuff.CODEC);
        event.registrar("1").playToServer(LogisticsPayload.TYPE, LogisticsPayload.CODEC, LogisticsPayload::handle);
        event.registrar("1").playToServer(GolemBuilderPayload.TYPE, GolemBuilderPayload.CODEC, GolemBuilderPayload::handle);
        event.registrar("1").playToServer(AdvanceResearchPayload.TYPE, AdvanceResearchPayload.CODEC, AdvanceResearchPayload::handle);
        event.registrar("1").playToServer(ResearchFlagsPayload.TYPE, ResearchFlagsPayload.CODEC, ResearchFlagsPayload::handle);
        event.registrar("1").playToServer(FocusNodesPayload.TYPE, FocusNodesPayload.CODEC, FocusNodesPayload::handle);
        event.registrar("1").playToServer(HarnessTogglePayload.TYPE, HarnessTogglePayload.CODEC, HarnessTogglePayload::handle);
        event.registrar("1").playToServer(CasterKeyPayload.TYPE, CasterKeyPayload.CODEC, CasterKeyPayload::handle);
        event.registrar("1").playToServer(PlayerFlagPayload.TYPE, PlayerFlagPayload.CODEC, PlayerFlagPayload::handle);
        event.registrar("1").playToServer(ThaumatoriumSelectPayload.TYPE, ThaumatoriumSelectPayload.CODEC, ThaumatoriumSelectPayload::handle);
        event.registrar("1").playBidirectional(NotePayload.TYPE, NotePayload.CODEC, NotePayload::handle);
    }
    private ResearchNetworking() {}
}
