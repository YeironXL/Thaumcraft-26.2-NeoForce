package art.arcane.thaumcraft.progression;

import art.arcane.thaumcraft.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import java.util.List;

public final class ProgressionEvents {
    public static void register() {
        DustSwaps.register();
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::crafted);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::pickedUp);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::wokeUp);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::periodic);
        NeoForge.EVENT_BUS.addListener(ProgressionEvents::hurt);
        // Client research mirror (TC6 PacketSyncKnowledge on login and dimension change).
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.connection != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(p, false));
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.connection != null) net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(p, false));
        });
    }
    private static void hurt(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof ServerPlayer player)||event.getOriginalDamage()<=0||!event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE))return;
        var state=PlayerProgressManager.get(player);
        if(state.has("BASEAUROMANCY@2")&&state.unlock("f_onfire")&&player.connection!=null){
            player.sendOverlayMessage(Component.translatable("got.onfire").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(player,false));
        }
    }
    private static void periodic(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount%200 != 0) return;
        var research=PlayerProgressManager.get(player);
        var biome=player.level().getBiome(player.blockPosition());
        if (biome.is(net.minecraft.tags.BiomeTags.IS_NETHER)) discover(player,"m_hellandback","got.hellandback");
        if (biome.is(net.minecraft.tags.BiomeTags.IS_END)) discover(player,"m_endoftheworld","got.endoftheworld");
        // TC6 ConfigResearch.checkPeriodicStuff: activity milestones from the player's statistics (needed by Motus).
        milestone(player,"m_walker",net.minecraft.stats.Stats.WALK_ONE_CM,160000);
        milestone(player,"m_runner",net.minecraft.stats.Stats.SPRINT_ONE_CM,80000);
        milestone(player,"m_jumper",net.minecraft.stats.Stats.JUMP,500);
        milestone(player,"m_swimmer",net.minecraft.stats.Stats.SWIM_ONE_CM,8000);
        if (research.has("UNLOCKAUROMANCY@1") && !research.has("UNLOCKAUROMANCY@2")) {
            if (player.getY() < player.level().getMinY()+10) discover(player,"m_deepdown","got.deepdown");
            if (player.getY() > player.level().getMinY()+player.level().getHeight()*0.4) discover(player,"m_uphigh","got.uphigh");
        }
    }
    private static void milestone(ServerPlayer player,String key,net.minecraft.resources.Identifier stat,int threshold) {
        if (PlayerProgressManager.has(player,key)) return;
        if (player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(stat)) <= threshold) return;
        if (PlayerProgressManager.unlock(player,key) && player.connection != null)
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(player,false));
    }
    private static void discover(ServerPlayer player,String key,String translation) {
        if (PlayerProgressManager.unlock(player,key) && player.connection != null) player.sendOverlayMessage(Component.translatable(translation));
    }
    private static void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getCrafting().isEmpty())
            PlayerProgressManager.get(player).recordCraft(BuiltInRegistries.ITEM.getKey(event.getCrafting().getItem()).toString());
    }
    private static void pickedUp(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)
                || event.getOriginalStack().getCount() <= event.getCurrentStack().getCount()) return;
        var stack = event.getOriginalStack();
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.is(ModItems.CRYSTAL_ESSENCE.get())) {
            if (PlayerProgressManager.unlock(player, "!gotcrystals") && player.connection != null) player.sendSystemMessage(Component.translatable("got.crystals"));
        }
        if (stack.is(ModItems.THAUMONOMICON.get())) PlayerProgressManager.unlock(player, "!gotthaumonomicon");
    }
    private static void wokeUp(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var research = PlayerProgressManager.get(player);
        if (!research.has("!gotcrystals") || research.has("!gotdream")) return;
        ItemStack journal = new ItemStack(Items.WRITTEN_BOOK);
        journal.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Strange Dreams"),
                player.getName().getString(), 3, List.of(Filterable.passThrough(Component.translatable("book.start.1")),
                Filterable.passThrough(Component.translatable("book.start.2")), Filterable.passThrough(Component.translatable("book.start.3"))), false));
        journal.set(DataComponents.CUSTOM_NAME, Component.translatable("book.start.title"));
        research.unlock("!gotdream");
        if (!player.getInventory().add(journal)) player.drop(journal, false);
        if (player.connection != null) player.sendSystemMessage(Component.translatable("got.dream"));
    }
    private ProgressionEvents() {}
}
