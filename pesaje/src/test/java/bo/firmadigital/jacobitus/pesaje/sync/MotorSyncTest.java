package bo.firmadigital.jacobitus.pesaje.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
 * Pruebas del motor de sincronizacion con un transporte falso: no se
 * toca la red.
 */
public class MotorSyncTest {

    @Rule
    public TemporaryFolder carpeta = new TemporaryFolder();

    @Before
    public void prepararBaseTemporal() {
        System.setProperty(Database.PROPIEDAD_SISTEMA_RUTA,
                new java.io.File(carpeta.getRoot(), Database.ARCHIVO_BASE).getAbsolutePath());
        Database.cerrarInstancia();
    }

    @After
    public void cerrarBase() {
        Database.cerrarInstancia();
        System.clearProperty(Database.PROPIEDAD_SISTEMA_RUTA);
    }

    /** Transporte falso que responde por ruta. */
    private static final class TransporteFalso implements Transporte {
        private final Map<String, RespuestaHttp> respuestas = new HashMap<>();
        private final List<String> peticiones = new ArrayList<>();
        private RespuestaHttp porDefecto = RespuestaHttp.deCodigo(200, "{}");

        void responder(String metodoRuta, RespuestaHttp respuesta) {
            respuestas.put(metodoRuta, respuesta);
        }

        @Override
        public RespuestaHttp enviar(String metodo, String url, String cuerpo,
                Map<String, String> cabeceras) {
            String clave = metodo + " " + url;
            peticiones.add(clave);
            RespuestaHttp respuesta = respuestas.get(clave);
            return respuesta != null ? respuesta : porDefecto;
        }
    }

    private SesionSync sesion(TransporteFalso transporte) {
        return new SesionSync(transporte, "http://servidor", "admin@sistema.com", "clave");
    }

    @Test
    public void pideTokenAntesDeEnviar() {
        TransporteFalso transporte = new TransporteFalso();
        transporte.responder("POST http://servidor/api/auth/token",
                RespuestaHttp.deCodigo(200, "{\"access_token\":\"abc\",\"refresh_token\":\"r1\"}"));

        SesionSync sesion = sesion(transporte);
        assertTrue(sesion.asegurarToken());
        assertTrue(sesion.tieneToken());
        assertEquals("abc", sesion.cabeceras().get("Cookie").substring("access_token=".length()));
    }

    @Test
    public void sinTokenValidoNoIntentaEnviar() {
        TransporteFalso transporte = new TransporteFalso();
        transporte.responder("POST http://servidor/api/auth/token",
                RespuestaHttp.deCodigo(401, "{\"message\":\"Credenciales invalidas\"}"));

        MotorSync motor = new MotorSync(transporte, sesion(transporte),
                new RepositorioOutbox(), new AplicadorDelta());
        ResultadoSync resultado = motor.sincronizar();
        assertEquals(0, resultado.getEnviados());
        assertFalse(resultado.getMensaje().isEmpty());
    }

    @Test
    public void elPayloadDePesajeNoLlevaGuia() {
        bo.firmadigital.jacobitus.pesaje.modelo.Pesaje pesaje =
                new bo.firmadigital.jacobitus.pesaje.modelo.Pesaje();
        pesaje.setGuia("PESO-2026-000001");
        pesaje.setVehiculoId(7);
        pesaje.setChoferId(null);
        pesaje.setPesoBruto(10000);
        pesaje.setTara(4000);
        pesaje.setEstadoBalanza(bo.firmadigital.jacobitus.pesaje.modelo.EstadoBalanza.FINALIZADO);

        String payload = MotorSync.payloadPesaje("evento-1", pesaje);
        assertFalse(payload.contains("guia"));
        assertTrue(payload.contains("\"client_event_id\":\"evento-1\""));
        assertTrue(payload.contains("\"vehiculo_id\":7"));
        // Una referencia inexistente va como null, nunca como 0.
        assertTrue(payload.contains("\"chofer_id\":null"));
        assertTrue(payload.contains("\"peso_bruto\":10000"));
    }
}
