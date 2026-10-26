package art.arcane.thaumcraft.research;

import net.minecraft.server.level.ServerPlayer;

/** TC6 research popups (ResearchToast) for research completed by world triggers. */
public final class ResearchNotifications {
    public static void complete(ServerPlayer player, String key) {
        if (player.connection == null) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new art.arcane.thaumcraft.network.ResearchToastPayload(key));
    }
    private ResearchNotifications() {}
}
