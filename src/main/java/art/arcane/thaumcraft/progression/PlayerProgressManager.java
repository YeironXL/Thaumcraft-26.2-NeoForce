package art.arcane.thaumcraft.progression;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.server.level.ServerPlayer;

import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.registry.ModAttachments;

/**
 * Server-owned progression saved with each player and copied on respawn.
 */
public final class PlayerProgressManager {

    private PlayerProgressManager() {}

    public static PlayerResearch get(ServerPlayer player) {
        PlayerResearch research = player.getData(ModAttachments.RESEARCH.get());
        if (research.warpListener() == null) research.setWarpListener(Warp.listener(player));
        return research;
    }

    public static boolean unlock(ServerPlayer player, String key) {
        return get(player).unlock(key);
    }

    public static boolean has(ServerPlayer player, String key) {
        return get(player).has(key);
    }

    public static boolean scan(ServerPlayer player, String key) {
        return get(player).scan(key);
    }

    public static Set<String> researched(ServerPlayer player) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(get(player).researched()));
    }

    public static Set<String> scanned(ServerPlayer player) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(get(player).scannedObjects()));
    }

    public static int getWarp(ServerPlayer player, WarpType type) {
        PlayerResearch research = get(player);
        return switch (type) {
            case NORMAL -> research.normalWarp();
            case TEMPORARY -> research.temporaryWarp();
            case PERMANENT -> research.permanentWarp();
        };
    }

    /** ThaumcraftApi.internalMethods.addWarpToPlayer: returns the new value of that pool. */
    public static int addWarp(ServerPlayer player, WarpType type, int delta) {
        PlayerResearch research = get(player);
        research.addWarp(type.ordinal(), delta);
        return getWarp(player, type);
    }

    public enum WarpType { PERMANENT, NORMAL, TEMPORARY }
}
