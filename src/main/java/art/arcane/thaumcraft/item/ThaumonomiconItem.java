package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.network.ResearchSnapshotPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/** Opens the client research browser with the server's saved player state. */
public final class ThaumonomiconItem extends Item {
    public ThaumonomiconItem(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        PlayerProgressManager.unlock(serverPlayer, "!gotthaumonomicon");
        if(serverPlayer.connection!=null)PacketDistributor.sendToPlayer(serverPlayer, ResearchSnapshotPayload.of(serverPlayer));
        return InteractionResult.SUCCESS;
    }
}
