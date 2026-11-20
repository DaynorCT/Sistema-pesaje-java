package bo.firmadigital.jacobitus.pesaje.balanza;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

/**
 * Pruebas del parser unico de peso.
 *
 * El parser tiene un unico contrato: recibe la trama cruda de la bascula y
 * devuelve el peso en kilos enteros, o vacio si la trama no contiene una
 * lectura utilizable. El metodo de lectura (AUTO o POSICION) se toma de la
 * configuracion, nunca de una heuristica interna.
 */
public class ParserPesoTest {

    private static final String TRAMA_CON_PREFIJO = "PESO: 12345 kg";

    private ConfigBalanza configAuto() {
        ConfigBalanza configuracion = new ConfigBalanza();
        configuracion.setMetodoLectura(MetodoLectura.AUTO);
        return configuracion;
    }

    private ConfigBalanza configPosicion(int inicio, int longitud) {
        ConfigBalanza configuracion = new ConfigBalanza();
        configuracion.setMetodoLectura(MetodoLectura.POSICION);
        configuracion.setPosicionInicio(inicio);
        configuracion.setPosicionLongitud(longitud);
        return configuracion;
    }

    private Optional<Integer> parsearAuto(String trama) {
        return new ParserPeso(configAuto()).parsear(trama);
    }

    private Optional<Integer> parsearPosicion(String trama, int inicio, int longitud) {
        return new ParserPeso(configPosicion(inicio, longitud)).parsear(trama);
    }

    // ───────────────────────────────────────────────────────────────────
    // Metodo AUTO
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void autoExtraeElPesoCuandoLaTramaArrancaConElNumero() {
        assertEquals(Optional.of(1234), parsearAuto("1234 kg"));
    }

    @Test
    public void autoExtraeElPesoCuandoLaTramaTienePrefijoDeTexto() {
        assertEquals(Optional.of(12345), parsearAuto(TRAMA_CON_PREFIJO));
    }

    @Test
    public void autoTruncaLaParteDecimalDelPeso() {
        assertEquals(Optional.of(1234), parsearAuto("ST: 1234.5 kg"));
    }

    @Test
    public void autoDevuelveVacioCuandoLaTramaNoTieneDigitos() {
        assertFalse(parsearAuto("SIN RESPUESTA DE LA BALANZA").isPresent());
    }

    @Test
    public void autoDevuelveVacioCuandoLaTramaEsDemasiadoCorta() {
        assertFalse("Una trama de un solo digito no es una lectura de peso", parsearAuto("7").isPresent());
        assertFalse(parsearAuto("").isPresent());
        assertFalse(parsearAuto("   ").isPresent());
    }

    @Test
    public void autoIgnoraLasSecuenciasDeUnSoloDigitoYTomaLaSiguiente() {
        assertEquals(Optional.of(2345), parsearAuto("1 2345 kg"));
    }

    @Test
    public void autoTomaLaPrimeraSecuenciaNumericaValida() {
        assertEquals(Optional.of(1000), parsearAuto("1000 2000 kg"));
    }

    @Test
    public void autoDevuelveVacioCuandoLaTramaEsNula() {
        assertFalse(parsearAuto(null).isPresent());
    }

    // ───────────────────────────────────────────────────────────────────
    // Metodo POSICION
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void posicionExtraeElPesoDeLaVentanaConfigurada() {
        assertEquals(Optional.of(12345), parsearPosicion(TRAMA_CON_PREFIJO, 7, 5));
    }

    @Test
    public void posicionUsaBaseUnoIgualQueElOriginal() {
        // La ventana arranca en la posicion 8 (base 1) y toma 4 caracteres.
        assertEquals(Optional.of(2345), parsearPosicion(TRAMA_CON_PREFIJO, 8, 4));
    }

    @Test
    public void posicionTruncaLaParteDecimalDelPeso() {
        assertEquals(Optional.of(1234), parsearPosicion("ST: 1234.5 kg", 5, 6));
    }

    @Test
    public void posicionDevuelveVacioCuandoLaVentanaSeSaleDelRango() {
        // La trama tiene 4 caracteres y la ventana pide 5 desde la posicion 8.
        assertFalse(parsearPosicion("1234", 8, 5).isPresent());
    }

    @Test
    public void posicionDevuelveVacioCuandoLaPosicionInicialEsInvalida() {
        assertFalse(parsearPosicion(TRAMA_CON_PREFIJO, 0, 5).isPresent());
        assertFalse(parsearPosicion(TRAMA_CON_PREFIJO, -3, 5).isPresent());
    }

    @Test
    public void posicionDevuelveVacioCuandoLaLongitudEsInvalida() {
        assertFalse(parsearPosicion(TRAMA_CON_PREFIJO, 7, 0).isPresent());
        assertFalse(parsearPosicion(TRAMA_CON_PREFIJO, 7, -2).isPresent());
    }

    @Test
    public void posicionDevuelveVacioCuandoLaVentanaNoContieneUnNumero() {
        assertFalse(parsearPosicion(TRAMA_CON_PREFIJO, 1, 4).isPresent());
    }

    @Test
    public void posicionDevuelveVacioCuandoLaTramaEsNula() {
        assertFalse(parsearPosicion(null, 7, 5).isPresent());
    }

    // ───────────────────────────────────────────────────────────────────
    // Despacho por metodo configurado
    // ───────────────────────────────────────────────────────────────────

    @Test
    public void elParserUsaElMetodoConfiguradoParaDecidirComoExtraeElPeso() {
        String trama = TRAMA_CON_PREFIJO;

        Optional<Integer> automatico = new ParserPeso(configAuto()).parsear(trama);
        Optional<Integer> porPosicion = new ParserPeso(configPosicion(1, 4)).parsear(trama);

        assertEquals(Optional.of(12345), automatico);
        assertFalse("La ventana 'PESO' no contiene un numero", porPosicion.isPresent());
        assertTrue(automatico.isPresent());
    }

    @Test
    public void elParserPorDefectoUsaElMetodoAuto() {
        ConfigBalanza configuracion = new ConfigBalanza();

        assertEquals(MetodoLectura.AUTO, configuracion.getMetodoLectura());
        assertEquals(Optional.of(12345), new ParserPeso(configuracion).parsear(TRAMA_CON_PREFIJO));
    }
}
