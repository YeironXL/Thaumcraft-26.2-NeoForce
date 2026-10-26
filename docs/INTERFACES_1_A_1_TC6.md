# Revisión de interfaces contra TC6 BETA26

Pedido recibido después de hola; automatización pausada. Referencia de implementación: clases Gui* del código1.12.2 BETA26. No se han visto nuevos videos durante esta revisión. Las comparaciones de posiciones acreditan solo composición, no animaciones o funcionalidad ausentes.

| Interfaz | Medida/composición original | Diferencia pendiente |
|---|---|---|
| Lectura | GuiResearchPage pane256x181, fondo a escala1.3; columnas140 separadas152 | Fix78 recupera geometría y encabezado; controles/progreso siguen adaptados |
| Recetas | GuiResearchPage.drawRecipe, papel255x255 sobre espacio256x256; centro128/128; flechas40/204,232 | Fix78 recupera papel/posiciones; faltan grupos, planos, infusión y control de superposición |
| Mesa arcana | GuiArcaneWorkbench/ContainerArcaneWorkbench, atlas arcaneworkbench | Revisar indicadores, tooltips y fuente además de ranuras |
| Horno | GuiSmelter, atlas gui_smelter176x166 | Revisar etiquetas/colores y correspondencia de variantes |
| Investigación | GuiResearchTable255x255; herramientas16,15; papel224,16; inventario77,190; barra rápida en3x3 a20,190 | Port previo320x236 con inventario estándar y cartas compactas; modificar menú/render juntos, sin alterar índices lógicos |
| Manipulador | GuiFocalManipulator231x231; inventario lateral71x239 a−71,−3; foco31,191 | Port previo220x236, inventario debajo y controles de Touch/Fire; falta editor original y animación |
| Mapa | GuiResearchBrowser con atlas/background/nodos originales | Revisar dimensiones y controles, dibujo/zoom/transiciones |

No marcar1:1 global hasta contrastar capturas bajo igual escala de interfaz y revisar interacción. Conservar guardado, costes y autoridad del servidor al mover ranuras. La tipografía serif aprobada difería de la original; se recuperó minecraft:default para el nuevo pedido literal1:1 tras consultar preferencia sin respuesta. Puede revertirse si el usuario conserva la serif.


Revisión posterior a fix78: retirado el botón gris ancho de categoría señalado por el usuario (Fundamentals); las pestañas de categoría conservan la navegación. Sustituido el botón gris Advance por el control de completar del atlas original (UV84,216,64x12), oculto en investigaciones completas. Cambios comprobados por compilación; pendientes de nueva sesión visual. La sesión abierta sigue usando fix78.

Mapa posterior a fix78: área de pantalla completa con márgenes16; bordes originales en teselas64, franjas22 y esquinas22 (GuiResearchBrowser731–754); categorías x1,y10+24n con hitbox16 y marco22; búsqueda mediante icono nativo UV160,16; eliminados título, barras y botones Center/Done añadidos. Marcos de nodos conservados del atlas; brillo disponible sinusoidal600ms y bloqueo0.3; eliminado borde dorado y marca artificial de completado. Compilación aprobada. Pendiente capturas y cotejo exhaustivo de iconos/variantes. Fuente consultada: código local GuiResearchBrowser; búsqueda web contextual https://ftb.fandom.com/wiki/Thaumonomicon_%28Thaumcraft_6%29 . No se certifica identidad total.


## Fix81

Investigación: fondo255x255 y ranuras originales; botones gui_base37/66 con texto a0.5; papel/inspiración/marca de cartas con escala6 y contenido0.0625. Manipulador: fondo231x231 y frontal gui_wandtable, inventario lateral71x239 y foco31/191; costes con iconos nativos, flechas/confirmar del atlas original; iconos Touch/Fire con colores11371909/16734721 y giro−90. Diez tests del servidor aprobados. Pendientes siguen detallados en estado fix81; no se certifica editor completo, ayudas/cards animations ni1:1 global.
