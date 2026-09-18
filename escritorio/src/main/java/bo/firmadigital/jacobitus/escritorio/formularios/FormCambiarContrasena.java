package bo.firmadigital.jacobitus.escritorio.formularios;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.token.GestorSlot;
import bo.firmadigital.jacobitus.token.IToken;
import bo.firmadigital.jacobitus.token.Slot;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FormCambiarContrasena extends Stage {
    private Stage stage;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    public FormCambiarContrasena(Stage parent, long slot) {
        stage = parent;
        setTitle("Pin del token");
        IconosVentana.aplicarAplicacion(this);
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        VBox root = new VBox();
        root.setPadding(new Insets(10));
        root.setSpacing(8);
        Label oldLabel = new Label("Introduzca pin actual:");
        root.getChildren().add(oldLabel);
        PasswordField oldPasswordField = new PasswordField();
        oldPasswordField.setPromptText("Pin actual");
        root.getChildren().add(oldPasswordField);
        Label label = new Label("Introduzca nuevo pin:");
        root.getChildren().add(label);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nuevo pin");
        root.getChildren().add(passwordField);
        Label label2 = new Label("Repita nuevo pin:");
        root.getChildren().add(label2);
        PasswordField passwordField2 = new PasswordField();
        passwordField2.setPromptText("Repita nuevo pin");
        root.getChildren().add(passwordField2);

        Button buttonAceptar = new Button("Aceptar");
        Button buttonCancelar = new Button("Cancelar");

        oldPasswordField.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });
        passwordField.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                passwordField2.requestFocus();
            }
        });
        passwordField2.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                cambiarContrasenia(slot, oldPasswordField.getText(), passwordField.getText(),
                        passwordField2.getText(), buttonAceptar, buttonCancelar);
            }
        });
        buttonAceptar.setOnAction(t -> {
            cambiarContrasenia(slot, oldPasswordField.getText(), passwordField.getText(),
                    passwordField2.getText(), buttonAceptar, buttonCancelar);
        });
        buttonCancelar.setOnAction(t -> {
            close();
        });

        HBox hBox = new HBox();
        hBox.getChildren().addAll(buttonAceptar, buttonCancelar);
        root.getChildren().add(hBox);
        Scene scene = new Scene(root, 300, 220);
        setScene(scene);
    }

    private void cambiarContrasenia(long slot, String oldPassword, String pass, String pass2,
            Button buttonAceptar, Button buttonCancelar) {
        String error = validarNuevaContrasenia(slot, pass, pass2);
        if (error != null) {
            mostrarError(error);
            return;
        }

        stage.getScene().setCursor(Cursor.WAIT);
        buttonAceptar.setDisable(true);
        buttonCancelar.setDisable(true);

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(FormCambiarContrasena.this.getConfigFirmador());
                Slot oSlot = gestorSlot.obtenerSlot(slot);
                if (oSlot == null) {
                    throw new JacobitusException("El slot " + slot + " no se encuentra disponible.");
                }
                IToken token = oSlot.getToken();
                if (oldPassword.startsWith("@unlock:")) {
                    token.unlockPin(oldPassword.split("@unlock:")[1], pass);
                } else if (oldPassword.startsWith("@so:")) {
                    token.modificarPinSo(oldPassword.split("@so:")[1], pass);
                } else {
                    token.modificarPin(oldPassword, pass);
                }
                return null;
            }
        };
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Se cambió la contraseña.", ButtonType.OK);
            alert.initOwner(stage);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            close();
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            stage.getScene().setCursor(Cursor.DEFAULT);
            buttonAceptar.setDisable(false);
            buttonCancelar.setDisable(false);
            mostrarError(task.getException().getMessage());
        });
        new Thread(task).start();
    }

    private String validarNuevaContrasenia(long slot, String pass, String pass2) {
        if (!pass.equals(pass2)) {
            return "Las contraseñas no coinciden.";
        }
        if (pass.length() < 8) {
            return "La contraseña es muy corta.";
        }
        int num = 0;
        int may = 0;
        int minu = 0;
        char[] password = pass.toCharArray();
        for (int i = 0; i < pass.length(); i++) {
            if (password[i] >= '0' && password[i] <= '9') {
                num++;
            } else if (password[i] >= 'A' && password[i] <= 'Z') {
                may++;
            } else if (password[i] >= 'a' && password[i] <= 'z') {
                minu++;
            }
        }
        if (slot == -1 && (num < 1 || may < 1 || minu < 1)) {
            return "La contraseña debe contener al menos un número, una letra mayúscula y una letra minúscula.";
        }
        return null;
    }

    private void mostrarError(String error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, error, ButtonType.OK);
        alert.initOwner(stage);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Jacobitus");
        IconosVentana.aplicarModal(alert);
        alert.showAndWait();
    }
}
