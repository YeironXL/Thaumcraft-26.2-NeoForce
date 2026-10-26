package art.arcane.thaumcraft.research;

import java.util.Map;

/** Explicit metadata migration. NBT-bearing references remain blocked until their components are mapped. */
public final class LegacyItemReferences {
    private static final String[] COLORS={"white","orange","magenta","lightblue","yellow","lime","pink","gray","silver","cyan","purple","blue","brown","green","red","black"};
    private static final Map<String,String[]> VARIANTS=Map.of(
            "thaumcraft:nitor",java.util.Arrays.stream(COLORS).map(c -> "thaumcraft:nitor_"+c).toArray(String[]::new),
            "thaumcraft:plate",new String[]{"thaumcraft:plate_brass","thaumcraft:plate_iron","thaumcraft:plate_thaumium","thaumcraft:plate_void"},
            "thaumcraft:ingot",new String[]{"thaumcraft:ingot_thaumium","thaumcraft:ingot_void","thaumcraft:ingot_brass"},
            "thaumcraft:nugget",new String[]{"thaumcraft:nugget_iron","thaumcraft:nugget_copper","thaumcraft:nugget_tin","thaumcraft:nugget_silver","thaumcraft:nugget_lead","thaumcraft:nugget_quicksilver","thaumcraft:nugget_thaumium","thaumcraft:nugget_void","thaumcraft:nugget_brass","thaumcraft:nugget_quartz","thaumcraft:nugget_rareearth"},
            "minecraft:stone",new String[]{"minecraft:stone","minecraft:granite","minecraft:polished_granite","minecraft:diorite","minecraft:polished_diorite","minecraft:andesite","minecraft:polished_andesite"});
    private static final Map<String,String> RENAMED=Map.of("minecraft:nether_brick","minecraft:nether_bricks",
            "minecraft:netherbrick","minecraft:nether_brick","minecraft:reeds","minecraft:sugar_cane",
            "minecraft:slime","minecraft:slime_block","minecraft:grass","minecraft:grass_block",
            "minecraft:brick_block","minecraft:bricks","minecraft:fireworks","minecraft:firework_rocket",
            "minecraft:hardened_clay","minecraft:terracotta","thaumcraft:seal","thaumcraft:seal_blank");
    public static String item(String reference) {
        String[] parts=reference.split(";",-1);
        if (parts.length>3 || !parts[0].matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return "";
        int metadata=0;
        try { if (parts.length>2) metadata=Integer.parseInt(parts[2]); }
        catch (NumberFormatException exception) { return ""; }
        var variants=VARIANTS.get(parts[0]);
        if (variants!=null) return metadata>=0 && metadata<variants.length ? variants[metadata] : "";
        if (metadata!=0 && metadata!=32767) return "";
        return RENAMED.getOrDefault(parts[0],parts[0]);
    }
    /** Map only the known TC6 single-aspect crystal/phial NBT to native components. */
    public static String requirementKey(String reference) {
        String[] parts=reference.split(";",4);
        if(parts.length<4)return item(reference);
        String nbt=parts[3].replaceAll("\\s+","");
        var match=java.util.regex.Pattern.compile("\\{Aspects:\\[\\{amount:(1|10),key:'([a-z]+)'\\}\\]\\}").matcher(nbt);
        if(!match.matches() || art.arcane.thaumcraft.aspect.AspectRegistry.get(match.group(2))==null)return "";
        String id;
        if(parts[0].equals("thaumcraft:crystal_essence") && parts[2].equals("0") && match.group(1).equals("1"))id="thaumcraft:crystal_essence";
        else if(parts[0].equals("thaumcraft:phial") && parts[2].equals("1") && match.group(1).equals("10"))id="thaumcraft:phial_filled";
        else return "";
        return id+"@"+match.group(2);
    }
    public static boolean crafted(PlayerResearch state,String reference) {
        String[] parts=reference.split(";",-1);
        var variants=VARIANTS.get(parts[0]);
        if (parts.length==3 && variants!=null && parts[2].equals("32767"))
            return java.util.Arrays.stream(variants).anyMatch(state::crafted);
        String resolved=item(reference);
        if (resolved.isEmpty()) return false;
        return state.crafted(resolved) || state.crafted(reference);
    }
    private LegacyItemReferences() {}
}
