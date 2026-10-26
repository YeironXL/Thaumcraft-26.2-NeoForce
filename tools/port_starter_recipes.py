"""Explicit TC6 ConfigRecipes entries; research gates and costs are retained."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/resources/data'
recipes = root / 'thaumcraft/recipe'
recipes.mkdir(parents=True, exist_ok=True)

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')

def shaped(name, research, vis, crystals, pattern, key, result=None, count=1):
    write(recipes / f'{name}.json', dict(type='thaumcraft:arcane',research=research,vis=vis,crystals=crystals,
        pattern=pattern,key=key,result=dict(id=result or f'thaumcraft:{name}',count=count)))

def shapeless(name, research, vis, crystals, ingredients, result=None, count=1):
    write(recipes / f'{name}.json', dict(type='thaumcraft:shapeless_arcane',research=research,vis=vis,crystals=crystals,
        ingredients=ingredients,result=dict(id=result or f'thaumcraft:{name}',count=count)))

for metal in ('iron','brass','thaumium','void'):
    write(root / f'c/tags/item/plates/{metal}.json', dict(replace=False,values=[f'thaumcraft:plate_{metal}']))
shaped('mechanism_simple','BASEARTIFICE',10,dict(ignis=1,aqua=1),[' B ','ISI',' B '],
       dict(B='#c:plates/brass',I='#c:plates/iron',S='minecraft:stick'))
shaped('mechanism_complex','BASEARTIFICE',50,dict(ignis=1,aqua=1),[' M ','TQT',' M '],
       dict(T='#c:plates/thaumium',Q='minecraft:piston',M='thaumcraft:mechanism_simple'))
shapeless('vis_resonator','UNLOCKAUROMANCY@2',50,dict(aer=1,aqua=1),['#c:plates/iron','minecraft:quartz'])
shapeless('activatorrail','FIRSTSTEPS',10,{},['minecraft:activator_rail'],result='thaumcraft:activator_rail')
shaped('workbenchcharger','WORKBENCHCHARGER',200,dict(aer=2,ordo=2),[' R ','W W','I I'],
       dict(I='minecraft:iron_ingot',R='thaumcraft:vis_resonator',W='thaumcraft:plank_greatwood'),result='thaumcraft:arcane_workbench_charger')
shaped('caster_basic','UNLOCKAUROMANCY@2',100,dict.fromkeys(('aer','terra','ignis','aqua','ordo','perditio'),1),
       ['III','LRL','LTL'],dict(I='minecraft:iron_ingot',L='minecraft:leather',R='thaumcraft:vis_resonator',T='thaumcraft:thaumometer'))
shapeless('mirrorglass','BASEARTIFICE',50,dict(aqua=1,ordo=1),['thaumcraft:quicksilver','minecraft:glass_pane'],result='thaumcraft:mirrored_glass')
shaped('enchanted_fabric','UNLOCKINFUSION',5,{},[' S ','SCS',' S '],dict(S='minecraft:string',C='#minecraft:wool'),result='thaumcraft:fabric')
