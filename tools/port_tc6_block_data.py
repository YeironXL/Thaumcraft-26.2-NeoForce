"""Blockstates, loot tables and tags for the TC6 basic/world blocks."""
import json, os
A='src/main/resources/assets/thaumcraft'; D='src/main/resources/data'
def w(p,o):
    os.makedirs(os.path.dirname(p),exist_ok=True); json.dump(o,open(p,'w'),indent=2)
# --- stairs blockstates (vanilla generator layout)
def stairs(model):
    v={}
    rot={'east':0,'south':90,'west':180,'north':270}
    for f,y in rot.items():
        for half in ['bottom','top']:
            for shape in ['straight','inner_left','inner_right','outer_left','outer_right']:
                m=model+('_inner' if 'inner' in shape else '_outer' if 'outer' in shape else '')
                yy=y
                if half=='bottom' and shape.endswith('left'): yy=(y+270)%360
                if half=='top' and shape.endswith('right'): yy=(y+90)%360
                e={'model':'thaumcraft:block/'+m.replace('_inner','').replace('_outer','')}
                base=model
                if 'inner' in shape: e['model']=f'thaumcraft:block/{base.replace("_stairs","_inner_stairs")}'
                elif 'outer' in shape: e['model']=f'thaumcraft:block/{base.replace("_stairs","_outer_stairs")}'
                else: e['model']=f'thaumcraft:block/{base}'
                if half=='top': e['x']=180
                if yy: e['y']=yy
                if half=='top' or yy: e['uvlock']=True
                v[f'facing={f},half={half},shape={shape}']=e
    return {'variants':v}
for b,m in {'stairs_arcane':'arcane_stairs','stairs_arcane_brick':'arcane_brick_stairs','stairs_ancient':'ancient_stairs','stairs_greatwood':'greatwood_stairs','stairs_silverwood':'silverwood_stairs'}.items():
    w(f'{A}/blockstates/{b}.json',stairs(m))
for t in ['greatwood','silverwood']:
    m=f'thaumcraft:block/log_{t}__base'
    w(f'{A}/blockstates/log_{t}.json',{'variants':{'axis=y':{'model':m},'axis=z':{'model':m,'x':90},'axis=x':{'model':m,'x':90,'y':90}}})
# greatwood leaves are foliage tinted (TC6 ColorHandler); silverwood stays white
w(f'{A}/models/block/leaves_greatwood__base.json',{'parent':'minecraft:block/leaves','textures':{'all':'thaumcraft:block/leaves_greatwood'}})
w(f'{A}/models/block/leaves_silverwood__base.json',{'parent':'minecraft:block/leaves','textures':{'all':'thaumcraft:block/leaves_silverwood'}})
# --- loot
def self_drop(b): return {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'thaumcraft:'+b}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]}
silk={'condition':'minecraft:match_tool','predicate':{'predicates':{'minecraft:enchantments':[{'enchantments':'minecraft:silk_touch','levels':{'min':1}}]}}}
shears={'condition':'minecraft:match_tool','predicate':{'items':'minecraft:shears'}}
selfs=['stone_arcane','stone_arcane_brick','stone_ancient','stone_ancient_tile','stone_ancient_glyphed','stone_eldritch_tile','matrix_speed','matrix_cost',
 'plank_greatwood','plank_silverwood','log_greatwood','log_silverwood','sapling_greatwood','sapling_silverwood','cinderpearl','shimmerleaf','vishroom',
 'ore_cinnabar','amber_block','amber_brick','metal_brass','metal_thaumium','metal_void','metal_alchemical','metal_alchemical_advanced','table_wood','table_stone',
 'stairs_arcane','stairs_arcane_brick','stairs_ancient','stairs_greatwood','stairs_silverwood']+['candle_'+c for c in 'white orange magenta lightblue yellow lime pink gray silver cyan purple blue brown green red black'.split()]
for b in selfs: w(f'{D}/thaumcraft/loot_table/blocks/{b}.json',self_drop(b))
for s in ['arcane_stone','arcane_brick','ancient','eldritch','greatwood','silverwood']:
    w(f'{D}/thaumcraft/loot_table/blocks/slab_{s}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'thaumcraft:slab_{s}','functions':[{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':f'thaumcraft:slab_{s}','properties':{'type':'double'}}]},{'function':'minecraft:explosion_decay'}]}]}]})
    w(f'{D}/thaumcraft/loot_table/blocks/slab_double_{s}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'thaumcraft:slab_{s}','functions':[{'function':'minecraft:set_count','count':2}]}]}]})
def ore(b,item,cnt,curio=False):
    alts=[{'type':'minecraft:item','name':'thaumcraft:'+b,'conditions':[silk]}]
    if curio: alts.append({'type':'minecraft:item','name':'thaumcraft:curio_preserved','conditions':[{'condition':'minecraft:random_chance','chance':0.066}]})
    fn=[{'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:ore_drops'},{'function':'minecraft:explosion_decay'}]
    if cnt!=1: fn.insert(0,{'function':'minecraft:set_count','count':cnt})
    alts.append({'type':'minecraft:item','name':item,'functions':fn})
    return {'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':alts}]}]}
w(f'{D}/thaumcraft/loot_table/blocks/ore_amber.json',ore('ore_amber','thaumcraft:amber',{'type':'minecraft:uniform','min':1,'max':2},True))
w(f'{D}/thaumcraft/loot_table/blocks/ore_quartz.json',ore('ore_quartz','minecraft:quartz',1))
def leaves(b,sap,extra):
    pools=[{'rolls':1,'entries':[{'type':'minecraft:alternatives','children':[
        {'type':'minecraft:item','name':'thaumcraft:'+b,'conditions':[{'condition':'minecraft:any_of','terms':[shears,silk]}]},
        {'type':'minecraft:item','name':'thaumcraft:'+sap,'conditions':[{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[1/75,1/60,1/50,1/40,1/30]}]}]}]}]
    if extra: pools.append({'rolls':1,'entries':[{'type':'minecraft:item','name':'thaumcraft:nugget_quicksilver'}],'conditions':[{'condition':'minecraft:inverted','term':{'condition':'minecraft:any_of','terms':[shears,silk]}},{'condition':'minecraft:survives_explosion'},{'condition':'minecraft:table_bonus','enchantment':'minecraft:fortune','chances':[1/150,1/135,1/120,1/90]}]})
    return {'type':'minecraft:block','pools':pools}
w(f'{D}/thaumcraft/loot_table/blocks/leaves_greatwood.json',leaves('leaves_greatwood','sapling_greatwood',False))
w(f'{D}/thaumcraft/loot_table/blocks/leaves_silverwood.json',leaves('leaves_silverwood','sapling_silverwood',True))
# --- tags
def tag(path,vals):
    p=f'{D}/{path}.json'; old=json.load(open(p))['values'] if os.path.exists(p) else []
    w(p,{'replace':False,'values':sorted(set(old)|{'thaumcraft:'+v for v in vals})})
stones=['stone_arcane','stone_arcane_brick','stone_ancient','stone_ancient_tile','stone_ancient_glyphed','stone_ancient_rock','stone_ancient_doorway','stone_eldritch_tile','stone_porous','matrix_speed','matrix_cost','stairs_arcane','stairs_arcane_brick','stairs_ancient','slab_arcane_stone','slab_arcane_brick','slab_ancient','slab_eldritch','slab_double_arcane_stone','slab_double_arcane_brick','slab_double_ancient','slab_double_eldritch','ore_amber','ore_cinnabar','ore_quartz','table_stone','metal_brass','metal_thaumium','metal_void','metal_alchemical','metal_alchemical_advanced']
woods=['plank_greatwood','plank_silverwood','log_greatwood','log_silverwood','stairs_greatwood','stairs_silverwood','slab_greatwood','slab_silverwood','slab_double_greatwood','slab_double_silverwood','table_wood']
tag('minecraft/tags/block/mineable/pickaxe',stones); tag('minecraft/tags/block/mineable/axe',woods)
tag('minecraft/tags/block/mineable/hoe',['leaves_greatwood','leaves_silverwood'])
for kind in ['block','item']:
    tag(f'minecraft/tags/{kind}/logs',['log_greatwood','log_silverwood']); tag(f'minecraft/tags/{kind}/logs_that_burn',['log_greatwood','log_silverwood'])
    tag(f'minecraft/tags/{kind}/planks',['plank_greatwood','plank_silverwood']); tag(f'minecraft/tags/{kind}/leaves',['leaves_greatwood','leaves_silverwood'])
    tag(f'minecraft/tags/{kind}/saplings',['sapling_greatwood','sapling_silverwood'])
    tag(f'minecraft/tags/{kind}/wooden_stairs',['stairs_greatwood','stairs_silverwood']); tag(f'minecraft/tags/{kind}/wooden_slabs',['slab_greatwood','slab_silverwood'])
    tag(f'minecraft/tags/{kind}/stairs',['stairs_arcane','stairs_arcane_brick','stairs_ancient']); tag(f'minecraft/tags/{kind}/slabs',['slab_arcane_stone','slab_arcane_brick','slab_ancient','slab_eldritch'])
tag('minecraft/tags/block/beacon_base_blocks',['metal_brass','metal_thaumium','metal_void','metal_alchemical','metal_alchemical_advanced','stone_arcane','stone_arcane_brick','stone_ancient','stone_ancient_tile','stone_ancient_glyphed','stone_ancient_rock','stone_ancient_doorway','stone_eldritch_tile','matrix_speed','matrix_cost','amber_block','amber_brick'])
tag('c/tags/block/ores',['ore_amber','ore_cinnabar','ore_quartz']); tag('c/tags/item/ores',['ore_amber','ore_cinnabar','ore_quartz'])
print('ok')
