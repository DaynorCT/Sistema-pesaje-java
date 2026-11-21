package bo.firmadigital.jacobitus.pesaje.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import bo.firmadigital.jacobitus.pesaje.auth.AuthService;
import bo.firmadigital.jacobitus.pesaje.auth.ResultadoAutenticacion;
import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.config.ConfigSistema;
import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.sync.ServicioSync;
import bo.firmadigital.jacobitus.pesaje.ui.comun.DialogoContrasena;
import bo.firmadigital.jacobitus.pesaje.ui.comun.EstilosModal;

/**
 * Aplicacion de escritorio del Sistema de Pesaje.
 *
 * Tras el login abre siempre el modo integrado (ventana compacta). El
 * sistema completo solo desde el menu del modo integrado (con contrasena).
 */
public class Aplicacion extends Application {

    private static final int ANCHO_LOGIN = 460;
    private static final int ALTO_LOGIN = 460;
    private static final int ANCHO_SISTEMA = 1280;
    private static final int ALTO_SISTEMA = 800;
    private static final int ANCHO_INTEGRADO = 480;
    private static final int ALTO_INTEGRADO = 680;

    private Stage stage;
    private AuthService authService;
    private UsuarioSesion usuario;
    private String correoSesion;
    private ShellPane shell;
    private ModoIntegradoPane modoIntegrado;
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
        stage.setAlwaysOnTop(false);
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
        correoSesion = correo;
        prepararSincronizacion(correo, clave);
        mostrarModoIntegrado();
    }

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

    private void mostrarModoIntegrado() {
        if (shell != null) {
            shell = null;
        }
        Runnable abrirSistema = () -> {
            if (DialogoContrasena.confirmar(stage, "Acceso al sistema completo", authService,
                    correoSesion)) {
                if (modoIntegrado != null) {
                    modoIntegrado.cerrar();
                }
                modoIntegrado = null;
                mostrarSistema();
            }
        };
        modoIntegrado = new ModoIntegradoPane(servicioSync, abrirSistema, this::cerrarSesion);
        Scene escena = new Scene(modoIntegrado, ANCHO_INTEGRADO, ALTO_INTEGRADO);
        aplicarEstilos(escena);
        stage.setScene(escena);
        stage.setResizable(true);
        stage.setMinWidth(420);
        stage.setMinHeight(600);
        stage.setAlwaysOnTop(config.isAlwaysOnTop());
        stage.centerOnScreen();
        Platform.runLater(() -> modoIntegrado.sincronizarAlMostrar());
    }

    private void mostrarSistema() {
        if (modoIntegrado != null) {
            modoIntegrado.cerrar();
            modoIntegrado = null;
        }
        // El shell se resuelve de forma perezosa: todavia no existe cuando
        // se registran los modulos, pero si cuando se crea el panel de
        // Empresas (que avisa los cambios de logo).
        RegistroModulos.inicializar(usuario, servicioSync, () -> shell);
        shell = new ShellPane();
        shell.setAlCerrarSesion(this::cerrarSesion);
        shell.setAlVolverAPesaje(this::volverAModoIntegrado);
        shell.agregarBotonUsuario(usuario.getNombre(), this::volverAModoIntegrado, this::cerrarSesion);

        Scene escena = new Scene(shell, ANCHO_SISTEMA, ALTO_SISTEMA);
        aplicarEstilos(escena);

        stage.setScene(escena);
        stage.setResizable(true);
        stage.setAlwaysOnTop(false);
        stage.centerOnScreen();
    }

    private void volverAModoIntegrado() {
        if (!DialogoContrasena.confirmar(stage, "Volver al modo báscula", authService, correoSesion)) {
            return;
        }
        mostrarModoIntegrado();
    }

    private void aplicarEstilos(Scene escena) {
        EstilosModal.aplicarEscena(escena);
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

    private void cerrarSesion() {
        if (modoIntegrado != null) {
            modoIntegrado.cerrar();
            modoIntegrado = null;
        }
        shell = null;
        usuario = null;
        correoSesion = null;
        if (servicioSync != null) {
            servicioSync.close();
            servicioSync = null;
        }
        mostrarLogin();
    }
}
