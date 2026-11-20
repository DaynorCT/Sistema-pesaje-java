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
| T2 | S2 | Esquema SQLite (18 tablas de `schema.sql`) + capa de datos: `Database`, migraciones, paths por SO, backup, seed admin | inline | `feat(pesaje): esquema sqlite y capa de datos` |
| T3 | S2 | Autenticación: PBKDF2 + salt (no SHA1), login, roles, verificación de contraseña para cambio de modo | inline | `feat(pesaje): autenticacion local con pbkdf2` |
| T4 | S2 | Configuración: `config.json` (url, apikey, autoenviar, modo_operacion, timeout) + `config_balanza` en BD + `Config` de Jacobitus | inline | `feat(pesaje): configuracion de sistema y balanza` |
| T5 | S3,S4 | Shell JavaFX: `Main`, login, sidebar con navegación, submenú Catálogo, menú de usuario, carga de logo | inline | `feat(pesaje): shell de escritorio y navegacion` |
| T6 | S2 | Serial RS232 (jSerialComm) + parser único configurable (AUTO o por posición) con logger de tramas | inline | `feat(pesaje): lectura rs232 y parser de peso` |
| T7 | S2 | Módulo Pesaje: captura bruto/tara, combos (vehículo/chofer/proveedor/producto/origen/destino), grilla, finalizar/anular, quick-add | inline | `feat(pesaje): modulo de pesaje` |
| T8 | S2 | Catálogos CRUD: ABM genérico + empresas, choferes, proveedores, vehículos, usuarios (soft delete, badges, logo) | inline | `feat(pesaje): catalogos abm` |
| T9 | S2 | Dashboard con estadísticas | inline | `feat(pesaje): dashboard` |
| T10 | S2 | Reportes PDF: boleta de pesaje (fppdf→PDFBox) y reporte por rango de fechas/placa | inline | `feat(pesaje): boleta y reporte en pdf` |
| T11 | S2 | Sincronización con la web: token+refresh, delta pull por cursor, push idempotente (`client_event_id`), outbox durable, backoff con jitter, LWW por `version`, conflictos | writer | `feat(pesaje): sincronizacion con la web` |
| T12 | S2,S3 | Modo integrado: pantalla mínima (conectar→capturar→enviar), menú engranaje, conmutación INTEGRADO/AUTONOMO con contraseña | inline | `feat(pesaje): modo integrado` |
| T13 | S5,S3 | Instaladores: `packageApp`/`packageRpm`/`packageArch` del módulo pesaje + guía de compilación en Windows (WiX, MSYS2) | inline | `build(pesaje): instaladores y guia de windows` |
| T14 | S2,S3 | Verificación: tests de dominio (parser, pesaje, sync policy, auth), build limpio, smoke de arranque | inline | `test(pesaje): cobertura de dominio y smoke de arranque` |

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
E2 (2026-11-20, T1): `:pesaje:jar` y `:pesaje:copyAllDependencies` en verde; el jar trae
`jacobitus.css` y `logo-agetic.png` copiados desde los recursos de `escritorio` (fuente
única de verdad) y el manifiesto con `Main-Class` y `Class-Path` a `dependencias/`. El
slice de T1 quedó por encima de las ~400 líneas porque la mayoría son copias literales
(licencia, plantillas de empaquetado y el template WiX); el código propio es la
convención de build (~680 líneas) más el módulo y el shell mínimo.
D5 (2026-11-20, T4): el `config.json` conserva las claves del sistema original
(`modo_operacion`, `timeout_http`, `ciclo_catalogos`, …) para que el archivo ya existente
sirva sin conversiones; si la carpeta del ejecutable no es escribible se guarda en la
carpeta de datos del usuario y se avisa por log en vez de fallar en silencio.
D6 (2026-11-20, T1): el módulo `:pesaje` no usa `org.openjfx.javafxplugin`; resuelve JavaFX
con el clasificador de plataforma explícito. Motivo: dos versiones del mismo plugin en un
mismo build es un error de Gradle y `:pesaje` no debe quedar atado a la versión que aplica
`:escritorio`.
