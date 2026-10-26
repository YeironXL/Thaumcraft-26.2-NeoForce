"""Generate a disposable native GameTest structure; no player world is used."""
import gzip
import json
import struct
from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/verification/resources/data/thaumcraft'
test = root / 'test_instance/starter_transactions.json'
test.parent.mkdir(parents=True, exist_ok=True)
test.write_text(json.dumps(dict(type='minecraft:function',function='thaumcraft:starter_transactions',
    environment=dict(type='minecraft:all_of',definitions=[]),structure='thaumcraft:verification/empty',max_ticks=650))+'\n')

def name(tag, key):
    encoded=key.encode()
    return bytes([tag])+struct.pack('>H',len(encoded))+encoded

data=name(10,'')
data+=name(3,'DataVersion')+struct.pack('>i',6000)
data+=name(9,'size')+bytes([3])+struct.pack('>iiii',3,5,5,5)
data+=name(9,'palette')+bytes([10])+struct.pack('>i',1)+name(8,'Name')+struct.pack('>H',13)+b'minecraft:air'+bytes([0])
data+=name(9,'blocks')+bytes([10])+struct.pack('>i',0)
data+=name(9,'entities')+bytes([10])+struct.pack('>i',0)+bytes([0])
structure = root / 'structure/verification/empty.nbt'
structure.parent.mkdir(parents=True, exist_ok=True)
structure.write_bytes(gzip.compress(data))
