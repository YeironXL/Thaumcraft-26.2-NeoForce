package art.arcane.thaumcraft.scan;
import java.util.*;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import art.arcane.thaumcraft.network.AspectSnapshotPayload;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
/** Builds the TC6 object-aspect table after recipes/tags load and sends it to clients. */
public final class DerivedAspectEvents{
    public static Map<String,Map<String,Integer>> rebuild(net.minecraft.server.MinecraftServer server){
        ObjectAspects.rebuild(server);return ObjectAspects.export();
    }
    public static void register(){
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event)->{ObjectAspects.rebuild(event.getServer());ScanManager.reset();});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event)->{
            if(event.getPlayer()==null||ObjectAspects.table().isEmpty())ObjectAspects.rebuild(event.getPlayerList().getServer());
            var payload=AspectSnapshotPayload.of(ObjectAspects.export());
            if(event.getPlayer()!=null)PacketDistributor.sendToPlayer(event.getPlayer(),payload);else for(var player:event.getPlayerList().getPlayers())PacketDistributor.sendToPlayer(player,payload);
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->ObjectAspects.clear());
    }
    private DerivedAspectEvents(){}
}
