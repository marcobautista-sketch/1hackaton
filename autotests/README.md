# autotests — la batería de pruebas del TA

**No modifiques nada de esta carpeta.** Es la misma que usa el TA para calificar.
Cambiarla es motivo de anulación de la hackathon.

<!-- Nota de mantenimiento: las clases Checkpoint*.java se generan desde el contrato v1.2. La fe de erratas v1.3 del README principal todavia no esta incorporada; ante diferencias, el contrato vigente es el README principal. -->

---

## Cómo se corre

Necesitas **dos terminales**.

**Terminal 1 — tu aplicación**, desde la raíz del repositorio:

```bash
./mvnw spring-boot:run
```

Déjala corriendo. Si la cierras, los autotests no encuentran nada.

**Terminal 2 — los autotests**, desde la raíz del repositorio:

```bash
cd autotests
./mvnw test
```

En Windows, en la terminal 2:

```
cd autotests
mvnw.cmd test
```

> La ruta importa: los comandos `./mvnw test` y `mvnw.cmd test` se ejecutan **dentro
> de `autotests/`**, no en la raíz. Si los corres en la raíz, estarás ejecutando tus
> propios tests unitarios, que es otra cosa.

---

## Qué vas a ver

Al final de la corrida aparece el marcador:

```
  ──────────────────────────────────────────────────────────────
   TUCKERSOFT · CONTROL DE CALIDAD
   motor: http://localhost:8080        corrida: MUBIC2ZM
  ──────────────────────────────────────────────────────────────

   ★★★☆☆   3 / 5   Prometedor. Pero se repite.

   ✔  ★1  SEGURIDAD    49 comprobaciones
   ✔  ★2  NODOS        37 comprobaciones
   ✔  ★3  PARTIDAS     34 comprobaciones
   ✘  ★4  DECISIONES   100 comprobaciones antes de fallar
        └ "Stefan destruye la camara..." se clasifica como RUPTURA_CUARTA_PARED
   ·  ★5  ASINCRONIA   no evaluado
  ──────────────────────────────────────────────────────────────
```

Más arriba, en la salida de Maven, está el detalle del fallo:

```
  ✘ "Stefan destruye la camara que lo estaba grabando." se clasifica como RUPTURA_CUARTA_PARED
      esperaba : RUPTURA_CUARTA_PARED
      recibido : REBELDIA
      pista    : Este texto cumple DOS reglas: 'camara' (regla 2) y 'destruye' (regla 4).
                 Gana la regla 2 porque se evalua primero.
```

---

## Reglas

1. **Córrelo todas las veces que quieras.** Cada corrida crea sus propios datos con
   un sufijo único (`QA-MUBIC2ZM-...`), así que nunca choca con corridas anteriores
   ni con lo que hayas metido a mano.
2. **Las estrellas van en orden.** Si la ★2 falla, la ★3 en adelante salen como
   *no evaluado*: arregla la ★2 primero.
3. **Solo se muestra el primer fallo de cada estrella.** El resto de comprobaciones
   de esa estrella se omiten para que no te confundan fallos derivados.
4. Los autotests **no tocan** tu código ni tu `pom.xml`. Solo hablan HTTP con tu app
   y escuchan el correo que envías.

## El tablero del auditorio

Al terminar cada corrida, los autotests publican tu resultado en el tablero que se
proyecta en el auditorio. Verás una de estas líneas debajo del marcador:

| Línea | Qué significa |
|:--|:--|
| `tablero: publicado como "G07"` | Listo, ya apareces. |
| `tablero: no se publico — completa equipo.json ...` | Tu `equipo.json` todavía tiene los valores de ejemplo. |
| `tablero: no alcanzable — tus estrellas no cambian` | Sin internet o el tablero no responde. No pasa nada. |

**El tablero es solo informativo.** Lo que cuenta es el resultado en tu terminal y la
revisión del TA. Si el tablero falla, tus tests no se enteran.

Para correr sin publicar nada:

```bash
./mvnw test -Dtablero.url=off
```

## El servidor de correo

Mientras corren, los autotests levantan un **servidor SMTP de pruebas en
`localhost:2525`**. Ahí es donde tiene que llegar tu Informe de Realidad, y de ahí se
comprueban el destinatario, el asunto y el cuerpo.

Por eso tu `.env` lleva `MAIL_HOST=localhost` y `MAIL_PORT=2525`. El usuario y la
contraseña dan igual: ese servidor acepta cualquiera.

> Tu código sí tiene que enviar de verdad, con `JavaMailSender`. Si guardas la fila en
> `RealityLog` sin enviar nada, el correo no llega y la ★5 falla.

Si el puerto 2525 está ocupado en tu máquina:

```bash
./mvnw test -Dsmtp.port=3025
```

y pon ese mismo puerto en `MAIL_PORT` de tu `.env`.

---

## Si algo no funciona

| Mensaje | Qué pasa |
|:--|:--|
| `No hay nadie escuchando en http://localhost:8080` | Tu app no está corriendo, o está en otro puerto. Arráncala en la terminal 1. |
| `Timeout de 30s` | Un endpoint tuyo se quedó colgado. Mira la consola de tu app. |
| Todo en `no evaluado` y la ★1 falla | Empieza por seguridad: registro, login y el filtro de JWT. |
| `Tests run: 0` | Estás corriendo Maven en la carpeta equivocada. Tiene que ser dentro de `autotests/`. |
| `No se pudo levantar el servidor SMTP` | El puerto 2525 está ocupado. Usa `-Dsmtp.port=3025` y ajusta tu `.env`. |

### Apuntar a otro puerto

Si levantaste tu app en un puerto distinto al 8080:

```bash
./mvnw test -Dbase.url=http://localhost:9090
```

---

## Qué comprueba cada estrella

| Estrella | Cubre |
|:--|:--|
| ★1 SEGURIDAD | Registro, login, JWT, roles, el admin del `DataInitializer`, que `password` no se filtre nunca |
| ★2 NODOS | Permisos de admin, valores iniciales, unicidad, validaciones, 404 |
| ★3 PARTIDAS | Valores iniciales, capacidad del nodo, aislamiento entre usuarios, recorrido vacío |
| ★4 DECISIONES | Las cinco reglas de clasificación, precedencia, tildes, stats, nodo destino, entradas corruptas, los tres finales, filtros y paginación |
| ★5 ASINCRONIA | El 201 inmediato, el listener tras el commit, `RealityLog`, **el correo recibido de verdad con su asunto y su cuerpo**, el fallo de correo y el recorrido |
