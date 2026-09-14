package bo.firmadigital.jacobitus.escritorio.extendidos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;

import bo.firmadigital.jacobitus.comun.BadPasswordException;
import bo.firmadigital.jacobitus.comun.DiagnosticoEstructuraPdf;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.utilidades.PdfHelper;
import bo.firmadigital.jacobitus.validador.base.ConfiguracionValidador;
import bo.firmadigital.jacobitus.validador.comun.Firma;

public class ValidadorExtendidoPdf extends ValidadorExtendido {
    private static final Logger LOG = Logger.getLogger(ValidadorExtendidoPdf.class.getName());

    protected String urlRespuesta = null;
    protected String tokenAutorizacion = null;

    protected ConfiguracionValidador configValidador = null;
    private String contrasenia = null;
    private PdfHelper pdfHelper;

    public ValidadorExtendidoPdf(File archivo, ConfiguracionValidador configValidador) {
        this.configValidador = configValidador;
        try {
            super.file = archivo;
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            try (InputStream is = new FileInputStream(archivo)) {
                firmas = listarCertificados(is);
            }
        } catch (Exception ex) {
            firmas = List.of();
            LOG.log(Level.SEVERE, "No se pudo leer/validar " + archivo.getName(), ex);
        }
    }

    public ValidadorExtendidoPdf(File archivo, String contrasenia, ConfiguracionValidador configValidador) {
        this.configValidador = configValidador;
        this.contrasenia = contrasenia;
        try {
            super.file = archivo;
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            try (InputStream is = new FileInputStream(archivo)) {
                firmas = listarCertificados(is);
            }
        } catch (Exception ex) {
            firmas = List.of();
            LOG.log(Level.SEVERE, "No se pudo leer/validar " + archivo.getName(), ex);
        }
    }

    public ValidadorExtendidoPdf(File archivo, String urlRespuesta, String tokenAutorizacion, ConfiguracionValidador configValidador) {
        this(archivo, configValidador);
        this.urlRespuesta = urlRespuesta;
        this.tokenAutorizacion = tokenAutorizacion;
    }
    
    public ValidadorExtendidoPdf(InputStream is, ConfiguracionValidador configValidador) {
        this.configValidador = configValidador;
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            firmas = listarCertificados(is);
        } catch (BadPasswordException ex) {
            if (this.contrasenia == null) {
                throw new JacobitusException("Documento protegido, se esperaba una contraseña.");
            } else {
                throw new JacobitusException("Contraseña inválida.");
            }
        } catch (Exception ex) {
            firmas = List.of();
            LOG.log(Level.SEVERE, "No se pudo leer/validar el documento", ex);
        }
    }

    public ValidadorExtendidoPdf(InputStream is, String contrasenia, ConfiguracionValidador configValidador) {
        this.configValidador = configValidador;
        this.contrasenia = contrasenia;
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            firmas = listarCertificados(is);
        } catch (BadPasswordException ex) {
            if (this.contrasenia == null) {
                throw new JacobitusException("Documento protegido, se esperaba una contraseña.");
            } else {
                throw new JacobitusException("Contraseña inválida.");
            }
        } catch (Exception ex) {
            firmas = List.of();
            LOG.log(Level.SEVERE, "No se pudo leer/validar el documento", ex);
        }
    }

    @Override
    public boolean isRemoto() {
        return urlRespuesta != null;
    }

    @Override
    public String getUrlRespuesta() {
        return urlRespuesta;
    }

    @Override
    public String getTokenAutorizacion() {
        return tokenAutorizacion;
    }

    @Override
    public String getRevisionPath(String revision) {
        return PdfHelper.obtenerRevision(file, revision);
    }    

    public List<Firma> listarCertificados(InputStream is) throws OperatorCreationException, IOException, GeneralSecurityException {
        this.pdfHelper = new PdfHelper();
        pdfHelper.setConfigValidador(this.configValidador);
        return pdfHelper.listarCertificados(is, this.contrasenia);
    }

    // Advertencias de xRef/trailer al abrir el documento; no reemplaza a Firma#getObjetoPdf() (shadow attack).
    public List<String> getAdvertenciasEstructura() {
        DiagnosticoEstructuraPdf diagnostico = pdfHelper == null ? null : pdfHelper.getDiagnosticoEstructura();
        return diagnostico == null ? List.of() : diagnostico.getMensajes();
    }

    public List<String> getFirmasInvalidas() {
        return pdfHelper == null ? List.of() : pdfHelper.getFirmasInvalidas();
    }

    @Override
    public String getPathValidated() {
        StringBuilder res = new StringBuilder(super.getPathValidated());
        for (String firmaInvalida : getFirmasInvalidas()) {
            res.append("\n\t✘ ").append(firmaInvalida);
        }
        return res.toString();
    }

    public boolean getBloqueado() {
        boolean respuesta = false;
        try {
            for (Firma firma : firmas) {
                if (Boolean.TRUE.equals(firma.getBloqueado())) {
                    respuesta = true;
                    break;
                }
            }
        } catch (Exception ignore) {
            LOG.severe(ignore.getMessage());
        }
        return respuesta;
    }
}
