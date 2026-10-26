"""Map TC6 field references (ItemsTC.x / BlocksTC.x, metas) and 1.12 vanilla/oredict to port ids."""
import re,glob,os
REF=os.path.join(os.environ.get('TC6_REF','../TC6-ref'),'src/main/java/thaumcraft/')
cls_src={}
for p in glob.glob(REF+'**/*.java',recursive=True): cls_src[os.path.basename(p)[:-5]]=open(p,encoding='utf-8',errors='replace').read()
def class_name(cls):
    s=cls_src.get(cls,'')
    m=re.search(r'super\(\s*"([a-z0-9_]+)"',s) or re.search(r'setRegistryName\(\s*"(?:thaumcraft",\s*")?([a-z0-9_]+)"',s) or re.search(r'this\(\s*"([a-z0-9_]+)"',s)
    return m.group(1) if m else None
FIELDS={};VARIANTS={}
for f in ('common/config/ConfigItems.java','common/config/ConfigBlocks.java'):
    for line in open(REF+f).read().split('\n'):
        m=re.search(r'(ItemsTC|BlocksTC)\.(\w+) = .*?new (\w+)\((.*)',line)
        if not m: continue
        kind,field,cls,rest=m.groups()
        strs=re.findall(r'"([a-z0-9_]+)"',rest.split('))')[0]+')')
        if strs:
            FIELDS[field]=strs[0]
            if cls=='ItemTCBase' and len(strs)>1: VARIANTS[field]=[f'{strs[0]}_{v}' for v in strs[1:]]
        else:
            n=class_name(cls)
            if n: FIELDS[field]=n
        if cls not in ('ItemTCBase',) and not strs:
            s=cls_src.get(cls,'');mv=re.search(r'super\(\s*"([a-z0-9_]+)"\s*,\s*((?:"[a-z0-9_]+"\s*,?\s*)+)\)',s)
            if mv: VARIANTS[field]=[f'{mv.group(1)}_{v}' for v in re.findall(r'"([a-z0-9_]+)"',mv.group(2))]
VARIANTS['phial']=['phial_empty','phial_filled']
VARIANTS['label']=['label_blank','label_filled']
VARIANTS['baubles']=['baubles_amulet_mundane','baubles_ring_mundane','baubles_girdle_mundane','baubles_ring_apprentice','baubles_amulet_fancy','baubles_ring_fancy','baubles_girdle_fancy']
VARIANTS['chunks']=['chunk_beef','chunk_chicken','chunk_pork','chunk_fish','chunk_rabbit','chunk_mutton']
FIELDS.update(dict(crystalEssence='crystal_essence',thaumonomicon='thaumonomicon',salisMundus='salis_mundus',jarBrace='jar_brace',
    tripleMeatTreat='triple_meat_treat',brain='brain',bottleTaint='bottle_taint',sanitySoap='sanity_soap',bathSalts='bath_salts',
    resonator='resonator',sanityChecker='sanity_checker',handMirror='hand_mirror',grappleGun='grapple_gun',goggles='goggles',
    focusPouch='focus_pouch',scribingTools='scribing_tools',thaumometer='thaumometer',alumentum='alumentum',celestialNotes='celestial_notes',
    travellerBoots='traveller_boots',amuletVis='amulet_vis',charmVerdant='verdant_charm',bandCuriosity='curiosity_band',ringCloud='cloud_ring',
    charmUndying='charm_undying',charmVoidseer='voidseer_charm',causalityCollapser='causality_collapser',turretPlacer='turret_placer',
    golemBell='golem_bell',golemPlacer='golem_placer',seals='seal_blank',primordialPearl='primordial_pearl',pechWand='pech_wand',curio='curio',lootBag='loot_bag',
    arcaneWorkbench='arcane_workbench',arcaneWorkbenchCharger='arcane_workbench_charger',dioptra='dioptra',researchTable='research_table',crucible='crucible',
    arcaneEarToggle='arcane_ear_toggle',levitator='levitator',centrifuge='centrifuge',bellows='bellows',smelterAux='smelter_aux',smelterVent='smelter_vent',
    alembic='alembic',rechargePedestal='recharge_pedestal',wandWorkbench='wand_workbench',hungryChest='hungry_chest',infusionMatrix='infusion_matrix',
    infernalFurnace='infernal_furnace',everfullUrn='everfull_urn',thaumatorium='thaumatorium',thaumatoriumTop='thaumatorium_top',brainBox='brain_box',spa='spa',
    golemBuilder='golem_builder',redstoneRelay='redstone_relay',patternCrafter='pattern_crafter',potionSprayer='potion_sprayer',stabilizer='stabilizer',
    visGenerator='vis_generator',condenser='condenser',condenserlattice='condenser_lattice',condenserlatticeDirty='condenser_lattice_dirty',voidSiphon='void_siphon',
    visBattery='vis_battery',inlay='inlay',fleshBlock='flesh_block',amberBlock='amber_block',amberBrick='amber_brick',shimmerleaf='shimmerleaf',cinderpearl='cinderpearl',
    vishroom='vishroom',stonePorous='stone_porous',taintFeature='taint_feature',taintLog='taint_log',barrier='barrier',hole='hole',activatorRail='activator_rail',plankGreatwood='plank_greatwood',plankSilverwood='plank_silverwood',
    slabGreatwood='slab_greatwood',slabSilverwood='slab_silverwood',slabArcaneStone='slab_arcane_stone',slabArcaneBrick='slab_arcane_brick',slabAncient='slab_ancient',slabEldritch='slab_eldritch',
    metalBlockBrass='metal_brass',metalBlockThaumium='metal_thaumium',metalBlockVoid='metal_void',fluxGoo='flux_goo',liquidDeath='liquid_death',purifyingFluid='purifying_fluid'))
COLORS=['white','orange','magenta','lightblue','yellow','lime','pink','gray','silver','cyan','purple','blue','brown','green','red','black']
MODERN_COLOR={'lightblue':'light_blue','silver':'light_gray'}
DYE_ORE={'dyeBlack':'black','dyeRed':'red','dyeGreen':'green','dyeBrown':'brown','dyeBlue':'blue','dyePurple':'purple','dyeCyan':'cyan','dyeLightGray':'light_gray',
 'dyeGray':'gray','dyePink':'pink','dyeLime':'lime','dyeYellow':'yellow','dyeLightBlue':'light_blue','dyeMagenta':'magenta','dyeOrange':'orange','dyeWhite':'white'}
ORE={'ingotIron':'#c:ingots/iron','ingotGold':'#c:ingots/gold','ingotBrass':'thaumcraft:ingot_brass','ingotThaumium':'thaumcraft:ingot_thaumium','ingotVoid':'thaumcraft:ingot_void',
 'nuggetIron':'#c:nuggets/iron','nuggetGold':'#c:nuggets/gold','nuggetBrass':'thaumcraft:nugget_brass','nuggetThaumium':'thaumcraft:nugget_thaumium','nuggetVoid':'thaumcraft:nugget_void',
 'nuggetQuicksilver':'thaumcraft:nugget_quicksilver','nuggetQuartz':'thaumcraft:nugget_quartz','nuggetMeat':'#thaumcraft:meat_nuggets',
 'plateIron':'#c:plates/iron','plateBrass':'#c:plates/brass','plateThaumium':'#c:plates/thaumium','plateVoid':'#c:plates/void',
 'gemDiamond':'#c:gems/diamond','gemQuartz':'#c:gems/quartz','gemEmerald':'#c:gems/emerald','gemAmber':'thaumcraft:amber','gemLapis':'#c:gems/lapis',
 'dustRedstone':'#c:dusts/redstone','dustGlowstone':'#c:dusts/glowstone','stickWood':'#c:rods/wooden','plankWood':'#minecraft:planks','logWood':'#minecraft:logs',
 'slabWood':'#minecraft:wooden_slabs','stone':'#c:stones','cobblestone':'#c:cobblestones','blockGlass':'#c:glass_blocks','paneGlass':'#c:glass_panes',
 'string':'#c:strings','leather':'#c:leathers','slimeball':'#c:slime_balls','feather':'#c:feathers','gunpowder':'#c:gunpowders','enderpearl':'#c:ender_pearls',
 'trapdoorWood':'#minecraft:wooden_trapdoors','chestWood':'#c:chests/wooden','blockIron':'#c:storage_blocks/iron','blockGold':'#c:storage_blocks/gold',
 'bookshelf':'#c:bookshelves','workbench':'minecraft:crafting_table','obsidian':'#c:obsidians','sand':'#c:sands','dirt':'minecraft:dirt','torch':'minecraft:torch',
 'cropWheat':'#c:crops/wheat','egg':'#c:eggs','bone':'#c:bones','blockRedstone':'#c:storage_blocks/redstone','blockLapis':'#c:storage_blocks/lapis','blockDiamond':'#c:storage_blocks/diamond',
 'oreIron':'#c:ores/iron','oreGold':'#c:ores/gold','oreCinnabar':'thaumcraft:ore_cinnabar','oreCopper':'#c:ores/copper','oreTin':'#c:ores/tin','oreSilver':'#c:ores/silver','oreLead':'#c:ores/lead',
 'quicksilver':'thaumcraft:quicksilver','nitor':'#thaumcraft:nitor','dustSalt':'#c:dusts/salt','listAllseed':'#c:seeds','treeSapling':'#minecraft:saplings','treeLeaves':'#minecraft:leaves','vine':'minecraft:vine',
 'blockWool':'#minecraft:wool','paper':'minecraft:paper','ingotCopper':'#c:ingots/copper'}
for o,c in DYE_ORE.items(): ORE[o]=f'#c:dyes/{c}'
VAN_RENAME={'golden_apple':'golden_apple','wool':'#minecraft:wool','planks':'#minecraft:planks','log':'#minecraft:logs','stone_slab':'minecraft:smooth_stone_slab',
 'stonebrick':'minecraft:stone_bricks','quartz_block':'minecraft:quartz_block','iron_bars':'minecraft:iron_bars','redstone_torch':'minecraft:redstone_torch',
 'brewing_stand':'minecraft:brewing_stand','web':'minecraft:cobweb','reeds':'minecraft:sugar_cane','fish':'#minecraft:fishes','flower_pot':'minecraft:flower_pot',
 'skull':'#minecraft:skulls','sapling':'#minecraft:saplings','leaves':'#minecraft:leaves','glass_pane':'minecraft:glass_pane','dye':None,'coal':'minecraft:coal',
 'speckled_melon':'minecraft:glistering_melon_slice','fireworks':'minecraft:firework_rocket','boat':'#minecraft:boats','trapdoor':'minecraft:oak_trapdoor',
 'wooden_pressure_plate':'minecraft:oak_pressure_plate','sign':'minecraft:oak_sign','golden_rail':'minecraft:powered_rail','noteblock':'minecraft:note_block',
 'lit_pumpkin':'minecraft:jack_o_lantern','melon_block':'minecraft:melon','red_flower':'minecraft:poppy','yellow_flower':'minecraft:dandelion','tallgrass':'minecraft:short_grass',
 'deadbush':'minecraft:dead_bush','end_bricks':'minecraft:end_stone_bricks','magma':'minecraft:magma_block','nether_brick':'minecraft:nether_bricks','netherbrick':'minecraft:nether_brick',
 'lever':'minecraft:lever','stone_button':'minecraft:stone_button','wooden_button':'minecraft:oak_button','fence':'minecraft:oak_fence','fence_gate':'minecraft:oak_fence_gate',
 'cauldron':'minecraft:cauldron','piston':'minecraft:piston','sticky_piston':'minecraft:sticky_piston','hopper':'minecraft:hopper','dispenser':'minecraft:dispenser','dropper':'minecraft:dropper',
 'ender_eye':'minecraft:ender_eye','repeater':'minecraft:repeater','comparator':'minecraft:comparator','daylight_detector':'minecraft:daylight_detector','record_13':'minecraft:music_disc_13',
 'water_bucket':'minecraft:water_bucket','lava_bucket':'minecraft:lava_bucket','bucket':'minecraft:bucket','map':'minecraft:map','filled_map':'minecraft:filled_map'}
def vanilla(kind,const,meta=None):
    n=const.lower()
    if n=='dye' and meta is not None: return 'minecraft:'+['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal'][meta]
    if n=='coal' and meta==1: return 'minecraft:charcoal'
    if n=='wool' and meta not in (None,32767,0): return 'minecraft:'+['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black'][meta]+'_wool'
    if n=='skull' and meta is not None and meta!=32767: return 'minecraft:'+['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','dragon_head'][meta]
    if n=='stone' and meta not in (None,0,32767): return 'minecraft:'+['stone','granite','polished_granite','diorite','polished_diorite','andesite','polished_andesite'][meta]
    if n=='golden_apple' and meta==1: return 'minecraft:enchanted_golden_apple'
    if n=='fish' and meta is not None and meta!=32767: return 'minecraft:'+['cod','salmon','tropical_fish','pufferfish'][meta]
    if n in VAN_RENAME and VAN_RENAME[n]: return VAN_RENAME[n]
    return 'minecraft:'+n
def tc(field,meta=None,color=None):
    if color is not None:
        base={'nitor':'nitor','candles':'candle','banners':'banner'}[field]
        return f'thaumcraft:{base}_{color}'
    if field in VARIANTS and field not in ('thaumonomicon','celestialNotes','seals','turretPlacer','amuletVis') and meta!=32767: return 'thaumcraft:'+VARIANTS[field][meta or 0]
    if field=='crystalEssence': return 'thaumcraft:crystal_essence'
    if field in FIELDS: return 'thaumcraft:'+FIELDS[field]
    raise KeyError(field)
