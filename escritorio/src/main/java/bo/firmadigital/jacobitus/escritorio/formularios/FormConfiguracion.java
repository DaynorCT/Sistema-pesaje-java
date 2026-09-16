package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.utilidades.AutoInicioHelper;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.token.Slot;
import bo.firmadigital.jacobitus.token.TokenPKCS12;
import bo.firmadigital.jacobitus.utilidades.SistemaOperativoHelper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FormConfiguracion extends Stage {
    private Config config;
    private CheckBox checkBox;
    private final CheckBox checkBoxPort2;
    private final CheckBox checkBoxPort3;
    private boolean checkBoxEvent = true;
    private TextField textFieldIP;
    private TextField textFieldPort;
    private TextField textFieldToken;
    private final Button buttonControlador;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    public FormConfiguracion(Stage parent) {
        setTitle("Opciones de configuración");
        IconosVentana.aplicarConfiguracion(this);
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        config = Config.getInstance();
        HBox root = new HBox();
        VBox vbox1 = new VBox();
        vbox1.setPadding(new Insets(10));
        vbox1.setSpacing(8);
        vbox1.setMinWidth(210);

        // Opciones de configuracion de proxy
        Label titleP = new Label("Proxy");
        titleP.setStyle("-fx-font-weight: bold");
        vbox1.getChildren().add(titleP);
        checkBox = new CheckBox("Utilizar proxy");
        checkBox.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    textFieldIP.setDisable(!newValue);
                    textFieldPort.setDisable(!newValue);
                    if (newValue == false && config.isProxyEnabled() == false) {
                        textFieldIP.setText(config.getProxyIP());
                        textFieldPort.setText(config.getProxyPort());
                    }
                });
        vbox1.getChildren().add(checkBox);
        Label labelIP = new Label("Introduzca la IP:");
        vbox1.getChildren().add(labelIP);
        textFieldIP = new TextField();
        vbox1.getChildren().add(textFieldIP);
        Label labelPort = new Label("Introduzca el puerto:");
        vbox1.getChildren().add(labelPort);
        textFieldPort = new TextField("3128");
        vbox1.getChildren().add(textFieldPort);
        Button buttonGuardar = new Button("Guardar Proxy");
        buttonGuardar.setOnAction(t -> {
            config.setProxyEnabled(checkBox.isSelected());
            config.setProxyIP(textFieldIP.getText());
            config.setProxyPort(textFieldPort.getText());
            config.save();
            close();
        });
        vbox1.getChildren().add(buttonGuardar);
        vbox1.getChildren().add(new Separator(Orientation.HORIZONTAL));

        // Configuracion de puertos (compatibilidad con otros firmadores)
        Label titleS = new Label("Puerto secundario");
        titleS.setStyle("-fx-font-weight: bold");
        vbox1.getChildren().add(titleS);
        checkBoxPort2 = new CheckBox("Habilitar puerto 4637");
        checkBoxPort2.setSelected(config.isSecondaryPortEnabled());
        vbox1.getChildren().add(checkBoxPort2);
        checkBoxPort3 = new CheckBox("Habilitar puerto 3200");
        checkBoxPort3.setSelected(config.isTertiaryPortEnabled());
        checkBoxPort2.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    config.setSecondaryPortEnabled(newValue);
                    config.save();
                    if (checkBoxEvent) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "Para que este cambio tenga efecto,\ndeberá reiniciar la aplicación.", ButtonType.OK);
                        alert.initOwner(parent);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.setTitle("Jacobitus");
                        IconosVentana.aplicarModal(alert);
                        alert.showAndWait();
                    }
                });
        checkBoxPort3.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    config.setTertiaryPortEnabled(newValue);
                    config.save();
                    if (checkBoxEvent) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "Para que este cambio tenga efecto,\ndeberá reiniciar la aplicación.", ButtonType.OK);
                        alert.initOwner(parent);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.setTitle("Jacobitus");
                        IconosVentana.aplicarModal(alert);
                        alert.showAndWait();
                    }
                });
        vbox1.getChildren().add(checkBoxPort3);
        vbox1.getChildren().add(new Separator(Orientation.HORIZONTAL));

        Label titleG = new Label("General");
        titleG.setStyle("-fx-font-weight: bold");
        vbox1.getChildren().add(titleG);
        CheckBox checkBoxTrayIcon = new CheckBox("Ejecutar en segundo plano");
        final boolean[] checkBoxTrayIconEvent = { true };
        checkBoxTrayIcon.setSelected(config.isTrayIconEnabled());
        checkBoxTrayIcon.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    if (!checkBoxTrayIconEvent[0]) {
                        return;
                    }
                    if (newValue && !SistemaOperativoHelper.esUnix() && !java.awt.SystemTray.isSupported()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                TaskBar.diagnosticoBandejaNoDisponible(), ButtonType.OK);
                        alert.initOwner(parent);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.setTitle("Jacobitus");
                        alert.setHeaderText(null);
                        IconosVentana.aplicarModal(alert);
                        alert.showAndWait();
                        checkBoxTrayIconEvent[0] = false;
                        checkBoxTrayIcon.setSelected(false);
                        checkBoxTrayIconEvent[0] = true;
                        return;
                    }
                    config.setTrayIconEnabled(newValue);
                    config.save();
                    if (AutoInicioHelper.estaActivado()) {
                        try {
                            AutoInicioHelper.activar();
                        } catch (RuntimeException ex) {
                            Logger.getLogger(FormAplicacion.class.getName()).log(Level.WARNING,
                                    "No se pudo actualizar el inicio automatico", ex);
                        }
                    }
                    if (newValue && SistemaOperativoHelper.esUnix()) {
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                TaskBar.diagnosticoActivacionUnix(), ButtonType.OK);
                        alert.initOwner(parent);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.setTitle("Jacobitus");
                        alert.setHeaderText(null);
                        IconosVentana.aplicarModal(alert);
                        alert.showAndWait();
                    }
                    Alert alert = new Alert(Alert.AlertType.WARNING,
                            "Para que este cambio tenga efecto,\ndeberá reiniciar la aplicación.", ButtonType.OK);
                    alert.initOwner(parent);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    alert.setHeaderText(null);
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                });

        CheckBox checkBoxAutoInicio = new CheckBox("Iniciar automáticamente");
        final boolean[] checkBoxAutoInicioEvent = { true };
        checkBoxAutoInicio.setDisable(!AutoInicioHelper.estaSoportado());
        boolean autoInicioActivado = AutoInicioHelper.estaActivado();
        checkBoxAutoInicio.setSelected(autoInicioActivado);
        config.setAutoStartEnabled(autoInicioActivado);
        config.save();
        checkBoxAutoInicio.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    if (!checkBoxAutoInicioEvent[0]) {
                        return;
                    }
                    try {
                        if (newValue) {
                            AutoInicioHelper.activar();
                        } else {
                            AutoInicioHelper.desactivar();
                        }
                        boolean estadoReal = AutoInicioHelper.estaActivado();
                        config.setAutoStartEnabled(estadoReal);
                        config.save();
                        if (estadoReal != newValue) {
                            Alert alert = new Alert(Alert.AlertType.WARNING,
                                    "El sistema operativo no permitió cambiar el inicio automático.\n"
                                            + "Revise la configuración de aplicaciones de inicio del sistema.",
                                    ButtonType.OK);
                            alert.initOwner(parent);
                            alert.initModality(Modality.APPLICATION_MODAL);
                            alert.setTitle("Jacobitus");
                            alert.setHeaderText(null);
                            IconosVentana.aplicarModal(alert);
                            alert.showAndWait();
                            checkBoxAutoInicioEvent[0] = false;
                            checkBoxAutoInicio.setSelected(estadoReal);
                            checkBoxAutoInicioEvent[0] = true;
                        }
                    } catch (RuntimeException ex) {
                        Logger.getLogger(FormAplicacion.class.getName()).log(Level.WARNING,
                                "No se pudo configurar el inicio automatico", ex);
                        Alert alert = new Alert(Alert.AlertType.WARNING,
                                "No se pudo cambiar el inicio automático.\n"
                                        + "Revise la configuración de aplicaciones de inicio de Windows.",
                                ButtonType.OK);
                        alert.initOwner(parent);
                        alert.initModality(Modality.APPLICATION_MODAL);
                        alert.setTitle("Jacobitus");
                        alert.setHeaderText(null);
                        IconosVentana.aplicarModal(alert);
                        alert.showAndWait();
                        checkBoxAutoInicioEvent[0] = false;
                        checkBoxAutoInicio.setSelected(oldValue);
                        checkBoxAutoInicioEvent[0] = true;
                    }
                });
        vbox1.getChildren().add(checkBoxAutoInicio);
        vbox1.getChildren().add(checkBoxTrayIcon);

        CheckBox checkBoxConfirmarSalida = new CheckBox("Confirmar antes de salir");
        checkBoxConfirmarSalida.setSelected(config.isConfirmarSalidaEnabled());
        checkBoxConfirmarSalida.selectedProperty()
                .addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    config.setConfirmarSalidaEnabled(newValue);
                    config.save();
                });
        vbox1.getChildren().add(checkBoxConfirmarSalida);

        root.getChildren().add(vbox1);

        Separator separator = new Separator(Orientation.VERTICAL);
        root.getChildren().add(separator);

        VBox vbox2 = new VBox();
        vbox2.setPadding(new Insets(10));
        vbox2.setSpacing(8);
        vbox2.setMinWidth(210);

        // Configuracion de controlador por defecto
        Label titleD = new Label("Controlador");
        titleD.setStyle("-fx-font-weight: bold");
        vbox2.getChildren().add(titleD);
        final Label labelDriver = new Label(
                "Nombre: " + (config.getDriver() == null ? "Ninguno" : config.getDriver().getName()));
        vbox2.getChildren().add(labelDriver);
        if (config.getDriver() == null) {
            buttonControlador = new Button("Seleccionar");
        } else {
            buttonControlador = new Button("Remover");
        }
        vbox2.getChildren().add(buttonControlador);
        buttonControlador.setOnAction(t -> {
            buttonControlador.setDisable(true);
            if (config.getDriver() == null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Abrir Controlador");
                if (SistemaOperativoHelper.esWindows()) {
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Controlador (*.dll)",
                            "*.dll");
                    fileChooser.getExtensionFilters().add(extFilter);
                } else if (SistemaOperativoHelper.esUnix()) {
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Controlador (*.so)",
                            "*.so");
                    fileChooser.getExtensionFilters().add(extFilter);
                    FileChooser.ExtensionFilter extFilterDocs = new FileChooser.ExtensionFilter("Todos", "*.*", "*.*");
                    fileChooser.getExtensionFilters().add(extFilterDocs);
                } else {
                    FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Controlador (*.dylib)",
                            "*.dylib");
                    fileChooser.getExtensionFilters().add(extFilter);
                }
                File file = fileChooser.showOpenDialog(parent);
                if (file != null) {
                    config.setDriver(file);
                    config.save();
                    buttonControlador.setText("Remover");
                }
            } else {
                config.setDriver(null);
                config.save();
                buttonControlador.setText("Seleccionar");
            }
            labelDriver.setText("Nombre: " + (config.getDriver() == null ? "Ninguno" : config.getDriver().getName()));
            buttonControlador.setDisable(false);
        });
        vbox2.getChildren().add(new Separator(Orientation.HORIZONTAL));

        // Configuracion de softoken
        Label titleT = new Label("Softoken");
        titleT.setStyle("-fx-font-weight: bold");
        vbox2.getChildren().add(titleT);
        Label labelToken = new Label("Archivo para token/software:");
        vbox2.getChildren().add(labelToken);
        textFieldToken = new TextField();
        textFieldToken.setDisable(true);
        vbox2.getChildren().add(textFieldToken);
        Button buttonCrear = new Button("Crear Token");
        Button btnAbrirUbicacion = new Button("Abrir ubicación");
        buttonCrear.setOnAction(t -> {
            FormContrasenaNueva contrasena = new FormContrasenaNueva(parent);
            contrasena.showAndWait();
            if (contrasena.getPass() != null) {
                Slot slot = new Slot(config.getTokenToCreate(), this.getConfigFirmador());
                TokenPKCS12 token = new TokenPKCS12(slot);
                try {
                    token.crear(contrasena.getPass());
                    textFieldToken.setText(config.getToken().getName());
                    buttonCrear.setDisable(true);
                    btnAbrirUbicacion.setDisable(false);
                } catch (GeneralSecurityException ex) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, ex.getMessage(), ButtonType.OK);
                    alert.initOwner(parent);
                    alert.initModality(Modality.APPLICATION_MODAL);
                    alert.setTitle("Jacobitus");
                    IconosVentana.aplicarModal(alert);
                    alert.showAndWait();
                }
            }
        });
        vbox2.getChildren().add(buttonCrear);
        btnAbrirUbicacion.setOnAction(t -> {
            try {
                if (SistemaOperativoHelper.esUnix()) {
                    Runtime.getRuntime().exec(new String[] { "sh", "-c", "/usr/bin/xdg-open '" + config.getToken().getParentFile().getPath() + "'" });
                }
                if (SistemaOperativoHelper.esWindows()) {
                    Runtime.getRuntime().exec("explorer " + config.getToken().getParentFile().getPath());
                }
                if (SistemaOperativoHelper.esMacOS()) {
                    Runtime.getRuntime().exec(new String[]{"/usr/bin/open", config.getToken().getParentFile().getPath()});
                }
            } catch (IOException ex) {
                Logger.getLogger(FormAplicacion.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        vbox2.getChildren().add(btnAbrirUbicacion);

        root.getChildren().add(vbox2);

        // Establecer valores configurados
        checkBox.setSelected(true);
        checkBox.setSelected(config.isProxyEnabled());
        textFieldIP.setText(config.getProxyIP());
        textFieldPort.setText(config.getProxyPort());
        if (config.getToken() == null) {
            textFieldToken.setText("Ninguno");
            buttonCrear.setDisable(false);
            btnAbrirUbicacion.setDisable(true);
        } else {
            textFieldToken.setText(config.getToken().getName());
            buttonCrear.setDisable(true);
            btnAbrirUbicacion.setDisable(false);
        }

        Scene scene = new Scene(root);
        setScene(scene);
    }
}
