package art.arcane.thaumcraft.progression;

import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchEvents;
import art.arcane.thaumcraft.research.ResearchRules;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server side of TC6 InternalMethodHandler.addWarpToPlayer / IPlayerWarp: the warp change message
 * (PacketWarpMessage), the "WARP" research unlock and the client sync (PacketSyncWarp).
 */
public final class Warp {
    private Warp() {}

    /** Listener installed on each player's research state; see {@link PlayerResearch#addWarp}. */
    public static PlayerResearch.WarpListener listener(ServerPlayer player) {
        return (type, amount) -> {
            if (player.connection != null)
                PacketDistributor.sendToPlayer(player, new FxPayload("warp_message", player.getX(), player.getY(), player.getZ(), List.of((float) type, (float) amount)));
            PlayerResearch state = player.getData(art.arcane.thaumcraft.registry.ModAttachments.RESEARCH.get());
            if (type != PlayerResearch.WARP_TEMPORARY && ResearchRules.complete(state, "FIRSTSTEPS") && !ResearchRules.complete(state, "WARP")) {
                completeResearch(player, "WARP");
                if (player.connection != null) player.sendOverlayMessage(Component.translatable("research.WARP.warn"));
            }
            ResearchEvents.markDirty(player);
            ResearchEvents.sync(player);
        };
    }

    /** TC6 ResearchManager.completeResearch: progress until nothing more happens. */
    public static boolean completeResearch(ServerPlayer player, String key) {
        boolean any = false;
        for (int guard = 0; guard < 64 && ResearchEvents.progress(player, key); guard++) any = true;
        if (any) ResearchEvents.sync(player);
        return any;
    }

    /** TC6 ThaumcraftCapabilities.knowsResearch. */
    public static boolean knows(ServerPlayer player, String key) {
        return ResearchRules.knows(PlayerProgressManager.get(player), key);
    }

    public static int total(ServerPlayer player) {
        PlayerResearch r = PlayerProgressManager.get(player);
        return r.permanentWarp() + r.normalWarp() + r.temporaryWarp();
    }

    /** InternalMethodHandler.getActualWarp: permanent + normal. */
    public static int actual(ServerPlayer player) {
        PlayerResearch r = PlayerProgressManager.get(player);
        return r.permanentWarp() + r.normalWarp();
    }
}
