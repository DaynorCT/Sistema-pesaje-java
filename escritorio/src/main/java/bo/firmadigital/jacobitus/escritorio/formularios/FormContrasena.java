package bo.firmadigital.jacobitus.escritorio.formularios;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FormContrasena extends Stage {
    private String pass;
    private boolean bloquea = false;
    private boolean forzarEnveloped = false;
    private boolean usarPrefijo = false;
    private TextField  nodeField;

    public FormContrasena(Stage parent, int tipo) {
        setTitle("Jacobitus · Autorización de firma");
        IconosVentana.aplicarAplicacion(this);
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(14, 18, 12, 18));
        VBox vBox = new VBox();
        vBox.setSpacing(8);
        Label etiquetaPin = new Label("PIN del token");
        etiquetaPin.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #52616f;");
        vBox.getChildren().add(etiquetaPin);
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Ingrese el PIN");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER && !passwordField.getText().isEmpty()) {
                pass = passwordField.getText();
                close();
            }
        });
        vBox.getChildren().addAll(passwordField, IndicadorTeclado.crearIndicadorBloqMayus(passwordField));
        vBox.getChildren().add(new Separator(Orientation.HORIZONTAL));
        switch (tipo) {
            case 0:
                vBox.getChildren().add(new Label("Información"));
                break;
            case 1:
                CheckBox checkBox = new CheckBox("Bloquear documento.");
                vBox.getChildren().add(checkBox);
                checkBox.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    bloquea = newValue;
                });
                break;
            case 2:
                vBox.getChildren().add(new Label("PKCS#7"));
                break;
            case 3:
                nodeField = new TextField();
                vBox.getChildren().add(nodeField);
                nodeField.setPromptText("Nodo");
                CheckBox checkBoxEnveloped = new CheckBox("Forzar enveloped.");
                vBox.getChildren().add(checkBoxEnveloped);
                checkBoxEnveloped.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    forzarEnveloped = newValue;
                });
                CheckBox checkBoxPrefix = new CheckBox("Usar prefijo.");
                vBox.getChildren().add(checkBoxPrefix);
                checkBoxPrefix.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    usarPrefijo = newValue;
                });
                break;
        }
        root.setCenter(vBox);

        Button buttonAceptar = new Button("Aceptar");
        buttonAceptar.setDisable(true);
        passwordField.textProperty().addListener((observable, anterior, nuevo) ->
                buttonAceptar.setDisable(nuevo == null || nuevo.isEmpty()));
        buttonAceptar.setOnAction(t -> {
            pass = passwordField.getText();
            close();
        });

        Button buttonCancelar = new Button("Cancelar");
        buttonCancelar.setOnAction(t -> {
            close();
        });
        HBox hBox = new HBox();
        hBox.setSpacing(8);
        hBox.setAlignment(Pos.CENTER_RIGHT);
        hBox.getChildren().addAll(buttonCancelar, buttonAceptar);
        root.setBottom(hBox);
        Scene scene = new Scene(root);
        setScene(scene);
        setMinWidth(360);
        setWidth(360);
        setResizable(true);
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
        if (nodeField == null){
            return null;
        }
        if (nodeField.getText().trim().equals("")) {
            return null;
        }
        return nodeField.getText();
    }
}
