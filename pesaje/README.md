# Sistema de Pesaje — Java

Sistema de escritorio para el registro de pesajes con báscula RS232. Es la
recreación en Java/JavaFX del sistema original Lazarus/FPC
(`/Users/jaru/dev/lazarus-pesaje`), sobre la base de build de Jacobitus
Escritorio.

## Requisitos

- OpenJDK 17 (o superior) con `jpackage`
- Gradle 7.6.2 (viene con el wrapper del repo)

El módulo `:pesaje` **no** necesita compilador nativo: a diferencia de
Jacobitus Escritorio, no usa PKCS#11 ni `changepin`, así que no requiere
MSYS2/MinGW ni el submódulo `jacobitus-libreria`.

## Compilación

Desde la raíz del repo:

```bash
./gradlew :pesaje:jar
```

Ejecución en desarrollo:

```bash
./gradlew :pesaje:run
```

Tests:

```bash
./gradlew :pesaje:test
```

## Distribución

Los instaladores se generan en `pesaje/dist/`:

```bash
./gradlew :pesaje:packageApp --app-profile=prod
```

| Sistema | Artefacto | Requisitos |
|---------|-----------|------------|
| Linux | `sistema-pesaje-<version>-x64.deb` | `fakeroot`/`dpkg` |
| macOS | `sistema-pesaje-<version>-<arch>.pkg` | Xcode Command Line Tools |
| Windows | `sistema-pesaje-<version>-x64.msi` | WiX 3.11.x (ver `README-WINDOWS.md`) |

Perfiles disponibles: `prod` (por defecto), `dev`, `test`, `demo`.

Otros paquetes: `:pesaje:packageRpm` (RPM) y `:pesaje:packageArch`
(ArchLinux).

## Estructura

| Paquete | Contenido |
|---------|-----------|
| `bo.firmadigital.jacobitus.pesaje` | punto de entrada |
| `...pesaje.config` | configuración del sistema (`config.json`) |
| `...pesaje.datos` | esquema y acceso a SQLite |
| `...pesaje.balanza` | lectura RS232 y parser de peso |
| `...pesaje.auth` | autenticación local |
| `...pesaje.ui` | shell JavaFX y módulos |

## Configuración

El sistema lee un `config.json` junto al ejecutable. Si esa carpeta no es
escribible, se usa `~/.config/SistemaPesaje/config.json` (Linux) o el
equivalente de la plataforma.

```json
{
  "url": "http://localhost:3000",
  "apikey": "",
  "autoenviar": true,
  "modo_operacion": "INTEGRADO",
  "alwaysontop": false,
  "timeout_http": 4000,
  "ciclo_catalogos": 5
}
```

`modo_operacion` admite `INTEGRADO` (solo captura y envío del peso al
sistema web) y `AUTONOMO` (sistema completo).
