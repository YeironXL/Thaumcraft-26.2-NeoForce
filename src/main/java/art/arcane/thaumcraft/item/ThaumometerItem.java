package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.scan.ScanRegistry;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Server ray casting for blocks, dropped items and registered entity aspects. */
public final class ThaumometerItem extends Item {
    public ThaumometerItem(Item.Properties properties) {
        super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    }

    private static void emitScanRunes(Level level,Player player){
        var hit=player.pick(16,0,true);var start=player.getEyePosition();var direction=player.getViewVector(0).scale(9);
        double distance=Math.min(81,hit.getType()==HitResult.Type.MISS?81:start.distanceToSqr(hit.getLocation()));
        var entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,start,start.add(direction),player.getBoundingBox().expandTowards(direction).inflate(1),entity->entity.isAlive()&&!entity.isSpectator(),distance);
        double x,y,z;int duration;
        if(entityHit!=null){var entity=entityHit.getEntity();x=entity.getX();y=entity.getY()+entity.getEyeHeight()/2F+.5;z=entity.getZ();duration=Math.max(1,(int)(entity.getBbHeight()*15));}
        else if(hit instanceof BlockHitResult block&&hit.getType()==HitResult.Type.BLOCK){x=block.getBlockPos().getX()+.5;y=block.getBlockPos().getY()+.75;z=block.getBlockPos().getZ()+.5;duration=15;}
        else return;
        for(int i=0;i<10;i++){
            float red=.3F+level.getRandom().nextFloat()*.7F,blue=.3F+level.getRandom().nextFloat()*.7F;
            var option=net.minecraft.core.particles.ColorParticleOption.create(art.arcane.thaumcraft.registry.ModParticles.SCAN_RUNE.get(),net.minecraft.util.ARGB.colorFromFloat(1,red,0,blue));
            level.addParticle(option,x,y,z,duration,.03,0);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack,net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity owner,net.minecraft.world.entity.EquipmentSlot slot){
        if(!(owner instanceof ServerPlayer player)||owner.tickCount%20!=0||slot!=net.minecraft.world.entity.EquipmentSlot.MAINHAND&&slot!=net.minecraft.world.entity.EquipmentSlot.OFFHAND)return;
        var chunk=level.getChunkSource().getChunkNow(player.blockPosition().getX()>>4,player.blockPosition().getZ()>>4);
        if(chunk==null)return;
        var aura=art.arcane.thaumcraft.aura.NativeAura.get(level,chunk);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new art.arcane.thaumcraft.network.ThaumometerAuraPayload(aura.base(),aura.vis(),aura.flux()));
        // TC6 ItemThaumometer.updateAura: ResearchManager.startResearchWithPopup(player,"FLUX").
        if((aura.flux()>aura.vis()||aura.flux()>aura.base()/3F)&&!art.arcane.thaumcraft.research.ResearchRules.knows(PlayerProgressManager.get(player),"FLUX")&&art.arcane.thaumcraft.research.ResearchEvents.progress(player,"FLUX")){
            art.arcane.thaumcraft.research.ResearchNotifications.complete(player,"FLUX");art.arcane.thaumcraft.research.ResearchEvents.sync(player);
            player.sendOverlayMessage(Component.translatable("research.FLUX.warn").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public void onUseTick(Level level, net.minecraft.world.entity.LivingEntity entity, ItemStack stack, int ticksRemaining) {
        // Refresh only visual feedback: the server records the scan once in use().
        int elapsed = getUseDuration(stack, entity) - ticksRemaining;
        if (level.isClientSide() && entity instanceof Player player && elapsed > 0 && elapsed % 15 == 0)
            emitScanRunes(level, player);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) { return 72000; }

    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) { return net.minecraft.world.item.ItemUseAnimation.NONE; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        if (level.isClientSide()) {
            emitScanRunes(level,player);
            return InteractionResult.CONSUME;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        // TC6 ItemThaumometer.doScan: pointed entity within 9 blocks, else the block in reach, else the sky.
        var start=player.getEyePosition(); var direction=player.getViewVector(0).scale(9);
        HitResult hit = player.pick(player.blockInteractionRange() + 0.5, 0.0F, true);
        double distance=Math.min(81,hit.getType()==HitResult.Type.MISS ? 81 : start.distanceToSqr(hit.getLocation()));
        var entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player,start,start.add(direction),
                player.getBoundingBox().expandTowards(direction).inflate(1),entity -> entity.isAlive() && !entity.isSpectator(),distance);
        if (entityHit != null) art.arcane.thaumcraft.scan.ScanManager.scan(serverPlayer, entityHit.getEntity());
        else if (hit instanceof BlockHitResult blockHit && hit.getType()==HitResult.Type.BLOCK) art.arcane.thaumcraft.scan.ScanManager.scan(serverPlayer, blockHit.getBlockPos());
        else art.arcane.thaumcraft.scan.ScanManager.scan(serverPlayer, null);
        serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(net.minecraft.core.Holder.direct(art.arcane.thaumcraft.registry.ModSounds.get("scan")),net.minecraft.sounds.SoundSource.PLAYERS,player.getX(),player.getY(),player.getZ(),0.5F,1.0F,level.getRandom().nextLong()));
        return InteractionResult.CONSUME;
    }
}


