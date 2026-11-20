package bo.firmadigital.jacobitus.pesaje.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * La configuracion debe seguir siendo compatible con el archivo
 * `config.json` que genera el sistema original (claves en snake_case),
 * para que la instalacion existente se pueda reutilizar tal cual.
 */
public class ConfigSistemaTest {

    private String rutaEjecutableOriginal;
    private File carpetaTemporal;

    @Before
    public void preparar() throws IOException {
        rutaEjecutableOriginal = System.getProperty("pesaje.ejecutable");
        carpetaTemporal = Files.createTempDirectory("pesaje-config-test").toFile();
        System.setProperty("pesaje.ejecutable",
                new File(carpetaTemporal, "pesaje").getAbsolutePath());
    }

    @After
    public void limpiar() {
        if (rutaEjecutableOriginal == null) {
            System.clearProperty("pesaje.ejecutable");
        } else {
            System.setProperty("pesaje.ejecutable", rutaEjecutableOriginal);
        }
        borrar(carpetaTemporal);
    }

    @Test
    public void cargaElArchivoConLasClavesDelSistemaOriginal() throws IOException {
        File ejecutable = new File(carpetaTemporal, "pesaje");
        ejecutable.getParentFile().mkdirs();
        escribir(new File(ejecutable.getParentFile(), "config.json"),
                "{\n"
                        + "  \"url\" : \"http://localhost:3000/\",\n"
                        + "  \"apikey\" : \"abc123\",\n"
                        + "  \"autoenviar\" : false,\n"
                        + "  \"modo_operacion\" : \"AUTONOMO\",\n"
                        + "  \"alwaysontop\" : true,\n"
                        + "  \"timeout_http\" : 2500,\n"
                        + "  \"ciclo_catalogos\" : 3\n"
                        + "}");

        ConfigSistema config = new ConfigSistema();
        config.cargar();

        assertTrue(config.isCargado());
        assertEquals("http://localhost:3000", config.getUrl());
        assertEquals("abc123", config.getApiKey());
        assertFalse(config.isAutoEnviar());
        assertEquals(ModoOperacion.AUTONOMO, config.getModoOperacion());
        assertTrue(config.isAlwaysOnTop());
        assertEquals(2500, config.getTimeoutHttp());
        assertEquals(3, config.getCicloCatalogos());
    }

    @Test
    public void conservaLosValoresPorDefectoSiElArchivoEstaCorrupto() throws IOException {
        File ejecutable = new File(carpetaTemporal, "pesaje");
        ejecutable.getParentFile().mkdirs();
        escribir(new File(ejecutable.getParentFile(), "config.json"), "{ esto no es json");

        ConfigSistema config = new ConfigSistema();
        config.cargar();

        assertFalse(config.isCargado());
        assertEquals(ConfigSistema.URL_POR_DEFECTO, config.getUrl());
        assertEquals(ModoOperacion.INTEGRADO, config.getModoOperacion());
        assertTrue(config.isAutoEnviar());
    }

    @Test
    public void guardaYCargaLoMismo() {
        ConfigSistema config = new ConfigSistema();
        config.setUrl("https://pesaje.example.com/api/");
        config.setApiKey("secreto");
        config.setAutoEnviar(false);
        config.setModoOperacion(ModoOperacion.AUTONOMO);
        config.setTimeoutHttp(1200);
        config.guardar();

        ConfigSistema recargada = new ConfigSistema();
        recargada.cargar();

        assertTrue(recargada.isCargado());
        assertEquals("https://pesaje.example.com/api", recargada.getUrl());
        assertEquals("secreto", recargada.getApiKey());
        assertFalse(recargada.isAutoEnviar());
        assertEquals(ModoOperacion.AUTONOMO, recargada.getModoOperacion());
        assertEquals(1200, recargada.getTimeoutHttp());
    }

    private static void escribir(File archivo, String contenido) throws IOException {
        try (FileWriter writer = new FileWriter(archivo)) {
            writer.write(contenido);
        }
    }

    private static void borrar(File archivo) {
        if (archivo == null || !archivo.exists()) {
            return;
        }
        File[] hijos = archivo.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                borrar(hijo);
            }
        }
        archivo.delete();
    }
}
