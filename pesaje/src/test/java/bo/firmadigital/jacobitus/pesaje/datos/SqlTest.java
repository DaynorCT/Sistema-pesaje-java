package bo.firmadigital.jacobitus.pesaje.datos;

import java.io.File;
import java.nio.file.Path;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Pruebas de los helpers de SQL: consultas con mapeador, actualizacion,
 * insercion con id generado y manejo explicito de valores nulos.
 *
 * Igual que en {@link DatabaseTest}, la base es temporal y esta fuera del
 * repositorio.
 */
public class SqlTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Database base;

    @Before
    public void prepararBaseTemporal() {
        Path rutaBase = new File(carpeta.getRoot(), Database.ARCHIVO_BASE).toPath();
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA, rutaBase.toString());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    @Test
    public void insertarDevuelveElIdGenerado() {
        long id = base.sql().insertar(
                "INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                "Ana", "ACTIVO", "2026-11-20 12:00:00");

        assertTrue("El id generado debe ser positivo", id > 0);
        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM personas WHERE id = ?", id));
    }

    @Test
    public void actualizarDevuelveLasFilasAfectadas() {
        long id = base.sql().insertar(
                "INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                "Luis", "ACTIVO", "2026-11-20 12:00:00");

        int filas = base.sql().actualizar("UPDATE personas SET estado = ? WHERE id = ?", "INACTIVO", id);

        assertEquals(1, filas);
        List<String> estados = base.sql().consultar("SELECT estado FROM personas WHERE id = ?",
                rs -> rs.getString("estado"), id);
        assertEquals(Arrays.asList("INACTIVO"), estados);
    }

    @Test
    public void consultarUsaElMapeadorIndicado() {
        base.sql().insertar("INSERT INTO vehiculos (placa, tipo_vehiculo, tara, estado) VALUES (?, ?, ?, ?)",
                "5678-XYZ", "CAMION", 8200, "ACTIVO");

        List<String> placas = base.sql().consultar("SELECT placa FROM vehiculos ORDER BY id",
                rs -> rs.getString("placa"));

        assertEquals(Arrays.asList("5678-XYZ"), placas);
    }

    @Test
    public void escribeYLeeValoresNulosSinAmbiguedad() {
        long id = base.sql().insertar(
                "INSERT INTO personas (nombre, apellido_paterno, telefono, estado, fecha_creacion) VALUES (?, ?, ?, ?, ?)",
                "Carlos", Sql.nulo(), Sql.nulo(Types.VARCHAR), "ACTIVO", "2026-11-20 12:00:00");

        assertTrue(id > 0);

        List<String> apellidos = base.sql().consultar("SELECT apellido_paterno FROM personas WHERE id = ?",
                rs -> Sql.texto(rs, "apellido_paterno"), id);
        assertEquals(Arrays.asList((String) null), apellidos);

        List<Integer> telefonos = base.sql().consultar("SELECT telefono FROM personas WHERE id = ?",
                rs -> Sql.entero(rs, "telefono"), id);
        assertEquals(Arrays.asList((Integer) null), telefonos);
    }

    @Test
    public void distingueUnValorNuloDeUnValorVacio() {
        base.sql().insertar("INSERT INTO personas (nombre, apellido_paterno, estado, fecha_creacion) VALUES (?, ?, ?, ?)",
                "Vacio", "", "ACTIVO", "2026-11-20 12:00:00");

        List<String> apellidos = base.sql().consultar(
                "SELECT apellido_paterno FROM personas WHERE nombre = ?",
                rs -> Sql.texto(rs, "apellido_paterno"), "Vacio");

        assertEquals(1, apellidos.size());
        assertEquals("", apellidos.get(0));
        assertFalse("Una cadena vacia no es un valor nulo", apellidos.get(0) == null);
    }

    @Test
    public void enlazaLosTiposDeParametroMasUsados() {
        base.sql().insertar(
                "INSERT INTO vehiculos (placa, tipo_vehiculo, tara, estado) VALUES (?, ?, ?, ?)",
                "1111-AAA", "TRACTOR", 9500, "ACTIVO");

        List<Integer> taras = base.sql().consultar("SELECT tara FROM vehiculos",
                rs -> Sql.entero(rs, "tara"));
        assertEquals(Arrays.asList(9500), taras);
    }

    @Test
    public void unaTransaccionSeConfirmaYOtraSeRevierte() {
        base.sql().enTransaccion(sql -> {
            sql.insertar("INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                    "Confirmado", "ACTIVO", "2026-11-20 12:00:00");
            return null;
        });

        assertEquals(1L, base.sql().contar("SELECT COUNT(*) FROM personas WHERE nombre = ?", "Confirmado"));

        try {
            base.sql().enTransaccion(sql -> {
                sql.insertar("INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                        "Reviertido", "ACTIVO", "2026-11-20 12:00:00");
                throw new IllegalStateException("fallo simulado");
            });
            fail("La transaccion deberia haber fallado");
        } catch (IllegalStateException esperado) {
            assertEquals("fallo simulado", esperado.getMessage());
        }

        assertEquals("La transaccion fallida no debe dejar datos",
                0L, base.sql().contar("SELECT COUNT(*) FROM personas WHERE nombre = ?", "Reviertido"));
    }

    @Test
    public void laConexionSeUsaConSentenciasPreparadas() {
        // Un valor con comillas no debe alterar la consulta: la capa de datos
        // no concatena valores en el SQL (D3).
        String peligroso = "Robert'); DROP TABLE personas; --";

        long id = base.sql().insertar(
                "INSERT INTO personas (nombre, estado, fecha_creacion) VALUES (?, ?, ?)",
                peligroso, "ACTIVO", "2026-11-20 12:00:00");

        List<String> nombres = base.sql().consultar("SELECT nombre FROM personas WHERE id = ?",
                rs -> rs.getString("nombre"), id);

        assertEquals(Arrays.asList(peligroso), nombres);
        assertTrue("La tabla debe seguir existiendo",
                base.sql().contar("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'personas'") == 1);
    }

    @Test
    public void leerUnValorNuloDevuelveNullYNoCero() {
        base.sql().insertar("INSERT INTO personas (nombre, apellido_paterno, estado, fecha_creacion) VALUES (?, ?, ?, ?)",
                "Nulos", null, "ACTIVO", "2026-11-20 12:00:00");

        List<Integer> valores = base.sql().consultar(
                "SELECT apellido_paterno FROM personas WHERE nombre = ?",
                rs -> Sql.entero(rs, "apellido_paterno"), "Nulos");

        assertEquals(1, valores.size());
        assertNull("Un NULL de SQL debe leerse como null y no como 0", valores.get(0));
    }
}
