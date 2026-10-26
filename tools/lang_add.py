"""Add/overwrite translation keys: python3 tools/lang_add.py key 'english' 'español' ..."""
import json,sys,collections
from pathlib import Path
base=Path(__file__).resolve().parents[1]/'src/main/resources/assets/thaumcraft/lang'
def add(pairs):
    for lang,idx in (('en_us',0),('es_es',1)):
        p=base/f'{lang}.json';raw=p.read_bytes().decode('utf-8');crlf='\r\n' in raw
        d=json.loads(raw,object_pairs_hook=collections.OrderedDict)
        for k,v in pairs.items(): d[k]=v[idx]
        out=json.dumps(d,ensure_ascii=False,indent=2)+'\n'
        p.write_bytes((out.replace('\n','\r\n') if crlf else out).encode('utf-8'))
if __name__=='__main__':
    a=sys.argv[1:];add({a[i]:(a[i+1],a[i+2]) for i in range(0,len(a),3)})
