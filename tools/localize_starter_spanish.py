import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
file=root / 'src/main/resources/assets/thaumcraft/lang/es_es.json'
language=json.loads(file.read_text(encoding='utf-8'))
language.update(dict(aer='Aire',terra='Tierra',ignis='Fuego',aqua='Agua',ordo='Orden',perditio='Entropía',vacuos='Vacío',
    lux='Luz',motus='Movimiento',gelum='Frío',vitreus='Cristal',metallum='Metal',victus='Vida',mortuus='Muerte',potentia='Energía',
    permutatio='Intercambio',praecantatio='Magia',auram='Aura',alkimia='Alquimia',vitium='Corrupción',tenebrae='Oscuridad',alienis='Eldritch',
    volatus='Vuelo',herba='Planta',instrumentum='Herramienta',fabrico='Fabricación',machina='Mecanismo',vinculum='Trampa',
    spiritus='Espíritu',cognitio='Mente',sensus='Sentido',aversio='Aversión',praemunio='Protección',desiderium='Deseo',exanimis='No muerto',bestia='Bestia',humanus='Humano'))
names=dict(arcane_workbench='Mesa de trabajo arcana',arcane_workbench_charger='Cargador de mesa arcana',crucible='Crisol',hungry_chest='Cofre hambriento')
for name,value in names.items(): language[f'block.thaumcraft.{name}']=value
items=dict(salis_mundus='Salis Mundus',thaumonomicon='Thaumonomicon',thaumometer='Taumómetro',alumentum='Alumentum',vis_resonator='Resonador de vis',
    mechanism_simple='Mecanismo simple',mechanism_complex='Mecanismo complejo',fabric='Tela encantada',quicksilver='Mercurio',nugget_quartz='Pepita de cuarzo')
for name,value in items.items(): language[f'item.thaumcraft.{name}']=value
for metal,translation in dict(iron='hierro',brass='latón',thaumium='taumio',void='metal vacío').items():
    language[f'item.thaumcraft.plate_{metal}']='Placa de '+translation
    if metal != 'iron': language[f'item.thaumcraft.ingot_{metal}']='Lingote de '+translation
colors=dict(black='negro',blue='azul',brown='marrón',cyan='cian',gray='gris',green='verde',lightblue='azul claro',lime='lima',magenta='magenta',
    orange='naranja',pink='rosa',red='rojo',silver='gris claro',white='blanco',yellow='amarillo',purple='morado')
for color,translation in colors.items(): language[f'block.thaumcraft.nitor_{color}']='Nitor '+translation
for aspect in ('aer','terra','ignis','aqua','ordo','perditio','vitium'):
    language[f'block.thaumcraft.crystal_{aspect}']='Cristales de '+language[aspect].lower()
language.update({'book.start.title':'Sueños extraños','got.crystals':'Estos cristales parecen tener propiedades extrañas.',
    'got.dream':'Has tenido un sueño extraño. Anotaste tus recuerdos en un diario.',
    'got.deepdown':'Has explorado las profundidades del mundo.','got.uphigh':'Has contemplado el mundo desde las alturas.',
    'got.hellandback':'Has visitado el Nether.','got.endoftheworld':'Has llegado al End.',
    'tc.knownobject':'Has descubierto algo nuevo.','tc.unknownobject':'No encuentras nada nuevo que escanear.'})
file.write_text(json.dumps(language,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
