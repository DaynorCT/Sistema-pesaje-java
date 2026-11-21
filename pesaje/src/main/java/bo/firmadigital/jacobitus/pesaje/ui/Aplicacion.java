package bo.firmadigital.jacobitus.pesaje.ui;

import java.net.URL;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import bo.firmadigital.jacobitus.pesaje.auth.AuthService;
import bo.firmadigital.jacobitus.pesaje.auth.ResultadoAutenticacion;
import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.config.ConfigSistema;
import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.sync.ServicioSync;

/**
 * Aplicacion de escritorio del Sistema de Pesaje.
 *
 * Arranca con la pantalla de login y, al iniciar sesion, muestra el
 * sistema completo. En el primer arranque, cuando se crea la base, se
 * informa la contrasena generada del administrador.
 */
public class Aplicacion extends Application {

    private static final int ANCHO_LOGIN = 460;
    private static final int ALTO_LOGIN = 460;
    private static final int ANCHO_SISTEMA = 1280;
    private static final int ALTO_SISTEMA = 800;

    private Stage stage;
    private AuthService authService;
    private UsuarioSesion usuario;
    private ShellPane shell;
    private ConfigSistema config = new ConfigSistema();
    private ServicioSync servicioSync;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Sistema de Pesaje");
        authService = new AuthService();
        config.cargar();
        mostrarLogin();
    }

    private void mostrarLogin() {
        LoginPane panel = new LoginPane();

        String claveInicial = Database.obtenerInstancia().claveAdministrador();
        if (claveInicial != null && !claveInicial.isEmpty()) {
            panel.mostrarAviso("Primer inicio: usuario admin@sistema.com con la contraseña "
                    + claveInicial + ". Cámbiela después de entrar.");
        }

        StackPane contenedor = new StackPane(panel);
        Scene escena = new Scene(contenedor, ANCHO_LOGIN, ALTO_LOGIN);
        aplicarEstilos(escena);

        stage.setScene(escena);
        stage.setResizable(false);
        stage.show();

        panel.getBotonEntrar().setOnAction(event -> iniciarSesion(panel));
    }

    private void iniciarSesion(LoginPane panel) {
        String correo = panel.getCampoCorreo().getText();
        String clave = panel.getCampoClave().getText();
        ResultadoAutenticacion resultado = authService.autenticar(correo, clave);
        if (!resultado.esExito()) {
            panel.mostrarError(mensajeDe(resultado.getEstado()));
            return;
        }
        usuario = resultado.getUsuario();
        prepararSincronizacion(correo, clave);
        mostrarSistema();
    }

    /**
     * Prepara la sincronizacion con la web.
     *
     * <p>La contrasena en claro solo se usa para pedir el token: la base
     * local guarda el hash, asi que no se puede recuperar de ahi. Es el
     * mismo criterio del sistema original, que pide las credenciales de
     * la web al iniciar sesion.
     */
    private void prepararSincronizacion(String correo, String clave) {
        servicioSync = null;
        if (config.getUrl() == null || config.getUrl().isEmpty()) {
            return;
        }
        servicioSync = ServicioSync.crear(config, correo, clave);
        if (config.isAutoEnviar()) {
            servicioSync.iniciarAutomatico(ServicioSync.INTERVALO_AUTOMATICO_MINUTOS);
        }
    }

    private void mostrarSistema() {
        RegistroModulos.inicializar(usuario, servicioSync);
        shell = new ShellPane();
        Runnable volver = this::volverAPesaje;
        Runnable cerrar = this::cerrarSesion;
        shell.setAlVolverAPesaje(volver);
        shell.setAlCerrarSesion(cerrar);
        shell.agregarBotonUsuario(usuario.getNombre(), volver, cerrar);

        Scene escena = new Scene(shell, ANCHO_SISTEMA, ALTO_SISTEMA);
        aplicarEstilos(escena);

        stage.setScene(escena);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    private void aplicarEstilos(Scene escena) {
        URL comun = getClass().getResource("/jacobitus.css");
        if (comun != null) {
            escena.getStylesheets().add(comun.toExternalForm());
        }
        URL propio = getClass().getResource("/pesaje.css");
        if (propio != null) {
            escena.getStylesheets().add(propio.toExternalForm());
        }
    }

    private String mensajeDe(bo.firmadigital.jacobitus.pesaje.auth.EstadoAutenticacion estado) {
        switch (estado) {
            case EMAIL_INVALIDO:
                return "El correo no existe en la base local.";
            case CONTRASENA_INVALIDA:
                return "La contraseña no es correcta.";
            case USUARIO_INACTIVO:
                return "El usuario está inactivo. Consulte al administrador.";
            default:
                return "No se pudo iniciar sesión.";
        }
    }

    /**
     * Vuelve a la pantalla de pesaje desde el sistema completo.
     */
    private void volverAPesaje() {
        if (shell != null) {
            shell.navegar(Navegacion.PESAJE);
        }
    }

    /**
     * Cierra la sesion y vuelve a la pantalla de login.
     */
    private void cerrarSesion() {
        usuario = null;
        if (servicioSync != null) {
            servicioSync.close();
            servicioSync = null;
        }
        mostrarLogin();
    }
}
