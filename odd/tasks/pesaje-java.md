# Sistema de Pesaje — Java (sobre base Jacobitus)

Recrear en Java/JavaFX las funcionalidades del sistema de escritorio Lazarus/FPC
`/Users/jaru/dev/lazarus-pesaje`, usando este repo (Jacobitus Escritorio) como base de
infraestructura y estilo. `lazarus-pesaje` no se modifica: es solo referencia funcional.

## Specs

S1. "sabes esta sera la base para hacer el sistema de pesaje en java"
S2. "quiero de sisteam recuar sus funciaonalsidares" — replicar los módulos, el modelo de
    datos, la lectura de báscula, la sincronización y los reportes del sistema Lazarus,
    con la base técnica de Jacobitus (Gradle multi-módulo, JavaFX, `jpackage`, API local
    Jetty+Jersey, `Config`, `TaskBar`, `AutoInicioHelper`, `jacobitus.css`).
S3. "deja todo como esta ya implementemoslo" — lo existente (`escritorio`,
    `jacobitus-libreria`) queda intacto; el pesaje entra como módulo nuevo `:pesaje`.
S4. "el estilo ya lo tiene este sisteam jacobitus" — la UI del pesaje usa el CSS, recursos
    y convenciones visuales de Jacobitus, no los de Lazarus.
S5. "este siustea de escrito tiuene todo vr isbtar ya dp en windows" — el sistema de pesaje
    debe salir con instalador Windows (MSI) desde el mismo pipeline `jpackage`.

## Tasks

| ID | Spec | Tarea | Ruta | Commit |
|----|------|-------|------|--------|
| T1 | S1,S3,S4 | Módulo Gradle `:pesaje`: `build.gradle`, convención en `buildSrc`, `settings.gradle`, JavaFX 17, pipeline `jpackage` (deb/rpm/arch/msi/pkg) con perfil `--app-profile` | inline | `92657c1` |
| T2 | S2 | Esquema SQLite (18 tablas de `schema.sql`) + capa de datos: `Database`, migraciones, paths por SO, backup, seed admin | inline | `a18e299` |
| T3 | S2 | Autenticación: PBKDF2 + salt (no SHA1), login, roles, verificación de contraseña para cambio de modo | inline | `76649c7` (hash), `c84bb85` (login) |
| T4 | S2 | Configuración: `config.json` (url, apikey, autoenviar, modo_operacion, timeout) + `config_balanza` en BD + `Config` de Jacobitus | inline | `8ada20f` |
| T5 | S3,S4 | Shell JavaFX: `Main`, login, sidebar con navegación, submenú Catálogo, menú de usuario, carga de logo | inline | `f9a0788` |
| T6 | S2 | Serial RS232 (jSerialComm) + parser único configurable (AUTO o por posición) con logger de tramas | inline | `9177b87` |
| T7 | S2 | Módulo Pesaje: captura bruto/tara, combos (vehículo/chofer/proveedor/producto/origen/destino), grilla, finalizar/anular, quick-add | inline | `ac7e6dd` (captura y combos), `7fecba7` (grilla y quick-add) |
| T8 | S2 | Catálogos CRUD: ABM genérico + empresas, choferes, proveedores, vehículos, usuarios (soft delete, badges, logo) | inline | `ac7e6dd` (ABM; falta logo) |
| T9 | S2 | Dashboard con estadísticas | inline | `ac7e6dd` |
| T10 | S2 | Reportes PDF: boleta de pesaje (fppdf→PDFBox) y reporte por rango de fechas/placa | inline | `72cde86` |
| T11 | S2 | Sincronización con la web: token+refresh, delta pull por cursor, push idempotente (`client_event_id`), outbox durable, backoff con jitter, LWW por `version`, conflictos | writer | `9e9ab02` (politica), `e5f95a2` (motor) |
| T12 | S2,S3 | Modo integrado compacto (artboard 02): barra superior, capturar→enviar peso vivo, menú, INTEGRADO/AUTONOMO con contraseña | inline | handoff 02 implementado (sin commit handoff) |
| T13 | S5,S3 | Instaladores: `packageApp`/`packageRpm`/`packageArch` del módulo pesaje + guía de compilación en Windows (WiX, MSYS2) | inline | `96901a7` (guías), `15abf16` (jpackage con module-path) |
| T14 | S2,S3 | Verificación: tests de dominio (parser, pesaje, sync policy, auth), build limpio, smoke de arranque | inline | `15abf16` (smoke `pesaje:run` verde) |

## Log

L1. "sabes esta sera la base para hacer el sistema de pesaje en java me dejo entnder revisa"
L2. "sabes quiero poder usar este como base para hacer esto , relca esto /Users/jaru/dev/lazarus-pesaje , sus mopdulos ecta como esat no me interesa su stylo el estulo ya lotiene este sisteam jacobitus, la cuesn queir de sisteam recuar sus funciaonalsidares me dejo entnder ???"
L3. "dime se pedue calro este siustea de escrito tiuene todo vr isbtar ya dp en windows es as??"
L4. "deja todo como esta vera impolemetyemos no lo que die de esesisytema medejo entender ??"

D1 (2026-11-20, tras L4): `lazarus-pesaje` queda intacto como referencia funcional; se
implementa sobre este repo. `escritorio` y `jacobitus-libreria` no se tocan: el pesaje entra
como módulo Gradle nuevo `:pesaje`, con su propio pipeline `jpackage`. Se conserva el
submódulo `jacobitus-libreria` (firma digital) sin cambios.
D2 (2026-11-20): la UI del pesaje reutiliza `jacobitus.css` y los recursos de Jacobitus;
no se replica el estilo Lazarus (el usuario lo pidió explícitamente).
D3 (2026-11-20): no se replican los defectos del original Lazarus: SHA1+salt → PBKDF2;
peso simulado (`Random(4001)+1000`) → parser real con fuente injectable; SQL por
concatenación → prepared statements; password admin por defecto `admin123` → generado en
el primer arranque.
D4 (2026-11-20): el escritorio del sistema Lazarus es *cliente* de la web
(`/Users/jaru/dev/sistema-pesaje-next`, Next.js). El módulo `:pesaje` replica ese contrato
(POST /api/pesajes, GET /api/sync/:recurso, POST /api/auth/token). La API local estilo
Jacobitus (Jetty en 127.0.0.1:9000) queda como capacidad opcional para consumidores
externos, no como sustituto del sync.
E1 (2026-11-20): entorno verificado — Gradle 7.6.2 sobre Zulu JDK 17 (Java 21 instalado no
sirve: Gradle 7.6.2 no lo soporta), macOS arm64, red a Maven Central OK (200 en
sqlite-jdbc y jSerialComm). `escritorio` compila (BUILD SUCCESSFUL) sin cambios.
E3 (2026-11-20, T3/T4/T11-policy): `76649c7` (hash PBKDF2), `8ada20f` (config), `9e9ab02`
(politica de reintentos), `96901a7` (guias). 39 tests verdes. La politica de reintentos se
portó desde `lazarus-pesaje/tests/sync_policy_test.pas` con el mismo contrato.
E4 (2026-11-20, estrategia de entrega): el corte de ~400 líneas se esta aplicando por
commit (cada commit es una unidad revisable). T1 y los commits de infraestructura quedan
fuera de ese corte porque son copias literales (licencia, plantillas de empaquetado, template
WiX) mas la convención de build. Cuando el usuario pida el PR, la estrategia es PRs
encadenados por grupo de tareas: (1) scaffolding + instaladores, (2) datos + auth, (3) balanza
+ pesaje, (4) catalogos + dashboard, (5) sync, (6) reportes + modo integrado.
D5 (2026-11-20, T4): el `config.json` conserva las claves del sistema original
(`modo_operacion`, `timeout_http`, `ciclo_catalogos`, …) para que el archivo ya existente
sirva sin conversiones; si la carpeta del ejecutable no es escribible se guarda en la
carpeta de datos del usuario y se avisa por log en vez de fallar en silencio.
D6 (2026-11-20, T1): el módulo `:pesaje` no usa `org.openjfx.javafxplugin`; resuelve JavaFX
con el clasificador de plataforma explícito. Motivo: dos versiones del mismo plugin en un
mismo build es un error de Gradle y `:pesaje` no debe quedar atado a la versión que aplica
`:escritorio`.
D7 (2026-11-20, T5/T7/T8): el menú deja de ser una lista de entradas: cada módulo del
sistema original tiene su panel con datos reales de la base local. El alcance se limita a
las funcionalidades del Lazarus; no se agrega firma digital ni otros módulos de Jacobitus.
E5 (2026-11-20, smoke): `gradlew pesaje:run` arranca, muestra el login y la ventana queda
viva; la base se crea en la ruta por defecto
(`~/Library/Application Support/SistemaPesaje/pesaje.db`) y se imprimen el usuario y la
contraseña iniciales del primer arranque. 78 tests verdes.
E6 (2026-11-20, bugs de arranque corregidos): (1) `No main class specified`: el plugin
`application` no leía `mainClassName` desde `ext`; ahora se declara a nivel de proyecto y
se fija `application { mainClass }`. (2) `ErrorDeBaseDeDatos` al abrir la base: en macOS
`/tmp` es un symlink a `/private/tmp` y `Files.createDirectories` lanzaba
`FileAlreadyExistsException`; el directorio padre solo se crea si no existe. (3) JavaFX
solo funciona como módulo: los jars con clasificador de plataforma van en `--module-path`
(tanto en `run` como en `jpackage`), no en el classpath junto a los jars vacíos.
D8 (2026-11-20, T7): el peso de la pantalla de pesaje viene de la báscula real leída por
`ServicioBalanza`; la fuente simulada queda solo para pruebas. El panel cierra la conexión
al navegar (`Cerrable`), para no dejar el puerto serie abierto.
E7 (2026-11-20, T7): la pantalla de pesaje queda con las tres partes del sistema
original — lectura en vivo de la balanza (hilo propio, sin bloquear la interfaz), captura
con combos y neto calculado, y grilla con los pesajes registrados. Se agrega el alta rápida
de vehículo, chofer y proveedor desde la propia pantalla. Verificado con un arranque
JavaFX que construye los 13 paneles (`PANELES OK`) y con `gradlew pesaje:run` (ventana
viva). 81 tests verdes.
E8 (2026-11-20, T11): sincronización verificada de punta a punta contra la web gemela
(`sistema-pesaje-next` en localhost:3000, con una base de prueba aparte). Pull: 12 filas
bajadas (2 vehículos, 1 producto, 1 origen, 1 destino, 2 choferes, 1 proveedor, 4
pesajes) y aplicadas en la base local. Push: el pesaje local llegó a la web
(`id=8`, `guia=PESO-2026-000008`) y el escritorio adoptó la guía del servidor, de modo que
los dos lados muestran la misma. La cola quedó vacía (`pendientes=0`). 88 tests verdes.
E9 (2026-11-20, T11): dos defectos encontrados por esa prueba y corregidos: (1) una fila
que no se podía aplicar cortaba toda la vuelta de sincronización — ahora cada fila se
aísla y el resto sigue; (2) un pesaje del servidor sin vehículo violaba el `NOT NULL` del
esquema local (`pesajes.vehiculo_id`), igual que en el sistema original — ahora se salta y
queda registrado en `sync_conflictos`.
D9 (2026-11-20, T11): los pesajes se envían a `POST /api/pesajes` (idempotente por
`client_event_id`) y los catálogos a `POST /api/sync/:recurso` (resuelve la versión con
LWW), igual que en el sistema original. El token viaja en la cookie `access_token` porque
así lo espera la web (`getTokenFromHeader`).
X1 (2026-11-20, defecto en la web gemela): `POST /api/sync/:recurso` responde 500 al
crear una fila: en `app/api/sync/[recurso]/route.ts` la variable se llama `userId` pero se
usa `usuarioId` (línea 134). Afecta al push de catálogos; el pull no se ve afectado. El
escritorio lo reporta como evento con error y reintenta, y queda listo para cuando se
corrija.
E20 (2026-11-21): ShellPane barra = Navegacion.principales(); Catálogo y Configuración con MenuFlotante; único disparador ./gradlew pesaje:run.
E19 (2026-11-21): ABM sin columna Acciones; solo badge Estado; activar/inactivar por combo + Guardar.
E18 (2026-11-21): ABM con Estado badge + Editar/Activar/Desactivar en todos; toggle sin dialogo; usuario en sesion no se desactiva.
E17 (2026-11-21): tabla Usuarios alineada a Lazarus (CI, nombres completos, correo, telefono, rol, estado).
E16 (2026-11-21): `DialogoAviso` reemplaza `Alert` en modo integrado y contraseña;
modales con `boton-primario`/`boton-secundario` y `pesaje.css`.
E15 (2026-11-21): política único lanzador documentada en `pesaje/README.md`;
`pesaje:run` / instalador `sistema-pesaje`; eliminado `mostrarVistaSegunConfig`.
E14 (2026-11-21): login siempre abre `ModoIntegradoPane`; ya no respeta `AUTONOMO` en
config al iniciar sesion; ir al shell no persiste modo en config.
E13 (2026-11-21, handoff T12): artboard `02-barra-superior-compacto.html` mapeado a
`ModoIntegradoPane` + clases `pesaje.css` (`barra-superior`, `display-peso`,
`valor-peso`, `boton-*`, `badge-*`). Ventana 480×680, `alwaysontop` en config. Sin
previewUrl (app JavaFX de escritorio).
E12 (2026-11-21, T12 implementación): `ModoIntegradoPane` (480×680, always-on-top
según config), arranque con `modo_operacion: INTEGRADO`, envío POST
`/api/pesajes/ultimo-peso`, menú engranaje con contraseña hacia sistema completo.
UI solo con clases `pesaje.css` / Jacobitus en JavaFX; los HTML en `design/modo-integrado/` son referencia de layout (canvas), no se copian a la app.
E11 (2026-11-21, T12 diseño): mockups en `design/modo-integrado/` — barra superior
como referencia Lazarus con paleta Jacobitus; artboards `01-barra-superior-operario` y
`02-barra-superior-compacto`. Implementación: `ModoIntegradoPane`.
E10 (2026-11-20, T10): la boleta y el reporte en PDF quedaron implementados con PDFBox y
verificados generando los archivos y **renderizandolos a imagen**: la boleta respeta el
diseno del original (carta apaisada, encabezado de tres columnas, dos columnas de datos,
bloque de pesos y firmas) y el reporte el suyo (carta vertical, bloque por pesaje y total
de registros). Los PDF van a `Documentos/SistemaPesaje`. 92 tests verdes.
