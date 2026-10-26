"""Regenerate every non-infusion TC6 BETA26 recipe from the original ConfigRecipes.java.

Usage: TC6_REF=<path to TheDarkTower314/Thaumcraft-6-Source-Code> python3 tools/port_tc6_recipes.py
Recipe ids are the original ResourceLocation paths in lower case, so the Thaumonomicon recipe lists
(research JSON "recipes") resolve directly. Book-only "fake" recipes go to assets/thaumcraft/book_recipes,
recipe groups to assets/thaumcraft/book_groups.json.
"""
import json,os,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent/'tc6ref'))
import convert_recipes as C, refmap
root=Path(__file__).resolve().parents[1]
data=root/'src/main/resources/data/thaumcraft/recipe'
book=root/'src/main/resources/assets/thaumcraft/book_recipes'
book.mkdir(parents=True,exist_ok=True)
GENERATED_MARK=root/'tools/tc6ref/generated_recipes.txt'
# Recipes implemented by custom serializers in the port (special NBT/stateful behaviour) keep their files.
CUSTOM={'salismundus':'salis_mundus','scribingtoolscraft1':'scribing_tools','scribingtoolscraft2':'scribing_tools','scribingtoolsrefill':'scribing_tools_refill'}
SKIP_TYPES={'thaumcraft:infusion'}
def fix(name,d):
    d=json.loads(json.dumps(d))
    if d['type']=='thaumcraft:crucible' and isinstance(d['catalyst'],dict):
        c=d['catalyst'];comp=c['components']
        d['catalyst']=c['items']
        d['catalyst_aspect']=comp.get('thaumcraft:aspect') or next(iter(comp['thaumcraft:essentia']))
    return d
def comp_ingredient(item,comps):
    return {'neoforge:ingredient_type':'neoforge:components','items':item,'components':comps,'strict':False}
recipes={};groups={}
for name,d in C.OUT.items():
    if d['type'] in SKIP_TYPES: continue
    if name.startswith('smelt:'):
        rid='smelting_'+d['result']['id'].split(':')[1]+'_from_'+d['ingredient'].split(':')[1].replace('/','_')
        recipes[rid]={'type':'minecraft:smelting','ingredient':d['ingredient'],'result':d['result'],'experience':d['experience'],'cookingtime':200}
        continue
    rid=name.split(':')[1].lower()
    grp=d.pop('_group',None);fake=d.pop('_fake',False)
    d=fix(rid,d)
    if grp: groups.setdefault(grp.split(':')[1].lower(),[]).append(rid)
    if fake: (book/f'{rid}.json').write_text(json.dumps(d,indent=2)+'\n');continue
    if rid in CUSTOM: continue
    recipes[rid]=d
# Loops in ConfigRecipes, expanded exactly.
COL=refmap.COLORS
for c in COL:
    m=refmap.MODERN_COLOR.get(c,c)
    recipes[f'nitordye{c}']={'type':'minecraft:crafting_shapeless','ingredients':[f'#c:dyes/{m}','#thaumcraft:nitor'],'result':{'id':f'thaumcraft:nitor_{c}'}}
    groups.setdefault('nitorgroup',[]).append(f'nitordye{c}')
    recipes[f'tallowcandle{c}']={'type':'minecraft:crafting_shapeless','ingredients':[f'#c:dyes/{m}','#thaumcraft:candles'],'result':{'id':f'thaumcraft:candle_{c}'}}
    groups.setdefault('tallowcandles',[]).append(f'tallowcandle{c}')
    recipes[f'banner{c}']={'type':'thaumcraft:arcane','research':'BASEINFUSION','vis':10,'crystals':{},'pattern':['WS','WS','WB'],
        'key':{'W':f'minecraft:{m}_wool','S':'#c:rods/wooden','B':'#minecraft:wooden_slabs'},'result':{'id':f'thaumcraft:banner_{c}'}}
    groups.setdefault('banners',[]).append(f'banner{c}')
groups.setdefault('nitorgroup',[]).insert(0,'nitor')
ASPECTS=sorted(set(C.ASP.values()))
for a in ASPECTS:
    recipes[f'vis_crystal_{a}']={'type':'thaumcraft:crucible','research':'BASEALCHEMY','catalyst':'thaumcraft:nugget_quartz','aspects':{a:2},
        'result':{'id':'thaumcraft:crystal_essence','components':{'thaumcraft:aspect':a}}}
    groups.setdefault('viscrystalgroup',[]).append(f'vis_crystal_{a}')
    recipes[f'label_{a}']={'type':'thaumcraft:jar_label','aspect':a}
    groups.setdefault('jarlabels',[]).append(f'label_{a}')
recipes['jarvoid']={'type':'thaumcraft:arcane','research':'WARDEDJARS','vis':50,'crystals':{'perditio':1},'pattern':['J'],
    'key':{'J':'thaumcraft:jar_normal'},'result':{'id':'thaumcraft:jar_void'},'keep_components':True}
# ConfigRecipes.postAspects: hedge alchemy costs equal the output's object aspects (minus the catalyst's for HEDGEALCHEMY@3).
def hedge(name,research,out,count,catalyst,of,minus=None):
    d={'type':'thaumcraft:crucible','research':research,'catalyst':catalyst,'aspects_of':of,'result':{'id':out}}
    if count!=1:d['result']['count']=count
    if minus:d['aspects_minus']=minus
    recipes[name]=d
hedge('hedge_gunpowder','HEDGEALCHEMY@2','minecraft:gunpowder',2,'minecraft:gunpowder','minecraft:gunpowder')
hedge('hedge_slime','HEDGEALCHEMY@2','minecraft:slime_ball',2,'minecraft:slime_ball','minecraft:slime_ball')
hedge('hedge_glowstone','HEDGEALCHEMY@2','minecraft:glowstone_dust',2,'#c:dusts/glowstone','minecraft:glowstone_dust')
hedge('hedge_dye','HEDGEALCHEMY@2','minecraft:ink_sac',2,'minecraft:ink_sac','minecraft:ink_sac')
hedge('hedge_clay','HEDGEALCHEMY@3','minecraft:clay_ball',1,'minecraft:dirt','minecraft:clay_ball','minecraft:dirt')
hedge('hedge_string','HEDGEALCHEMY@3','minecraft:string',1,'minecraft:wheat','minecraft:string','minecraft:wheat')
hedge('hedge_web','HEDGEALCHEMY@3','minecraft:cobweb',1,'minecraft:string','minecraft:cobweb','minecraft:string')
recipes['hedge_lava']={'type':'thaumcraft:crucible','research':'HEDGEALCHEMY@3','catalyst':'minecraft:bucket','aspects':{'ignis':15,'terra':5},'result':{'id':'minecraft:lava_bucket'}}
# ModConfig.foundXOre: optional metal purification and their cluster smelting only when the ore tag has entries.
for metal in ('copper','tin','silver','lead'):
    cond=[{'type':'neoforge:not','value':{'type':'neoforge:tag_empty','tag':f'c:ores/{metal}'}}]
    rid=f'metal_purification_{metal}'
    if rid in recipes: recipes[rid]['neoforge:conditions']=cond
    else: recipes[rid]={'neoforge:conditions':cond,'type':'thaumcraft:crucible','research':'METALPURIFICATION','catalyst':f'#c:ores/{metal}','aspects':{'metallum':5,'ordo':5},'result':{'id':f'thaumcraft:cluster_{metal}'}}
# ModConfig ingotCopper handling (copper is vanilla now): nugget conversions and cluster smelting to two ingots.
recipes['coppertonuggets']={'type':'minecraft:crafting_shaped','pattern':['#'],'key':{'#':'minecraft:copper_ingot'},'result':{'id':'thaumcraft:nugget_copper','count':9}}
recipes['coppernuggetstoingot']={'type':'minecraft:crafting_shaped','pattern':['###','###','###'],'key':{'#':'thaumcraft:nugget_copper'},'result':{'id':'minecraft:copper_ingot'}}
recipes['smelting_copper_ingot_from_cluster_copper']={'type':'minecraft:smelting','ingredient':'thaumcraft:cluster_copper','result':{'id':'minecraft:copper_ingot','count':2},'experience':1.0,'cookingtime':200}
# Nitor tag and candle tag used by the dye recipes.
tags=root/'src/main/resources/data/thaumcraft/tags/item'
tags.mkdir(parents=True,exist_ok=True)
(tags/'nitor.json').write_text(json.dumps({'replace':False,'values':[f'thaumcraft:nitor_{c}' for c in COL]},indent=2)+'\n')
(tags/'candles.json').write_text(json.dumps({'replace':False,'values':[f'thaumcraft:candle_{c}' for c in COL]},indent=2)+'\n')
(tags/'meat_nuggets.json').write_text(json.dumps({'replace':False,'values':[f'thaumcraft:chunk_{m}' for m in ('beef','chicken','pork','fish','rabbit','mutton')]},indent=2)+'\n')
# Remove recipes generated by an earlier run that no longer exist, then write.
old=set(GENERATED_MARK.read_text().split()) if GENERATED_MARK.exists() else set()
for rid in old-set(recipes):
    p=data/f'{rid}.json'
    if p.exists(): p.unlink()
for rid,d in sorted(recipes.items()):
    (data/f'{rid}.json').write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
GENERATED_MARK.write_text('\n'.join(sorted(recipes))+'\n')
(root/'src/main/resources/assets/thaumcraft/book_groups.json').write_text(json.dumps(groups,indent=2,sort_keys=True)+'\n')
print(len(recipes),'recipes,',len(groups),'groups; unconverted statements:')
for s,e in C.ERR:
    if 'Seal' not in s and 'seal' not in e: print(' -',s[:110],e[:80])
