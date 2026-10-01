package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.LinkedList;
import java.util.List;

import bo.firmadigital.jacobitus.comun.InfoCertificado;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.componentes.CertInformation;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.utilidades.TableViewHelper;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.token.GestorSlot;
import bo.firmadigital.jacobitus.token.IToken;
import bo.firmadigital.jacobitus.token.Slot;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class FormFirmante extends Stage {
    private final ProgressBar progressBar;
    private final TableView<InfoCertificado> table;
    private final Button buttonFirmar;
    private final long slot;
    private final int tipo;
    private String label;
    private String pass = null;
    private boolean bloquea;
    private boolean forzarEnveloped;
    private boolean usarPrefijo;
    private String node;
    private FormContrasena contrasenaInicial;
    private boolean errorCertificados;
    private String mensajeError;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    public FormFirmante(Stage parent, long slot, int tipo) {
        this.slot = slot;
        this.tipo = tipo;
        this.label = null;
        setTitle("Jacobitus · Autorización de firma");
        IconosVentana.aplicarAplicacion(this);
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        BorderPane root = new BorderPane();
        progressBar = new ProgressBar();
        progressBar.prefWidthProperty().bind(root.widthProperty());
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        Label instruccion = new Label("Seleccione un certificado y presione «Firmar documento».");
        instruccion.setStyle("-fx-text-fill: #3d4852;");
        VBox encabezado = new VBox(6, progressBar, instruccion);
        encabezado.setPadding(new javafx.geometry.Insets(8, 8, 6, 8));
        root.setTop(encabezado);
        table = new TableView<>();
        table.setPlaceholder(new Label("Los certificados aparecerán después de validar el PIN."));
        TableColumn<InfoCertificado, String> tokenCol = new TableColumn<>("Alias");
        tokenCol.setCellValueFactory(new PropertyValueFactory<>("alias"));
        TableColumn<InfoCertificado, String> nombreCol = new TableColumn<>("Titular");
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombreComunSujeto"));
        TableColumn<InfoCertificado, String> descCol = new TableColumn<>("Descripción");
        descCol.setCellValueFactory(new PropertyValueFactory<>("descripcionSujeto"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().setAll(tokenCol, nombreCol, descCol);
        table.setRowFactory(tv -> {
            return new TableRow<InfoCertificado>() {
                @Override
                public void updateItem(InfoCertificado infoCertificado, boolean empty) {
                    super.updateItem(infoCertificado, empty);
                    if (infoCertificado != null) {
                        CertInformation pane = new CertInformation(infoCertificado, false);
                        Tooltip tooltip = new Tooltip();
                        tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        tooltip.setGraphic(pane);
                        setTooltip(tooltip);
                    } else {
                        setTooltip(null);
                    }
                }
            };
        });
        root.setCenter(table);

        buttonFirmar = new Button("Firmar documento");
        buttonFirmar.setDisable(true);
        buttonFirmar.setMinWidth(125);
        Button buttonCancelar = new Button("Cancelar");
        buttonCancelar.setOnAction(event -> close());
        Region espacio = new Region();
        HBox.setHgrow(espacio, javafx.scene.layout.Priority.ALWAYS);
        HBox acciones = new HBox(8, espacio, buttonCancelar, buttonFirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        acciones.setPadding(new javafx.geometry.Insets(8, 18, 12, 18));
        root.setBottom(acciones);
        Scene scene = new Scene(root, 560, 300);
        setScene(scene);
        setMinWidth(560);
        setMinHeight(300);
        setResizable(true);
        setOpacity(0);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection == null) {
                buttonFirmar.setDisable(true);
                return;
            }
            buttonFirmar.setDisable(false);
        });
        buttonFirmar.setOnAction(event -> {
            InfoCertificado seleccionado = (InfoCertificado) table.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                label = seleccionado.getAlias();
                close();
            }
        });

        setOnShown((WindowEvent t) -> javafx.application.Platform.runLater(() -> {
            if (contrasenaInicial == null) {
                FormContrasena contrasena = new FormContrasena(FormFirmante.this, tipo);
                contrasena.showAndWait();
                if (contrasena.getPass() == null) {
                    close();
                } else {
                    cargarContrasena(contrasena);
                }
            } else {
                cargarContrasena(contrasenaInicial);
            }
        }));
    }

    public FormFirmante(Stage parent, long slot, int tipo, FormContrasena contrasena) {
        this(parent, slot, tipo);
        this.contrasenaInicial = contrasena;
    }

    private void cargarContrasena(FormContrasena contrasena) {
        pass = contrasena.getPass();
        forzarEnveloped = contrasena.getForzarEnveloped();
        usarPrefijo = contrasena.getUsarPrefijo();
        node = contrasena.getNode();
        new Thread(listarCertificados(pass)).start();
    }

    public String getLabel() {
        return label;
    }

    public String getPass() {
        return pass;
    }

    public boolean isBloquea() {
        return bloquea;
    }

    public boolean getForzarEnveloped() {
        return forzarEnveloped;
    }

    public boolean getUsarPrefijo() {
        return usarPrefijo;
    }

    public String getNode() {
        return node;
    }

    public boolean isErrorCertificados() {
        return errorCertificados;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public Task<List<InfoCertificado>> listarCertificados(String pass) {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        Task<List<InfoCertificado>> task = new Task<List<InfoCertificado>>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<InfoCertificado> call() {
                IToken token = null;
                try {
                    GestorSlot gestorSlot = GestorSlot.getInstance();
                    gestorSlot.setConfigFirmador(getConfigFirmador());
                    Slot oSlot = gestorSlot.obtenerSlot(slot);
                    if (oSlot == null) {
                        throw new IOException("El slot " + slot + " no se encuentra disponible.");
                    }
                    token = oSlot.getToken();
                    token.iniciar(pass);
                    List<String> listaAlias = token.listarIdentificadorClaves();
                    List<InfoCertificado> listaInfoCertificado = new LinkedList<>();
                    for (String alias : listaAlias) {
                        InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                        listaInfoCertificado.add(infoCertificado);
                    }
                    return listaInfoCertificado;
                } catch (GeneralSecurityException | IOException ex) {
                    if (ex.getCause() instanceof IOException) {
                        if (ex.getCause().getMessage() != null && ex.getCause().getMessage().equals("PKCS12 key store mac invalid - wrong password or corrupted file.")) {
                            throw new JacobitusException("El PIN es incorrecto. Inténtelo nuevamente.");
                        }
                    }
                    if (ex instanceof java.security.cert.CertificateExpiredException) {
                        throw new JacobitusException("El certificado se encuentra expirado.");
                    }
                    if (ex instanceof java.security.cert.CertificateNotYetValidException) {
                        throw new JacobitusException("El certificado aún no está vigente.");
                    }
                    if (ex.getCause() instanceof java.security.UnrecoverableKeyException) {
                        if (ex.getCause().getCause() instanceof javax.security.auth.login.FailedLoginException) {
                            throw new JacobitusException("Por favor verifique el PIN.");
                        }
                    }
                    if (ex.getCause() instanceof javax.security.auth.login.LoginException) {
                        if (ex.getCause().getCause() != null && ex.getCause().getCause().getMessage() != null && ex.getCause().getCause().getMessage().equals("CKR_PIN_LOCKED")) {
                            throw new JacobitusException("El token criptográfico se encuentra bloqueado por demasiados intentos fallidos al ingresar el PIN.");
                        }
                    }
                    throw new JacobitusException(ex.getMessage());
                } finally {
                    if (token != null) {
                        token.salir();
                    }
                    updateProgress(100, 100);
                }
            }
        };
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            table.setItems(FXCollections.observableList(task.getValue()));
            TableViewHelper.ajustarColumnas(table);
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            if (table.getItems().size() == 1) {
                table.getSelectionModel().selectFirst();
            }
            setOpacity(1);
            if (table.getItems().size() == 1) {
                javafx.application.Platform.runLater(table::requestFocus);
            }
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            String err = task.getException() == null
                    ? "No se pudieron obtener los certificados."
                    : task.getException().getMessage();
            this.pass = null;
            this.errorCertificados = true;
            this.mensajeError = err;
            close();
        });
        return task;
    }
}
