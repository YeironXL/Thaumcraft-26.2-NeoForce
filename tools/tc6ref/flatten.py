"""1.12.2 (Blocks/Items constant, metadata) -> modern item ids. 32767 = every variant."""
COLORS=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
WOODS=['oak','spruce','birch','jungle','acacia','dark_oak']
V={  # variant lists by metadata
 'WOOL':[f'{c}_wool' for c in COLORS],'STAINED_HARDENED_CLAY':[f'{c}_terracotta' for c in COLORS],'CONCRETE':[f'{c}_concrete' for c in COLORS],
 'CONCRETE_POWDER':[f'{c}_concrete_powder' for c in COLORS],'STAINED_GLASS':[f'{c}_stained_glass' for c in COLORS],'STAINED_GLASS_PANE':[f'{c}_stained_glass_pane' for c in COLORS],
 'CARPET':[f'{c}_carpet' for c in COLORS],'BED':[f'{c}_bed' for c in COLORS],'BANNER':[f'{c}_banner' for c in reversed(COLORS)],
 'PLANKS':[f'{w}_planks' for w in WOODS],'SAPLING':[f'{w}_sapling' for w in WOODS],'LOG':['oak_log','spruce_log','birch_log','jungle_log'],'LOG2':['acacia_log','dark_oak_log'],
 'LEAVES':['oak_leaves','spruce_leaves','birch_leaves','jungle_leaves'],'LEAVES2':['acacia_leaves','dark_oak_leaves'],'WOODEN_SLAB':[f'{w}_slab' for w in WOODS],
 'STONE':['stone','granite','polished_granite','diorite','polished_diorite','andesite','polished_andesite'],'DIRT':['dirt','coarse_dirt','podzol'],
 'SAND':['sand','red_sand'],'SANDSTONE':['sandstone','chiseled_sandstone','cut_sandstone'],'RED_SANDSTONE':['red_sandstone','chiseled_red_sandstone','cut_red_sandstone'],
 'STONEBRICK':['stone_bricks','mossy_stone_bricks','cracked_stone_bricks','chiseled_stone_bricks'],'SPONGE':['sponge','wet_sponge'],
 'DOUBLE_PLANT':['sunflower','lilac','tall_grass','large_fern','rose_bush','peony'],'TALLGRASS':['dead_bush','short_grass','fern'],
 'RED_FLOWER':['poppy','blue_orchid','allium','azure_bluet','red_tulip','orange_tulip','white_tulip','pink_tulip','oxeye_daisy'],
 'YELLOW_FLOWER':['dandelion'],'SKULL':['skeleton_skull','wither_skeleton_skull','zombie_head','player_head','creeper_head','dragon_head'],
 'DYE':['ink_sac','red_dye','green_dye','cocoa_beans','lapis_lazuli','purple_dye','cyan_dye','light_gray_dye','gray_dye','pink_dye','lime_dye','yellow_dye','light_blue_dye','magenta_dye','orange_dye','bone_meal'],
 'COAL':['coal','charcoal'],'GOLDEN_APPLE':['golden_apple','enchanted_golden_apple'],'FISH':['cod','salmon','tropical_fish','pufferfish'],'COOKED_FISH':['cooked_cod','cooked_salmon'],
 'QUARTZ_BLOCK':['quartz_block','chiseled_quartz_block','quartz_pillar'],'PRISMARINE':['prismarine','prismarine_bricks','dark_prismarine'],
 'COBBLESTONE_WALL':['cobblestone_wall','mossy_cobblestone_wall'],'ANVIL':['anvil','chipped_anvil','damaged_anvil'],
 'MONSTER_EGG':['infested_stone','infested_cobblestone','infested_stone_bricks','infested_mossy_stone_bricks','infested_cracked_stone_bricks','infested_chiseled_stone_bricks'],
 'STONE_SLAB':['smooth_stone_slab','sandstone_slab','petrified_oak_slab','cobblestone_slab','brick_slab','stone_brick_slab','nether_brick_slab','quartz_slab'],
}
R={ # renames for non-variant constants
 'GRASS':'grass_block','TALLGRASS':'short_grass','DEADBUSH':'dead_bush','WEB':'cobweb','WATERLILY':'lily_pad','REEDS':'sugar_cane','MOB_SPAWNER':'spawner',
 'HARDENED_CLAY':'terracotta','MAGMA':'magma_block','MELON_BLOCK':'melon','MELON':'melon_slice','SPECKLED_MELON':'glistering_melon_slice','NOTEBLOCK':'note_block',
 'TRAPDOOR':'oak_trapdoor','WOODEN_BUTTON':'oak_button','WOODEN_PRESSURE_PLATE':'oak_pressure_plate','GOLDEN_RAIL':'powered_rail','GRASS_PATH':'dirt_path',
 'LIT_REDSTONE_ORE':'redstone_ore','LIT_PUMPKIN':'jack_o_lantern','PORTAL':'nether_portal','BOAT':'oak_boat','OAK_DOOR':'oak_door','POTIONITEM':'potion',
 'FIREWORKS':'firework_rocket','FIREWORK_CHARGE':'firework_star','CHORUS_FRUIT_POPPED':'popped_chorus_fruit','SILVER_GLAZED_TERRACOTTA':'light_gray_glazed_terracotta',
 'SILVER_SHULKER_BOX':'light_gray_shulker_box','RECORD_11':'music_disc_11','RECORD_13':'music_disc_13','RECORD_BLOCKS':'music_disc_blocks','RECORD_CAT':'music_disc_cat',
 'RECORD_CHIRP':'music_disc_chirp','RECORD_FAR':'music_disc_far','RECORD_MALL':'music_disc_mall','RECORD_MELLOHI':'music_disc_mellohi','RECORD_STAL':'music_disc_stal',
 'RECORD_STRAD':'music_disc_strad','RECORD_WAIT':'music_disc_wait','RECORD_WARD':'music_disc_ward','NETHERBRICK':'nether_brick','NETHER_BRICK':'nether_bricks',
 'END_BRICKS':'end_stone_bricks','SIGN':'oak_sign','FENCE':'oak_fence','FENCE_GATE':'oak_fence_gate','RED_NETHER_BRICK':'red_nether_bricks','SLIME':'slime_block',
 'NETHER_WART_BLOCK':'nether_wart_block','BRICK_BLOCK':'bricks','STONE_STAIRS':'cobblestone_stairs','SNOW_LAYER':'snow','SNOW':'snow_block','PUMPKIN':'carved_pumpkin',
 'FIRE':None,'END_PORTAL':None,'TRIPWIRE':'string','CAKE':'cake','PISTON_HEAD':None,'REDSTONE_WIRE':'redstone','FARMLAND':'farmland','DOUBLE_STONE_SLAB':'smooth_stone',
 'PUMPKIN_STEM':None,'MELON_STEM':None,'BED':'red_bed','SKULL':'skeleton_skull','STONE_SLAB':'smooth_stone_slab','WOODEN_DOOR':'oak_door','STANDING_SIGN':'oak_sign',
}
def ids(const,meta=None):
    """Return the list of modern item ids for a 1.12 constant and metadata (None = no meta given = 0)."""
    if const in V:
        lst=V[const]
        if meta==32767: return ['minecraft:'+x for x in lst]
        m=meta or 0
        return ['minecraft:'+lst[m]] if m<len(lst) else []
    n=R.get(const,const.lower())
    if n is None: return []
    if const=='PUMPKIN': n='pumpkin'
    return ['minecraft:'+n]
