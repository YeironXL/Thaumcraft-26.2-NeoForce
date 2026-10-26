"""TC6 crucible recipes and basic material conversions from ConfigRecipes."""
import json
import re
import shutil
from pathlib import Path
project = Path(__file__).resolve().parents[1]
root = project / 'src/main/resources'
recipes = root / 'data/thaumcraft/recipe'

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

def crucible(name, research, catalyst, aspects, result):
    write(recipes / f'{name}.json', dict(type='thaumcraft:crucible',research=research,catalyst=catalyst,aspects=aspects,result=result))

registry = (project / 'src/main/java/art/arcane/thaumcraft/aspect/AspectRegistry.java').read_text()
for aspect in re.findall(r'define\("([a-z]+)"',registry):
    crucible(f'vis_crystal_{aspect}','BASEALCHEMY','thaumcraft:nugget_quartz',{aspect:2},
             dict(id='thaumcraft:crystal_essence',components={'thaumcraft:aspect':aspect}))
crucible('nitor','UNLOCKALCHEMY@3','minecraft:glowstone_dust',dict(potentia=10,ignis=10,lux=10),dict(id='thaumcraft:nitor_yellow'))
crucible('alumentum','ALUMENTUM',['minecraft:coal','minecraft:charcoal'],dict(potentia=10,ignis=10,perditio=5),dict(id='thaumcraft:alumentum'))
crucible('brassingot','METALLURGY@1','minecraft:iron_ingot',dict(instrumentum=5),dict(id='thaumcraft:ingot_brass'))
crucible('thaumiumingot','METALLURGY@2','minecraft:iron_ingot',dict(praecantatio=5,terra=5),dict(id='thaumcraft:ingot_thaumium'))
crucible('voidingot','BASEELDRITCH','thaumcraft:void_seed',dict(metallum=10,vitium=5),dict(id='thaumcraft:ingot_void'))
crucible('hedge_tallow','HEDGEALCHEMY@1','minecraft:rotten_flesh',dict(ignis=1),dict(id='thaumcraft:tallow'))
crucible('hedge_leather','HEDGEALCHEMY@1','minecraft:rotten_flesh',dict(aer=3,bestia=3),dict(id='minecraft:leather'))

def shaped(name, pattern, key, result, count=1):
    write(recipes / f'{name}.json',dict(type='minecraft:crafting_shaped',pattern=pattern,key=key,result=dict(id=result,count=count)))

for metal in ('iron','brass','thaumium','void'):
    ingot = 'minecraft:iron_ingot' if metal == 'iron' else f'thaumcraft:ingot_{metal}'
    shaped(f'{metal}plate',['BBB'],dict(B=ingot),f'thaumcraft:plate_{metal}',3)
for metal in ('quartz','brass','thaumium','void','quicksilver'):
    ingot = dict(quartz='minecraft:quartz',quicksilver='thaumcraft:quicksilver').get(metal,f'thaumcraft:ingot_{metal}')
    nugget = f'thaumcraft:nugget_{metal}'
    shaped(f'{metal}tonuggets',['#'],{'#':ingot},nugget,9)
    shaped(f'nuggetsto{metal}',['###','###','###'],{'#':nugget},ingot)

assets = root / 'assets/thaumcraft'
base = json.loads((assets / 'models/block/crucible.json').read_text())
variants = {'water=0':dict(model='thaumcraft:block/crucible')}
for water in range(1,5):
    model = json.loads(json.dumps(base))
    model['textures']['water']='minecraft:block/water_still'
    y=4.8+2*water
    model['elements'].append({'from':[2,y,2],'to':[14,y,14],
       'faces':{'up':{'texture':'#water','uv':[2,2,14,14],'tintindex':0}}})
    write(assets / f'models/block/crucible_water_{water}.json',model)
    variants[f'water={water}']=dict(model=f'thaumcraft:block/crucible_water_{water}')
write(assets / 'blockstates/crucible.json',dict(variants=variants))

# Nitor is a light without collision; the retained sprite is displayed in three crossed planes.
faces = {name:dict(texture='#core') for name in ('up','down','north','south','west','east')}
write(assets / 'models/block/nitor_native.json',dict(ambientocclusion=False,
    textures=dict(core='thaumcraft:block/nitor_core',particle='thaumcraft:block/nitor_core'),
    elements=[dict(**{'from':[5.3,5.3,8],'to':[10.6,10.6,8]},faces={k:faces[k] for k in ('north','south')}),
              dict(**{'from':[8,5.3,5.3],'to':[8,10.6,10.6]},faces={k:faces[k] for k in ('west','east')}),
              dict(**{'from':[5.3,8,5.3],'to':[10.6,8,10.6]},faces={k:faces[k] for k in ('up','down')})]))
colors = ('black','blue','brown','cyan','gray','green','lightblue','lime','magenta','orange','pink','red','silver','white','yellow','purple')
write(root / 'data/thaumcraft/tags/item/nitor.json',dict(replace=False,values=[f'thaumcraft:nitor_{color}' for color in colors]))
for color in colors:
    shutil.copyfile(assets / f'textures/item/nitor_core_{color}.png',assets / f'textures/block/nitor_core_{color}.png')
    write(assets / f'models/block/nitor_{color}_native.json',dict(parent='thaumcraft:block/nitor_native',
        textures=dict(core=f'thaumcraft:block/nitor_core_{color}',particle=f'thaumcraft:block/nitor_core_{color}')))
    write(assets / f'blockstates/nitor_{color}.json',dict(variants={'':dict(model=f'thaumcraft:block/nitor_{color}_native')}))
    dye = dict(lightblue='light_blue',silver='light_gray').get(color,color)
    write(recipes / f'nitor_dye_{color}.json',dict(type='minecraft:crafting_shapeless',
        ingredients=[f'minecraft:{dye}_dye','#thaumcraft:nitor'],result=dict(id=f'thaumcraft:nitor_{color}')))
blocks = project / 'src/main/java/art/arcane/thaumcraft/ModBlocks.java'
text = blocks.read_text()
for color in colors:
    text=text.replace(f'BLOCKS.registerSimpleBlock("nitor_{color}", () -> BlockBehaviour.Properties.of())',
        f'BLOCKS.registerBlock("nitor_{color}", art.arcane.thaumcraft.block.NitorBlock::new)')
blocks.write_text(text,encoding='utf-8')
for name in ('crucible','arcane_workbench','arcane_workbench_charger','hungry_chest',*[f'nitor_{color}' for color in colors]):
    write(root / f'data/thaumcraft/loot_table/blocks/{name}.json',dict(type='minecraft:block',pools=[dict(rolls=1,
        entries=[dict(type='minecraft:item',name=f'thaumcraft:{name}')],conditions=[dict(condition='minecraft:survives_explosion')])]))
