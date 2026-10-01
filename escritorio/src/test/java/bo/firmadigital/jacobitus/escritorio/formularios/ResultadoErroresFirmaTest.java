package bo.firmadigital.jacobitus.escritorio.formularios;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.ClasificadorErroresFirma;

public class ResultadoErroresFirmaTest {

    @Test
    public void errorDeCertificadoSinIncidenciasSePresentaComoErrorGeneral() {
        JacobitusException error = new JacobitusException(CategoriaErrorFirma.CERTIFICADO_NO_ECP,
                CategoriaErrorFirma.CERTIFICADO_NO_ECP.getMensajeUsuario(), null);

        ResultadoErroresFirma resultado = ResultadoErroresFirma.crear(error, Collections.emptyList(), 2, 0);

        assertEquals(ResultadoErroresFirma.Tipo.GENERAL, resultado.getTipo());
        assertEquals(CategoriaErrorFirma.CERTIFICADO_NO_ECP, resultado.getCategoria());
        assertEquals(CategoriaErrorFirma.CERTIFICADO_NO_ECP.getMensajeUsuario(), resultado.getMensajeUsuario());
        assertTrue("El fallo del certificado no debe inventar archivos afectados", resultado.getGrupos().isEmpty());
        assertEquals(2, resultado.getCantidadDocumentos());
        assertEquals(0, resultado.getCantidadFallidos());
    }

    @Test
    public void errorGeneralTrasAvanceParcialConservaElConteoSinAtribuirloAArchivos() {
        JacobitusException error = new JacobitusException(CategoriaErrorFirma.TOKEN_NO_DISPONIBLE,
                CategoriaErrorFirma.TOKEN_NO_DISPONIBLE.getMensajeUsuario(), null);

        ResultadoErroresFirma resultado = ResultadoErroresFirma.crear(error, Collections.emptyList(), 5, 2);

        assertEquals(ResultadoErroresFirma.Tipo.GENERAL, resultado.getTipo());
        assertEquals(CategoriaErrorFirma.TOKEN_NO_DISPONIBLE, resultado.getCategoria());
        assertEquals(5, resultado.getCantidadDocumentos());
        assertEquals(2, resultado.getCantidadFirmados());
        assertEquals(0, resultado.getCantidadFallidos());
        assertTrue(resultado.getGrupos().isEmpty());
    }

    @Test
    public void erroresDeArchivoSeAsocianSoloALosArchivosFallidos() {
        String ruta = Paths.get("entrada", "danado.pdf").toString();
        ResultadoErroresFirma resultado = ResultadoErroresFirma.crear(null,
                Collections.singletonList(new IncidenciaFirmaArchivo(ruta, CategoriaErrorFirma.ARCHIVO_NO_VALIDO)),
                3, 2);

        assertEquals(ResultadoErroresFirma.Tipo.ARCHIVOS, resultado.getTipo());
        assertNull(resultado.getCategoria());
        assertEquals(3, resultado.getCantidadDocumentos());
        assertEquals(2, resultado.getCantidadFirmados());
        assertEquals(1, resultado.getCantidadFallidos());
        assertEquals(1, resultado.getGrupos().size());
        assertEquals(Collections.singletonList("danado.pdf"), resultado.getGrupos().get(0).getNombresArchivos());
    }

    @Test
    public void agrupaFallosPorCategoriaYConservaElOrdenDeAparicion() {
        ResultadoErroresFirma resultado = ResultadoErroresFirma.crear(null, Arrays.asList(
                incidencia(Paths.get("entrada", "uno.pdf").toString(), CategoriaErrorFirma.ACCESO_ARCHIVO),
                incidencia(Paths.get("entrada", "dos.pdf").toString(), CategoriaErrorFirma.ACCESO_ARCHIVO),
                incidencia(Paths.get("entrada", "tres.pdf").toString(), CategoriaErrorFirma.ARCHIVO_NO_VALIDO)), 4, 1);

        assertEquals(2, resultado.getGrupos().size());
        assertEquals(CategoriaErrorFirma.ACCESO_ARCHIVO, resultado.getGrupos().get(0).getCategoria());
        assertEquals(Arrays.asList("uno.pdf", "dos.pdf"), resultado.getGrupos().get(0).getNombresArchivos());
        assertEquals(CategoriaErrorFirma.ARCHIVO_NO_VALIDO, resultado.getGrupos().get(1).getCategoria());
        assertEquals(Collections.singletonList("tres.pdf"), resultado.getGrupos().get(1).getNombresArchivos());
        assertEquals(4, resultado.getCantidadDocumentos());
        assertEquals(1, resultado.getCantidadFirmados());
        assertEquals(3, resultado.getCantidadFallidos());
    }

    @Test
    public void categoriaTipadaEnLaCausaTienePrioridadSobreUnEnvoltorioGenerico() {
        JacobitusException errorTipado = new JacobitusException(CategoriaErrorFirma.CERTIFICADO_NO_ECP,
                CategoriaErrorFirma.CERTIFICADO_NO_ECP.getMensajeUsuario(), null);

        CategoriaErrorFirma resultado = ResultadoErroresFirma.clasificar(
                new IOException("Permission denied", errorTipado));

        assertEquals(CategoriaErrorFirma.CERTIFICADO_NO_ECP, resultado);
    }

    @Test
    public void clasificacionNoDependeDelIdiomaDelMensajeDelSistema() {
        CategoriaErrorFirma ingles = ResultadoErroresFirma.clasificar(new IOException("Permission denied"));
        CategoriaErrorFirma espanol = ResultadoErroresFirma.clasificar(new IOException("Permiso denegado"));

        assertEquals(CategoriaErrorFirma.ACCESO_ARCHIVO, ingles);
        assertEquals(ingles, espanol);
    }

    @Test
    public void cambiarIdiomaDelErrorNoCambiaElResultadoNiElAvanceDeFirma() {
        ResultadoErroresFirma ingles = ResultadoErroresFirma.crear(new IOException("Permission denied"),
                Collections.emptyList(), 4, 2);
        ResultadoErroresFirma espanol = ResultadoErroresFirma.crear(new IOException("Permiso denegado"),
                Collections.emptyList(), 4, 2);

        assertEquals(ingles.getTipo(), espanol.getTipo());
        assertEquals(ingles.getCategoria(), espanol.getCategoria());
        assertEquals(ingles.getCantidadDocumentos(), espanol.getCantidadDocumentos());
        assertEquals(ingles.getCantidadFirmados(), espanol.getCantidadFirmados());
        assertEquals(ingles.getCantidadFallidos(), espanol.getCantidadFallidos());
        assertEquals(ingles.getGrupos().size(), espanol.getGrupos().size());
    }

    @Test
    public void reconexionDependeDeLaCategoriaYNoDelTextoLocalizado() {
        JacobitusException tokenIngles = new JacobitusException(CategoriaErrorFirma.TOKEN_NO_CONECTADO,
                "Connect a token", null);
        JacobitusException tokenEspanol = new JacobitusException(CategoriaErrorFirma.TOKEN_NO_CONECTADO,
                CategoriaErrorFirma.TOKEN_NO_CONECTADO.getMensajeUsuario(), null);

        assertTrue(ClasificadorErroresFirma.debeEsperarReconexionToken(tokenIngles));
        assertTrue(ClasificadorErroresFirma.debeEsperarReconexionToken(tokenEspanol));
        assertFalse(ClasificadorErroresFirma.debeEsperarReconexionToken(
                new JacobitusException(CategoriaErrorFirma.TOKEN_NO_DISPONIBLE, "No disponible", null)));
    }

    @Test
    public void causaDeAccesoReconocidaMantieneLaPrioridadDelFlujoDeReconexion() {
        JacobitusException errorToken = new JacobitusException(CategoriaErrorFirma.TOKEN_NO_CONECTADO,
                "Token desconectado", null);

        assertFalse(ClasificadorErroresFirma.debeEsperarReconexionToken(
                new IOException("Permission denied", errorToken)));
        assertTrue(ClasificadorErroresFirma.debeEsperarReconexionToken(
                new IllegalArgumentException("Invalid input", errorToken)));
    }

    @Test
    public void clasificaCausasConocidasPorTipoYNoPorSuTexto() {
        assertEquals(CategoriaErrorFirma.CLAVE_PRIVADA,
                ResultadoErroresFirma.clasificar(new InvalidKeyException("clave inválida")));
        assertEquals(CategoriaErrorFirma.CERTIFICADO_NO_VALIDO,
                ResultadoErroresFirma.clasificar(new CertificateException("certificate expired")));
        assertEquals(CategoriaErrorFirma.DATOS_NO_VALIDOS,
                ResultadoErroresFirma.clasificar(new IllegalArgumentException("formato incorrecto")));
        assertEquals(CategoriaErrorFirma.ACCESO_ARCHIVO,
                ResultadoErroresFirma.clasificar(new SecurityException("Access denied")));
        assertEquals(CategoriaErrorFirma.OTRO, ResultadoErroresFirma.clasificar(new Exception("error desconocido")));
        assertEquals(CategoriaErrorFirma.OTRO, ResultadoErroresFirma.clasificar(null));
    }

    @Test
    public void causaConCategoriaOtroPuedeAprovecharUnaCausaMasEspecifica() {
        JacobitusException errorGenerico = new JacobitusException(CategoriaErrorFirma.OTRO,
                CategoriaErrorFirma.OTRO.getMensajeUsuario(), new IOException("I/O error"));

        assertEquals(CategoriaErrorFirma.ACCESO_ARCHIVO, ResultadoErroresFirma.clasificar(errorGenerico));
    }

    private static IncidenciaFirmaArchivo incidencia(String ruta, CategoriaErrorFirma categoria) {
        return new IncidenciaFirmaArchivo(ruta, categoria);
    }
}
