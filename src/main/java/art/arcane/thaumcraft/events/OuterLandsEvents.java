package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity;
import art.arcane.thaumcraft.world.outer.OuterLands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Outer Lands rules: no flight (TC6 PlayerEvents.handleMisc) and Eldritch Eyes as rare Guardian drops (TC4). */
public final class OuterLandsEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(OuterLandsEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(OuterLandsEvents::drops);
    }

    private static void playerTick(PlayerTickEvent.Post e) {
        Player p = e.getEntity();
        if (p.level().isClientSide() || !OuterLands.is(p.level()) || p.tickCount % 20 != 0) return;
        if (!p.isSpectator() && !p.getAbilities().instabuild && p.getAbilities().flying) {
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.sendOverlayMessage(Component.translatable("tc.break.fly").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
    }

    private static void drops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof EldritchGuardianEntity g) || !(e.getSource().getEntity() instanceof Player)) return;
        if (g.getRandom().nextInt(20) != 0) return;
        e.getDrops().add(new ItemEntity(g.level(), g.getX(), g.getY(), g.getZ(), new ItemStack(ModItems.ELDRITCH_EYE.get())));
    }

    private OuterLandsEvents() {}
}
