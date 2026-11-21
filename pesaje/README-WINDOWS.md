# Compilar el Sistema de Pesaje en Windows

El instalador de Windows se genera con `jpackage` **desde Windows**: no es
posible cruzar de sistema operativo (a diferencia de la version Lazarus,
que usaba un cross-compiler de Free Pascal).

## Requisitos

1. **OpenJDK 17 o superior** con `jpackage` (incluido en el JDK, no en el
   JRE). Configurar `JAVA_HOME` y `PATH`.
2. **WiX 3.11.x** — necesario porque el build usa el template propio
   `pesaje/plantillas/windows/main.wxs`.
3. **Git** para clonar el repo.

No hace falta MSYS2/MinGW: el módulo `:pesaje` no compila codigo nativo
(el token criptografico y su driver son exclusivos de Jacobitus Escritorio).

## Pasos

```bash
git clone <repo> sistema-pesaje-java
cd sistema-pesaje-java
```

Compilar y ejecutar (un solo lanzador en desarrollo):

```bash
./gradlew :pesaje:jar
./gradlew pesaje:run
```

El MSI instala **un** acceso directo a `sistema-pesaje`; no hay binarios
separados para báscula y oficina.

Generar el instalador:

```bash
./gradlew :pesaje:packageApp --app-profile=prod
```

El instalador queda en `pesaje/dist/`:

```
sistema-pesaje-1.0.0-x64.msi
```

Para 32 bits hay que compilar en una maquina de 32 bits (la arquitectura se
detecta automaticamente y define el nombre del artefacto).

## Notas

- El MSI incluye el runtime de Java embebido: el cliente no necesita
  instalar Java.
- El instalador crea acceso directo en el escritorio y grupo de menus
  "AGETIC".
- El UUID de actualizacion (`--win-upgrade-uuid`) del modulo `:pesaje` es
  propio y no coincide con el de Jacobitus Escritorio: son productos
  distintos y pueden instalarse en la misma maquina.
