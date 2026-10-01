package bo.firmadigital.jacobitus.escritorio.formularios;

import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;

final class IncidenciaFirmaArchivo {
    final String ruta;
    final CategoriaErrorFirma categoria;
    final String codigoDiagnostico;

    IncidenciaFirmaArchivo(String ruta, CategoriaErrorFirma categoria) {
        this(ruta, categoria, null);
    }

    IncidenciaFirmaArchivo(String ruta, CategoriaErrorFirma categoria, String codigoDiagnostico) {
        this.ruta = ruta == null ? "" : ruta;
        this.categoria = categoria == null ? CategoriaErrorFirma.OTRO : categoria;
        this.codigoDiagnostico = codigoDiagnostico;
    }
}
