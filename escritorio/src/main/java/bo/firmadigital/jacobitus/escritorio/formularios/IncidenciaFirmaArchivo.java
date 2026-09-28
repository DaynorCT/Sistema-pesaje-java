package bo.firmadigital.jacobitus.escritorio.formularios;

import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;

final class IncidenciaFirmaArchivo {
    final String ruta;
    final CategoriaErrorFirma categoria;

    IncidenciaFirmaArchivo(String ruta, CategoriaErrorFirma categoria) {
        this.ruta = ruta == null ? "" : ruta;
        this.categoria = categoria == null ? CategoriaErrorFirma.OTRO : categoria;
    }
}
