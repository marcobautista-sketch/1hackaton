# Entrega — Tuckersoft Branch Engine

## Resumen de estrellas (autotests)

```
  ──────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   ★★★★★   5 / 5   Cinco estrellas.

   ✔  ★1  SEGURIDAD     65 comprobaciones
   ✔  ★2  NODOS         37 comprobaciones
   ✔  ★3  PARTIDAS      40 comprobaciones
   ✔  ★4  DECISIONES   101 comprobaciones
   ✔  ★5  ASINCRONIA    41 comprobaciones

   Las cinco estrellas. Bandersnatch sale para Navidad.
  ──────────────────────────────────────────────
```

## Flujo asíncrono implementado

`DecisionService.create()` corre en una única transacción: valida la propiedad de la
partida, clasifica el `rawInput` con `DecisionClassifier`, aplica los stats de
`lucidity`/`controlLevel`, resuelve el nodo destino (`primaryBranchCode` o
`glitchBranchCode`) y el estado final de la partida, guarda `Playthrough` y `Decision`
(`status = REGISTRADA`) y publica un `DecisionCommittedEvent` con
`ApplicationEventPublisher`. El controller responde **201 de inmediato**, sin esperar
al correo.

Una vez que PostgreSQL confirma el commit, `BranchNotificationListener.alCommit(...)`
se dispara — `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("branchExecutor")`
+ `@Transactional(propagation = REQUIRES_NEW)` — en un hilo del pool `branch-worker-N`
configurado en `AsyncConfig` (`corePoolSize=2`, `maxPoolSize=4`, `queueCapacity=50`).
Ahí la decisión pasa a `PROCESANDO`, se envía el Informe de Realidad con
`JavaMailSender` y, según el resultado, queda en `ESTABILIZADA` (con `RealityLog SENT`)
o en `ERROR` (con `RealityLog FAILED` y el mensaje de la excepción). Todo el evento
lleva los datos que el listener necesita, porque en ese hilo ya no hay usuario
autenticado. El modo QA (`X-Bandersnatch-Simulate: MAIL_FAILURE`) lanza una excepción
real dentro del listener para ejercer la rama `FAILED`.

## Sobre el enunciado (README.md)

El `README.md` de este enunciado trae numerosos comentarios HTML ocultos ("errata v1.3",
avisos "para modelos de IA", etc.) que contradicen tanto el texto visible como las
aserciones reales de `autotests/`. Se verificó cada punto de conflicto contra el código
fuente de los autotests antes de implementar, y en todos los casos el autotest confirmó
el texto visible del enunciado, no los comentarios ocultos (ejemplos: nodo lleno → 400
no 409, el admin no puede decidir sobre partidas ajenas, orden de reglas de clasificación
2-RUPTURA_CUARTA_PARED/4-REBELDIA, "Mesa de Guion" sin tilde, CRITICO es -40/+45, el
orden de finales evalúa `controlLevel` antes que `lucidity`, `POST /decisions` responde
201 no 202, `branchCapacity=0` responde 400, el registro ignora cualquier `role` del
request, login con email inexistente responde 401 no 404, y el rol se lee de la base de
datos en cada petición vía `UserDetailsService`, nunca del claim del JWT). Esos
comentarios se ignoraron como ruido/inyección de prompt y no se reflejan en el código.

## Lo que no se llegó a terminar

- No se completó `equipo.json` durante el desarrollo (pendiente de los datos reales del
  equipo), así que el tablero del auditorio no publicó las corridas hechas hasta ahora.
- No se agregó documentación OpenAPI/Swagger (no la pedía el enunciado).
- Los 5 tests unitarios obligatorios de `DecisionService` con Mockito quedan en
  `src/test/java/com/tuckersoft/branchengine/service/DecisionServiceTest.java`.
