package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.fx.FXDispatcher;
import art.arcane.thaumcraft.network.PlayerFlagPayload;
import art.arcane.thaumcraft.util.Baubles;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;

/** TC6 ItemCloudRing.onWornTick client half: a second jump in mid air while the ring of clouds is worn. */
public final class BaubleClient {
    private static final Map<String, Boolean> jumpList = new HashMap<>();
    public static void register() { NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick()); }

    private static void tick() {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !Baubles.isEquipped(player, ModItems.CLOUD_RING.get())) return;
        String name = player.getName().getString();
        boolean spacePressed = mc.options.keyJump.consumeClick();
        if (spacePressed && !jumpList.containsKey(name)) jumpList.put(name, true);
        if (spacePressed && !player.onGround() && !player.isInWater() && player.noJumpDelay == 0 && Boolean.TRUE.equals(jumpList.get(name))) {
            FXDispatcher.drawBamf(player.getX(), player.getY() + 0.5, player.getZ(), 1.0f, 1.0f, 1.0f, false, false, Direction.UP);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.1f, 1.0f + (float) player.getRandom().nextGaussian() * 0.05f, false);
            jumpList.put(name, false);
            Vec3 m = player.getDeltaMovement();
            double my = 0.75;
            var jb = player.getEffect(MobEffects.JUMP_BOOST);
            if (jb != null) my += (jb.getAmplifier() + 1) * 0.1f;
            double mx = m.x, mz = m.z;
            if (player.isSprinting()) {
                float f = player.getYRot() * 0.017453292f;
                mx -= Mth.sin(f) * 0.2f;
                mz += Mth.cos(f) * 0.2f;
            }
            player.setDeltaMovement(mx, my, mz);
            player.fallDistance = 0.0;
            ClientPacketDistributor.sendToServer(new PlayerFlagPayload(1));
            CommonHooks.onLivingJump(player);
        }
        if (player.onGround() && player.noJumpDelay == 0) jumpList.remove(name);
    }
    private BaubleClient() {}
}
