# Incidencia intermitente de entidades en la verificación inicial

Fecha: 4 de octubre de 2026. Producción: fix59.

La primera comprobación de fix59 falló al buscar el objeto físico de una mesa arcana recién destruida. Una repetición pasó. Cuatro ejecuciones adicionales en universos nuevos, con las mismas condiciones, dieron tres resultados correctos y un fallo al buscar un cristal de Aversion recién creado. Por tanto, el síntoma no se limita a la mesa.

Se añadió diagnóstico de objetos cercanos al fallo de recogida de la mesa. No se cambió la condición de éxito: continúa exigiendo exactamente un objeto físico y su consumo posterior en el crisol.

Se añadió `setup_ticks: 20` a la instancia de prueba `early_focus`, campo nativo de TestData que espera veinte ticks antes de iniciar el recorrido. La hipótesis es que el inicio inmediato coincide en algunos mundos con la preparación de las entidades de los chunks. PersistentEntitySectionManager registra las entidades visibles según el estado de sus secciones. Esta explicación todavía no se ha demostrado en una captura del fallo.

No se cambiaron recetas, producción de objetos, reglas del mundo ni lógica de juego. El código de verificación se excluye de la entrega de producción. Una serie finita de ejecuciones correctas solo acredita estabilidad observada, no ausencia definitiva de errores.

Resultado: las cuatro ejecuciones posteriores con preparación de veinte ticks pasaron las diez pruebas requeridas. Los ocho registros de comparación se conservan en outputs/validacion-fix59. La causa exacta continúa como hipótesis y no se afirma resuelta definitivamente.
