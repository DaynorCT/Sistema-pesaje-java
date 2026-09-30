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
        private final String codigoDiagnostico;
        private final List<String> nombresArchivos;

        private Grupo(CategoriaErrorFirma categoria, String codigoDiagnostico, List<String> nombresArchivos) {
            this.categoria = categoria;
            this.codigoDiagnostico = codigoDiagnostico;
            this.nombresArchivos = Collections.unmodifiableList(new ArrayList<>(nombresArchivos));
        }

        CategoriaErrorFirma getCategoria() {
            return categoria;
        }

        String getMensajeUsuario() {
            if (codigoDiagnostico != null) {
                return mensajePorCodigo(codigoDiagnostico, categoria);
            }
            return categoria.getMensajeUsuario();
        }

        private String mensajePorCodigo(String codigo, CategoriaErrorFirma categoria) {
            switch (codigo) {
                case "PDF-101":
                    return "El PDF parece dañado o incompleto.";
                case "PDF-102":
                    return "El archivo no parece ser un PDF completo.";
                case "PDF-103":
                    return "El contenido del PDF está incompleto o dañado.";
                case "PDF-104":
                    return "La estructura de este PDF impide firmarlo.";
                case "PDF-105":
                    return "No se identificó una causa específica para el error al firmar el PDF.";
                default:
                    return categoria.getMensajeUsuario();
            }
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
        Map<CategoriaErrorFirma, Map<String, List<String>>> archivosPorCategoriaYCodigo = new LinkedHashMap<>();
        if (incidencias != null) {
            for (IncidenciaFirmaArchivo incidencia : incidencias) {
                if (incidencia == null) {
                    continue;
                }
                CategoriaErrorFirma categoria = incidencia.categoria == null
                        ? CategoriaErrorFirma.OTRO : incidencia.categoria;
                String nombreArchivo = new File(incidencia.ruta).getName();
                String codigo = incidencia.codigoDiagnostico == null ? "" : incidencia.codigoDiagnostico;
                archivosPorCategoriaYCodigo.computeIfAbsent(categoria, key -> new LinkedHashMap<>())
                        .computeIfAbsent(codigo, key -> new ArrayList<>()).add(nombreArchivo);
            }
        }

        if (archivosPorCategoriaYCodigo.isEmpty()) {
            CategoriaErrorFirma categoria = clasificar(error);
            return new ResultadoErroresFirma(Tipo.GENERAL, categoria, cantidadDocumentos, cantidadFirmados,
                    Collections.emptyList());
        }

        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<CategoriaErrorFirma, Map<String, List<String>>> entrada : archivosPorCategoriaYCodigo
                .entrySet()) {
            for (Map.Entry<String, List<String>> grupoPorCodigo : entrada.getValue().entrySet()) {
                String codigo = grupoPorCodigo.getKey().isEmpty() ? null : grupoPorCodigo.getKey();
                grupos.add(new Grupo(entrada.getKey(), codigo, grupoPorCodigo.getValue()));
            }
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
