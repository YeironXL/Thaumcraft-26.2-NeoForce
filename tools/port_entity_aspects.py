"""Read explicit original entity tags, mapping vanilla entities to current registry names."""
import json
import re
from pathlib import Path
root=Path(__file__).resolve().parents[1]
reference=root.parent / 'reference-tc6/src/main/java/thaumcraft'
aspect_source=(reference / 'api/aspects/Aspect.java').read_text()
aspects=dict(re.findall(r'public static Aspect\s+(\w+)\s*=\s*new Aspect\("(\w+)"',aspect_source))
config=(reference / 'common/config/ConfigAspects.java').read_text()
renamed=dict(XPOrb='experience_orb',MushroomCow='mooshroom',SnowMan='snow_golem',Ozelot='ocelot',
    PigZombie='zombified_piglin',LavaSlime='magma_cube',WitherBoss='wither',VillagerGolem='iron_golem',
    EnderCrystal='end_crystal',EvocationIllager='evoker',VindicationIllager='vindicator',IllusionIllager='illusioner')
entities={}
for line in config.splitlines():
    match=re.search(r'registerEntityTag\("([^"]+)"',line)
    if not match or 'EntityTagsNBT' in line or match[1].startswith('Thaumcraft.'):
        continue
    name=match[1]
    snake=re.sub(r'(?<!^)(?=[A-Z])','_',name).lower()
    key='minecraft:'+renamed.get(name,snake)
    entities[key]={aspects[a]:int(n) for a,n in re.findall(r'\.add\(Aspect\.(\w+),\s*(\d+)\)',line)}
entities['minecraft:elder_guardian']=dict(bestia=10,alienis=15,aqua=15)
entities['minecraft:creeper/charged']=dict(herba=15,ignis=15,potentia=15)
path=root / 'src/main/resources/assets/thaumcraft/migration/entity_aspects.json'
path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps(entities,indent=2)+'\n')
print(f'{len(entities)} vanilla entity aspect entries migrated from TC6')
