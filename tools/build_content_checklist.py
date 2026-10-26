from pathlib import Path
import re,json,collections
root=Path(__file__).resolve().parents[1];ref=root.parent/'reference-tc6';out=root.parents[1]/'outputs'
def read(p):return p.read_text(encoding='utf-8-sig')
lang={}
for line in read(ref/'src/main/resources/assets/thaumcraft/lang/en_us.lang').splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1);lang[k.strip()]=v.strip()
es=json.loads(read(root/'src/main/resources/assets/thaumcraft/lang/es_es.json'))
moditems=read(root/'src/main/java/art/arcane/thaumcraft/ModItems.java');modblocks=read(root/'src/main/java/art/arcane/thaumcraft/ModBlocks.java')
itemids=set(re.findall(r'ITEMS\.register\w*\("([^"]+)"',moditems));blockids=set(re.findall(r'BLOCKS\.register\w*\("([^"]+)"',modblocks))
recipes=[json.loads(read(p)) for p in (root/'src/main/resources/data/thaumcraft/recipe').glob('*.json')]
outputs={d.get('result',{}).get('id','').removeprefix('thaumcraft:') for d in recipes if isinstance(d.get('result'),dict)}
G='🟢';Y='🟡';W='⚪';rows=[]
def add(group,key,name,status,note,source):rows.append(dict(group=group,key=key,name=name,status=status,note=note,source=source))
greenblocks={'arcane_workbench':'Motor de fabricación y pagos probado; el catálogo global de recetas sigue incompleto.','arcane_workbench_charger':'Consumo de aura de nueve chunks cargados.','hungry_chest':'Inventario, recogida y automatización; falta la animación de tapa original.','centrifuge':'Separación, puertos, redstone y persistencia; modelo estático.','tube':'Transporte normal y conexiones.','tube_valve':'Control manual y redstone; indicador estático.','tube_filter':'Filtrado, configuración y color.','tube_restrict':'Reducción de succión.','tube_oneway':'Salida direccional.','tube_buffer':'Diez unidades multiaspecto y reguladores por cara.'}
partial={'goggles':'Receta, casco nativo, durabilidad/reparación y descuento5% del lanzamiento; faltan HUD/revelación, otros usos y comprobación de textura vestida.','smelter_aux':'Receta, orientación y tres columnas auxiliares de alambiques funcionales; colocación manual/modelo y colisiones fieles pendientes.','wand_workbench':'Receta, menú y fabricación de Toque/Fuego con costes, aura, inventario bloqueado y persistencia; otros nodos de foco pendientes.','table_stone':'Receta con materiales del original; geometría de colisión pendiente.','stone_arcane':'Receta original de nueve unidades y uso en componentes; propiedades del bloque pendientes.','stone_arcane_brick':'Receta y uso en losas; propiedades del bloque pendientes.','slab_arcane_stone':'Receta y bloque de losa nativo; propiedades del material pendientes.','slab_arcane_brick':'Receta y bloque de losa nativo; propiedades del material pendientes.','research_table':'Mesa, dieciséis cartas, ayudas de biblioteca/crisol/manipulador, costes y persistencia funcionales; faltan diecinueve cartas y ayudas posteriores.','table_wood':'Receta y conversión con tintero implementadas; geometría de colisión pendiente.','crucible':'Agua/calor/recetas funcionales; 23 bases y 17 derivados iniciales añadidos; cobertura completa de aspectos pendiente.','jar_normal':'Almacenamiento y frascos funcionan; faltan etiquetas/abrazaderas.','jar_void':'Almacenamiento y exceso funcionan; faltan etiquetas/abrazaderas.','alembic':'Destilación/trasvase funcionan; falta etiquetado.','bellows':'Funciona con depósitos y acelera hornos de esencia; faltan otras máquinas y animación.','smelter_basic':'Destila, tiene interfaz e inventario automático por caras; faltan accesorios.','smelter_thaumium':'Destila, tiene interfaz e inventario automático por caras; receta de mejora implementada; faltan accesorios.','smelter_void':'Destila, tiene interfaz e inventario automático por caras; faltan receta de mejora y accesorios.','infernal_furnace':'Modelo en mano corregido; procesamiento original pendiente.'}
greenitems={'scribing_tools':'Fabricación, recarga sin pérdida de componentes, creación de mesa y cien usos de tinta.','crystal_essence':'37 variantes con componente de aspecto y recetas de crisol.','phial_empty':'Receta y trasvase de diez unidades.','phial_filled':'37 variantes, trasvase y persistencia.'}
materials={'ingot_thaumium','ingot_void','ingot_brass','plate_brass','plate_iron','plate_thaumium','plate_void','mechanism_simple','mechanism_complex','fabric','tallow','filter','morphic_resonator','vis_resonator','mirrored_glass'}
# Human-facing catalog from the actual original localization, including metadata variants.
for key,name in lang.items():
 if not key.endswith('.name') or not key.startswith(('tile.','item.')):continue
 family,body=key.split('.',1);body=body[:-5];ident=body.replace('.','_');status=W;note='Comportamiento original pendiente.'
 candidates=[ident,body.split('.')[0]]
 if body=='thaumonomicon.normal':candidates=['thaumonomicon']
 if body=='phial.empty':candidates=['phial_empty']
 if body=='phial.filled':candidates=['phial_filled']
 chosen=next((i for i in candidates if i in (blockids if family=='tile' else itemids)),ident)
 native=(chosen in blockids if family=='tile' else chosen in itemids)
 if native:status=Y;note='Registro/recurso presente; no acreditado como contenido terminado.'
 if family=='tile':
  if chosen in greenblocks:status=G;note=greenblocks[chosen]
  elif chosen.startswith('nitor_'):status=G;note='Luz, calor, partículas y recetas de color.'
  elif chosen in partial:status=Y;note=partial[chosen]
  elif chosen.startswith('crystal_'):status=Y;note='Crecimiento, gotas y generación básica; distribución/geometría originales aún parciales.'
 else:
  if chosen in greenitems:status=G;note=greenitems[chosen]
  elif chosen in materials and chosen in outputs:status=G;note='Material/componente con receta nativa; esto no completa las máquinas que lo utilizan.'
  elif chosen=='focus_1':status=Y;note='Receta, configuración y lanzamiento Toque/Fuego funcionales; otros grafos pendientes.'
  elif chosen=='caster_basic':status=Y;note='Receta, montaje/intercambio/retirada y lanzamiento Toque/Fuego funcionales; descuentos y otros grafos pendientes.'
  elif chosen=='alumentum':status=G;note='Combustible de 4.800 ticks y proyectil explosivo al impacto; partículas adaptadas al cliente actual.'
  elif chosen in {'thaumonomicon','thaumometer','scribing_tools','salis_mundus','sanity_soap','sanity_checker','bath_salts','triple_meat_treat','alumentum'}:status=Y;note='Parte de su uso o receta implementada; no toda su funcionalidad/progresión original.'
 translations={'crystal_essence':'Cristales de vis por aspecto','phial_empty':'Frasco vacío','phial_filled':'Frasco lleno de esencia por aspecto','tube':'Tubo de esencia','tube_valve':'Válvula de esencia','tube_filter':'Tubo con filtro','tube_restrict':'Tubo restrictor','tube_oneway':'Tubo direccional','tube_buffer':'Depósito de esencia','filter':'Filtro de esencia','tallow':'Sebo mágico','mirrored_glass':'Vidrio espejado'}
 localized=translations.get(chosen,es.get(('block.' if family=='tile' else 'item.')+'thaumcraft.'+chosen,name))
 add('Bloques y variantes' if family=='tile' else 'Objetos y variantes',key,localized,status,note,'assets/thaumcraft/lang/en_us.lang')
# Every aspect is explicitly defined, independent of coverage of item aspect tags.
s=read(root/'src/main/java/art/arcane/thaumcraft/aspect/AspectRegistry.java')
for tag in re.findall(r'define\("([^"]+)"',s):add('Aspectos',tag,es.get(tag,tag),G,'Identidad, composición y color definidos. No implica etiquetas para todos los objetos.','thaumcraft/api/aspects/Aspect.java')
# Only registered entities; avoids treating renderer/helper classes as mobs.
entities=read(ref/'src/main/java/thaumcraft/common/config/ConfigEntities.java')
for name,cls in re.findall(r'registerModEntity\(new ResourceLocation\("thaumcraft",\s*"([^"]+)"\),\s*(\w+)\.class',entities):
 status=G if name.lower()=='alumentum' else W
 note='Proyectil explosivo: lanzamiento, propietario, colisiones y representación nativa.' if status==G else 'Entidad, comportamiento y aparición no portados. Incluye proyectiles y entidades técnicas.'
 add('Entidades registradas',name,lang.get('entity.Thaumcraft.'+name+'.name',lang.get('entity.thaumcraft.'+name+'.name',name)),status,note,f'thaumcraft/common/config/ConfigEntities.java:{cls}')
for p in sorted((ref/'src/main/java/thaumcraft/common/items/casters/foci').glob('Focus*.java')):
 text=read(p);keys=re.findall(r'return\s+"(thaumcraft\.[^"]+)"',text);name=next((lang.get(k+'.name',lang.get(k,k)) for k in keys),p.stem)
 add('Componentes de focos',p.stem,name,Y if p.stem in {'FocusMediumTouch','FocusEffectFire'} else W,'Toque/Fuego implementado como grafo inicial; motor general de nodos pendiente.' if p.stem in {'FocusMediumTouch','FocusEffectFire'} else 'Efecto/medio/modificador de hechizo pendiente.',p.relative_to(ref/'src/main/java').as_posix())
config=read(ref/'src/main/java/thaumcraft/common/config/ConfigItems.java')
for cls in re.findall(r'SealHandler\.registerSeal\(new (\w+)\(',config):add('Sellos de gólems',cls,re.sub(r'(?<!^)(?=[A-Z])',' ',cls).removeprefix('Seal '),W,'Comportamiento de sello e IA de gólems pendiente.','thaumcraft/common/config/ConfigItems.java')
for key,name in lang.items():
 if key.startswith('golem.') and '.text.' not in key and key.count('.')==2:
  add('Gólems: rasgos y piezas',key,name,W,'Parte del sistema de gólems pendiente.','assets/thaumcraft/lang/en_us.lang')
 if key.startswith('potion.') and key.count('.')==1:add('Efectos propios',key,name,W,'Efecto original no implementado como efecto nativo completo.','assets/thaumcraft/lang/en_us.lang')
 if key.startswith('enchantment.infusion.') and key.count('.')==2:add('Encantamientos por infusión',key,name,W,'Infusión y encantamiento propios pendientes.','assets/thaumcraft/lang/en_us.lang')
 if key.startswith('card.') and key.endswith('.name'):
  basic=key.split('.')[1] in {'experimentation','analyze','ponder','inspired','rethink','reject','balance','study','notation','concentrate','reactions','synthesis','focus','awareness','spellbinding','tinker','scripting','curio','revelation','truth','realization','celestial'}
  add('Cartas de teoría',key,es.get('screen.thaumcraft.theory.card.'+key.split('.')[1],name) if basic else name,G if basic else W,'Carta funcional en la mesa: costes, efecto y persistencia; cristales/experiencia/distorsión según su tipo.' if basic else 'Texto disponible; mecánica de la carta pendiente.','assets/thaumcraft/lang/en_us.lang')
for folder in ['world/biomes','world/objects']:
 for p in sorted((ref/'src/main/java/thaumcraft/common'/folder).glob('*.java')):
  if p.stem.endswith('Handler'):continue
  add('Mundo y generación',p.stem,re.sub(r'(?<!^)(?=[A-Z])',' ',p.stem),W,'Generación/comportamiento propio pendiente.',p.relative_to(ref/'src/main/java').as_posix())
# Preserve every actual original research entry, not a count of UI labels.
for p in sorted((ref/'src/main/resources/assets/thaumcraft/research').glob('*.json')):
 try:d=json.loads(read(p))
 except json.JSONDecodeError:continue
 for entry in d.get('entries',[]):
  name=lang.get(entry.get('name',''),entry.get('key',''))
  add('Investigaciones: '+entry.get('category',p.stem),entry['key'],name,Y,'Entrada/etapas migradas; su función y recorrido completos se evalúan por sistema.',p.relative_to(ref/'src/main/resources').as_posix())
# API fields ensure language omissions do not silently drop declared content.
for family,file,configfile in [('Bloques','blocks/BlocksTC.java','ConfigBlocks.java'),('Objetos','items/ItemsTC.java','ConfigItems.java')]:
 api=read(ref/'src/main/java/thaumcraft/api'/file);cfg=read(ref/'src/main/java/thaumcraft/common/config'/configfile)
 for field in re.findall(r'public static (?:BlockSlab|Block|Item)\s+(\w+)\s*;',api):
  assigned=bool(re.search(r'(?:BlocksTC|ItemsTC)\.'+re.escape(field)+r'\s*=',cfg))
  add('Índice técnico de declaraciones: '+family,field,field,Y if assigned else W,'Declaración con asignación en la configuración; consultar estado funcional en el catálogo.' if assigned else 'Declaración sin asignación detectada en la configuración: puede ser contenido heredado/inactivo, no un objetivo confirmado.',f'thaumcraft/api/{file}')
# List native recipes individually without implying the complete original recipe catalog.
for p in sorted((root/'src/main/resources/data/thaumcraft/recipe').glob('*.json')):
 d=json.loads(read(p));add('Recetas ya migradas',p.stem,p.stem,G,'Receta nativa cargada; no implica que el objeto de salida tenga todas sus funciones.','src/main/resources/data/thaumcraft/recipe/'+p.name)
manifest={'version':'fix81','date':'2026-10-04','reference':'Thaumcraft 1.12.2-6.1.BETA26','legend':{G:'Función principal indicada implementada',Y:'Parcial, registro/recurso o progresión incompleta',W:'Pendiente o entrada de referencia sin implementación'},'caveat':'No calcular porcentajes a partir de estas filas: hay variantes, referencias cruzadas, declaraciones heredadas y recetas que se solapan. Las combinaciones de focos/gólems se describen por componentes. El inventario procede del código y recursos disponibles, no de una certificación de exhaustividad de todas las combinaciones posibles.','entries':rows}
(root/'docs').mkdir(exist_ok=True);(root/'docs/LISTA_CONTENIDO_TC6.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
summary='''# Lista de contenido de Thaumcraft 6 y estado del port

Referencia: **Thaumcraft 1.12.2 — 6.1.BETA26**. Estado: **fix81, 4 de octubre de 2026**. No mezcla automáticamente el contenido exclusivo de Thaumcraft 5 para 1.8.9.

- 🟢 Función principal indicada implementada. Las diferencias visuales o de alcance se anotan junto a ella.
- 🟡 Parcial: hay código, receta, registro, modelo o texto, pero queda trabajo funcional o de progresión.
- ⚪ Pendiente. En el índice técnico también identifica declaraciones posiblemente heredadas/inactivas, señaladas expresamente.

**Un objeto visible en creativo no se considera terminado.** Esta lista no es un cálculo de porcentaje: recetas, variantes y sistemas se solapan. El inventario recoge las entradas identificables del código y recursos disponibles; las combinaciones de focos/gólems se enumeran por componentes. Los nombres originales se conservan cuando no hay traducción española disponible.

## Resumen funcional

### Ya implementado en el alcance indicado

- 🟢 Los 37 aspectos: nombres, composiciones y colores.
- 🟢 Crystal Essence: variantes persistentes y recetas de cristales.
- 🟢 Motor de mesa arcana: ingredientes, investigación, cristales y pago de vis.
- 🟢 Cargador de mesa: uso de aura de nueve chunks cargados.
- 🟢 Crisol: agua, calentamiento y ejecución de las recetas migradas.
- 🟢 Nitor: 16 colores, luz y fuente de calor.
- 🟢 Alumentum: combustible de 4.800 ticks y lanzamiento explosivo al impacto.
- 🟢 Mejora solicitada para el port: Thaumonomicon inicial en el inventario, con entrega única por jugador.
- 🟢 Requisitos de investigación: cristales y frascos por aspecto, con cobro atómico de inventario.
- 🟢 Tarros: almacenamiento, trasvase con frascos y líquido visible.
- 🟢 Tarro del vacío: absorción de exceso y contaminación probabilística.
- 🟢 Frascos: 37 aspectos, diez unidades por trasvase.
- 🟢 Hornos alquímicos: destilación, interfaz, rendimientos de tres versiones e inventario automático por caras.
- 🟢 Alambiques: almacenamiento y salida hacia tubos.
- 🟢 Tubo normal, válvula, filtro, restrictor, direccional y depósito.
- 🟢 Fuelles orientados hacia los depósitos.
- 🟢 Centrífuga: separación, redstone, persistencia y transporte.
- 🟢 Hungry Chest: inventario, recogida y automatización.
- 🟢 99 recetas nativas cargadas; se enumeran individualmente al final.

### Parcial: no equivale todavía al sistema original completo

- 🟡 Thaumonomicon, etapas de investigación y progresión inicial.
- 🟡 Thaumómetro y escaneo: cobertura de objetos/entidades incompleta.
- 🟡 Salis Mundus: transformaciones iniciales, no todas las estructuras.
- 🟡 Aura y flux: almacenamiento/gasto/regeneración; faltan simulación completa y grietas.
- 🟡 Cristales del mundo: generación, crecimiento y geometría originales aún parciales.
- 🟡 Alquimia completa: faltan recetas y derivación de aspectos para todos los objetos.
- 🟡 Tarros/alambiques completos: faltan etiquetas y abrazaderas.
- 🟡 Hornos completos: faltan auxiliares y mejoras.
- 🟡 Fuelles: faltan otros usos y su animación.
- 🟡 Sal de baño, jabón, medidor de cordura y otros consumibles: implementación desigual.
- 🟡 Bloques decorativos, escaleras/losas, texturas y modelos: no todos están auditados como terminados.
- 🟡 Animaciones y efectos visuales originales de máquinas.

### Grandes sistemas pendientes

- ⚪ Mesa de investigación, teorías, cartas y ayudas de investigación.
- ⚪ Guantelete, manipulador de focos y lanzamiento de hechizos.
- ⚪ Infusión, estabilidad, fallos y encantamientos por infusión.
- ⚪ Constructor de gólems, piezas, rasgos, sellos e IA.
- ⚪ Criaturas propias, jefes, proyectiles y apariciones.
- ⚪ Árboles, biomas y estructuras originales.
- ⚪ Corrupción, grietas, eventos de warp y contenido eldritch.
- ⚪ Comportamientos especiales de herramientas, armaduras y accesorios.
- ⚪ Thaumatorium, condensador, espejos, lámparas, generadores y otras máquinas.

## Catálogo detallado

En las secciones de bloques/objetos una entrada 🟡 puede tener una función concreta ya marcada 🟢 en el resumen. Por ejemplo: el tarro almacena esencia, pero el tarro original completo también tiene etiquetas y abrazaderas. Las entradas procedentes solo de traducciones pueden incluir restos de versiones previas; el índice técnico permite contrastarlas con declaraciones y asignaciones del código.
'''
sections=collections.OrderedDict()
for row in rows:sections.setdefault(row['group'],[]).append(row)
parts=[summary]
for group,entries in sections.items():
 parts.append('\n### '+group+f' ({len(entries)})\n\n')
 for row in entries:
  name=row['name'].replace('\n',' ').replace('|','/')
  parts.append(f"- {row['status']} **{name}** — `{row['key']}`. {row['note']}\n")
parts.append('''\n## Revisión visual fix81\n\nThaumonomicon conserva pergamino original; lectura recupera proporciones y flechas originales, y recetas usan papel cuadrado original; mesa arcana y horno recuperan atlas y disposición original, investigación usa cartas verticales sobre madera y manipulador metal original en dos capas. Se recupera la fuente pixelada original para el pedido1:1; serif conservada como recurso. Auditoría por archivo: AUDITORIA_VISUAL_TC6.md/json. Guanteletes con referencias de atlas corregidas. Mapa original, lectura en doble página con párrafos, saltos de página e ilustraciones originales y selector de categorías con iconos y desbloqueos incorporados; vista sin mundo corregida para usar modelos nativos de objetos y conexiones curvas con flechas, tonos originales y enlaces secundarios del atlas original; primeras páginas de fabricación normal, arcana y alquimia en crisol con cuadrícula, resultado, cristales y vis del datapack incorporadas; demás recetas ilustradas, animaciones de cartas, renderizadores restantes, modelos y animaciones pendientes.\n\nFuego del primer foco con secuencia original de diez fotogramas; caducidad y geometría nativa verificadas. Destellos centrales de fabricación del manipulador con nueve fotogramas originales; haces de cristales pendientes. Espuma del crisol con fotogramas y tono originales; activa únicamente con agua hirviendo. Agua con tinte original por concentración de aspectos y sincronización del contenido al cliente. Burbujas con colores por aspecto, duración y fotogramas originales emitidas desde el cliente. Rebose morado con agua, calor superior a 150 y más de 500 aspectos, ocho partículas por tick y fotogramas originales. Vista completa en mundo, colisión real y mezcla aditiva original pendientes.

## Trazabilidad y mantenimiento\n\nFuentes: BlocksTC, ItemsTC, ConfigItems, ConfigBlocks, ConfigEntities, clases de focos/mundo, idioma original, JSON originales de investigación y las clases/recetas nativas de fix81. La referencia contiene 902 archivos Java inventariados; ese número no mide contenido terminado.\n\n`LISTA_CONTENIDO_TC6.json` conserva las claves, el estado y la fuente de cada fila. El generador `tools/build_content_checklist.py` permite actualizar el catálogo, pero las marcas verdes requieren revisión humana del comportamiento; no se infieren de la mera presencia de archivos.\n''')
(root/'docs/LISTA_CONTENIDO_TC6.md').write_text(''.join(parts),encoding='utf-8')
import shutil
for name in ['LISTA_CONTENIDO_TC6.md','LISTA_CONTENIDO_TC6.json']:shutil.copyfile(root/'docs'/name,out/name)
print(json.dumps({'entries':len(rows),'groups':{k:len(v) for k,v in sections.items()},'markdown':str(out/'LISTA_CONTENIDO_TC6.md')},ensure_ascii=False,indent=2))

