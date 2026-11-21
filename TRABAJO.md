# Sistema de Pesaje — Java

Producto propio de **AGETIC**: aplicación de escritorio para el registro de
pesajes con báscula RS232.

- Base técnica heredada de Jacobitus Escritorio (Gradle multi-módulo, JavaFX,
  `jpackage`, `jacobitus.css`), pero **el módulo `:pesaje` es autónomo**: no
  depende de `:escritorio` ni de la librería nativa de firma digital.
- Copia de referencia de la base (intacta): `../jacobitus-escritorio-base`.

## Estado

- Módulo único: `:pesaje`.
- Documento de la feature (specs, tareas y evidencia): `odd/tasks/pesaje-java.md`.
- Guía de uso y empaquetado: `pesaje/README.md` y `pesaje/README-WINDOWS.md`.
