package bo.firmadigital.jacobitus.pesaje.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import bo.firmadigital.jacobitus.pesaje.balanza.ConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.balanza.ServicioBalanza;
import bo.firmadigital.jacobitus.pesaje.ui.comun.DialogoAviso;
import bo.firmadigital.jacobitus.pesaje.ui.comun.EstilosModal;
import bo.firmadigital.jacobitus.pesaje.ui.comun.MenuFlotante;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.sync.ResultadoSync;
import bo.firmadigital.jacobitus.pesaje.sync.ServicioSync;
import bo.firmadigital.jacobitus.pesaje.ui.comun.Cerrable;
import bo.firmadigital.jacobitus.pesaje.ui.modulos.BalanzaPane;

/**
 * Modo integrado: barra superior y registro de peso para el operario en la
 * báscula. Usa las mismas clases de estilo que el resto del escritorio.
 */
public final class ModoIntegradoPane extends BorderPane implements Cerrable {

    private final ServicioSync servicioSync;
    private final Runnable alSistemaCompleto;
    private final Runnable alCerrarSesion;

    private final Label estadoBalanza = new Label("Balanza");
    private final Label estadoBalanzaFuerte = new Label("—");
    private final Label pillSync = new Label();
    private final Label valorPeso = new Label("0 kg");
    private final Label pesoCapturado = new Label("Peso capturado: 0 kg");
    private final Button botonConectar = new Button("Conectar balanza");
    private final Button botonCapturar = new Button("Capturar peso");
    private final Button botonEnviar = new Button("Enviar a la web");

    private ServicioBalanza servicioBalanza;
    private int pesoVivo;
    private int pesoTomado;
    private final MenuFlotante menuFlotante = new MenuFlotante();

    public ModoIntegradoPane(ServicioSync servicioSync, Runnable alSistemaCompleto,
            Runnable alCerrarSesion) {
        this.servicioSync = servicioSync;
        this.alSistemaCompleto = alSistemaCompleto;
        this.alCerrarSesion = alCerrarSesion;

        setTop(construirBarraSuperior());
        setCenter(construirRegistro());

        botonCapturar.setDisable(true);
        botonEnviar.setDisable(true);
        botonConectar.setOnAction(e -> alternarBalanza());
        botonCapturar.setOnAction(e -> capturarPeso());
        botonEnviar.setOnAction(e -> enviarPeso());

        javafx.animation.Timeline timer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(2), e -> refrescarEstado()));
        timer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timer.play();

        refrescarEstado();
    }

    public void sincronizarAlMostrar() {
        if (servicioSync == null) {
            refrescarEstado();
            return;
        }
        new Thread(() -> {
            servicioSync.sincronizarAhora();
            Platform.runLater(this::refrescarEstado);
        }, "integrado-sync-inicial").start();
    }

    public void refrescarEstado() {
        boolean balanzaOk = servicioBalanza != null && servicioBalanza.estaConectado();
        estadoBalanzaFuerte.setText(balanzaOk ? "conectada" : "desconectada");
        estadoBalanzaFuerte.getStyleClass().removeAll("mensaje-exito", "subtitulo");
        estadoBalanzaFuerte.getStyleClass().add(balanzaOk ? "mensaje-exito" : "subtitulo");
        estadoBalanza.getStyleClass().add("subtitulo");

        if (pillSync.getStyleClass().isEmpty() || !pillSync.getStyleClass().contains("badge")) {
            pillSync.getStyleClass().add("badge");
        }
        if (servicioSync == null) {
            pillSync.setText("Sin web");
            pillSync.getStyleClass().removeAll("badge-activo", "badge-proceso", "badge-inactivo");
            pillSync.getStyleClass().add("badge-proceso");
            return;
        }
        long pendientes = servicioSync.pendientes();
        long errores = servicioSync.errores();
        pillSync.getStyleClass().removeAll("badge-activo", "badge-proceso", "badge-inactivo");
        if (errores > 0) {
            pillSync.setText("Con error");
            pillSync.getStyleClass().add("badge-inactivo");
        } else if (pendientes > 0) {
            pillSync.setText("Pend. " + pendientes);
            pillSync.getStyleClass().add("badge-proceso");
        } else {
            pillSync.setText("Al día");
            pillSync.getStyleClass().add("badge-activo");
        }
    }

    private VBox construirBarraSuperior() {
        Label titulo = new Label("Sistema de pesaje");
        titulo.getStyleClass().add("titulo-modulo");

        estadoBalanza.getStyleClass().add("subtitulo");
        estadoBalanzaFuerte.getStyleClass().add("subtitulo");
        pillSync.getStyleClass().addAll("badge", "badge-activo");
        pillSync.setText("Al día");

        HBox filaEstado = new HBox(6, estadoBalanza, estadoBalanzaFuerte, pillSync);
        filaEstado.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filaEstado, Priority.ALWAYS);

        Button sync = new Button("Sincronizar ahora");
        sync.getStyleClass().add("boton-secundario");
        sync.setOnAction(e -> sincronizarAhora());

        Button menu = new Button("Menú");
        menu.getStyleClass().add("boton-secundario");
        menu.setOnAction(e -> alternarMenuFlotante(menu));

        HBox acciones = new HBox(8, filaEstado, sync, menu);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox barra = new VBox(8, titulo, acciones);
        barra.setPadding(new Insets(8, 12, 8, 12));
        barra.getStyleClass().add("barra-superior");
        return barra;
    }

    private void alternarMenuFlotante(Button ancla) {
        menuFlotante.alternar(ancla,
                MenuFlotante.opcion("Sistema escritorio", alSistemaCompleto),
                MenuFlotante.separador(),
                MenuFlotante.opcion("Configurar balanza", () -> abrirConfigBalanza(ancla)),
                MenuFlotante.separador(),
                MenuFlotante.opcion("Cerrar sesión", this::confirmarCerrarSesion));
    }

    private void abrirConfigBalanza(Button ancla) {
        Stage dialogo = new Stage();
        dialogo.initModality(Modality.WINDOW_MODAL);
        if (ancla.getScene() != null) {
            dialogo.initOwner(ancla.getScene().getWindow());
        }
        dialogo.setTitle("Configuración balanza RS232");
        BalanzaPane panel = new BalanzaPane();
        javafx.scene.Scene escena = new javafx.scene.Scene(panel, 560, 520);
        EstilosModal.aplicarEscena(escena);
        dialogo.setScene(escena);
        dialogo.showAndWait();
    }

    private void confirmarCerrarSesion() {
        javafx.stage.Window ventana = getScene() != null ? getScene().getWindow() : null;
        if (DialogoAviso.confirmar(ventana, "Cerrar sesión",
                "¿Seguro que desea cerrar sesión?")) {
            alCerrarSesion.run();
        }
    }

    private VBox construirRegistro() {
        Label tituloTarjeta = new Label("Registro de peso");
        tituloTarjeta.getStyleClass().add("titulo-tarjeta");

        valorPeso.getStyleClass().add("valor-peso");
        StackPane display = new StackPane(valorPeso);
        display.setAlignment(Pos.CENTER);
        display.getStyleClass().add("display-peso");

        pesoCapturado.getStyleClass().add("subtitulo");
        pesoCapturado.setMaxWidth(Double.MAX_VALUE);
        pesoCapturado.setAlignment(Pos.CENTER);

        botonConectar.getStyleClass().add("boton-secundario");
        botonCapturar.getStyleClass().add("boton-secundario");
        botonEnviar.getStyleClass().add("boton-primario");
        botonConectar.setMaxWidth(Double.MAX_VALUE);
        botonCapturar.setMaxWidth(Double.MAX_VALUE);
        botonEnviar.setMaxWidth(Double.MAX_VALUE);

        VBox cuerpo = new VBox(8, display, botonConectar, botonCapturar, pesoCapturado, botonEnviar);
        cuerpo.setPadding(new Insets(16));

        Region separador = new Region();
        separador.getStyleClass().add("separador-horizontal");
        VBox encabezado = new VBox(tituloTarjeta);
        encabezado.setPadding(new Insets(12, 16, 12, 16));

        VBox tarjeta = new VBox(encabezado, separador, cuerpo);
        tarjeta.getStyleClass().addAll("tarjeta", "tarjeta-sin-padding");
        tarjeta.setMaxWidth(Double.MAX_VALUE);

        VBox contenedor = new VBox(tarjeta);
        contenedor.setPadding(new Insets(16));
        VBox.setVgrow(tarjeta, Priority.ALWAYS);
        return contenedor;
    }

    private void alternarBalanza() {
        if (servicioBalanza != null && servicioBalanza.estaConectado()) {
            servicioBalanza.desconectar();
            botonConectar.setText("Conectar balanza");
            botonCapturar.setDisable(true);
            refrescarEstado();
            return;
        }
        ConfigBalanza cfg = new RepositorioConfigBalanza().obtenerActiva();
        servicioBalanza = new ServicioBalanza(cfg);
        servicioBalanza.agregarObservador(new ServicioBalanza.Observador() {
            @Override
            public void pesoActualizado(int peso) {
                Platform.runLater(() -> {
                    pesoVivo = peso;
                    valorPeso.setText(formatearPeso(peso));
                    if (servicioBalanza != null && servicioBalanza.estaConectado()) {
                        botonCapturar.setDisable(false);
                    }
                });
            }

            @Override
            public void estadoCambiado(boolean conectado, String texto) {
                Platform.runLater(() -> {
                    botonConectar.setText(conectado ? "Desconectar balanza" : "Conectar balanza");
                    if (!conectado) {
                        botonCapturar.setDisable(true);
                        valorPeso.setText("0 kg");
                        pesoVivo = 0;
                    }
                    refrescarEstado();
                });
            }
        });
        if (!servicioBalanza.conectar()) {
            mostrarInfo("Balanza", servicioBalanza.ultimoError());
        }
        refrescarEstado();
    }

    private void capturarPeso() {
        if (servicioBalanza == null || !servicioBalanza.estaConectado()) {
            mostrarInfo("Balanza", "Conecte la balanza primero.");
            return;
        }
        if (pesoVivo <= 0) {
            mostrarInfo("Peso", "Peso inválido.");
            return;
        }
        pesoTomado = pesoVivo;
        pesoCapturado.setText("Peso capturado: " + formatearPeso(pesoTomado));
        botonEnviar.setDisable(false);
    }

    private void enviarPeso() {
        if (pesoTomado <= 0) {
            mostrarInfo("Peso", "Capture el peso primero.");
            return;
        }
        if (servicioSync == null) {
            mostrarInfo("Sincronización", "Servicio de sincronización no disponible.");
            return;
        }
        botonEnviar.setDisable(true);
        new Thread(() -> {
            boolean ok = servicioSync.enviarPesoVivo(pesoTomado);
            Platform.runLater(() -> {
                if (ok) {
                    mostrarInfo("Envío", "Peso " + formatearPeso(pesoTomado) + " enviado a la web.", true);
                } else {
                    mostrarInfo("Envío",
                            "No se pudo enviar el peso. Revise la conexión con el sistema web.", false);
                }
                refrescarEstado();
                botonEnviar.setDisable(pesoTomado <= 0);
            });
        }, "enviar-peso-vivo").start();
    }

    private void sincronizarAhora() {
        if (servicioSync == null) {
            mostrarInfo("Sincronización", "La sincronización no está configurada.");
            return;
        }
        new Thread(() -> {
            ResultadoSync r = servicioSync.sincronizarAhora();
            Platform.runLater(() -> {
                refrescarEstado();
                if (!r.getMensaje().isEmpty()) {
                    mostrarInfo("Sincronización", r.getMensaje(), true);
                }
            });
        }, "integrado-sync").start();
    }

    private static String formatearPeso(int peso) {
        return String.format("%,d kg", peso).replace(',', '\u00a0');
    }

    private void mostrarInfo(String titulo, String mensaje) {
        mostrarInfo(titulo, mensaje, true);
    }

    private void mostrarInfo(String titulo, String mensaje, boolean informativo) {
        javafx.stage.Window ventana = getScene() != null ? getScene().getWindow() : null;
        if (informativo) {
            DialogoAviso.informar(ventana, titulo, mensaje);
        } else {
            DialogoAviso.error(ventana, titulo, mensaje);
        }
    }

    @Override
    public void cerrar() {
        if (servicioBalanza != null) {
            servicioBalanza.close();
            servicioBalanza = null;
        }
    }
}
