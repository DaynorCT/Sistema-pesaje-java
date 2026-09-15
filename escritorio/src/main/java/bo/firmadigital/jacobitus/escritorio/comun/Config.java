package bo.firmadigital.jacobitus.escritorio.comun;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.filechooser.FileSystemView;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.utilidades.Controlador;
import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;

public class Config {
    protected Properties options;
    protected File user;
    protected File fileOptions;
    protected File token;

    private static Config ourInstance;

    protected Config() {
        try {
            options = new Properties();
            user = new File(FileSystemView.getFileSystemView().getDefaultDirectory(), "Jacobitus");
            fileOptions = new File(user, "jacobitus.properties");
            if (user.exists()) {
                if (fileOptions.exists()) {
                    options.load(new FileInputStream(fileOptions));
                }
                token = new File(user, "softoken.p12");
                if (!token.exists()) {
                    token = null;
                }
            } else {
                token = null;
            }
        } catch (IOException ex) {
            throw new JacobitusException("No se pudieron obtener las opciones.");
        }
    }

    public static Config getInstance() {
        if (ourInstance == null) {
            ourInstance = new Config();
        }
        return ourInstance;
    }

    public boolean isProxyEnabled() {
        String proxy = options.getProperty("proxy");
        return proxy != null && proxy.equals("true");
    }

    public void setProxyEnabled(boolean proxy) {
        if (proxy) {
            options.setProperty("proxy", "true");
        } else {
            options.setProperty("proxy", "false");
        }
    }

    public String getProxyIP() {
        if (isProxyEnabled()) {
            return options.getProperty("proxyIP");
        } else {
            return "Ninguna";
        }
    }

    public void setProxyIP(String ip) {
        options.setProperty("proxyIP", ip);
    }

    public String getProxyPort() {
        if (isProxyEnabled()) {
            return options.getProperty("proxyPort");
        } else {
            return "3128";
        }
    }

    public void setProxyPort(String port) {
        options.setProperty("proxyPort", port);
    }

    public boolean isSecondaryPortEnabled() {
        String proxy = options.getProperty("secondaryPort");
        return proxy != null && proxy.equals("true");
    }

    public void setSecondaryPortEnabled(boolean secondaryPort) {
        if (secondaryPort) {
            options.setProperty("secondaryPort", "true");
        } else {
            options.setProperty("secondaryPort", "false");
        }
    }

    public boolean isTertiaryPortEnabled() {
        String proxy = options.getProperty("tertiaryPort");
        return proxy != null && proxy.equals("true");
    }

    public void setTertiaryPortEnabled(boolean tertiaryPort) {
        if (tertiaryPort) {
            options.setProperty("tertiaryPort", "true");
        } else {
            options.setProperty("tertiaryPort", "false");
        }
    }

    public boolean isConfirmarSalidaEnabled() {
        String confirmarSalida = options.getProperty("confirmarSalida");
        return confirmarSalida == null ? SistemaOperativoHelper.esUnix() : confirmarSalida.equals("true");
    }

    public void setConfirmarSalidaEnabled(boolean confirmarSalida) {
        if (confirmarSalida) {
            options.setProperty("confirmarSalida", "true");
        } else {
            options.setProperty("confirmarSalida", "false");
        }
    }

    public boolean isTrayIconEnabled() {
        String trayIcon = options.getProperty("trayIcon");
        return trayIcon == null ? !SistemaOperativoHelper.esUnix() : trayIcon.equals("true");
    }

    public void setTrayIconEnabled(boolean trayIcon) {
        if (trayIcon) {
            options.setProperty("trayIcon", "true");
        } else {
            options.setProperty("trayIcon", "false");
        }
    }

    public File getToken() {
        token = new File(user, "softoken.p12");
        if (!token.exists()) {
            token = null;
        }
        return token;
    }

    public String getTokenToCreate() {
        if (!user.exists()) {
            if (!user.mkdir()) {
                throw new JacobitusException("No se pudo crear el directorio " + user);
            }
        }
        token = new File(user, "softoken.p12");
        return token.getPath();
    }

    public File getDriver() {
        String driver = options.getProperty("driver");
        if (driver != null) {
            File file = new File(driver);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public void setDriver(File file) {
        if (file == null) {
            options.remove("driver");
        } else {
            options.setProperty("driver", file.getPath());
        }
    }

    public String getDirectorioControladores() {
        return Controlador.obtenerDirectorio();
    }

    public String getDispositivosCompatibles() {
        return Controlador.obtenerDispositivosCompatibles();
    }

    public String getEnlaceInstaladores() {
        return "https://firmadigital.bo/herramientas/jacobitus-escritorio/instaladores";
    }

    public String getEnlaceVersion() {
        return "https://firmadigital.bo/herramientas/jacobitus-escritorio/version.json";
    }

    public void save() {
        try {
            if (!user.exists()) {
                if (!user.mkdir()) {
                    throw new JacobitusException("No se pudo crear el directorio " + user);
                }
            }
            options.store(new FileWriter(fileOptions), "AGETIC - Jacobitus options");
        } catch (IOException ex) {
            throw new JacobitusException(ex.getMessage());
        }
    }
}
