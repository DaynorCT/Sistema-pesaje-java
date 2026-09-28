package bo.firmadigital.jacobitus.escritorio.comun;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;
import bo.firmadigital.jacobitus.comun.JacobitusException;

/** Clasifica errores por tipo y contrato, sin interpretar mensajes localizados del sistema operativo. */
public final class ClasificadorErroresFirma {

    private ClasificadorErroresFirma() {
    }

    public static CategoriaErrorFirma clasificar(Throwable error) {
        List<Throwable> causas = obtenerCadenaCausas(error);

        // Las categorías explícitas de la librería tienen prioridad sobre envoltorios genéricos.
        for (Throwable causa : causas) {
            if (causa instanceof JacobitusException) {
                CategoriaErrorFirma categoria = ((JacobitusException) causa).getCategoriaErrorFirma();
                if (categoria != null && categoria != CategoriaErrorFirma.OTRO) {
                    return categoria;
                }
            }
        }

        if (contieneTipo(causas, InvalidKeyException.class)) {
            return CategoriaErrorFirma.CLAVE_PRIVADA;
        }
        if (contieneTipo(causas, CertificateException.class)) {
            return CategoriaErrorFirma.CERTIFICADO_NO_VALIDO;
        }
        if (contieneTipo(causas, IllegalArgumentException.class)) {
            return CategoriaErrorFirma.DATOS_NO_VALIDOS;
        }
        if (contieneTipo(causas, IOException.class) || contieneTipo(causas, SecurityException.class)) {
            return CategoriaErrorFirma.ACCESO_ARCHIVO;
        }
        return CategoriaErrorFirma.OTRO;
    }

    /**
     * Conserva la regla de flujo de FormService: esperar una reconexión solo cuando
     * la primera causa reconocida corresponde a un token no conectado.
     */
    public static boolean debeEsperarReconexionToken(Throwable error) {
        for (Throwable causa : obtenerCadenaCausas(error)) {
            if (causa instanceof JacobitusException) {
                CategoriaErrorFirma categoria = ((JacobitusException) causa).getCategoriaErrorFirma();
                if (categoria != null && categoria != CategoriaErrorFirma.OTRO) {
                    return categoria == CategoriaErrorFirma.TOKEN_NO_CONECTADO;
                }
            }
            if (causa instanceof InvalidKeyException || causa instanceof CertificateException
                    || causa instanceof IOException || causa instanceof SecurityException) {
                return false;
            }
        }
        return false;
    }

    private static List<Throwable> obtenerCadenaCausas(Throwable error) {
        List<Throwable> causas = new ArrayList<>();
        Set<Throwable> visitadas = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable causa = error; causa != null && visitadas.add(causa); causa = causa.getCause()) {
            causas.add(causa);
        }
        return causas;
    }

    private static boolean contieneTipo(List<Throwable> causas, Class<? extends Throwable> tipo) {
        for (Throwable causa : causas) {
            if (tipo.isInstance(causa)) {
                return true;
            }
        }
        return false;
    }
}
