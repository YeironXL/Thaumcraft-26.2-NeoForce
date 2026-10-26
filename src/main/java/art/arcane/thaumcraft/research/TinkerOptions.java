package art.arcane.thaumcraft.research;
import java.util.List;
import art.arcane.thaumcraft.scan.ScanRegistry;
public final class TinkerOptions {
    public static final List<String> IDS=List.of("thaumcraft:vis_resonator","thaumcraft:thaumometer","minecraft:anvil","minecraft:activator_rail","minecraft:dispenser","minecraft:dropper","minecraft:enchanting_table","minecraft:ender_chest","minecraft:jukebox","minecraft:daylight_detector","minecraft:piston","minecraft:hopper","minecraft:sticky_piston","minecraft:map","minecraft:compass","minecraft:tnt_minecart","minecraft:comparator","minecraft:clock");
    public static int minimum(int option){if(option<0||option>=IDS.size())return 0;long total=ScanRegistry.get(IDS.get(option)).values().stream().mapToLong(Integer::longValue).sum();return 2*(int)Math.sqrt(total);}
    private TinkerOptions(){}
}
