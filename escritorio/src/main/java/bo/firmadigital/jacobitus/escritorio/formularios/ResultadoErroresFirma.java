package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;
import bo.firmadigital.jacobitus.escritorio.comun.ClasificadorErroresFirma;

/** Modelo de presentación de errores de firma, separado de JavaFX para poder probarlo sin interfaz gráfica. */
final class ResultadoErroresFirma {
    enum Tipo {
        GENERAL,
        ARCHIVOS
    }

    static final class Grupo {
        private final CategoriaErrorFirma categoria;
        private final List<String> nombresArchivos;

        private Grupo(CategoriaErrorFirma categoria, List<String> nombresArchivos) {
            this.categoria = categoria;
            this.nombresArchivos = Collections.unmodifiableList(new ArrayList<>(nombresArchivos));
        }

        CategoriaErrorFirma getCategoria() {
            return categoria;
        }

        String getMensajeUsuario() {
            return categoria.getMensajeUsuario();
        }

        List<String> getNombresArchivos() {
            return nombresArchivos;
        }
    }

    private final Tipo tipo;
    private final CategoriaErrorFirma categoria;
    private final int cantidadDocumentos;
    private final int cantidadFirmados;
    private final List<Grupo> grupos;

    private ResultadoErroresFirma(Tipo tipo, CategoriaErrorFirma categoria, int cantidadDocumentos,
            int cantidadFirmados, List<Grupo> grupos) {
        this.tipo = tipo;
        this.categoria = categoria;
        this.cantidadDocumentos = cantidadDocumentos;
        this.cantidadFirmados = cantidadFirmados;
        this.grupos = Collections.unmodifiableList(new ArrayList<>(grupos));
    }

    static ResultadoErroresFirma crear(Throwable error, List<IncidenciaFirmaArchivo> incidencias,
            int cantidadDocumentos, int cantidadFirmados) {
        Map<CategoriaErrorFirma, List<String>> archivosPorCategoria = new LinkedHashMap<>();
        if (incidencias != null) {
            for (IncidenciaFirmaArchivo incidencia : incidencias) {
                if (incidencia == null) {
                    continue;
                }
                CategoriaErrorFirma categoria = incidencia.categoria == null
                        ? CategoriaErrorFirma.OTRO : incidencia.categoria;
                String nombreArchivo = new File(incidencia.ruta).getName();
                archivosPorCategoria.computeIfAbsent(categoria, key -> new ArrayList<>()).add(nombreArchivo);
            }
        }

        if (archivosPorCategoria.isEmpty()) {
            CategoriaErrorFirma categoria = clasificar(error);
            return new ResultadoErroresFirma(Tipo.GENERAL, categoria, cantidadDocumentos, cantidadFirmados,
                    Collections.emptyList());
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<CategoriaErrorFirma, List<String>> entrada : archivosPorCategoria.entrySet()) {
            grupos.add(new Grupo(entrada.getKey(), entrada.getValue()));
        }
        return new ResultadoErroresFirma(Tipo.ARCHIVOS, null, cantidadDocumentos, cantidadFirmados, grupos);
    }

    static CategoriaErrorFirma clasificar(Throwable error) {
        return ClasificadorErroresFirma.clasificar(error);
    }

    Tipo getTipo() {
        return tipo;
    }

    CategoriaErrorFirma getCategoria() {
        return categoria;
    }

    String getMensajeUsuario() {
        return categoria == null ? null : categoria.getMensajeUsuario();
    }

    int getCantidadDocumentos() {
        return cantidadDocumentos;
    }

    int getCantidadFirmados() {
        return cantidadFirmados;
    }

    int getCantidadFallidos() {
        return grupos.stream().mapToInt(grupo -> grupo.nombresArchivos.size()).sum();
    }

    List<Grupo> getGrupos() {
        return grupos;
    }
}
