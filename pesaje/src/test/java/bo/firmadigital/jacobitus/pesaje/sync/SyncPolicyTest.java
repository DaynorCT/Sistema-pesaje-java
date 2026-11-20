package bo.firmadigital.jacobitus.pesaje.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.junit.Test;

/**
 * Checks de la politica de reintentos y clasificacion de errores.
 * Mismo contrato que la prueba equivalente del sistema original.
 */
public class SyncPolicyTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @Test
    public void laEsperaEntreReintentosCreceYTieneTope() {
        assertEquals(30, SyncPolicy.esperaBackoffSegundos(0));
        assertEquals(60, SyncPolicy.esperaBackoffSegundos(1));
        assertEquals(120, SyncPolicy.esperaBackoffSegundos(2));
        assertEquals(300, SyncPolicy.esperaBackoffSegundos(20));
        assertTrue(SyncPolicy.esperaBackoffSegundos(-3) >= 30);
    }

    @Test
    public void elProximoIntentoSeCalculaSobreLaHoraActual() {
        assertEquals("2026-11-20 10:00:30", SyncPolicy.calcularProximoIntento(0, BASE, 0));
        assertEquals("2026-11-20 10:00:37", SyncPolicy.calcularProximoIntento(0, BASE, 7));
        assertEquals("2026-11-20 10:01:00", SyncPolicy.calcularProximoIntento(1, BASE, 0));
    }

    @Test
    public void queErroresSeReintentanYCualesQuedanParaRevisar() {
        assertTrue(SyncPolicy.esErrorPermanente(400));
        assertTrue(SyncPolicy.esErrorPermanente(409));
        assertTrue(SyncPolicy.esErrorPermanente(422));
        assertFalse(SyncPolicy.esErrorPermanente(401));
        assertFalse(SyncPolicy.esErrorPermanente(429));
        assertFalse(SyncPolicy.esErrorPermanente(500));
        assertFalse(SyncPolicy.esErrorPermanente(0));
        assertFalse(SyncPolicy.esErrorPermanente(200));
    }

    @Test
    public void elCodigoCeroEsUnFalloDeRed() {
        assertTrue(SyncPolicy.esFalloDeRed(0));
        assertFalse(SyncPolicy.esFalloDeRed(200));
        assertFalse(SyncPolicy.esFalloDeRed(404));
        assertFalse(SyncPolicy.esFalloDeRed(500));
    }

    @Test
    public void losCatalogosNoSeBajanEnCadaVuelta() {
        assertTrue(SyncPolicy.debeSincronizarCatalogos(0, 5));
        assertFalse(SyncPolicy.debeSincronizarCatalogos(1, 5));
        assertTrue(SyncPolicy.debeSincronizarCatalogos(5, 5));
        assertTrue(SyncPolicy.debeSincronizarCatalogos(10, 5));
        assertTrue(SyncPolicy.debeSincronizarCatalogos(7, 1));
    }

    @Test
    public void comoSeResuelveUnaFilaDeCatalogoQueVieneDelServidor() {
        assertEquals(AccionVersion.APLICAR, SyncPolicy.resolverVersion(3, -1, false));
        assertEquals(AccionVersion.APLICAR, SyncPolicy.resolverVersion(5, 3, false));
        assertEquals(AccionVersion.NADA, SyncPolicy.resolverVersion(3, 5, false));
        assertEquals(AccionVersion.NADA, SyncPolicy.resolverVersion(4, 4, false));
        assertEquals(AccionVersion.CONFLICTO, SyncPolicy.resolverVersion(5, 3, true));
        assertEquals(AccionVersion.NADA, SyncPolicy.resolverVersion(3, 3, true));
    }
}
