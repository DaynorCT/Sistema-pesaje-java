package bo.firmadigital.jacobitus.pesaje.datos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Base64;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Pruebas del repositorio de empresas.
 *
 * Las pruebas trabajan sobre una base temporal fuera del repositorio: la
 * ruta se sobreescribe con la propiedad de sistema {@code pesaje.db.path}
 * (mismo mecanismo que la variable de entorno PESAJE_DB_PATH), de modo que
 * la base real del usuario nunca se toca.
 */
public class RepositorioEmpresaTest {

    /** PNG de 1x1 pixel transparente. */
    private static final byte[] PNG_UN_PIXEL = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    private Database base;

    @Before
    public void prepararBaseTemporal() {
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA,
                new File(carpeta.getRoot(), Database.ARCHIVO_BASE).getAbsolutePath());
        Database.cerrarInstancia();
        base = Database.obtenerInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    @Test
    public void guardaElLogoDeLaEmpresa() {
        RepositorioEmpresa repo = new RepositorioEmpresa();
        String logo = dataUrl(PNG_UN_PIXEL);

        repo.crear("Transporte Andino", "Transporte", "contacto@andino.bo", "70000000", logo);

        List<RegistroEmpresa> empresas = repo.listarTodos();
        assertEquals(1, empresas.size());
        assertEquals("El logo debe quedar guardado", logo, empresas.get(0).getLogo());
    }

    @Test
    public void actualizaElLogoDeLaEmpresa() {
        RepositorioEmpresa repo = new RepositorioEmpresa();
        repo.crear("Transporte Andino", "Transporte", "contacto@andino.bo", "70000000",
                dataUrl(PNG_UN_PIXEL));
        int id = repo.listarTodos().get(0).getId();
        String otroLogo = dataUrl(PNG_UN_PIXEL);

        repo.actualizar(id, "Transporte Andino", "Transporte", "contacto@andino.bo", "70000000",
                "ACTIVO", otroLogo);

        List<RegistroEmpresa> empresas = repo.listarTodos();
        assertEquals(1, empresas.size());
        assertEquals("El logo debe quedar actualizado", otroLogo, empresas.get(0).getLogo());
    }

    @Test
    public void guardaCadenaVaciaCuandoNoHayLogo() {
        RepositorioEmpresa repo = new RepositorioEmpresa();

        repo.crear("Transporte Andino", "Transporte", "contacto@andino.bo", "70000000", null);

        List<RegistroEmpresa> empresas = repo.listarTodos();
        assertEquals(1, empresas.size());
        assertEquals("Sin logo se guarda una cadena vacia", "", empresas.get(0).getLogo());
    }

    @Test
    public void laEmpresaNuevaQuedaActiva() {
        RepositorioEmpresa repo = new RepositorioEmpresa();

        repo.crear("Transporte Andino", "Transporte", "contacto@andino.bo", "70000000", "");

        List<RegistroEmpresa> empresas = repo.listarTodos();
        assertEquals(1, empresas.size());
        assertEquals("La empresa nueva debe quedar activa", "ACTIVO", empresas.get(0).getEstado());
        assertEquals("Transporte Andino", empresas.get(0).getNombreEmpresa());
        assertTrue("La fecha de creacion debe quedar informada",
                base.sql().contar("SELECT COUNT(*) FROM empresas WHERE fecha_creacion IS NOT NULL") == 1);
    }

    @Test
    public void laBajaLogicaNoBorraLaEmpresa() {
        RepositorioEmpresa repo = new RepositorioEmpresa();
        repo.crear("Transporte Andino", "Transporte", "contacto@andino.bo", "70000000", "");
        int id = repo.listarTodos().get(0).getId();

        repo.cambiarEstado(id, "INACTIVO");

        List<RegistroEmpresa> empresas = repo.listarTodos();
        assertEquals("La empresa no debe borrarse", 1, empresas.size());
        assertEquals("INACTIVO", empresas.get(0).getEstado());
    }

    private String dataUrl(byte[] imagen) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imagen);
    }
}
