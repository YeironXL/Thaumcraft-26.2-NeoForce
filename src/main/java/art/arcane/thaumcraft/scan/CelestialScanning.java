package art.arcane.thaumcraft.scan;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.research.ResearchRules;
import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;

/** TC6 ScanSky targeting and daily notes, adapted to the native overworld clock. */
public final class CelestialScanning {
    public static int target(long clock, float yaw, float pitch, net.minecraft.core.Direction facing) {
        if (pitch > 0) return -1;
        double fraction = Math.floorMod(clock,24000L)/24000D-.25;
        if(fraction<0)fraction++;
        double eased=1-(Math.cos(fraction*Math.PI)+1)/2;
        int angle=(int)((fraction+(eased-fraction)/3+.25)*360)%360;
        boolean night=angle>180;if(night)angle-=180;
        int y=(int)(yaw+90)%360,p=(int)Math.abs(pitch);
        boolean aligned=angle>90?Math.abs(Math.abs(y)-180)<10&&Math.abs(180-angle-p)<7:Math.abs(y)<10&&Math.abs(angle-p)<7;
        if(aligned)return night?5+(int)Math.floorMod(clock/24000L,8):0;
        if(!night)return -1;
        return switch(facing){case NORTH->1;case SOUTH->2;case WEST->3;case EAST->4;default->-1;};
    }
    /** TC6 ScanSky.checkThing. */
    public static boolean scannable(ServerPlayer player) {
        var world=player.level();var research=PlayerProgressManager.get(player);
        if(player.getXRot()>0||!world.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||!world.canSeeSky(player.blockPosition().above())||!ResearchRules.complete(research,"CELESTIALSCANNING"))return false;
        return target(world.getOverworldClockTime(),player.getYRot(),player.getXRot(),player.getDirection())>=0;
    }
    public static boolean scan(ServerPlayer player) {
        var world=player.level();var research=PlayerProgressManager.get(player);
        if(!scannable(player))return false;
        int type=target(world.getOverworldClockTime(),player.getYRot(),player.getXRot(),player.getDirection());
        String prefix="CEL_"+world.getGameTime()/24000L+"_";
        String key=prefix+(type==0?"Sun":type<5?"Star"+(type-1):"Moon"+(type-5));
        if(research.has(key)){if(player.connection!=null)player.sendOverlayMessage(Component.translatable("tc.celestial.fail.1"));return true;}
        int paper=-1;boolean tools=false;
        for(int i=0;i<player.getInventory().getContainerSize();i++){
            var stack=player.getInventory().getItem(i);
            if(stack.is(ModItems.SCRIBING_TOOLS.get()))tools=true;
            if(stack.is(Items.PAPER)&&paper<0)paper=i;
        }
        if(!tools||paper<0){if(player.connection!=null)player.sendOverlayMessage(Component.translatable("tc.celestial.fail.2"));return true;}
        player.getInventory().getItem(paper).shrink(1);
        var note=new ItemStack(ModItems.CELESTIAL_NOTES.get());note.set(ModComponents.CELESTIAL_NOTE.get(),type);
        String variant=type==0?"sun":type<5?"stars_"+type:"moon_"+(type-4);
        note.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,new net.minecraft.world.item.component.CustomModelData(java.util.List.of(),java.util.List.of(),java.util.List.of(variant),java.util.List.of()));
        if(!player.getInventory().add(note))player.drop(note,false);
        research.unlock(key);research.clearOldCelestial(prefix);return true;
    }
    private CelestialScanning(){}
}

