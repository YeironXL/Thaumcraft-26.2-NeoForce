from pathlib import Path
import re,json,hashlib,collections
root=Path('work/fix21');ref=Path('work/reference-tc6/src/main/resources/assets/thaumcraft');port=root/'src/main/resources/assets/thaumcraft'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
by_hash=collections.defaultdict(list)
for p in port.rglob('*'):
 if p.is_file():by_hash[digest(p)].append(p.relative_to(port).as_posix())
records=[]
for p in sorted(ref.rglob('*')):
 if not p.is_file() or p.suffix.lower() not in ('.png','.jpg','.obj','.mtl','.ogg','.json'):continue
 rel=p.relative_to(ref).as_posix();target=port/rel
 normalized=rel.replace('textures/blocks/','textures/block/').replace('textures/items/','textures/item/')
 alternative=port/normalized
 h=digest(p);matches=by_hash.get(h,[])
 status='idéntico' if target.exists() and digest(target)==h else 'reubicado idéntico' if matches else 'modificado' if target.exists() or alternative.exists() else 'ausente'
 records.append({'original':rel,'estado':status,'copias_identicas':matches,'archivo_port':normalized if alternative.exists() else rel if target.exists() else None})
original_java=Path('work/reference-tc6/src/main/java/thaumcraft')
visual_classes=[p.relative_to(original_java).as_posix() for p in sorted((original_java/'client').rglob('*.java')) if '/render' in p.as_posix() or '/fx/' in p.as_posix() or '/gui/' in p.as_posix()]
a=Path('work/reference-tc6/src/main/java/thaumcraft/api/aspects/Aspect.java').read_text(encoding='utf-8');b=(root/'src/main/java/art/arcane/thaumcraft/aspect/AspectRegistry.java').read_text(encoding='utf-8')
a=re.sub(r'//[^\n]*|/\*[\s\S]*?\*/','',a)
ac={k:int(v,16) for k,v in re.findall(r'new Aspect\("([^" ]+)",\s*(0x[0-9a-fA-F]+)',a)}
bc={k:int(v,16) for k,v in re.findall(r'define\("([^" ]+)"[^;]*?,\s*(0x[0-9a-fA-F]+)\)',b)}
colors=[{'aspecto':k,'original':f'{v:06x}','port':f'{bc[k]:06x}' if k in bc else None,'coincide':bc.get(k)==v} for k,v in ac.items()]
summary=dict(collections.Counter(row['estado'] for row in records))
d={'referencia':'TC6 1.12.2 6.1.BETA26 (fuente comunitaria)','version':'fix81','recursos':records,'resumen_recursos':summary,'colores_aspectos':colors,'clases_visuales_originales':visual_classes,'limite':'La igualdad de bytes acredita recursos conservados, no su uso ni fidelidad en juego. Las clases originales enumeradas no implican implementación en el port.'}
(root/'docs/AUDITORIA_VISUAL_TC6.json').write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
lines=['# Auditoría visual TC6 — fix81','', 'Comparación de todos los recursos PNG/JPG/OBJ/MTL/OGG/JSON disponibles de la referencia con el port. No es una certificación de fidelidad en juego.','', '## Inventario de recursos','']
lines += [f'- {k}: {v}' for k,v in summary.items()]
lines += ['',f'Colores de aspectos coincidentes: {sum(x["coincide"] for x in colors)}/{len(colors)}.','', '## Sistemas artísticos revisados','', '| Sistema | Estado real |','|---|---|','| Libro | Textura original abierta; tipografía original recuperada para el pedido1:1; mapa con posiciones, marcos e iconos originales y lectura en doble página con proporciones originales; selector con marcos e iconos originales, sujeto a los desbloqueos del jugador. Conexiones y flechas con los segmentos curvos del atlas original; enlaces secundarios sin flecha y tonos originales incorporados; párrafos y saltos de página originales conservados; ilustraciones IMG y ornamentos LINE/DIV originales incorporados; primeras páginas de fabricación normal, arcana y alquimia en crisol con recursos del datapack y atlas original incorporadas; demás recetas ilustradas y animaciones pendientes. |','| Mesa arcana | Atlas original y disposición original de ranuras y seis cristales. |','| Horno alquímico | Atlas e indicadores originales de llama, progreso y reserva; cantidades en tooltips. |','| Investigación | Madera y papel originales; tres cartas verticales con iconos de categoría, inspiración y detalles al pasar el cursor. Diseño compacto adaptado; animación original pendiente. |','| Crisol | Geometría y cuatro niveles de agua conservados; espuma original incorporada. Agua con multiplicadores RGB originales según concentración, sin tinte de bioma; contenido y cruces del umbral de ebullición sincronizados al cliente y actualización del modelo. Paleta y datos verificados; vista integrada en mundo pendiente. |','| Manipulador | Metal original adaptado; editor de grafo original pendiente. |','| Objetos sostenidos | Guanteletes con atlas corregido; comprobación del horno existente. Transformaciones de todos los objetos pendientes de revisión en mundo. |','| Bloques | Recursos/modelos existentes conservados; igualdad de archivo no acredita renderer especial, animación ni estado en mundo. |','| Entidades | Solo Alumentum tiene renderer de entidad registrado en el cliente actual; criaturas, gólems y demás renderers originales pendientes. |','| Efectos | Fuego del primer foco con diez fotogramas originales, gravedad, duración y frenado de TC6; transparencia adaptada al renderizador moderno. Espuma del crisol con fotogramas 64–66 y tono original, activa solo con agua hirviendo. Destellos centrales del manipulador con fotogramas 448–456, activos durante fabricación. Burbujas por aspecto con fotogramas originales, colores y vida incorporados; codec, proveedor y geometría verificados. Rebose morado por los cuatro bordes con umbral original y fotogramas 73, 66 y 65 incorporado. Vista integrada, rayos, aura, demás partículas y shaders originales pendientes. |','| Armaduras | Gafas con capa original de equipo; resto y vista sobre jugador pendientes. |','| Aspectos | Comparación de iconos y colores incluida en JSON; no equivale a validar HUD/revelación. |','| Sonido | Inventario binario comparado; activación y ambientes originales pendientes. |','', '## Recursos ausentes','']
lines += [f'- `{x["original"]}`' for x in records if x['estado']=='ausente']
lines += ['', '## Trazabilidad','', 'Detalle por archivo y clases visuales originales en `AUDITORIA_VISUAL_TC6.json`. Se respetan los recursos originales; la fuente Alegreya es una adaptación aprobada en esta conversación.']
(root/'docs/AUDITORIA_VISUAL_TC6.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(json.dumps({'resumen':summary,'colores_diferentes':[x for x in colors if not x['coincide']],'clases_visuales':len(visual_classes)},ensure_ascii=False))
