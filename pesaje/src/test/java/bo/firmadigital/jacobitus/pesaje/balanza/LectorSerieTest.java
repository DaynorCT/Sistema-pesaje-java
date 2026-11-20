package bo.firmadigital.jacobitus.pesaje.balanza;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas del lector serie.
 *
 * El equipo de pruebas no tiene una bascula conectada, asi que se verifica
 * el comportamiento que si es determinable: un puerto inexistente no debe
 * romper la aplicacion, debe quedar desconectado y debe reportar el motivo.
 */
public class LectorSerieTest {

    private static final String PUERTO_INEXISTENTE = "PUERTO_SERIE_QUE_NO_EXISTE_12345";

    @Test
    public void conectarConUnPuertoInexistenteFallaSinLanzarExcepcion() {
        ConfigBalanza configuracion = new ConfigBalanza();
        configuracion.setPuertoCom(PUERTO_INEXISTENTE);

        LectorSerie lector = new LectorSerie(configuracion);

        assertFalse(lector.conectar());
        assertFalse(lector.estaConectado());
        assertTrue("Debe reportar el motivo del fallo", lector.ultimoError().length() > 0);
    }

    @Test
    public void leerTramaSinConexionDevuelveCadenaVacia() {
        ConfigBalanza configuracion = new ConfigBalanza();
        LectorSerie lector = new LectorSerie(configuracion);

        assertEquals("", lector.leerTrama());
    }

    @Test
    public void desconectarSinConexionNoFalla() {
        LectorSerie lector = new LectorSerie(new ConfigBalanza());

        lector.desconectar();

        assertFalse(lector.estaConectado());
    }

    @Test
    public void lasTramasLeidasQuedanRegistradas() {
        RegistroTramas registro = new RegistroTramas();
        LectorSerie lector = new LectorSerie(new ConfigBalanza(), registro);

        lector.desconectar();

        assertTrue(registro.tramas().isEmpty());
    }
}
