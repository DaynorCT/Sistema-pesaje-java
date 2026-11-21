# Sistema de diseño — Jacobitus / Sistema de Pesaje

Tokens desde `pesaje/src/main/java/.../ui/Tema.java` y `pesaje/src/main/resources/pesaje.css`.

## Color

| Token | Valor | Uso |
|-------|-------|-----|
| fondo | #f7f8fa | Ventana |
| tarjeta | #ffffff | Paneles |
| borde | #e0e0e0 | Bordes |
| texto | #1f2933 | Principal |
| texto-secundario | #5f6c7b | Subtítulos |
| titulo | #263238 | Títulos |
| primario | #9a3412 | Acción / menú activo |
| primario-suave | #f6ece6 | Fondo activo |
| exito | #1b6e3c | OK |

## Tipografía

Fuente: **System** (JavaFX `.root`), 13px base; `titulo-modulo` 18px bold; `valor-peso` 42px bold.

## Componentes JavaFX

`tarjeta`, `boton-primario`, `boton-secundario`, `badge-*`, `barra-lateral` — ver `pesaje.css`.

Componentes JavaFX adicionales en `pesaje.css`: `.barra-superior`, `.display-peso`, `.separador-horizontal`, `.tarjeta-sin-padding`.

Los HTML en `design/modo-integrado/` son **referencia de layout**; la app usa solo JavaFX.
