package bo.firmadigital.jacobitus.pesaje.balanza;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

/**
 * Pruebas de la fuente de peso simulada.
 *
 * La fuente simulada existe para poder ejercitar el flujo de pesaje sin
 * hardware: entrega tramas con el mismo formato que la bascula real, de
 * modo que el parser unico las interpreta igual. Nunca se usa por defecto
 * en produccion.
 */
public class FuentePesoSimuladaTest {

    @Test
    public void laFuenteSimuladaEntregaTramasQueElParserInterpreta() {
        FuentePesoSimulada fuente = new FuentePesoSimulada(1234L);
        ParserPeso parser = new ParserPeso(new ConfigBalanza());

        fuente.conectar();
        String trama = fuente.leerTrama();

        Optional<Integer> peso = parser.parsear(trama);
        assertTrue("La trama simulada debe contener un peso: " + trama, peso.isPresent());
        assertEquals(Optional.of(fuente.getUltimoPeso()), peso);
    }

    @Test
    public void laFuenteSimuladaEsDeterministaConLaMismaSemilla() {
        FuentePesoSimulada primera = new FuentePesoSimulada(42L);
        FuentePesoSimulada segunda = new FuentePesoSimulada(42L);

        primera.conectar();
        segunda.conectar();

        for (int i = 0; i < 5; i++) {
            assertEquals(primera.leerTrama(), segunda.leerTrama());
        }
    }

    @Test
    public void laFuenteSimuladaNoEntregaTramasSiNoEstaConectada() {
        FuentePesoSimulada fuente = new FuentePesoSimulada(7L);

        assertFalse(fuente.estaConectado());
        assertEquals("", fuente.leerTrama());
    }

    @Test
    public void laFuenteSimuladaGeneraPesosEnElRangoDelOriginal() {
        FuentePesoSimulada fuente = new FuentePesoSimulada(99L);
        fuente.conectar();

        for (int i = 0; i < 50; i++) {
            fuente.leerTrama();
            int peso = fuente.getUltimoPeso();
            assertTrue("Peso fuera de rango: " + peso, peso >= 1000 && peso <= 5000);
        }
    }
}
