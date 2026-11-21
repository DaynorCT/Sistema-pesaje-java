package bo.firmadigital.jacobitus.pesaje.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import bo.firmadigital.jacobitus.pesaje.datos.Database;

/**
 * Pruebas de la aplicacion de los cambios que llegan del servidor.
 */
public class AplicadorDeltaTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Database base;

    @Before
    public void prepararBaseTemporal() {
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA,
                new java.io.File(carpeta.getRoot(), Database.ARCHIVO_BASE).getAbsolutePath());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    private org.codehaus.jackson.JsonNode fila(String json) {
        return Json.leer(json);
    }

    @Test
    public void aplicaUnVehiculoDelServidor() {
        AplicadorDelta aplicador = new AplicadorDelta();
        boolean aplicado = aplicador.aplicarCatalogo("vehiculos",
                fila("{\"id\":1,\"placa\":\"VGI-233\",\"tipo_vehiculo\":\"VOLVO\","
                        + "\"tara\":3030,\"estado\":\"ACTIVO\",\"version\":1}"));
        assertTrue(aplicado);
        List<String> placas = base.sql().consultar(
                "SELECT placa FROM vehiculos WHERE id = 1", r -> r.getString("placa"));
        assertEquals(1, placas.size());
        assertEquals("VGI-233", placas.get(0));
    }

    @Test
    public void laVersionDelServidorGanaSobreElCambioLocal() {
        base.sql().insertar(
                "INSERT INTO productos (id, nombre, descripcion, estado, version, pendiente_push)"
                        + " VALUES (5, 'Local', 'sin enviar', 'ACTIVO', 1, 1)");
        AplicadorDelta aplicador = new AplicadorDelta();
        boolean aplicado = aplicador.aplicarCatalogo("productos",
                fila("{\"id\":5,\"nombre\":\"Del servidor\",\"descripcion\":\"nuevo\","
                        + "\"estado\":\"ACTIVO\",\"version\":2}"));
        assertTrue(aplicado);
        List<String> nombres = base.sql().consultar(
                "SELECT nombre FROM productos WHERE id = 5", r -> r.getString("nombre"));
        assertEquals("Del servidor", nombres.get(0));
        // El conflicto queda registrado para que el operador lo vea.
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM sync_conflictos"));
    }

    @Test
    public void unPesajeSinVehiculoNoRompeLaSincronizacion() {
        AplicadorDelta aplicador = new AplicadorDelta();
        // El esquema local exige vehiculo (NOT NULL), igual que el del
        // sistema original: el pesaje se salta, pero no revienta.
        boolean aplicado = aplicador.aplicarPesaje(
                fila("{\"id\":9,\"guia\":\"PESO-2026-000009\",\"vehiculo_id\":null,"
                        + "\"peso_bruto\":100,\"tara\":10}"));
        assertFalse(aplicado);
        assertEquals(0L, base.sql().contar("SELECT COUNT(*) FROM pesajes"));
        // Y queda registrado como conflicto, no se pierde en silencio.
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM sync_conflictos"));
    }

    @Test
    public void guardaElCursorDeSincronizacion() {
        AplicadorDelta aplicador = new AplicadorDelta();
        assertEquals("", aplicador.leerCursor("productos"));
        aplicador.guardarCursor("productos", "42");
        assertEquals("42", aplicador.leerCursor("productos"));
        aplicador.guardarCursor("productos", "43");
        assertEquals("43", aplicador.leerCursor("productos"));
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM sync_cursor"));
    }
}
