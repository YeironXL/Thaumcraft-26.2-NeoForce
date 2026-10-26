# Taumómetro contra TC6 BETA26

Referencia: ItemThaumometer y blockstates/thaumometer.json del código legado.

Implementado en esta revisión: recuperación de scanner.obj y sus dos materiales; loader OBJ moderno; transformaciones iniciales traducidas de unidades de bloque a unidades de display. Rareza UNCOMMON. Alcance de entidades9 bloques, limitado por la primera colisión de bloque. Sonido scan a volumen0.5/tono1. Retirado chat de depuración; progreso y recompensas permanecen en el servidor.

Pendiente de comprobar: carga y capturas del modelo en inventario/ambas manos/tercera persona; orden de las rotaciones y centrado del cargador moderno; transparencia del cristal; objetivo visual y runas; overlay de aspectos; animación aditiva del indicador de aura; escaneos especiales del original y cobertura total de objetos/variantes/entidades. No certificar100% funcional o idéntico. Sonido enviado únicamente al escaneador mediante paquete de sonido.

La sesión de Minecraft abierta aún utiliza fix78. Se necesita reiniciar para la revisión visual y ejecutar los tests del servidor después del cambio de alcance.

Sincronización de aura20 ticks en ambas manos, sin cargar chunks. Descubrimiento FLUX y aviso original implementados. HUD con regiones nativas y escala525; limpieza al salir del servidor.

## Runas en desarrollo después de fix79

ScanRuneParticle porta FXBlockRunes: diez símbolos por uso, glifos0..15 de fila6 del atlas original (UV y0.09375), color rojo/azul aleatorio0.3..1, verde0, planos verticales a rotaciones0/90/180/270, offset0.51 y variación local original, escala0.3, alfa original/2 con rampa de un quinto de duración; gravedad0.03 y caída0.04*gravedad por tick. Duración3*15 para bloques,3*int(altura*15) para entidades. Solo cliente; recompensas siguen en servidor. Compilación aprobada. Prueba del proveedor y capturas aún pendientes de reiniciar el cliente abierto. Resaltado de objetivos aún pendiente.

## Fix83 — canalización solicitada

Uso sostenido con transición de seis ticks y pulso leve; transformación simétrica en primera persona y brazo sostenido en tercera persona. El escaneo ocurre una vez por pulsación. Cambio artístico solicitado, no paridad certificada con TC6. Pendiente observar en un mundo las dos manos y tercera persona.
