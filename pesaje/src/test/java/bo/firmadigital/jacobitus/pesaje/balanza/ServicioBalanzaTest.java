package bo.firmadigital.jacobitus.pesaje.balanza;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Pruebas del servicio de lectura en vivo del peso.
 *
 * Se inyecta un lector falso: el servicio no depende del puerto serie y
 * se puede ejercitar sin hardware.
 */
public class ServicioBalanzaTest {

    /** Lector falso que entrega tramas con un peso fijo. */
    private static final class LectorFalso implements LectorPeso {
        private final String trama;
        private boolean conectado;

        LectorFalso(String trama) {
            this.trama = trama;
        }

        @Override
        public boolean conectar() {
            conectado = true;
            return true;
        }

        @Override
        public void desconectar() {
            conectado = false;
        }

        @Override
        public String leerTrama() {
            return conectado ? trama : "";
        }

        @Override
        public boolean estaConectado() {
            return conectado;
        }
    }

    @Test
    public void entregaElPesoParseadoAlObservador() throws InterruptedException {
        ConfigBalanza cfg = new ConfigBalanza();
        cfg.setMetodoLectura(MetodoLectura.AUTO);
        ServicioBalanza servicio = new ServicioBalanza(
                new LectorFalso("ST: 2500 kg"), new ParserPeso(cfg));

        CountDownLatch latch = new CountDownLatch(1);
        List<Integer> pesos = new ArrayList<>();
        servicio.agregarObservador(new ServicioBalanza.Observador() {
            @Override
            public void pesoActualizado(int peso) {
                pesos.add(peso);
                latch.countDown();
            }

            @Override
            public void estadoCambiado(boolean conectado, String mensaje) {
                // No interesa en esta prueba.
            }
        });

        try {
            assertTrue(servicio.conectar());
            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(2500), pesos.get(0));
            assertEquals(2500, servicio.getUltimoPeso());
        } finally {
            servicio.close();
        }
    }

    @Test
    public void desconectarDejaElServicioSinConexion() {
        ConfigBalanza cfg = new ConfigBalanza();
        ServicioBalanza servicio = new ServicioBalanza(
                new LectorFalso("ST: 2500 kg"), new ParserPeso(cfg));
        assertTrue(servicio.conectar());
        servicio.desconectar();
        assertFalse(servicio.estaConectado());
        servicio.close();
    }

    @Test
    public void noConectaSiElLectorFalla() {
        ConfigBalanza cfg = new ConfigBalanza();
        LectorPeso falla = new LectorPeso() {
            @Override
            public boolean conectar() {
                return false;
            }

            @Override
            public void desconectar() {
                // Nada que cerrar.
            }

            @Override
            public String leerTrama() {
                return "";
            }

            @Override
            public boolean estaConectado() {
                return false;
            }

            @Override
            public String ultimoError() {
                return "No se encontro el puerto serie.";
            }
        };
        ServicioBalanza servicio = new ServicioBalanza(falla, new ParserPeso(cfg));
        assertFalse(servicio.conectar());
        assertEquals("No se encontro el puerto serie.", servicio.ultimoError());
        servicio.close();
    }
}
