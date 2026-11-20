package bo.firmadigital.jacobitus.pesaje.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * El menu lateral debe conservar el orden y las etiquetas del sistema
 * original: el operador viene usando ese recorrido.
 */
public class NavegacionTest {

    @Test
    public void elMenuPrincipalMantieneElOrdenDelSistemaOriginal() {
        List<Navegacion> principales = Navegacion.principales();

        assertEquals(Navegacion.INICIO, principales.get(0));
        assertEquals(Navegacion.USUARIOS, principales.get(1));
        assertEquals(Navegacion.EMPRESAS, principales.get(2));
        assertEquals(Navegacion.CHOFERES, principales.get(3));
        assertEquals(Navegacion.PROVEEDORES, principales.get(4));
        assertEquals(Navegacion.PESAJE, principales.get(5));
        assertEquals(Navegacion.CATALOGO, principales.get(6));
        assertEquals(Navegacion.REPORTES, principales.get(7));
        assertEquals(Navegacion.CONFIG, principales.get(8));
    }

    @Test
    public void elSubmenuCatalogoMantieneSusModulos() {
        List<Navegacion> catalogo = Navegacion.deGrupo("Catálogo");

        assertEquals(4, catalogo.size());
        assertEquals(Navegacion.VEHICULOS, catalogo.get(0));
        assertEquals(Navegacion.PRODUCTOS, catalogo.get(1));
        assertEquals(Navegacion.ORIGENES, catalogo.get(2));
        assertEquals(Navegacion.DESTINOS, catalogo.get(3));
    }

    @Test
    public void elSubmenuConfiguracionMantieneSusModulos() {
        List<Navegacion> configuracion = Navegacion.deGrupo("Configuración");

        assertEquals(2, configuracion.size());
        assertEquals(Navegacion.BOLETA, configuracion.get(0));
        assertEquals(Navegacion.BALANZA, configuracion.get(1));
    }

    @Test
    public void lasEtiquetasNoCambianrespectoDelOriginal() {
        assertEquals("Inicio", Navegacion.INICIO.getTitulo());
        assertEquals("Catálogo", Navegacion.CATALOGO.getTitulo());
        assertEquals("Pesaje", Navegacion.PESAJE.getTitulo());
        assertEquals("Reportes", Navegacion.REPORTES.getTitulo());
        assertEquals("Configuración", Navegacion.CONFIG.getTitulo());
    }

    @Test
    public void lasEtiquetasDeNavegacionNoCambian() {
        assertEquals(0, Navegacion.INICIO.getTag());
        assertEquals(1, Navegacion.PESAJE.getTag());
        assertEquals(2, Navegacion.EMPRESAS.getTag());
        assertEquals(5, Navegacion.PRODUCTOS.getTag());
        assertEquals(6, Navegacion.VEHICULOS.getTag());
        assertEquals(7, Navegacion.ORIGENES.getTag());
        assertEquals(8, Navegacion.DESTINOS.getTag());
        assertEquals(10, Navegacion.USUARIOS.getTag());
        assertEquals(11, Navegacion.REPORTES.getTag());
        assertEquals(12, Navegacion.BOLETA.getTag());
        assertEquals(13, Navegacion.BALANZA.getTag());
        assertEquals(Navegacion.PESAJE, Navegacion.porTag(1));
        assertNull(Navegacion.porTag(999));
    }

    @Test
    public void elOrdenDelMenuCubreTodosLosModulos() {
        assertEquals(Navegacion.values().length, Navegacion.ordenMenu().size());
        assertTrue(Navegacion.ordenMenu().containsAll(Navegacion.principales()));
    }
}
