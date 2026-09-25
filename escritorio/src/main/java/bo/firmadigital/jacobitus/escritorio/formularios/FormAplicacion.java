package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.AccessDeniedException;
import java.nio.file.StandardCopyOption;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jettison.json.JSONArray;

import bo.firmadigital.jacobitus.comun.InfoCertificado;
import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.Informacion;
import bo.firmadigital.jacobitus.escritorio.WebServer;
import bo.firmadigital.jacobitus.escritorio.componentes.CertInformation;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.comun.Constants;
import bo.firmadigital.jacobitus.escritorio.comun.ECA;
import bo.firmadigital.jacobitus.escritorio.comun.TokenSelected;
import bo.firmadigital.jacobitus.escritorio.comun.actualizaciones.ActualizacionInfo;
import bo.firmadigital.jacobitus.escritorio.comun.plataforma.PlataformaInfo;
import bo.firmadigital.jacobitus.escritorio.extendidos.ValidadorExtendido;
import bo.firmadigital.jacobitus.escritorio.extendidos.ValidadorExtendidoJws;
import bo.firmadigital.jacobitus.escritorio.extendidos.ValidadorExtendidoPKCS7;
import bo.firmadigital.jacobitus.escritorio.extendidos.ValidadorExtendidoPdf;
import bo.firmadigital.jacobitus.escritorio.extendidos.ValidadorExtendidoXml;
import bo.firmadigital.jacobitus.escritorio.utilidades.ActualizacionesHelper;
import bo.firmadigital.jacobitus.escritorio.utilidades.CertUtil;
import bo.firmadigital.jacobitus.escritorio.utilidades.Conversor;
import bo.firmadigital.jacobitus.escritorio.utilidades.PlataformaHelper;
import bo.firmadigital.jacobitus.escritorio.utilidades.TableViewHelper;
import bo.firmadigital.jacobitus.escritorio.utilidades.UrlFileName;
import bo.firmadigital.jacobitus.firmador.FirmadorJws;
import bo.firmadigital.jacobitus.firmador.FirmadorPKCS7;
import bo.firmadigital.jacobitus.firmador.FirmadorPdf;
import bo.firmadigital.jacobitus.firmador.FirmadorXml;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.firmador.base.IFirmador;
import bo.firmadigital.jacobitus.firmador.base.SmartCard;
import bo.firmadigital.jacobitus.firmador.comun.MagicBytes;
import bo.firmadigital.jacobitus.pkcs11.CK_TOKEN_INFO;
import bo.firmadigital.jacobitus.token.ChangePinJNI;
import bo.firmadigital.jacobitus.token.GestorSlot;
import bo.firmadigital.jacobitus.token.IToken;
import bo.firmadigital.jacobitus.token.Slot;
import bo.firmadigital.jacobitus.utilidades.CertificadoHelper;
import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;
import bo.firmadigital.jacobitus.validador.base.ConfiguracionValidador;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("rawtypes")
public class FormAplicacion extends Application {
    private ProgressBar progressBar;
    private TableView<CK_TOKEN_INFO> tbvDispositivos;
    private TableView<ValidadorExtendido> tbvArchivos;

    private File rutaDestino;
    private ValidadorExtendido validador;
    private CK_TOKEN_INFO tokenInfoSeleccionado;

    private static Stage stage;

    private static boolean usarServicioLocalhost;
    private static boolean usarBarraTareas = true;
    private static boolean iniciarEnSegundoPlano = false;
    
    private static String urlArchivo = null;
    private static String tokenAutorizacion;
    private static String urlRespuesta;
    
    private static String urlParametro = null;
    private static final TokenSelected tokenSelected = new TokenSelected();
    private static String contraseniaMacOS = null;
    private static boolean lanzada = false;
    
    private static FormAplicacion app;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    private ConfiguracionValidador getConfigValidador() {
        Config config = Config.getInstance();
        ConfiguracionValidador configValidador = new ConfiguracionValidador();
        configValidador.setProxyHabilitado(config.isProxyEnabled());
        configValidador.setProxyIp(config.getProxyIP());
        configValidador.setProxyPuerto(Integer.parseInt(config.getProxyPort()));
        return configValidador;
    }

    private FormFirmante prepararFormFirmante(long slot, int tipo) {
        while (true) {
            FormContrasena contrasena = new FormContrasena(stage, tipo);
            contrasena.showAndWait();
            if (contrasena.getPass() == null) {
                return null;
            }

            FormFirmante formFirmante = new FormFirmante(stage, slot, tipo, contrasena);
            formFirmante.showAndWait();
            if (!formFirmante.isErrorCertificados()) {
                return formFirmante;
            }

            Alert alert = new Alert(AlertType.WARNING, formFirmante.getMensajeError(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
        }
    }

    private boolean confirmarSalida() {
        Config config = Config.getInstance();
        if (!config.isConfirmarSalidaEnabled()) {
            return true;
        }

        ButtonType salirButton = new ButtonType("Salir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarButton = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText("¿Seguro que deseas salir?");
        alert.getButtonTypes().setAll(cancelarButton, salirButton);
        if (FormAplicacion.stage != null) {
            alert.initOwner(FormAplicacion.stage);
            alert.initModality(Modality.WINDOW_MODAL);
        }

        Label mensaje = new Label("Se cerrará la aplicación y todos sus servicios locales.");
        mensaje.setWrapText(true);
        mensaje.setMaxWidth(300);

        CheckBox noPreguntar = new CheckBox("No volver a preguntar");
        VBox contenido = new VBox(12);
        contenido.getChildren().add(mensaje);
        contenido.getChildren().add(noPreguntar);
        alert.getDialogPane().setContent(contenido);

        IconosVentana.aplicarModal(alert);
        Optional<ButtonType> result = alert.showAndWait();
        boolean confirmado = result.isPresent() && result.get() == salirButton;
        if (confirmado && noPreguntar.isSelected()) {
            config.setConfirmarSalidaEnabled(false);
            config.save();
        }
        return confirmado;
    }

    public static void salir() {
        Platform.runLater(() -> {
            if (app != null && !app.confirmarSalida()) {
                return;
            }
            try {
                if (usarServicioLocalhost) {
                    WebServer.detener();
                }
            } catch (Exception ex) {
                Logger.getLogger(FormAplicacion.class.getName()).log(Level.SEVERE, null, ex);
            }
            Platform.exit();
            System.exit(0);
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(Stage stage) throws IOException, URISyntaxException, InterruptedException {
        MenuItem exportarItem;
        ContextMenu contextMenu;
        ContextMenu tokenContextMenu;
        FormAplicacion.stage = stage;
        FormAplicacion.app = this;
        Config config = Config.getInstance();

        TaskBar barraTareas = new TaskBar();

        boolean usarBarraTareasConfiguracion;
        boolean errorAlIniciarBandejaSistema = false;
        if (SistemaOperativoHelper.esUnix()) {
            usarBarraTareasConfiguracion = config.isTrayIconEnabled();
        } else {
            usarBarraTareasConfiguracion = usarBarraTareas && config.isTrayIconEnabled();
        }
        if (usarBarraTareasConfiguracion) {
            usarBarraTareas = barraTareas.iniciarBandejaSistema();
            if (!usarBarraTareas) {
                errorAlIniciarBandejaSistema = true;
            }
        } else {
            usarBarraTareas = false;
        }

        String versionVisible = Informacion.VERSION_VISIBLE;
        String variante = Informacion.PROFILE.isEmpty() ? null : Informacion.PROFILE.toUpperCase();
        stage.setTitle("Jacobitus" + (variante != null ? " [PRE-RELEASE]" : "") + " - " + versionVisible);
        IconosVentana.aplicarAplicacion(stage);
        if (!usarServicioLocalhost) {
            // Alert alert = new Alert(AlertType.ERROR, "Servicio detenido, no podrá
            // interactuar con aplicaciones web.", ButtonType.OK);
            Alert alert = new Alert(AlertType.ERROR, WebServer.mensaje, ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.setTitle("Jacobitus" + (variante != null ? " [PRE-RELEASE]" : "") + " - " + versionVisible + " (Servicio detenido)");
        }
        if (usarBarraTareasConfiguracion && !usarBarraTareas) {
            String mensaje = barraTareas.getUltimoDiagnostico();
            if (mensaje == null) {
                mensaje = TaskBar.diagnosticoBandejaNoDisponible();
            }
            Alert alert = new Alert(AlertType.WARNING, mensaje, ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            alert.setHeaderText(null);
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
        }
        PlataformaInfo plataformaInfo = PlataformaHelper.identificar();
        ActualizacionesHelper actualizacionHelper = new ActualizacionesHelper(
                config.getEnlaceInstaladores());

        ActualizacionInfo actualizacionInfo = actualizacionHelper.verificarActualizacion(
                config.getEnlaceVersion(),
                Informacion.VERSION,
                plataformaInfo
                        .getSistemaOperativo()
                        .getValor(),
                plataformaInfo
                        .getArquitectura()
                        .getValor());

        BorderPane root = new BorderPane();
        MenuBar menuBar = new MenuBar();

        Menu archivoMenu = new Menu("Archivo");
        MenuItem actualizarItem = new MenuItem("Actualizar Tokens");
        actualizarItem.setOnAction((ActionEvent e) -> {
            new Thread(listarTokens()).start();
        });
        MenuItem abrirItem = new MenuItem("Abrir PDF");
        abrirItem.setOnAction((ActionEvent e) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir PDF");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos PDF (*.pdf)", "*.pdf");
            fileChooser.getExtensionFilters().add(extFilter);
            FileChooser.ExtensionFilter extFilterDocs = new FileChooser.ExtensionFilter("Documentos", "*.odt",
                    "*.docx");
            fileChooser.getExtensionFilters().add(extFilterDocs);
            List<File> archivosSeleccionados = fileChooser.showOpenMultipleDialog(stage);
            if (archivosSeleccionados != null && !archivosSeleccionados.isEmpty()) {
                new Thread(validar(archivosSeleccionados)).start();
            }
        });
        MenuItem abrirOtroItem = new MenuItem("Abrir otros formatos");
        abrirOtroItem.setOnAction((ActionEvent e) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir Otro");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos P7S (*.p7s)", "*.p7s");
            fileChooser.getExtensionFilters().add(extFilter);
            extFilter = new FileChooser.ExtensionFilter("Archivos XML (*.xml)", "*.xml");
            fileChooser.getExtensionFilters().add(extFilter);
            extFilter = new FileChooser.ExtensionFilter("Archivos JSON (*.json)", "*.json");
            fileChooser.getExtensionFilters().add(extFilter);
            extFilter = new FileChooser.ExtensionFilter("Archivos JWS (*.jws)", "*.jws");
            fileChooser.getExtensionFilters().add(extFilter);
            extFilter = new FileChooser.ExtensionFilter("Todos los archivos (*)", "*");
            fileChooser.getExtensionFilters().add(extFilter);
            List<File> archivosSeleccionados = fileChooser.showOpenMultipleDialog(stage);
            if (archivosSeleccionados != null && !archivosSeleccionados.isEmpty()) {
                new Thread(validarPKCS7(archivosSeleccionados)).start();
            }
        });
        MenuItem convertirAPdf = new MenuItem("Convertir a PDF");
        convertirAPdf.setOnAction((ActionEvent e) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Convertir a PDF");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Archivos ODT (*.odt)", "*.odt");
            fileChooser.getExtensionFilters().add(extFilter);
            extFilter = new FileChooser.ExtensionFilter("Archivos DOCX (*.docx)", "*.docx");
            fileChooser.getExtensionFilters().add(extFilter);
            List<File> archivosSeleccionados = fileChooser.showOpenMultipleDialog(stage);
            if (archivosSeleccionados != null && !archivosSeleccionados.isEmpty()) {
                new Thread(validar(archivosSeleccionados)).start();
            }
        });
        MenuItem limpiarItem = new MenuItem("Limpiar Lista");
        limpiarItem.setOnAction((ActionEvent e) -> {
            tbvArchivos.getItems().clear();
        });
        MenuItem opcionesItem = new MenuItem("Opciones");
        opcionesItem.setOnAction((ActionEvent e) -> {
            FormConfiguracion formConfiguracion = new FormConfiguracion(stage);
            formConfiguracion.showAndWait();
        });
        MenuItem abrirCrt = new MenuItem("Ver Certificado");
        abrirCrt.setOnAction((ActionEvent e) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Ver Certificado");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Certificados", "*.crt", "*.pem");
            fileChooser.getExtensionFilters().add(extFilter);
            File archivoSeleccionado = fileChooser.showOpenDialog(stage);
            if (archivoSeleccionado != null) {
                try {
                    X509Certificate x509Certificate;
                    try (FileInputStream is = new FileInputStream(archivoSeleccionado)) {
                        byte[] certificado = is.readAllBytes();
                        x509Certificate = CertificadoHelper.obtenerCertificado(certificado);
                    }
                    InfoCertificado infoCertificado = new InfoCertificado(archivoSeleccionado.getName(), x509Certificate);
                    CertInformation certInformation = new CertInformation(infoCertificado, true);
                    VBox vBox = new VBox();
                    vBox.setPadding(new Insets(10));
                    vBox.setSpacing(4);
                    vBox.getChildren().add(certInformation);
                    Stage certificadoStage = new Stage();
                    certificadoStage.setTitle("Certificado");
                    IconosVentana.aplicarAplicacion(certificadoStage);
                    certificadoStage.initOwner(stage);
                    certificadoStage.initModality(Modality.APPLICATION_MODAL);
                    Scene scene = new Scene(vBox);
                    certificadoStage.setScene(scene);
                    certificadoStage.showAndWait();
                } catch (IOException | CertificateEncodingException ex) {
                    throw new JacobitusException(ex.getMessage());
                }
            }
        });
        MenuItem salirItem = new MenuItem("Salir");
        salirItem.setOnAction((ActionEvent e) -> {
            salir();
        });
        archivoMenu.getItems().addAll(actualizarItem, abrirItem, abrirOtroItem, convertirAPdf, limpiarItem, opcionesItem,
                abrirCrt, salirItem);
        menuBar.getMenus().add(archivoMenu);

        Menu firmaMenu = new Menu("Firma");
        MenuItem firmarItem = new MenuItem("Firmar PDF");
        firmarItem.setOnAction((ActionEvent e) -> {
            if (tbvArchivos.getItems().isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "No se tienen documentos para firmar.", ButtonType.OK);
                alert.initOwner(stage);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Jacobitus");
                IconosVentana.aplicarModal(alert);
                alert.showAndWait();
            } else {
                CK_TOKEN_INFO itemSeleccionado = tbvDispositivos.getSelectionModel().getSelectedItem();
                if (itemSeleccionado == null) {
                    Alert alert = new Alert(AlertType.INFORMATION, "Por favor seleccione un Token.", ButtonType.OK);
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                } else {
                    if ((tbvArchivos.getItems().get(0)).isRemoto()) {
                        rutaDestino = new File(System.getProperty("java.io.tmpdir"));
                    } else {
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setTitle("Seleccione directorio de destino");
                        rutaDestino = directoryChooser.showDialog(stage);
                    }
                    if (rutaDestino != null) {
                        FormFirmante formFirmante = prepararFormFirmante(itemSeleccionado.getSlot(), Constants.PDF);
                        if (formFirmante != null && formFirmante.getLabel() != null) {
                            new Thread(firmarPdf(formFirmante.isBloquea(), itemSeleccionado.getSlot(), formFirmante.getLabel(),
                                    formFirmante.getPass())).start();
                        }
                    }
                }
            }
        });
        MenuItem firmarPKCS7Item = new MenuItem("Firmar PKCS#7");
        firmarPKCS7Item.setOnAction((ActionEvent e) -> {
            if (tbvArchivos.getItems().isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "No se tienen documentos para firmar.", ButtonType.OK);
                alert.initOwner(stage);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Jacobitus");
                IconosVentana.aplicarModal(alert);
                alert.showAndWait();
            } else {
                CK_TOKEN_INFO itemSeleccionado = tbvDispositivos.getSelectionModel().getSelectedItem();
                if (itemSeleccionado == null) {
                    Alert alert = new Alert(AlertType.INFORMATION, "Por favor seleccione un Token.", ButtonType.OK);
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                } else {
                    if ((tbvArchivos.getItems().get(0)).isRemoto()) {
                        rutaDestino = new File(System.getProperty("java.io.tmpdir"));
                    } else {
                        DirectoryChooser directoryChooser = new DirectoryChooser();
                        directoryChooser.setTitle("Seleccione directorio de destino");
                        rutaDestino = directoryChooser.showDialog(stage);
                    }
                    if (rutaDestino != null) {
                        FormFirmante formFirmante = prepararFormFirmante(itemSeleccionado.getSlot(), Constants.PKCS7);
                        if (formFirmante != null && formFirmante.getLabel() != null) {
                            new Thread(firmarPKCS7(itemSeleccionado.getSlot(), formFirmante.getLabel(), formFirmante.getPass())).start();
                        }
                    }
                }
            }
        });
        MenuItem firmarXmlItem = new MenuItem("Firmar XML");
        firmarXmlItem.setOnAction((ActionEvent e) -> {
            if (tbvArchivos.getItems().isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "No se tienen documentos para firmar.", ButtonType.OK);
                alert.initOwner(stage);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Jacobitus");
                IconosVentana.aplicarModal(alert);
                alert.showAndWait();
            } else {
                CK_TOKEN_INFO itemSeleccionado = tbvDispositivos.getSelectionModel().getSelectedItem();
                if (itemSeleccionado == null) {
                    Alert alert = new Alert(AlertType.INFORMATION, "Por favor seleccione un Token.", ButtonType.OK);
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                } else {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Seleccione directorio de destino");
                    rutaDestino = directoryChooser.showDialog(stage);
                    if (rutaDestino != null) {
                        FormFirmante formFirmante = prepararFormFirmante(itemSeleccionado.getSlot(), Constants.DSIG);
                        if (formFirmante != null && formFirmante.getLabel() != null) {
                            new Thread(firmarXml(itemSeleccionado.getSlot(), formFirmante.getLabel(), formFirmante.getPass(),
                                    formFirmante.getNode(), formFirmante.getForzarEnveloped(), formFirmante.getUsarPrefijo()))
                                    .start();
                        }
                    }
                }
            }
        });
        MenuItem firmarJwsItem = new MenuItem("Firmar JSON");
        firmarJwsItem.setOnAction((ActionEvent e) -> {
            if (tbvArchivos.getItems().isEmpty()) {
                Alert alert = new Alert(AlertType.WARNING, "No se tienen documentos para firmar.", ButtonType.OK);
                alert.initOwner(stage);
                alert.initModality(Modality.APPLICATION_MODAL);
                alert.setTitle("Jacobitus");
                IconosVentana.aplicarModal(alert);
                alert.showAndWait();
            } else {
                CK_TOKEN_INFO itemSeleccionado = tbvDispositivos.getSelectionModel().getSelectedItem();
                if (itemSeleccionado == null) {
                    Alert alert = new Alert(AlertType.INFORMATION, "Por favor seleccione un Token.", ButtonType.OK);
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                } else {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    directoryChooser.setTitle("Seleccione directorio de destino");
                    rutaDestino = directoryChooser.showDialog(stage);
                    if (rutaDestino != null) {
                        FormFirmante formFirmante = prepararFormFirmante(itemSeleccionado.getSlot(), Constants.JWS);
                        if (formFirmante != null && formFirmante.getLabel() != null) {
                            new Thread(firmarJws(itemSeleccionado.getSlot(), formFirmante.getLabel(), formFirmante.getPass())).start();
                        }
                    }
                }
            }
        });
        firmaMenu.getItems().addAll(firmarItem, firmarPKCS7Item, firmarXmlItem, firmarJwsItem);
        menuBar.getMenus().add(firmaMenu);

        Menu ayudaMenu = new Menu("Ayuda");
        MenuItem certificadoLocalItem = new MenuItem("Verificar servicio");
        certificadoLocalItem.setOnAction((ActionEvent e) -> {
            new Thread(configurarCertificadoLocal()).start();
        });

        MenuItem acercaDeItem = new MenuItem("Acerca de ...");
        acercaDeItem.setOnAction((ActionEvent e) -> {
            String versionLibreria = bo.firmadigital.jacobitus.Informacion.VERSION;
            String javaVersion = System.getProperty("java.version");
            String javafxVersion = System.getProperty("javafx.version");
            String changePinVersion = new ChangePinJNI().version();
            ImageView logo = new ImageView(
                    new Image(this.getClass().getClassLoader().getResource("logo-agetic-sin-tag.png").toExternalForm()));
            logo.setFitWidth(240);
            logo.setPreserveRatio(true);
            logo.setSmooth(true);
            Label nombre = new Label("Jacobitus Escritorio");
            nombre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1f2933;");
            nombre.setWrapText(true);
            Label versionAplicacion = new Label("Versión " + Informacion.VERSION_VISIBLE);
            versionAplicacion.setStyle("-fx-font-size: 15px; -fx-text-fill: #263238;");
            Label versionVariante = new Label(variante != null ? "PRE-RELEASE · " + variante : "");
            versionVariante.setVisible(variante != null);
            versionVariante.setManaged(variante != null);
            versionVariante.setStyle("-fx-background-color: #9a3412; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 10 4 10;");
            Label descripcion = new Label(Informacion.DESCRIPCION);
            descripcion.setWrapText(true);
            descripcion.setMaxWidth(340);
            descripcion.setAlignment(Pos.CENTER);
            descripcion.setTextAlignment(TextAlignment.CENTER);
            descripcion.setStyle("-fx-text-fill: #263238;");
            VBox presentacion = new VBox(5, nombre, versionAplicacion, versionVariante, descripcion);
            presentacion.setAlignment(Pos.CENTER);
            VBox cabecera = new VBox(8, logo, presentacion);
            cabecera.setAlignment(Pos.CENTER);

            Label detalle = new Label("Información técnica");
            detalle.setStyle("-fx-font-weight: bold;");
            GridPane versiones = new GridPane();
            versiones.setHgap(42);
            versiones.setVgap(6);
            String[][] componentes = {
                {"Jacobitus Librería", versionLibreria},
                {"ChangePin Library", changePinVersion},
                {"JavaFX", javafxVersion},
                {"Java", javaVersion}
            };
            for (int i = 0; i < componentes.length; i++) {
                Label valor = new Label(componentes[i][1]);
                valor.setWrapText(true);
                versiones.addRow(i, new Label(componentes[i][0]), valor);
            }
            Label institucion = new Label(
                    "Agencia de Gobierno Electrónico y Tecnologías de Información y Comunicación");
            institucion.setWrapText(true);
            institucion.setMaxWidth(340);
            Hyperlink repositorio = new Hyperlink("Código fuente · GitLab Software Libre");
            repositorio.setPadding(new Insets(4, 0, 4, 0));
            repositorio.setWrapText(true);
            repositorio.setOnAction(event -> getHostServices().showDocument(
                    "https://gitlab.softwarelibre.gob.bo/adsib/jacobitus-total/jacobitus-escritorio"));
            VBox informacionTecnica = new VBox(9, detalle, versiones);
            VBox pie = new VBox(5, institucion, repositorio);
            VBox contenido = new VBox(14, cabecera, new Separator(), informacionTecnica,
                    new Separator(), pie);
            contenido.setPadding(new Insets(4, 18, 8, 18));

            Alert alert = new Alert(AlertType.INFORMATION);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Acerca de Jacobitus");
            alert.setHeaderText(null);
            alert.setGraphic(null);
            alert.getDialogPane().setPrefWidth(430);
            alert.getDialogPane().setContent(contenido);
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
        });

        SeparatorMenuItem separador = new SeparatorMenuItem();
        ayudaMenu.getItems().addAll(certificadoLocalItem, separador, acercaDeItem);
        menuBar.getMenus().add(ayudaMenu);
        if (variante != null) {
            Label versionBanner = new Label("⚠ Versión preliminar · Puede contener errores");
            versionBanner.getStyleClass().add("version-banner");
            versionBanner.setMaxWidth(Double.MAX_VALUE);
            versionBanner.setAlignment(Pos.CENTER);
            root.setTop(new VBox(menuBar, versionBanner));
        } else {
            root.setTop(menuBar);
        }

        tokenContextMenu = new ContextMenu();
        MenuItem informacionItem = new MenuItem("Información");
        informacionItem.setOnAction((ActionEvent e) -> {
            FormTokenInfo formTokenInfo = new FormTokenInfo(stage, tokenInfoSeleccionado.getSlot());
            formTokenInfo.showAndWait();
        });
        MenuItem cambiarPinItem = new MenuItem("Cambiar pin");
        cambiarPinItem.setOnAction((ActionEvent e) -> {
            FormCambiarContrasena formCambiarContrasena = new FormCambiarContrasena(stage, tokenInfoSeleccionado.getSlot());
            formCambiarContrasena.showAndWait();
        });
        MenuItem exportarSoftokenItem = new MenuItem("Exportar Softoken");
        exportarSoftokenItem.setOnAction((ActionEvent e) -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Seleccione directorio de destino");
            File destino = directoryChooser.showDialog(stage);
            if (destino != null) {
                try {
                    Files.copy(config.getToken().toPath(), new File(destino, "softoken.p12").toPath(),
                            StandardCopyOption.COPY_ATTRIBUTES);
                    Alert alert = new Alert(AlertType.INFORMATION, "El softoken se exportó correctamente.");
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                } catch (IOException ex) {
                    Alert alert = new Alert(AlertType.ERROR, ex.getMessage());
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                }
            }
        });
        tokenContextMenu.getItems().addAll(informacionItem, cambiarPinItem, exportarSoftokenItem);

        contextMenu = new ContextMenu();
        MenuItem detalleItem = new MenuItem("Detalle Validación");
        detalleItem.setOnAction((ActionEvent e) -> {
            FormDetalle formDetalle = new FormDetalle(stage, validador, getHostServices());
            formDetalle.showAndWait();
        });
        exportarItem = new MenuItem("Exportar contenido");
        exportarItem.setVisible(false);
        exportarItem.setOnAction((ActionEvent e) -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exportar archivo");
            File archivoSeleccionado = fileChooser.showSaveDialog(stage);
            if (archivoSeleccionado != null) {
                validador.export(archivoSeleccionado);
            }
        });
        contextMenu.getItems().addAll(detalleItem, exportarItem);

        tbvDispositivos = new TableView();
        TableColumn tbcDispositivo = new TableColumn("Token");
        tbcDispositivo.setCellValueFactory(new PropertyValueFactory("label"));
        tbvDispositivos.getColumns().setAll(tbcDispositivo);
        TableViewHelper.ajustarColumnaUnica(tbvDispositivos, tbcDispositivo);
        tbvDispositivos.setMaxHeight(76);
        tbvDispositivos.setRowFactory(tv -> {
            TableRow<CK_TOKEN_INFO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    tokenInfoSeleccionado = row.getItem();
                    tokenContextMenu.getItems().get(2).setVisible(tokenInfoSeleccionado.getSlot() == -1);
                    tokenContextMenu.show(tbvDispositivos, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });

        tbvArchivos = new TableView();
        TableColumn tbcArchivo = new TableColumn("Archivo");
        tbcArchivo.setCellValueFactory(new PropertyValueFactory("pathValidated"));
        tbvArchivos.getColumns().setAll(tbcArchivo);
        TableViewHelper.ajustarColumnaUnica(tbvArchivos, tbcArchivo);
        tbvArchivos.setRowFactory(tv -> {
            TableRow<ValidadorExtendido> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    HostServices hostServices = getHostServices();
                    hostServices.showDocument(row.getItem().getAbsolutePath());
                }
                if (event.getButton() == MouseButton.SECONDARY && !row.isEmpty()) {
                    validador = row.getItem();
                    try {
                        exportarItem.setVisible(MagicBytes.P7S.is(validador.getFile()));
                    } catch (IOException ignore) {
                        exportarItem.setVisible(false);
                    }
                    contextMenu.show(tbvArchivos, event.getScreenX(), event.getScreenY());
                }
            });
            return row;
        });
        tbvArchivos.setOnDragOver((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            } else {
                event.consume();
            }
        });
        tbvArchivos.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            boolean exito = false;
            if (db.hasFiles()) {
                exito = true;
                new Thread(validar(db.getFiles())).start();
            }
            event.setDropCompleted(exito);
            event.consume();
        });

        BorderPane tables = new BorderPane(tbvArchivos);
        BorderPane middle = new BorderPane(tables);
        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        if (variante != null) {
            progressBar.getStyleClass().add("progress-bar-prerelease");
        }
        progressBar.prefWidthProperty().bind(middle.widthProperty());

        tables.setTop(tbvDispositivos);

        root.setCenter(middle);
        middle.setTop(progressBar);

        BorderPane footer = new BorderPane();
        footer.setPadding(new Insets(5, 10, 5, 10));

        Label pie = new Label("AGETIC - https://firmadigital.bo");
        pie.setPadding(new Insets(5, 5, 5, 5));
        footer.setRight(pie);

        Platform.runLater(() -> {
            if (actualizacionInfo != null && actualizacionInfo.isActualizacionDisponible()) {
                Hyperlink enlaceActualizacion = new Hyperlink("Hay una actualización disponible");
                enlaceActualizacion.setStyle(
                        "-fx-text-fill: #2a7ae2; -fx-underline: true;");
                enlaceActualizacion.setOnAction(e -> new FormActualizacionDisponible(stage, actualizacionInfo).showAndWait());
                footer.setLeft(enlaceActualizacion);
            }
        });

        footer.setStyle(
                "-fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1 0 0 0;");
        root.setBottom(footer);

        Scene scene = new Scene(root, 640, 480);
        scene.getStylesheets().add(this.getClass().getClassLoader().getResource("jacobitus.css").toExternalForm());
        stage.setScene(scene);
        boolean mostrarVentanaInicio = !iniciarEnSegundoPlano || !usarBarraTareas;
        if (mostrarVentanaInicio) {
            stage.show();
            stage.setIconified(false);
            stage.toFront();
        } else {
            Platform.setImplicitExit(false);
        }

        if (errorAlIniciarBandejaSistema) {
            Thread thread = new Thread(() -> {
                Platform.runLater(() -> {
                    String mensaje = barraTareas.getUltimoDiagnostico();
                    if (mensaje == null) {
                        mensaje = TaskBar.diagnosticoBandejaNoDisponible();
                    }
                    Alert alert = new Alert(AlertType.WARNING, mensaje, ButtonType.OK);
                    alert.initOwner(stage);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    alert.setHeaderText(null);
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                });
            }, "Jacobitus-TrayIcon");
            thread.setDaemon(true);
            thread.start();
        }

        if (usarBarraTareas) {
            Platform.setImplicitExit(false);
            if (mostrarVentanaInicio) {
                new Thread(listarTokens()).start();
            }
        } else {
            Platform.runLater(() -> {
                if (actualizacionInfo != null && actualizacionInfo.isActualizacionDisponible()) {
                    new FormActualizacionDisponible(stage, actualizacionInfo).showAndWait();
                    new Thread(listarTokens()).start();
                } else {
                    new Thread(listarTokens()).start();
                }
            });
        }

        stage.setOnCloseRequest((WindowEvent e) -> {
            if (!usarBarraTareas) {
                e.consume();
                salir();
            } else {
                Platform.setImplicitExit(false);
                stage.close();
            }
        });

        if (urlArchivo == null) {
            if (urlParametro == null) {
                if (usarBarraTareas && mostrarVentanaInicio) {
                    SmartCard.cards(this.getConfigFirmador());
                } else {
                    // new Thread(listarTokens()).start();
                }
            } else {
                File archivo = new File(urlParametro);
                new Thread(validar(Arrays.asList(archivo))).start();
            }
        } else {
            new Thread(descargarArchivo(urlArchivo, tokenAutorizacion, urlRespuesta)).start();
        }
        
        stage.setOnShown((WindowEvent e) -> {
            Platform.runLater(() -> {
                if (actualizacionInfo != null && actualizacionInfo.isActualizacionDisponible()) {
                    new FormActualizacionDisponible(stage, actualizacionInfo).showAndWait();
                    new Thread(listarTokens()).start();
                } else {
                    new Thread(listarTokens()).start();
                }
            });
        });
    }

    public Task<List<CK_TOKEN_INFO>> listarTokens() {
        progressBar.progressProperty().unbind();
        Task<List<CK_TOKEN_INFO>> task = new Task<List<CK_TOKEN_INFO>>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<CK_TOKEN_INFO> call() throws Exception {
                try {
                    GestorSlot gestorSlot = GestorSlot.getInstance();
                    gestorSlot.setConfigFirmador(getConfigFirmador());
                    Slot[] slots = gestorSlot.listarSlots();
                    List<CK_TOKEN_INFO> listaInfoToken = new LinkedList();
                    for (Slot slot : slots) {
                        listaInfoToken.add(slot.detalleToken());
                    }
                    updateProgress(100, 100);
                    return listaInfoToken;
                } catch (RuntimeException ex) {
                    updateProgress(100, 100);
                    throw ex;
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            tbvDispositivos.setItems(FXCollections.observableList(task.getValue()));
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            tbvDispositivos.getItems().clear();
            String error = task.getException().getMessage();
            if (!stage.isShowing()) {
                Logger.getLogger(FormAplicacion.class.getName()).log(Level.INFO, error);
                stage.getScene().setCursor(Cursor.DEFAULT);
                return;
            }
            Alert alert = new Alert(AlertType.WARNING);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            BorderPane pane = new BorderPane();
            Label label = new Label(error);
            pane.setTop(label);
            alert.getDialogPane().setContent(pane);
            if (error.startsWith("http")) {
                Hyperlink enlace = new Hyperlink(error);
                enlace.setOnAction((ActionEvent t) -> {
                    getHostServices().showDocument(error);
                });
                pane.setCenter(enlace);
                label.setText(
                        "No se encontró el controlador del token; por favor, descárguelo e instálelo desde el siguiente enlace.");
            }
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<List<ValidadorExtendido>> validar(List<File> archivos) {
        progressBar.progressProperty().unbind();
        Task<List<ValidadorExtendido>> task = new Task<List<ValidadorExtendido>>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<ValidadorExtendido> call() throws Exception {
                List<ValidadorExtendido> archivosProcesados = new LinkedList();
                for (int i = 0; i < archivos.size(); i++) {
                    if (archivos.get(i).getName().endsWith(".odt")) {
                        archivosProcesados.add(new ValidadorExtendidoPdf(Conversor.odtAPdf(archivos.get(i)), getConfigValidador()));
                    } else if (archivos.get(i).getName().endsWith(".docx")) {
                        archivosProcesados.add(new ValidadorExtendidoPdf(Conversor.docxAPdf(archivos.get(i)), getConfigValidador()));
                    } else if (archivos.get(i).getName().endsWith(".pdf")) {
                        archivosProcesados.add(new ValidadorExtendidoPdf(archivos.get(i), getConfigValidador()));
                        // TODO: Ajustar los mensajes de validacion de documentos xml y json,
                        // considerando que no se puede determinar si fueron firmados dentro del periodo
                        // de vigencia del certificado
                    } else if (archivos.get(i).getName().endsWith(".xml")) {
                        archivosProcesados.add(new ValidadorExtendidoXml(archivos.get(i), null, getConfigValidador()));
                    } else if (archivos.get(i).getName().endsWith(".jws")) {
                        archivosProcesados.add(new ValidadorExtendidoJws(archivos.get(i), null, getConfigValidador()));
                    } else {
                        archivosProcesados.add(new ValidadorExtendidoPKCS7(archivos.get(i), getConfigValidador()));
                    }
                    updateProgress(i + 1, archivos.size());
                }
                return archivosProcesados;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            tbvArchivos.setItems(FXCollections.observableList(task.getValue()));
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<Boolean> firmarPdf(boolean bloquear, long slotID, String alias, String contrasenia) {
        progressBar.progressProperty().unbind();
        int cantidadDocumentos = tbvArchivos.getItems().size();
        List<IncidenciaFirmaPdf> incidencias = new ArrayList<>();
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(getConfigFirmador());
                IToken token = gestorSlot.obtenerSlot(slotID).getToken();
                token.iniciar(contrasenia);
                InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                token.salir();
                if (!ECA.esValida(infoCertificado) || !ECA.esPublica(infoCertificado)) {
                    updateProgress(100, 100);
                    throw new JacobitusException("Certificado no emitido por la ECP.");
                }

                List<ValidadorExtendido> archivos = tbvArchivos.getItems();
                if (archivos.isEmpty()) {
                    updateProgress(100, 100);
                } else {
                    for (int i = 0; i < archivos.size(); i++) {
                        try {
                            IFirmador firmador = FirmadorPdf.getInstance(slotID, alias, contrasenia, getConfigFirmador());
                            String nomArchivo = new File(archivos.get(i).getAbsolutePath()).getName();
                            if (!nomArchivo.endsWith(".pdf")) {
                                nomArchivo += ".firmado.pdf";
                            } else {
                                nomArchivo = nomArchivo.replace(".pdf", ".firmado.pdf");
                            }
                            File archivoFirmado = new File(rutaDestino, nomArchivo);
                            try (InputStream is = new FileInputStream(archivos.get(i).getAbsolutePath());
                                    OutputStream os = new FileOutputStream(archivoFirmado)) {
                                firmador.firmar(is, os, bloquear, false);
                            }
                            updateProgress(i + 1, archivos.size());
                            final int indice = i;
                            ValidadorExtendido archivoValidado = new ValidadorExtendidoPdf(archivoFirmado, getConfigValidador());
                            Platform.runLater(() -> tbvArchivos.getItems().set(indice, archivoValidado));
                        } catch (Exception ex) {
                            updateProgress(i + 1, archivos.size());
                            CategoriaErrorFirma categoria = obtenerCategoriaErrorFirma(ex);
                            String mensajeUsuario = ex instanceof JacobitusException
                                    && ((JacobitusException) ex).getCategoriaErrorFirma() != null
                                            ? ex.getMessage() : categoria.getMensajeUsuario();
                            String detalleTecnico = obtenerDetalleTecnico(ex);
                            incidencias.add(new IncidenciaFirmaPdf(archivos.get(i).getAbsolutePath(), categoria,
                                    mensajeUsuario, detalleTecnico));
                        }
                    }
                    updateProgress(100, 100);
                }
                if (incidencias.isEmpty()) {
                    return true;
                } else {
                    throw new JacobitusException("La firma terminó con errores.");
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnFailed((WorkerStateEvent evt) -> {
            String detalleErrores = task.getException() == null
                    ? "No se pudieron obtener los detalles del error."
                    : task.getException().getMessage();
            Alert alert = incidencias.isEmpty()
                    ? crearAlertaErroresFirma(detalleErrores)
                    : crearAlertaErroresFirma(incidencias, cantidadDocumentos);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    private Alert crearAlertaErroresFirma(String detalleErrores) {
        Map<String, List<String>> erroresPorCausa = new LinkedHashMap<>();
        Arrays.stream(detalleErrores.split("\\R"))
                .filter(linea -> !linea.trim().isEmpty())
                .forEach(linea -> {
                    int separador = linea.indexOf(": ");
                    String ruta = separador < 0 ? null : linea.substring(0, separador);
                    String causa = separador < 0 ? linea : linea.substring(separador + 2);
                    String archivo = ruta == null ? null : new File(ruta).getName();
                    erroresPorCausa.computeIfAbsent(causa, key -> new LinkedList<>()).add(archivo);
                });

        StringBuilder textoAgrupado = new StringBuilder();
        erroresPorCausa.forEach((causa, archivos) -> {
            textoAgrupado.append(causa).append('\n');
            archivos.forEach(archivo -> {
                if (archivo != null && !archivo.isEmpty()) {
                    textoAgrupado.append("  • ").append(archivo).append('\n');
                }
            });
            textoAgrupado.append('\n');
        });

        Alert alert = new Alert(AlertType.WARNING, null, ButtonType.OK);
        alert.setHeaderText("La firma terminó con errores");
        Label resumen = new Label("Se detectaron " + erroresPorCausa.values().stream().mapToInt(List::size).sum()
                + " incidencias. Los archivos se agrupan por causa.");
        resumen.setWrapText(true);

        TextArea detalles = new TextArea(textoAgrupado.toString().trim());
        detalles.setEditable(false);
        detalles.setWrapText(true);
        detalles.setMinSize(120, 80);
        detalles.setPrefSize(480, 190);
        detalles.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox contenido = new VBox(8, resumen, new Label("Archivos y causas"), detalles);
        VBox.setVgrow(detalles, Priority.ALWAYS);
        alert.getDialogPane().setContent(contenido);
        alert.getDialogPane().setPrefSize(540, 330);
        alert.getDialogPane().setMinSize(400, 230);
        alert.setResizable(true);
        return alert;
    }

    private CategoriaErrorFirma obtenerCategoriaErrorFirma(Exception ex) {
        if (ex instanceof JacobitusException) {
            CategoriaErrorFirma categoria = ((JacobitusException) ex).getCategoriaErrorFirma();
            if (categoria != null) {
                return categoria;
            }
        }
        if (ex instanceof FileNotFoundException || ex instanceof AccessDeniedException) {
            return CategoriaErrorFirma.ACCESO_ARCHIVO;
        }
        return CategoriaErrorFirma.OTRO;
    }

    private String obtenerDetalleTecnico(Exception ex) {
        String detalle = null;
        Throwable causa = ex.getCause();
        while (causa != null) {
            if (causa.getMessage() != null) {
                detalle = causa.getMessage();
            }
            causa = causa.getCause();
        }
        if (detalle != null) {
            return detalle;
        }
        if (ex instanceof JacobitusException
                && ((JacobitusException) ex).getCategoriaErrorFirma() != null) {
            return null;
        }
        return ex.getMessage();
    }

    private Alert crearAlertaErroresFirma(List<IncidenciaFirmaPdf> incidencias, int cantidadDocumentos) {
        Map<String, List<IncidenciaFirmaPdf>> grupos = new LinkedHashMap<>();
        for (IncidenciaFirmaPdf incidencia : incidencias) {
            String clave = incidencia.categoria == CategoriaErrorFirma.OTRO
                    ? incidencia.categoria.name() + ":" + incidencia.detalleTecnico
                    : incidencia.categoria.name();
            grupos.computeIfAbsent(clave, key -> new LinkedList<>()).add(incidencia);
        }

        StringBuilder texto = new StringBuilder();
        for (List<IncidenciaFirmaPdf> grupo : grupos.values()) {
            IncidenciaFirmaPdf primera = grupo.get(0);
            texto.append(primera.mensajeUsuario).append('\n');
            for (IncidenciaFirmaPdf incidencia : grupo) {
                texto.append("  • ").append(new File(incidencia.ruta).getName()).append('\n');
            }
            grupo.stream().map(incidencia -> incidencia.detalleTecnico)
                    .filter(detalle -> detalle != null && !detalle.isBlank()).distinct()
                    .forEach(detalle -> texto.append("  Detalle técnico: ").append(detalle).append('\n'));
            texto.append('\n');
        }

        Alert alert = new Alert(AlertType.WARNING, null, ButtonType.OK);
        alert.setHeaderText("Resultado de la firma");
        int cantidadFirmados = cantidadDocumentos - incidencias.size();
        VBox resumen = new VBox(4,
                new Label("Firmados correctamente: " + cantidadFirmados),
                new Label("Fallidos: " + incidencias.size()));
        TextArea detalles = new TextArea(texto.toString().trim());
        detalles.setEditable(false);
        detalles.setWrapText(true);
        detalles.setMinSize(120, 80);
        detalles.setPrefSize(480, 190);
        detalles.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        VBox contenido = new VBox(8, resumen, new Label("Archivos que no se pudieron firmar"), detalles);
        VBox.setVgrow(detalles, Priority.ALWAYS);
        alert.getDialogPane().setContent(contenido);
        alert.getDialogPane().setPrefSize(540, 330);
        alert.getDialogPane().setMinSize(400, 230);
        alert.setResizable(true);
        return alert;
    }

    private static class IncidenciaFirmaPdf {
        private final String ruta;
        private final CategoriaErrorFirma categoria;
        private final String mensajeUsuario;
        private final String detalleTecnico;

        private IncidenciaFirmaPdf(String ruta, CategoriaErrorFirma categoria, String mensajeUsuario,
                String detalleTecnico) {
            this.ruta = ruta;
            this.categoria = categoria;
            this.mensajeUsuario = mensajeUsuario;
            this.detalleTecnico = detalleTecnico;
        }
    }

    public Task<List<ValidadorExtendido>> validarPKCS7(List<File> archivos) {
        progressBar.progressProperty().unbind();
        Task<List<ValidadorExtendido>> task = new Task<List<ValidadorExtendido>>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<ValidadorExtendido> call() throws Exception {
                List<ValidadorExtendido> archivosProcesados = new LinkedList();
                for (int i = 0; i < archivos.size(); i++) {
                    if (MagicBytes.PDF.is(archivos.get(i))) {
                        archivosProcesados.add(new ValidadorExtendidoPdf(archivos.get(i), getConfigValidador()));
                    } else {
                        if (MagicBytes.XML.is(archivos.get(i))) {
                            archivosProcesados.add(new ValidadorExtendidoXml(archivos.get(i), null, getConfigValidador()));
                        } else {
                            if (MagicBytes.P7S.is(archivos.get(i))) {
                                archivosProcesados.add(new ValidadorExtendidoPKCS7(archivos.get(i), getConfigValidador()));
                            } else {
                                if (MagicBytes.isJWS(archivos.get(i))) {
                                    archivosProcesados.add(new ValidadorExtendidoJws(archivos.get(i), null, getConfigValidador()));
                                } else {
                                    archivosProcesados.add(new ValidadorExtendidoPKCS7(archivos.get(i), getConfigValidador()));
                                }
                            }
                        }
                    }
                    updateProgress(i + 1, archivos.size());
                }
                return archivosProcesados;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            tbvArchivos.setItems(FXCollections.observableList(task.getValue()));
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<Boolean> firmarPKCS7(long slotID, String alias, String contrasenia) {
        progressBar.progressProperty().unbind();
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(getConfigFirmador());
                IToken token = gestorSlot.obtenerSlot(slotID).getToken();
                token.iniciar(contrasenia);
                InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                token.salir();
                if (!ECA.esValida(infoCertificado) || !ECA.esPublica(infoCertificado)) {
                    updateProgress(100, 100);
                    throw new JacobitusException("Certificado no emitido por la ECP.");
                }

                StringBuilder errores = new StringBuilder();
                List<ValidadorExtendido> archivos = tbvArchivos.getItems();
                if (archivos.isEmpty()) {
                    updateProgress(100, 100);
                } else {
                    IFirmador firmador = FirmadorPKCS7.getInstance(slotID, alias, contrasenia, getConfigFirmador());
                    for (int i = 0; i < archivos.size(); i++) {
                        try {
                            File archivoFirmado = new File(rutaDestino, archivos.get(i).getFile().getName() + ".p7s");
                            try (InputStream is = new BufferedInputStream(new FileInputStream(archivos.get(i).getFile()));
                                    FileOutputStream os = new FileOutputStream(archivoFirmado)) {
                                firmador.firmar(is, os);
                            }
                            updateProgress(i + 1, archivos.size());
                            final int indice = i;
                            ValidadorExtendido archivoValidado = new ValidadorExtendidoPKCS7(archivoFirmado, getConfigValidador());
                            Platform.runLater(() -> tbvArchivos.getItems().set(indice, archivoValidado));
                        } catch (Exception ex) {
                            updateProgress(i + 1, archivos.size());
                            errores.append(archivos.get(i).getAbsolutePath()).append(": ").append(ex.getMessage())
                                    .append("\n");
                        }
                    }
                    updateProgress(100, 100);
                }
                if (errores.length() == 0) {
                    return true;
                } else {
                    throw new JacobitusException(errores.toString());
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<Boolean> firmarXml(long slotID, String alias, String contrasenia, String nodoFirma, Boolean forzarEnvoltura,
            Boolean usarPrefijo) {
        progressBar.progressProperty().unbind();
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(getConfigFirmador());
                IToken token = gestorSlot.obtenerSlot(slotID).getToken();
                token.iniciar(contrasenia);
                InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                token.salir();
                if (!ECA.esValida(infoCertificado) || !ECA.esPublica(infoCertificado)) {
                    updateProgress(100, 100);
                    throw new JacobitusException("Certificado no emitido por la ECP.");
                }

                StringBuilder errores = new StringBuilder();
                List<ValidadorExtendido> archivos = tbvArchivos.getItems();
                if (archivos.isEmpty()) {
                    updateProgress(100, 100);
                } else {
                    IFirmador firmador = FirmadorXml.getInstance(slotID, alias, contrasenia, nodoFirma, getConfigFirmador());
                    for (int i = 0; i < archivos.size(); i++) {
                        try {
                            String nomArchivo = new File(archivos.get(i).getAbsolutePath()).getName();
                            if (!nomArchivo.endsWith(".xml")) {
                                nomArchivo += ".firmado.xml";
                            } else {
                                nomArchivo = nomArchivo.replace(".xml", ".firmado.xml");
                            }
                            File archivoFirmado = new File(rutaDestino, nomArchivo);
                            try (InputStream is = new BufferedInputStream(new FileInputStream(archivos.get(i).getFile()));
                                    FileOutputStream os = new FileOutputStream(archivoFirmado)) {
                                firmador.firmar(is, os, forzarEnvoltura, usarPrefijo);
                            }
                            updateProgress(i + 1, archivos.size());
                            final int indice = i;
                            ValidadorExtendido archivoValidado = new ValidadorExtendidoXml(archivoFirmado, null, getConfigValidador());
                            Platform.runLater(() -> tbvArchivos.getItems().set(indice, archivoValidado));
                        } catch (Exception ex) {
                            updateProgress(i + 1, archivos.size());
                            errores.append(archivos.get(i).getAbsolutePath()).append(": ").append(ex.getMessage())
                                    .append("\n");
                        }
                    }
                    updateProgress(100, 100);
                }
                if (errores.length() == 0) {
                    return true;
                } else {
                    throw new JacobitusException(errores.toString());
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<Boolean> firmarJws(long slotID, String alias, String contrasenia) {
        progressBar.progressProperty().unbind();
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(getConfigFirmador());
                IToken token = gestorSlot.obtenerSlot(slotID).getToken();
                token.iniciar(contrasenia);
                InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                token.salir();
                if (!ECA.esValida(infoCertificado) || !ECA.esPublica(infoCertificado)) {
                    updateProgress(100, 100);
                    throw new JacobitusException("Certificado no emitido por la ECP.");
                }

                StringBuilder errores = new StringBuilder();
                List<ValidadorExtendido> archivos = tbvArchivos.getItems();
                if (archivos.isEmpty()) {
                    updateProgress(100, 100);
                } else {
                    IFirmador firmador = FirmadorJws.getInstance(slotID, alias, contrasenia, getConfigFirmador());
                    for (int i = 0; i < archivos.size(); i++) {
                        try {
                            String nomArchivo = new File(archivos.get(i).getAbsolutePath()).getName();
                            if (nomArchivo.endsWith(".json")) {
                                nomArchivo = nomArchivo.replace(".json", ".jws");
                            } else {
                                nomArchivo = nomArchivo + ".jws";
                            }
                            File archivoFirmado = new File(rutaDestino, nomArchivo);
                            try (InputStream is = new BufferedInputStream(new FileInputStream(archivos.get(i).getFile()));
                                    FileOutputStream os = new FileOutputStream(archivoFirmado)) {
                                firmador.firmar(is, os, false, false);
                            }
                            updateProgress(i + 1, archivos.size());
                            final int indice = i;
                            ValidadorExtendido archivoValidado = new ValidadorExtendidoJws(archivoFirmado, null, getConfigValidador());
                            Platform.runLater(() -> tbvArchivos.getItems().set(indice, archivoValidado));
                        } catch (Exception ex) {
                            updateProgress(i + 1, archivos.size());
                            errores.append(archivos.get(i).getAbsolutePath()).append(": ").append(ex.getMessage())
                                    .append("\n");
                        }
                    }
                    updateProgress(100, 100);
                }
                if (errores.length() == 0) {
                    return true;
                } else {
                    throw new JacobitusException(errores.toString());
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<List<ValidadorExtendido>> descargarArchivo(String urlArchivo, String tokenAutorizacion, String urlRespuesta) {
        progressBar.progressProperty().unbind();
        Task<List<ValidadorExtendido>> task = new Task<List<ValidadorExtendido>>() {
            @SuppressWarnings("unchecked")
            @Override
            protected List<ValidadorExtendido> call() throws Exception {
                URL url = new URL(urlArchivo);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (tokenAutorizacion != null) {
                    connection.setRequestProperty("Authorization", tokenAutorizacion);
                }
                connection.connect();
                int size = 0;
                List contentLength = connection.getHeaderFields().get("content-Length");
                if (contentLength != null && !contentLength.isEmpty()) {
                    String sLength = (String) contentLength.get(0);
                    if (sLength != null) {
                        size = Integer.parseInt(sLength);
                    }
                }
                String nomArchivo = UrlFileName.getFileName(connection);
                if (connection.getResponseCode() >= HttpURLConnection.HTTP_OK &&
                        connection.getResponseCode() <= HttpURLConnection.HTTP_PARTIAL) {
                    InputStream responseStream = connection.getInputStream();
                    File f = new File(System.getProperty("java.io.tmpdir"), nomArchivo);
                    try (OutputStream outStream = new FileOutputStream(f)) {
                        byte[] buffer = new byte[8 * 1024];
                        int t = 0, bytesRead;
                        while ((bytesRead = responseStream.read(buffer)) != -1) {
                            outStream.write(buffer, 0, bytesRead);
                            if (size > 0) {
                                t += bytesRead;
                                updateProgress(t, size);
                            }
                        }
                    }
                    List<ValidadorExtendido> archivosDescargados = new LinkedList();
                    if (MagicBytes.PDF.is(f)) {
                        archivosDescargados.add(new ValidadorExtendidoPdf(f, urlRespuesta, tokenAutorizacion, getConfigValidador()));
                    } else {
                        archivosDescargados.add(new ValidadorExtendidoPKCS7(f, urlRespuesta, tokenAutorizacion, getConfigValidador()));
                    }
                    if (size == 0) {
                        updateProgress(1, 1);
                    }
                    return archivosDescargados;
                } else {
                    throw new JacobitusException("No se pudo descargar el archivo.");
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        stage.getScene().setCursor(Cursor.WAIT);
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            tbvArchivos.setItems(FXCollections.observableList(task.getValue()));
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            stage.getScene().setCursor(Cursor.DEFAULT);
        });
        return task;
    }

    public Task<Void> configurarCertificadoLocal() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    stage.getScene().setCursor(Cursor.WAIT);

                    String estadoCertificado = "";
                    boolean certificadoInstalado = false;
                    boolean errorVerificacion = false;

                    try {
                        if (SistemaOperativoHelper.esMacOS()) {
                            FormContrasenaMac contrasena = new FormContrasenaMac(stage);
                            contrasena.showAndWait();
                            FormAplicacion.contraseniaMacOS = contrasena.getPass();

                            if (FormAplicacion.contraseniaMacOS == null) {
                                stage.getScene().setCursor(Cursor.DEFAULT);
                                return;
                            }
                        } else {
                            FormAplicacion.contraseniaMacOS = null;
                        }

                        if (SistemaOperativoHelper.esMacOS()) {
                            certificadoInstalado = CertUtil
                                    .verificarCertificadoServicioLocal(FormAplicacion.contraseniaMacOS);
                        } else {
                            certificadoInstalado = CertUtil.verificarCertificadoServicioLocal();
                        }

                        estadoCertificado = certificadoInstalado
                                ? "✔ Instalado"
                                : "No instalado";

                    } catch (IOException e1) {
                        estadoCertificado = "No se pudo verificar";
                        errorVerificacion = true;
                    }

                    ImageView logo = new ImageView(
                            new Image(
                                    this.getClass()
                                            .getClassLoader()
                                            .getResource("logo-agetic-sin-tag.png")
                                            .toExternalForm()));

                    logo.setFitWidth(180);
                    logo.setPreserveRatio(true);
                    logo.setSmooth(true);

                    Label titulo = new Label("Certificado del servicio local");
                    titulo.setStyle(
                            "-fx-font-size: 20px; " +
                                    "-fx-font-weight: bold;");
                    titulo.setWrapText(true);

                    Label descripcion = new Label(
                            "Permite que el navegador confíe en la conexión HTTPS con Jacobitus.");
                    descripcion.setWrapText(true);
                    descripcion.setAlignment(Pos.CENTER);

                    VBox cabecera = new VBox(
                            6,
                            logo,
                            titulo,
                            descripcion);
                    cabecera.setAlignment(Pos.CENTER);

                    Label seccionEstado = new Label("Estado del certificado");
                    seccionEstado.setStyle("-fx-font-weight: bold;");

                    Label lblCertificado = new Label("Certificado local");

                    Label lblEstado = new Label(estadoCertificado);
                    lblEstado.setWrapText(true);

                    if (certificadoInstalado) {
                        lblEstado.setStyle(
                                "-fx-text-fill: #2e7d32; " +
                                        "-fx-font-weight: bold;");
                    } else if (errorVerificacion) {
                        lblEstado.setStyle(
                                "-fx-text-fill: #c62828; " +
                                        "-fx-font-weight: bold;");
                    } else {
                        lblEstado.setStyle("-fx-font-weight: bold;");
                    }

                    GridPane estado = new GridPane();
                    estado.setHgap(20);
                    estado.setVgap(6);

                    estado.add(lblCertificado, 0, 0);
                    estado.add(lblEstado, 1, 0);

                    Alert certificadoLocalAlerta = new Alert(AlertType.INFORMATION);
                    certificadoLocalAlerta.initOwner(stage);
                    certificadoLocalAlerta.initModality(Modality.APPLICATION_MODAL);
                    certificadoLocalAlerta.setTitle("Verificar servicio");
                    certificadoLocalAlerta.setHeaderText(null);
                    certificadoLocalAlerta.setGraphic(null);
                    certificadoLocalAlerta.getDialogPane().setPrefWidth(400);

                    if (!errorVerificacion) {
                        Hyperlink accionCertificado = new Hyperlink(
                                certificadoInstalado
                                        ? "Desinstalar"
                                        : "Instalar");

                        accionCertificado.setPadding(Insets.EMPTY);

                        if (certificadoInstalado) {
                            accionCertificado.setOnAction(e1 -> {
                                Alert confirmacion = new Alert(
                                        Alert.AlertType.CONFIRMATION);

                                confirmacion.initOwner(stage);
                                confirmacion.initModality(
                                        Modality.APPLICATION_MODAL);
                                confirmacion.setHeaderText(null);
                                confirmacion.setTitle(
                                        "Desinstalar certificado local");
                                confirmacion.setContentText(
                                        "¿Desea desinstalar el certificado local? "
                                                + "El navegador podría mostrar advertencias de seguridad.");

                                IconosVentana.aplicarModal(confirmacion);
                                Optional<ButtonType> action = confirmacion.showAndWait();

                                if (action.isPresent()
                                        && action.get() == ButtonType.OK) {
                                    try {
                                        if (SistemaOperativoHelper.esMacOS()) {
                                            CertUtil.desinstalarCertificadoServicioLocal(
                                                    FormAplicacion.contraseniaMacOS);
                                        } else {
                                            CertUtil.desinstalarCertificadoServicioLocal();
                                        }

                                        certificadoLocalAlerta.close();

                                    } catch (IOException ex) {
                                        Logger.getLogger(
                                                FormAplicacion.class.getName())
                                                .log(
                                                        Level.SEVERE,
                                                        null,
                                                        ex);
                                    }
                                }
                            });

                        } else {
                            accionCertificado.setOnAction(e1 -> {
                                Alert confirmacion = new Alert(
                                        Alert.AlertType.CONFIRMATION);

                                confirmacion.initOwner(stage);
                                confirmacion.initModality(
                                        Modality.APPLICATION_MODAL);
                                confirmacion.setHeaderText(null);
                                confirmacion.setTitle(
                                        "Instalar certificado local");
                                confirmacion.setContentText(
                                        "¿Desea instalar el certificado local para "
                                                + "habilitar una conexión HTTPS confiable con Jacobitus?");

                                IconosVentana.aplicarModal(confirmacion);
                                Optional<ButtonType> action = confirmacion.showAndWait();

                                if (action.isPresent()
                                        && action.get() == ButtonType.OK) {
                                    try {
                                        if (SistemaOperativoHelper.esMacOS()) {
                                            CertUtil.instalarCertificadoServicioLocal(
                                                    FormAplicacion.contraseniaMacOS);
                                        } else {
                                            CertUtil.instalarCertificadoServicioLocal();
                                        }

                                        certificadoLocalAlerta.close();

                                    } catch (IOException
                                            | InterruptedException ex) {
                                        Logger.getLogger(
                                                FormAplicacion.class.getName())
                                                .log(
                                                        Level.SEVERE,
                                                        null,
                                                        ex);
                                    }
                                }
                            });
                        }

                        estado.add(accionCertificado, 2, 0);
                    }

                    VBox informacionCertificado = new VBox(
                            10,
                            seccionEstado,
                            estado);

                    VBox contenido = new VBox(
                            12,
                            cabecera,
                            new Separator(),
                            informacionCertificado);

                    contenido.setPadding(
                            new Insets(0, 16, 8, 16));

                    certificadoLocalAlerta
                            .getDialogPane()
                            .setContent(contenido);

                    IconosVentana.aplicarModal(certificadoLocalAlerta);
                    certificadoLocalAlerta.showAndWait();

                    stage.getScene().setCursor(Cursor.DEFAULT);
                });

                return null;
            }
        };

        task.setOnFailed((WorkerStateEvent evt) -> {
            Alert alert = new Alert(
                    AlertType.WARNING,
                    task.getException().getMessage(),
                    ButtonType.OK);

            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();

            stage.getScene().setCursor(Cursor.DEFAULT);
        });

        return task;
    }

    public static void show() {
        Platform.runLater(() -> {
            if (stage != null) {
                stage.show();
                stage.setIconified(false);
                stage.toFront();
            }
        });
    }

    public static void show(String error) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR, error, ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
        });
    }

    public static void show(File archivo) {
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
            } else {
                if (usarBarraTareas) {
                    stage.show();
                } else {
                    stage.setIconified(false);
                }
            }
            new Thread(app.validar(Arrays.asList(archivo))).start();
        });
    }

    public static void show(String urlArchivo, String tokenAutorizacion, String urlRespuesta) {
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
            } else {
                if (usarBarraTareas) {
                    stage.show();
                } else {
                    stage.setIconified(false);
                }
            }
            new Thread(app.descargarArchivo(urlArchivo, tokenAutorizacion, urlRespuesta)).start();
        });
    }

    public static void run(boolean usarServicioLocalhost, boolean usarBarraTareas) {
        run(usarServicioLocalhost, usarBarraTareas, false);
    }

    public static void run(boolean usarServicioLocalhost, boolean usarBarraTareas, boolean iniciarEnSegundoPlano) {
        FormAplicacion.usarServicioLocalhost = usarServicioLocalhost;
        FormAplicacion.usarBarraTareas = usarBarraTareas;
        FormAplicacion.iniciarEnSegundoPlano = iniciarEnSegundoPlano;
        if (!FormAplicacion.lanzada) {
            FormAplicacion.lanzada = true;
            launch();
        }
    }

    public static void run(boolean usarServicioLocalhost, boolean usarBarraTareas, String urlParametro) {
        FormAplicacion.usarServicioLocalhost = usarServicioLocalhost;
        FormAplicacion.usarBarraTareas = usarBarraTareas;
        FormAplicacion.urlParametro = urlParametro;
        if (!FormAplicacion.lanzada) {
            FormAplicacion.lanzada = true;
            launch();
        }
    }

    public static void run(boolean usarServicioLocalhost, boolean usarBarraTareas, String urlArchivo, String tokenAutorizacion,
            String urlRespuesta) {
        FormAplicacion.usarServicioLocalhost = usarServicioLocalhost;
        FormAplicacion.usarBarraTareas = usarBarraTareas;
        FormAplicacion.urlArchivo = urlArchivo;
        FormAplicacion.tokenAutorizacion = tokenAutorizacion;
        FormAplicacion.urlRespuesta = urlRespuesta;
        if (!FormAplicacion.lanzada) {
            FormAplicacion.lanzada = true;
            launch();
        }
    }

    public static TokenSelected service(Slot slot, String ci, JSONArray pdfArray) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(slot);
            tokenSelected.setCI(ci);
            tokenSelected.setFiles(pdfArray);
            tokenSelected.setFilesJson(null);
            tokenSelected.setHashArray(null);
            tokenSelected.setPinInicial(null);
            FormService service = new FormService(stage, tokenSelected, "pades");
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }

    public static TokenSelected serviceModoSeguro(String ci, JSONArray pdfArray, boolean software, String format) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(null);
            tokenSelected.setSlots(null);
            tokenSelected.setCI(ci);
            if ("jws".equals(format)) {
                tokenSelected.setFiles(null);
                tokenSelected.setFilesJson(pdfArray);
            } else {
                tokenSelected.setFiles(pdfArray);
                tokenSelected.setFilesJson(null);
            }
            tokenSelected.setHashArray(null);
            tokenSelected.setPinInicial(null);
            FormService service = new FormService(stage, tokenSelected, format, software);
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }

    public static TokenSelected serviceJWS(Slot slot, String ci, JSONArray jsonArray) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(slot);
            tokenSelected.setCI(ci);
            tokenSelected.setFiles(null);
            tokenSelected.setFilesJson(jsonArray);
            tokenSelected.setHashArray(null);
            tokenSelected.setPinInicial(null);
            FormService service = new FormService(stage, tokenSelected, "jws");
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }

    public static TokenSelected service(Slot[] slots, String ci, JSONArray pdfArray, JSONArray jsonArray) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(null);
            tokenSelected.setSlots(slots);
            tokenSelected.setCI(ci);
            tokenSelected.setFiles(pdfArray);
            tokenSelected.setFilesJson(jsonArray);
            tokenSelected.setHashArray(null);
            tokenSelected.setPinInicial(null);
            FormService service = new FormService(stage, tokenSelected, "both");
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }

    public static TokenSelected serviceHash(Slot slot, String ci, JSONArray hashArray, String digest, String pin) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(slot);
            tokenSelected.setCI(ci);
            tokenSelected.setFiles(null);
            tokenSelected.setFilesJson(null);
            tokenSelected.setHashArray(hashArray);
            tokenSelected.setDigest(digest);
            tokenSelected.setPinInicial(pin);
            FormService service = new FormService(stage, tokenSelected, "pades_hash");
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }

    public static TokenSelected serviceHashModoSeguro(String ci, JSONArray hashArray, String digest, String pin) {
        if (tokenSelected.isShown()) {
            throw new JacobitusException("Ya se tiene una solicitud de firma pendiente.");
        }
        Platform.runLater(() -> {
            tokenSelected.setSlot(null);
            tokenSelected.setSlots(null);
            tokenSelected.setCI(ci);
            tokenSelected.setFiles(null);
            tokenSelected.setFilesJson(null);
            tokenSelected.setHashArray(hashArray);
            tokenSelected.setDigest(digest);
            tokenSelected.setPinInicial(pin);
            FormService service = new FormService(stage, tokenSelected, "pades_hash");
            service.showAndWait();
            synchronized (tokenSelected) {
                tokenSelected.notify();
            }
        });
        tokenSelected.showAndWait();
        return tokenSelected;
    }
}
