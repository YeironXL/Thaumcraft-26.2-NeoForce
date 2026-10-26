# Etapas jugables del inicio de Thaumcraft 6

Alcance acordado: inicio, investigación, alquimia y primer guantelete/foco. Base: TC6 6.1.BETA26. Se completará cada recorrido con sus requisitos reales del Thaumonomicon; la presencia del objeto en creativo no basta.

| Etapa | Funciones que deben quedar jugables | Estado |
| --- | --- | --- |
| Inicio | Libro inicial, cristales, Salis Mundus, mesa arcana, taumómetro, observaciones y FIRSTSTEPS | Implementadas sus funciones básicas; falta prueba integral con jugador nuevo |
| Investigación | Recetas de mesa/tintero, conversión de mesa, papel/tinta, cartas, inspiración, ayudas iniciales y cobro de teorías | Base funcional en fix35: mesa, tintero, quince cartas y ayudas de biblioteca/crisol/manipulador; falta comprobación integral del inicio |
| Alquimia inicial | Crisol, Nitor, metales, Alumentum, cristales/frascos con aspecto y requisitos del libro | Motores principales implementados; cristales/frascos heredados por aspecto resueltos en fix33; aspectos derivados pendientes |
| Destilación inicial | Horno básico con menú, alambiques, tarros y tubos que permitan el recorrido de investigación | Funciones de máquina implementadas en fix32; recorrido/requisitos y accesorios pendientes |
| Primera auromancia | Descubrimientos de altura, resonador, guantelete, primer foco, manipulador y hechizo inicial con consumo de vis | Recetas y etapas de BASEAUROMANCY comprobadas en fix35; lanzamiento/configuración pendientes |

Fix32 completa el menú de los tres hornos alquímicos y conserva los avances de las versiones anteriores. No certifica aún que ninguna de estas etapas esté terminada de principio a fin.

La automatización fue reactivada por petición del usuario el 3 de octubre de 2026, sin el límite horario anterior. Terminará el early game acordado y seguirá por etapas de auromancia, alquimia y artificio del mid game. No se considera cerrada una etapa sin verificar su recorrido jugable completo.


## Trabajo en curso después de fix35

Se añadió `TouchFireFocus` y el componente nativo `touch_fire_focus` para persistir y sincronizar el grafo inicial Toque → Fuego. Potencia 1–5, duración 0–5 y complejidad máxima 15; 28 combinaciones válidas. Se mantienen las fórmulas originales: complejidad = 2 + 2×potencia + duración; fabricación = 10×complejidad + 3 vis y máximo(1, redondeo(raíz(complejidad))) niveles; un cristal de Aversion y uno de Ignis; lanzamiento = complejidad/5 vis; enfriamiento = máximo(5, división entera(complejidad/5)×división entera(complejidad/4)); daño = 3 + potencia; combustión = 1 + duración² segundos.

Este componente todavía no se obtiene mediante juego y no significa que el foco esté funcional. Siguiente paso: conectar el manipulador con validación de investigación, pago atómico de cristales/niveles, consumo gradual de aura y guardado del trabajo; después equipar el foco y lanzar desde el guantelete. Mantener el ZIP y JAR fix35 como última entrega hasta completar una nueva función verificable. Las comprobaciones del componente están en `EarlyFocusVerification`; evidencia en `focus-data-server.log`.

Validación del componente: las diez pruebas de servidor pasaron, incluyendo las 28 configuraciones válidas, rechazo de datos inválidos, costes originales y sincronización/persistencia. El lanzamiento y la interfaz del manipulador siguen pendientes.


Se añadió `FocusCraftingJob`: exige BASEAUROMANCY completada y un foco menor, verifica todos los recursos antes de cobrar cristales y niveles, conserva la configuración/progreso mediante codec, pausa sin aura y drena como máximo 20 vis por llamada cada 20 ticks. La configuración se aplica al objeto solo al completar el pago de vis. Las diez pruebas de servidor pasaron (`focus-job-server.log`), incluyendo costes atómicos, consumo parcial, persistencia y terminación. Todavía falta la entidad de bloque, bloquear su ranura durante el trabajo y conectar el menú; este servicio no se invoca aún desde el juego. No publicar ni marcar el manipulador como terminado por estos avances internos. El usuario autorizó continuar las implementaciones, pruebas y entregas sin confirmaciones rutinarias.


Integración del bloque: `wand_workbench` ahora usa `FocalManipulatorBlock` y una entidad dedicada `FocalManipulatorBlockEntity` (ranura de un foco menor). El trabajo cobrado bloquea reemplazo, retirada y vaciado del inventario hasta terminar; la entidad guarda configuración, vis restante y contador de 20 ticks. Al completar libera un foco con componente nativo. Las diez pruebas del servidor pasaron (`manipulator-server.log`), incluida carga tras 19 ticks y consumo total de 43 vis para Toque/Fuego mínimo. Compilación de producción previa correcta (`manipulator-build.log`). **Aún no tiene menú ni interacción para iniciar desde el cliente**; `createMenu` devuelve null provisionalmente y el bloque no intenta abrirlo. Próximo paso obligatorio: menú dedicado, sincronización de parámetros/progreso, interfaz en español, acciones con proximidad/estado vigentes y prueba de interfaz; después equipar/lanzar el guantelete. La entrega estable continúa siendo fix35; no empaquetar estas integraciones incompletas como funcionalidad terminada.


Fix36 conecta el menú dedicado y la pantalla del manipulador: potencia, duración, costes originales, cristales y progreso. El servidor rechaza selecciones antiguas, complejidad excesiva y retiradas durante el trabajo; el menú tiene 37 ranuras. Las diez pruebas de servidor pasaron (`manipulator-menu-server.log`). La configuración Toque/Fuego ya se inicia desde el juego; otros medios/efectos y el lanzamiento con guantelete continúan pendientes. La comprobación integral del jugador nuevo y los aspectos derivados siguen pendientes; no se cierra el early game.


## Trabajo posterior a fix36: guantelete

`CasterGauntletItem` reemplaza el objeto genérico. En el inventario, clic derecho sobre el guantelete con un foco menor configurado en el cursor lo monta; con otro configurado lo intercambia devolviendo el anterior; con cursor vacío lo retira. El componente `equipped_focus` conserva el objeto completo mediante ItemContainerContents, incluyendo configuración y nombre personalizado. Los focos vacíos y el clic izquierdo no activan esta operación. Pruebas de servidor: conservación de componentes, copia sin alias mutable, guardado/carga del ItemStack, intercambio, retirada y ausencia de duplicación.

Se añadió consumo fraccionario validado de vis en `ChunkAura.spend(float)` y `NativeAura.spendLocal`: solo el chunk cargado del jugador, sin aplicar el cargador de mesa a la magia del guantelete; rechaza cantidades negativas, no finitas e insuficientes sin mutación. La prueba comprueba 0,8 vis sin redondearlo a una unidad entera. Las diez pruebas de servidor pasaron (`gauntlet-mount-server.log`).

Pendiente inmediato: `CasterGauntletItem.use` para lanzar el Toque/Fuego montado, trazado que respete obstáculos/alcance, daño y fuego atribuidos al lanzador, consumo de vis con modificadores originales y enfriamiento compartido. Investigar `ItemCaster.getConsumptionModifier` y `CasterManager` antes de fijar sus valores. Añadir ayuda del gesto de montaje en interfaz/tooltip. Actualización todavía en código; última entrega empaquetada = fix36. No declarar funcional el lanzamiento hasta probarlo en mundo. APIs nativas encontradas: Entity.hurtServer(ServerLevel,DamageSource,float), Player.entityInteractionRange(); revisar alcance de bloque que utiliza el original Touch.


Lanzamiento en desarrollo: `TouchFireCasting` conectado a `CasterGauntletItem.use`, con trazado desde los ojos hasta alcance de bloque del jugador, límite por colisión del mundo, búsqueda de entidad con margen 0,25, vis fraccionario local, enfriamiento compartido por jugador (también ante fallo de vis, como ItemCaster), daño FIREBALL atribuido al lanzador, combustión y fuego sobre bloques cuando duración > 0. Hay sonido/partículas nativas provisionales. **No entregar todavía:** primera prueba de daño falló; se neutralizó la armadura innata del Husk de prueba para verificar el daño bruto y se añadió diagnóstico de vis/salud/fuego. Revisar `casting-server.log`. Falta validar obstáculos, límite de alcance, vis insuficiente y ambas manos. Los descuentos de equipo y agotamiento de vis originales todavía no están aplicados; la base consume el coste íntegro. Añadir tooltip/instrucciones de montaje. Última entrega estable: fix36.


Corrección/verificación del primer lanzamiento: el raycast encontraba terreno prácticamente en el punto de origen del jugador de ensayo; se despejó explícitamente el corredor de prueba y se neutralizó la armadura del Husk para medir el daño bruto. Ahora las diez pruebas de servidor pasan (`casting-server.log`): foco montado → lanzamiento, 0,8 vis, cuatro puntos de daño, combustión y rechazo de repetición por enfriamiento. La producción compila (`casting-build.log`). Mantener pendientes las pruebas explícitas de obstáculos, alcance, ausencia de vis y ambas manos, descuentos de equipo/agotamiento y ayuda de montaje antes de empaquetar fix37. El lanzamiento ahora tiene una comprobación inicial positiva; no cerrar aún el early game.


## Avance fix37

Guantelete nativo con montaje/intercambio/retirada del foco completo y lanzamiento Toque/Fuego. Pruebas de obstáculos, alcance, coste fraccionario, falta de aura y enfriamiento entre manos superadas. Diez pruebas de servidor pasan. Descuentos de equipo/agotamiento y motor general de nodos siguen pendientes. Próximo paso: aspectos derivados de recetas y recorrido integral temprano. No se cierra todavía el early game.


## Aspectos derivados en desarrollo tras fix37

`RecipeAspectMath` implementa la contabilidad original: suma de ingredientes, resta de objetos devueltos, factor 75 % dividido entre unidades producidas, tratamiento original de fracciones entre 0,75 y 1, aporte de Praecantatio por vis de receta arcana y selección de la receta de menor suma positiva. Las pruebas de cálculo pasaron con las diez verificaciones de servidor (`aspect-math-server.log`).

`RecipeAspectResolver` añade evaluación recursiva de un grafo de recetas con prioridad de etiquetas explícitas, detección de ciclos, límite de profundidad 100 y caché solo de consultas superiores, evitando almacenar resultados parciales dependientes del camino de evaluación. Pruebas de cadena log→plank→table, ciclos, entradas desconocidas y etiquetas explícitas en `aspect-graph-server.log`.

**Todavía no conectado a las recetas reales ni a ScanRegistry:** próximo paso indispensable es construir el grafo desde RecipeManager, incorporar etiquetas base originales y adiciones complejas (papel, mesa, etc.), incluir las asignaciones de copia del original como mesa arcana = mesa de fabricación + Praecantatio5 + Auram5, y sincronizar el mapa generado al cliente para que los filtros de ranuras del horno sean coherentes. Mantener el horno/crisol/taumómetro usando una única fuente de aspectos. No marcar aún los aspectos derivados como funcionales ni publicar fix38 solo por estas clases auxiliares. La última entrega sigue siendo fix37.


`NativeRecipeAspectGraph.build(RecipeManager)` ya extrae recetas cargadas normales shaped/shapeless, preserva huecos/dimensiones de las shaped, elige el primer objeto de cada ingrediente como TC6, deduce unidades producidas y objetos devueltos. Las diez pruebas de servidor pasan (`native-aspect-graph-server.log`), incluyendo conversión real de lingote de hierro a nueve pepitas (Metallum1) y prioridad del registro explícito del lingote frente al ciclo de compactación/descompactación.

**Aún se invoca solo en verificación, no por ScanRegistry**. Excluye recetas especiales, arcanas y entradas de cristales/frascos por aspecto que todavía requieren adaptador con claves de componentes. Antes de activar el mapa: completar etiquetas explícitas/copias/adiciones originales, construirlo en inicio/recarga de recursos, almacenar datos por servidor con limpieza al cerrar y sincronizar los valores al cliente por payload. Si se usa un mapa estático global, separar explícitos de derivados y evitar que sobrevivan datos de mundos/servidores anteriores. No inferir que todos los objetos ahora tienen aspectos; siguen usando ScanRegistry original en producción. Última entrega = fix37.


Etiquetas base ampliadas en ScanRegistry: 23 IDs modernos (seis troncos y sus seis bloques de madera de las familias históricas; arena/arena roja; pedernal, hilo, bola de slime, hueso, huevo, ojo de araña, pólvora, caña de azúcar y saco de tinta). Valores tomados de ConfigAspects TC6: en particular `logWood` tiene **Herba20**, no Arbor. Estas asignaciones explícitas sí se usan ya en producción por horno/crisol/taumómetro, en ambos lados, sin necesidad de sincronización adicional. Las diez pruebas pasan (`base-aspects-server.log`). El adaptador de recetas reales deriva oak_planks = Herba3 y la parte de ingredientes del papel = Herba3/Aqua2/Aer1; el papel definitivo todavía requiere la adición compleja Cognitio2.

Próximo paso: incorporar adiciones complejas originales al resolver, las copias de aspectos (mesa arcana desde crafting_table, etc.) y sincronización del mapa derivado. No publicar el papel sin Cognitio2 como si fuera el conjunto original. Las 23 etiquetas base actuales no implican cobertura completa de ConfigAspects ni activación del cálculo dinámico. Entrega estable sigue fix37; trabajo posterior en fuente con versión provisional igual, pendiente de paquete coherente fix38.


Adiciones/copias verificadas: `RecipeAspectResolver` acepta un mapa inmutable de adiciones complejas y otro de copias. Las adiciones se aplican antes de usar el objeto como ingrediente en otra receta. Se mantienen prioritarias las etiquetas explícitas. El adaptador real incluye papel Cognitio2, mesa de fabricación Fabrico20, estantería Cognitio8, antorcha Lux5; mesa arcana copia crafting_table y añade Praecantatio5/Auram5 sin factor 75 % adicional. Pruebas exactas: papel Herba3/Aqua2/Aer1/Cognitio2; crafting_table Herba9/Fabrico20; arcane_workbench Herba9/Fabrico20/Praecantatio5/Auram5. Las diez pruebas pasan (`complex-aspects-server.log`).

Esto **todavía no activa la destilación de la mesa arcana**: el adaptador se invoca solo en verificación. Siguiente trabajo: un mapa derivado separado de ScanRegistry.ENTRIES, construir/sustituir sus valores en inicio/recarga (conservar explícitos), payload de aspectos al cliente para igualdad de filtros de ranura, limpieza en desconexión/cierre, y prueba de destilación real de mesa arcana con Auram. Luego completar adaptadores de recetas especiales/arcanas y cristales por componente. No dar por cerrados aspectos derivados ni early game. Última entrega fix37.


Activación inicial: `DerivedAspectEvents` construye y sustituye un mapa de 14 objetos al entrar/recargar datos mediante OnDatapackSyncEvent y envía `AspectSnapshotPayload` al jugador o a todos en recarga. Incluye seis familias de tablones, papel, crafting_table, bookshelf, torch, stick, iron_nugget, gold_nugget y arcane_workbench. `ScanRegistry.get` conserva prioridad de explícitos y consulta el derivado como fallback; `all()` sigue devolviendo solo explícitos para no usar resultados antiguos como semillas. El mapa se valida y reemplaza atómicamente con copias inmutables; se limpia al cerrar servidor/desconectar cliente. Se usa un único mapa estático derivado idéntico para cliente/servidor integrado; la sincronización no cambia contenido en ese caso. Para servidor dedicado, procesos separados reciben sus mapas independientes.

Las diez pruebas pasan (`derived-activation-server.log`): mapa con 14 entradas, Auram5 de mesa arcana reconocido por EssentiaSmelterBlockEntity.tags, Cognitio2 del papel reconocido por ScanRegistry, roundtrip del payload mediante RegistryFriendlyByteBuf y limpieza sin borrar etiquetas explícitas. Compilación correcta (`derived-activation-build.log`). **Falta probar conexión real de cliente y destilación efectiva de la mesa arcana**, además de interfaz/captura si se entrega fix38. No afirmar sincronización extremo a extremo solo por el roundtrip del codec.

Próximo paso inmediato: instalar el mapa en prueba, alimentar una mesa arcana y combustible a un horno básico vivo, completar sus 78 ticks (39 aspectos ×2), comprobar consumo, conservación incluyendo flux y salida Auram; después ejecutar cliente de verificación con payload registrado y publicar fix38 con alcance parcial de derivación claro. El mapa está limitado voluntariamente a 14 objetos: no hay todavía cobertura de todas las recetas, recetas arcanas/especiales/cristales por componente. Última entrega estable sigue fix37.


## Avance fix38

23 registros base y mapa activo de 14 derivados iniciales con sincronización, adiciones/copias y limpieza. Destilación real de mesa arcana probada: 78 ticks, salida de Auram y conservación de 39 aspectos incluyendo flux. Diez pruebas de servidor pasan. Pendientes cobertura ampliada, conexión real con recarga y recorrido integral temprano. No se cierra early game.


## Comprobación de jugador nuevo después de fix38

`EarlyFocusVerification.verifyBeginning` parte de un MockServerPlayer supervivencia con investigación vacía; publica el login real, verifica un libro inicial, usa ThaumonomiconItem.use, inicia FIRSTSTEPS mediante ResearchProgression sin desbloquear capítulos a mano, convierte mesa de fabricación con Salis Mundus y comprueba coste/craft history, fabrica el taumómetro por ArcaneWorkbenchBlockEntity pagando exactamente 20 vis y seis cristales, comprueba bloqueo sin observaciones, obtiene observaciones por ScanProgression.record de los objetos registrados y completa FIRSTSTEPS/accede a UNLOCKALCHEMY. Las diez pruebas pasan (`beginning-server.log`).

**Límites del ensayo:** materiales y aura se suministran como fixtures; los escaneos invocan el motor de recompensas, no el raycast del taumómetro. El jugador mock carece de conexión: ThaumonomiconItem envía el snapshot solo cuando hay conexión, preservando el comportamiento de jugadores conectados. No es una sesión manual ni prueba de obtención natural de cada recurso. No se completa aún alquimia, cartas de investigación o foco con este mismo jugador.

Próximo paso para mantener continuidad: hacer que verifyBeginning devuelva su jugador y extender el recorrido con UNLOCKALCHEMY etapa1 (costes de observación), conversión de caldero con Salis, Nitor amarillo y final del capítulo, después mesa de investigación/teorías y altura/resonador/guantelete/foco, usando siempre ResearchProgression y las operaciones reales. Mantener explícitos los fixtures y no dar por cerrado early game sin registrar los límites de la prueba. Última entrega sigue fix38; no publicar fix39 únicamente por el guard del libro.


Recorrido continuo extendido: `verifyBeginning` devuelve el mismo jugador y `verifyInitialAlchemy` paga las observaciones reales (16 Basics + 16 Alchemy), comprueba bloqueo sin crisol, convierte el caldero con Salis y su coste, avanza el capítulo por el craft history y calienta agua suministrada usando una hoguera encendida debajo con el tick real de CrucibleBlockEntity. Disuelve carbón y el primer polvo de glowstone; el segundo es catalizador. Produce Nitor amarillo, cobra 50 mB y conserva Sensus5 residual, termina UNLOCKALCHEMY y entra en UNLOCKAUROMANCY sin asignar capítulos/stages directamente. Las diez pruebas pasan (`alchemy-route-server.log`).

El intento inicial con fuego sin soporte no fue un fallo del crisol: el fuego de la fixture no se mantenía; se sustituyó por una hoguera encendida válida. La ampliación expuso reinicios antiguos de aura mediante truncamiento entero en otras pruebas; todos los reinicios de fixtures de verificación pasan a `aura.spend(aura.vis())` para vaciar también fracciones. Esto modifica el banco de pruebas, no el consumo de las máquinas.

Siguiente paso: con `beginningPlayer` (auromancia etapa1), publicar eventos de altura reales y comprobar los descubrimientos/puertas; luego resonador y guantelete fabricados con mesa arcana y costes originales, primer foco/máquina/hechizo. Añadir el recorrido de investigación con este mismo jugador. Las fixtures todavía suministran materiales/agua/aura, invocan escaneo por motor y ticks de máquina manuales, y no prueban obtención natural ni conexión real. Mantener límites explícitos antes de cerrar early game. Última entrega sigue fix38.


## Recorrido continuo: introducción a la auromancia

`verifyInitialAuromancy` reutiliza el jugador del inicio y la alquimia, sin asignar investigaciones o etapas directamente. Publica PlayerTickEvent.Post en profundidad y altura, verifica que un descubrimiento solo no permite avanzar, fabrica el resonador con 50 vis y Aer/Aqua, y el guantelete con 100 vis y los seis cristales primales. El guantelete consume el resonador recién fabricado y el taumómetro realmente producido al inicio; todos los ingredientes y cristales quedan consumidos. Completa UNLOCKAUROMANCY y abre BASEAUROMANCY. Diez pruebas requeridas pasan (`auromancy-route-server.log`).

El aviso de descubrimiento se envía únicamente si existe conexión; los desbloqueos siguen ejecutándose para jugadores mock. Los materiales, aura y desplazamientos son fixtures. Continúan pendientes con este mismo jugador la mesa de investigación/teorías, fabricación y configuración del primer foco, montaje y lanzamiento; esos sistemas tienen pruebas independientes, pero eso no demuestra todavía el recorrido continuo completo. Tampoco se ha validado obtención natural de recursos ni una sesión de cliente conectado. Última entrega empaquetada: fix38.


## Fix39: recorrido continuo temprano verificado en servidor

El mismo jugador mock supervivencia completa inicio, alquimia, introducción a auromancia, primer foco y una sesión de investigación sin asignar capítulos ni etapas a mano. La prueba recoge el objeto físico producido por el crisol, fabrica un segundo resonador para el manipulador, comprueba la puerta de descubrimiento de fuego y completa BASEAUROMANCY por fabricación real. Configura Touch/Fire mínimo desde el menú: dos niveles, Aversio/Ignis, 43 vis en tres segundos. Monta ese foco en el guantelete fabricado antes y usa el ítem contra un husk: daño4, ignición y 0,8 vis. Después fabrica herramientas de escritura, forma mesa de investigación y termina una sesión con costes de papel/tinta, recursos especializados y cobro único de teorías. Diez pruebas pasan (`continuous-theory-server.log`).

Limitaciones: agua, materiales, aspectos del foco, cristales compuestos, experiencia y aura se suministran; escaneos invocan el motor de recompensas y se simulan desplazamientos/ticks. La piedra/losas/pedestal y herramientas se ensamblan por recetas cargadas, sin simular extracción de materiales ni interfaz de fabricación vanilla; la colocación del manipulador y mesa usa fixtures. El jugador no tiene conexión real. No demuestra obtención natural de todos los recursos, sincronización de una sesión conectada ni partida manual. La investigación se comprueba después del foco; no representa un orden obligatorio de progresión. No se certifica early game completo ni paridad total; pendientes auditoría del suministro natural de componentes, conexión y recorrido manual, y ampliación de cartas/focos/aspectos.


## Fix40: suministro de Aversio para el primer foco

Se incorpora la adición compleja original de Aversio5 de las flechas (ConfigAspects.java de TC6). Su receta normal aporta Terra1, Instrumentum1, Aer1 y Volatus1 tras escalado por cuatro salidas; Herba del palo no alcanza el redondeo mínimo. El mapa activo pasa de 14 a 15 derivados. No se inventan aspectos de armas ni se amplía automáticamente toda la lista.

La prueba del mismo jugador abre BASEALCHEMY por ResearchProgression, ensambla flechas con pedernal/palo/pluma y nueve pepitas de cuarzo por receta cargada, disuelve una flecha en un crisol caliente, cataliza una pepita y recoge el cristal físico de Aversio. Se cobran dos Aversio y 50 mB y quedan tres Aversio. Ese cristal entra en su inventario y se consume después al preparar su primer foco; se elimina la fixture de Aversio para esa preparación. Siguen suministrados Ignis, materias primas, agua/calor, aura y XP, así como los aspectos de fabricación del foco vacío. No es aún prueba de abastecimiento natural completo ni sesión conectada.

Validación fix40: las diez pruebas requeridas pasan (`server-verification-fix40.log`), incluido el uso del cristal producido en el foco continuo; compilación de producción correcta (`build-verification-fix40.log`).


## Comprobación posterior a fix40: materias del foco vacío

La prueba continua deja de inyectar directamente Vitreus20/Praecantatio10/Auram5. Con el mismo jugador, convierte otra mesa de fabricación mediante Salis Mundus, destruye la mesa arcana y recoge su drop real por loot table. Disuelve ese objeto (incluidos sus aspectos derivados), un Salis adicional y cuatro vidrios mediante CrucibleBlockEntity.processOne. Verifica las cantidades requeridas, usa cristal Ordo como catalizador y comprueba 50 mB de coste (900 mB después del Nitor) y 39 aspectos residuales. El mismo foco físico continúa por configuración, montaje y lanzamiento.

Diez pruebas pasan (`focus-materials-server.log`). El primer intento detectó una fixture que sobrescribía el guantelete seleccionado al equipar Salis; se conserva el objeto en inventario antes de convertir. No fue un fallo de fabricación del mod. Siguen suministrados materias primas, agua/aura/experiencia y cristales primales; no se certifica obtención natural, partida manual o conexión real. No hay cambio de producción posterior a fix40; la ampliación del banco de pruebas/documentación queda en el proyecto de trabajo para la siguiente entrega.


## Fix41: prioridad del crisol y cristal Ignis producido

El crisol selecciona ahora la receta compatible, conocida y pagable con mayor suma de aspectos; en empate conserva la primera. Es la regla de ThaumcraftCraftingManager.findMatchingCrucibleRecipe en TC6. Antes tomaba la primera compatible sin comparar el coste. La suma usa long para evitar desbordamiento. Se verifican prioridad, empate y exclusión por elegibilidad, junto al recorrido continuo real.

El jugador produce ahora también Ignis: disuelve un carbón (Ignis10/Potentia10), usa las nueve pepitas de cuarzo producidas por receta, recoge nueve cristales incluyendo Ignis y conserva dos aspectos residuales. Se consumen 450 mB de agua. Se retira la fixture de Ignis de la preparación del foco; ambos cristales requeridos proceden ahora de objetos físicos producidos por crisol. Los aspectos del foco vacío proceden de la mesa arcana convertida/recuperada, Salis y vidrio, según la comprobación posterior a fix40.

Se reubican fixtures de crisol/mesa dentro del área inicial tras un ensayo donde no se recuperó el cristal esperado; no se atribuye ese fallo a una causa de producción no demostrada. Diez pruebas requeridas pasan (`fire-supply-server.log`). Siguen suministrados recursos base, cristales primales para catalizador/construcción, agua, aura y XP; ticks/desplazamientos/escaneos son simulados. No se cierra early game ni se certifica partida manual/conexión real/paridad completa. Última función corregida: prioridad de recetas del crisol. Próximas etapas: abastecimiento base y cobertura de aspectos; ampliación de alquimia/artificio hacia infusión sin convertir registros vacíos en contenido terminado.


## Fix42: receta nativa del taumio e inicio de metalurgia

Se confirma la receta existente `thaumiumingot.json`: crisol, METALLURGY@2, lingote de hierro, Praecantatio5/Terra5, un lingote de taumio. Coincide con ConfigRecipes de TC6. La búsqueda inicial omitió el archivo; comparar el ZIP fix41 confirma que la receta ya existía con esos mismos datos. El cambio de producción del JSON se limita a normalizar espacios/saltos; la novedad funcional de esta entrega es la prueba continua de metalurgia. No se dan por completas herramientas, armaduras, propiedades especiales ni infusión por registrar este material.

La prueba continua inicia METALLURGY sin conceder etapas; disuelve Salis y piedra, verifica que intentar hierro antes de etapa2 no produce taumio ni paga agua (se disuelve como material), disuelve pedernal y fabrica latón, cobra 16 observaciones Alchemy y avanza a etapa2. Fabrica taumio con los aspectos ya presentes, cobra exactamente cinco Magic/cinco Earth y 50 mB y comprueba un lingote físico. Diez pruebas pasan (`metallurgy-server.log`). La teoría requerida para completar el capítulo aún no se incorpora a este paso, ni se certifica abastecimiento natural/partida conectada. Los recursos base, calor/agua/aura y XP permanecen suministrados; early game completo sigue pendiente de comprobación manual y auditoría del suministro natural.


## Fix43: fabricación del horno de esencia de taumio

Se añade la receta ausente `smelter_thaumium.json`, contrastada con ConfigRecipes.EssentiaSmelterThaumium: ESSENTIASMELTERTHAUMIUM, 250 vis, Ignis2, patrón BFB/IGI/III, dos placas de latón, cinco de taumio, horno básico y construcción alquímica. Un horno de taumio por operación. El bloque/procesamiento ya existía; esta entrega habilita su fabricación, no completa accesorios ni toda la etapa.

Una fixture independiente verifica bloqueo sin investigación, fallo sin consumir materiales con 249 vis, éxito con 250, consumo de todos los ingredientes/cristales y craft history. Esta prueba concede el capítulo completado a un jugador separado para aislar la receta: no demuestra aún la cadena continua de obtención de teoría/tubos y desbloqueo del capítulo. Diez pruebas pasan (`smelter-upgrade-server.log`). Se mantienen los límites del recorrido temprano y de las entregas anteriores.


## Fix44: fuelles conectados al horno de esencia

EssentiaSmelterBlockEntity cuenta hasta tres fuelles horizontales orientados hacia el horno, excluye la cara frontal y omite los vecinos alimentados por redstone, como la comprobación usada por depósitos nativos. Aplica la fórmula original de tiempo: entero de aspectos×2×(1−0,125×fuelles), mínimo1 para entrada válida. La interfaz y el procesamiento usan el mismo cálculo. La comprobación se actualiza desde los vecinos actuales, sin cachear una orientación obsoleta. No implementa todavía animación de fuelles ni SmelterAux.

Prueba viva con carbón: duración 40/35/30/25 según cero/uno/dos/tres fuelles, frontal excluido, mala orientación y redstone no cuentan. El carbón permanece hasta tick24 y se procesa en tick25 con tres; conserva los 20 aspectos entre depósito y flux. Diez pruebas pasan (`smelter-bellows-server.log`). El recorrido temprano conserva sus límites de materiales suministrados y jugador mock; no se certifica early game completo ni la etapa completa de alquimia.


## Fix45: accesorios auxiliares del horno de esencia

SmelterAux deja de ser un bloque genérico: tiene orientación horizontal y colocación solo contra caras laterales/posterior de un horno, rechazando frontal y vertical. El horno conserva su salida principal y añade hasta tres columnas de alambiques encima de accesorios orientados hacia él. Cada intervalo original de salida intenta transferir un aspecto por columna sin duplicarlo; no requiere combustible ni inventario separado del accesorio. Se añade la receta original de ImprovedSmelting: 100 vis, Aer1/Terra1, madera de greatwood, filtro, placas latón/hierro, fuelle y construcción alquímica. Estados del modelo rotados por orientación.

Diez pruebas pasan (`smelter-aux-full-server.log`): columnas principal/tres auxiliares, frontal excluido, conservación exacta y receta con puerta de investigación y consumo de costes. La prueba de receta usa un jugador independiente con investigación concedida para aislarla: no certifica la cadena natural de obtención de materiales/capítulo. Una fixture necesitaba ejecutar el tick en el intervalo real de salida; ajusta/restaura tiempo de mundo dentro de la misma llamada. La búsqueda de drop de mesa arcana se delimita al bloque destruido en lugar de incluir bloques vecinos. Pendientes animación/modelo y colisiones fieles del accesorio, pruebas de colocación manual y recorrido conectado. Early game sigue sin certificado manual.


## Fix46: fuentes de Machina y entrada continua a artificio

Palanca y pistón se añaden al mapa de aspectos derivados (17 entradas): adición original Machina5 a palanca y Machina10/Motus10 a pistón. La base de receta mantiene la fórmula75% y redondeo TC6; palanca queda Terra3/Machina5 (el Herba del palo se redondea a cero); pistón Herba6/Terra15/Perditio3/Metallum11/Potentia7 más las adiciones. No se añaden todavía todas las máquinas/recetas.

Para etiquetas modernas, el adaptador prefiere un ingrediente con aspectos explícitos o tablas de las seis maderas históricas; si no lo encuentra conserva el primer candidato. Es una adaptación de compatibilidad: TC6 empleaba el primer candidato histórico, mientras las etiquetas actuales incluyen materiales nuevos no cubiertos. No evalúa exhaustivamente todos los candidatos ni certifica aspectos de contenido moderno.

El mismo jugador del recorrido inicia UNLOCKARTIFICE tras metalurgia2, comprueba bloqueo sin descubrimiento Machine, ensambla una palanca por receta cargada, invoca ScanProgression con sus aspectos reales y completa el capítulo pagando16 observaciones Artifice. No concede manualmente ese capítulo. Sigue pendiente raycast/conexión real/obtención natural de todos los recursos. Diez pruebas requeridas pasan (`artifice-entry-server.log`). Los intentos previos ajustaron el esperado de Herba del palo conforme al redondeo original; no alteraron la fórmula para hacer pasar la prueba.


## Fix47: gafas equipables y descuento de lanzamiento

Goggles deja de ser un objeto genérico: equipamiento de cabeza nativo con armadura1, dureza1, durabilidad350, encantabilidad25 y reparación con lingote de latón, conforme ItemGoggles/ARMORMAT_SPECIAL. Usa un asset moderno de equipo con la textura heredada copiada sin alterar a la ruta humanoid. Receta original UNLOCKARTIFICE: 50 vis, sin cristales, cuero4/latón2/taumómetros2.

Nueva interfaz VisDiscountGear para equipo: el lanzamiento Toque/Fuego suma descuentos de las cuatro ranuras de armadura y aplica el límite mínimo original del10% de consumo. Por ahora el único implementado es Goggles5%; no incluye todavía baubles/otros equipos ni VisExhaust, descuento de banco arcano, HUD de aura/popup de aspectos o soporte de gafas en cascos. No considerar gafas totalmente completas.

La prueba del mismo jugador que abre artificio fabrica las gafas con recursos suministrados, verifica costes/consumo, ranura HEAD, durabilidad y reparación latón; una prueba separada verifica que llevar gafas en inventario no descuenta y equiparlas reduce el lanzamiento real mínimo de0,8 a0,76 vis. Diez pruebas pasan (`goggles-full-server.log`). Textura vestida aún requiere comprobación visual en jugador conectado. Se mantienen límites de obtención natural y sesión manual del recorrido temprano.


## Fix48: carta Trastear de artificio

Se añade CardTinker con sus18 opciones originales de objeto, una inspiración, requisito de posesión que no consume el objeto y recompensa Artificio desde2×entero(sqrt(sumaAspectos)) hasta ese valor+10. La opción se guarda y transmite; la puntuación se vuelve a calcular en activación como el original. Aparece en el mazo normal tras UNLOCKARTIFICE completado. No añade todavía ayudas de yunque/mesa encantamiento ni las otras cartas de artificio. El cálculo depende de la cobertura parcial de aspectos del port: opciones aún sin aspectos reciben rango0–10 según la regla de entrada desconocida; eso no certifica todas sus puntuaciones originales.

Se mantiene compatibilidad de cartas guardadas: kind acepta0..15 y second0..17; las cartas anteriores conservan datos/efectos. En menú se transmite el índice de objeto en los datos reservados para el requisito. Tooltip y nombre localizados en español/inglés. Diez pruebas pasan (`tinker-full-server.log`): requisito sin cobro al faltar, objeto conservado, coste1, score14..24 para pistón en50 semillas, persistencia, activación única y puerta de categoría en200 semillas. Las15 cartas anteriores y los recorridos pasan.


Fix53: estaciones recuperan atlas/materiales y disposición de mesa arcana/horno originales. Auditoría visual por archivo disponible. No certifica fidelidad artística completa ni avance funcional de etapa.


## Presentación fix54

Mapa del Thaumonomicon y lectura en doble página incorporados, con coordenadas y recursos originales. El avance continúa validado por el servidor. Esto no cierra una etapa funcional ni acredita paridad completa; siguen pendientes cartas verticales y páginas ilustradas.


## Presentación fix55

Opciones de teoría convertidas en cartas verticales con papel original, categoría y coste de inspiración. Totales compactos por icono con detalle al pasar el cursor. Se conserva la validación del servidor y el inventario. Diseño adaptado al tamaño de interfaz actual; animación de reparto y paridad completa pendientes.


## Efecto visual fix56

El primer foco de fuego sustituye la partícula vanilla por los diez fotogramas originales de TC6 (640–649 de la rejilla 64×64), duración de diez ticks, alfa 0,7, gravedad −0,2 y frenado 0,75. Recursos binarios conservados. Registro y geometría nativa verificados; mezcla aditiva original, vista integrada en mundo y efectos del manipulador pendientes. Sin cambios en costes, daño o desbloqueos.


## Efecto visual fix57

Espuma del crisol: sprites originales 64–66, duración de 4–6 ticks, tono (0,5; 0,5; 0,7), gravedad y tamaño de crucibleFroth. Solo emite con agua y calor >150, sobre la superficie del modelo de agua actual. Frecuencia adaptada a una emisión cada cinco ticks; azar visual separado del azar de recetas/derrames. Burbujeo por aspecto, rebose, mezcla aditiva original y revisión dentro del mundo siguen pendientes.


## Navegación fix58

Selector directo de categorías con iconos y marcos del navegador TC6. Solo Fundamentos y categorías cuyo UNLOCK esté completo aparecen, como en la referencia; rueda y clic del nombre también recorren únicamente esas categorías. El tooltip muestra investigación completada por el jugador, no porcentaje de implementación del port. Disposición horizontal adaptada; marcadores de novedades y disposición lateral exacta pendientes.


### Fix59 — vista de iconos sin mundo

El mapa extrae modelos nativos de objetos sin exigir un mundo abierto; la verificación prepara sus componentes de prueba. No altera la progresión ni acredita paridad de modelos en mano.

Verificación inicial: preparación nativa de veinte ticks incorporada tras reproducir búsquedas intermitentes sin objetos; cuatro ejecuciones completas posteriores aprobadas. Causa exacta pendiente de confirmación (INCIDENCIA_ENTIDADES_EARLY_GAME.md).


### Fix60 — conexiones originales del mapa

Conexiones con tramos, esquinas curvas y flechas del atlas original, incluida orientación REVERSE; progreso del servidor sin cambios. Enlaces entre hermanos y animaciones pendientes. Tres fotogramas de la guía de infusión documentados; esa etapa no se considera completa.


### Fix61 — relaciones secundarias del mapa

Enlaces siblings sin flecha, sin duplicar relaciones de requisito; tonos originales y coloreado según la etapa indicada por @. Instantánea gráfica ficticia de alquimia para revisar conexiones. No cambia las reglas de progreso ni completa la etapa de infusión.


### Fix62 — lectura y límites de página

Párrafos BR y saltos PAGE originales conservados; páginas vacías explícitas mantienen alineación. Descriptores IMG retirados de la prosa; ilustraciones y ornamentos LINE/DIV completos pendientes. No cambia progresión.


### Fix63 — ilustraciones del libro

Ilustraciones IMG, recortes UV originales, escala proporcional y ornamentos LINE/DIV incorporados a la paginación. Captura de la segunda etapa de investigación teórica; recetas ilustradas e interacción todavía pendientes. Sin cambios de progreso.


### Fix64 — destellos del manipulador

Destellos centrales originales durante trabajo activo, fotogramas 448–456 y caducidad breve; densidad adaptada. Costes/progreso sin cambios. Haces, cristales orbitando, mezcla aditiva exacta y vista completa en mundo pendientes.

## Estado vigente — fix89

El primer foco Toque/Fuego dispone de configuración, fabricación, montaje y lanzamiento con costes. El recorrido automatizado sigue aprobado con jugador mock y recursos base suministrados; no se certifica early game cerrado. Ampliada la derivación activa de aspectos a23 objetos con libro, botella, cubo, cuenco, cofre y horno; diez pruebas requeridas aprobadas. Próximas comprobaciones: abastecimiento natural y cobertura de objetos/requisitos tempranos, con prueba conectada del recorrido completo. Las secciones anteriores son historial, no estado vigente.

## Fix90 — abastecimiento de cristales

Feature de cristales ampliado a seis orientaciones. Verificados el feature real y la recogida por loot en suelo, paredes y techo con un soporte aislado por ensayo. Diez pruebas aprobadas. Conservadas frecuencia, tipos y tamaño; no certificar densidad/biomas originales ni abastecimiento íntegro del recorrido. Requiere chunks nuevos.

## Fix91 — notas del cielo

Escaneo celestial con investigación, papel/tintero, orientación y límite diario. Notas tipadas persistentes deSol/4estrellas/8Lunas con modelos originales en dos capas. Diez tests del servidor aprobados; capítulo concedido solo para aislar el escaneo. Pendiente carta Celestial y recorrido conectado de desbloqueo natural.

## Fix92 — uso de notas en investigación

CardCelestial funcional con dos variantes de notas, coste atómico, una inspiración, categoría dominante y efectos originales. Diez tests de servidor aprobados. El subsistema deja de producir notas sin uso; siguen pendientes recorrido conectado y cobertura global de cartas/ayudas del early game.
