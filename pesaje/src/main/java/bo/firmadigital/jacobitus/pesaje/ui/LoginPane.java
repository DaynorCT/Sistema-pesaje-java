package bo.firmadigital.jacobitus.pesaje.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Pantalla de inicio de sesion.
 *
 * Reproduce el comportamiento del sistema original: distingue el correo
 * inexistente de la contrasena incorrecta y pide el cambio de contrasena
 * cuando el usuario la tiene vencida.
 */
public class LoginPane extends VBox {

    private final TextField campoCorreo = new TextField();
    private final PasswordField campoClave = new PasswordField();
    private final Label mensaje = new Label();
    private final Button botonEntrar = new Button("Ingresar");

    public LoginPane() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(32));
        setMaxWidth(420);
        getStyleClass().add("tarjeta");

        Label titulo = new Label("Sistema de Pesaje");
        titulo.getStyleClass().add("titulo-modulo");

        Label subtitulo = new Label("Ingrese con su usuario local para continuar.");
        subtitulo.getStyleClass().add("subtitulo");

        campoCorreo.setPromptText("Correo electrónico");
        campoClave.setPromptText("Contraseña");
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("mensaje-error");

        botonEntrar.getStyleClass().add("boton-primario");
        botonEntrar.setDefaultButton(true);
        botonEntrar.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(titulo, subtitulo, campoCorreo, campoClave, mensaje, botonEntrar);
        setSpacing(10);
    }

    public TextField getCampoCorreo() {
        return campoCorreo;
    }

    public PasswordField getCampoClave() {
        return campoClave;
    }

    public Button getBotonEntrar() {
        return botonEntrar;
    }

    /**
     * Muestra un mensaje de error en la pantalla.
     *
     * @param texto texto del mensaje
     */
    public void mostrarError(String texto) {
        mensaje.setText(texto == null ? "" : texto);
    }

    /**
     * Muestra un mensaje de exito.
     *
     * @param texto texto del mensaje
     */
    public void mostrarExito(String texto) {
        mensaje.setText(texto == null ? "" : texto);
        mensaje.getStyleClass().remove("mensaje-error");
        mensaje.getStyleClass().add("mensaje-exito");
    }

    /**
     * Muestra un aviso informativo (por ejemplo, la contrasena inicial del
     * administrador en el primer arranque).
     *
     * @param texto texto del aviso
     */
    public void mostrarAviso(String texto) {
        mensaje.setText(texto == null ? "" : texto);
        mensaje.getStyleClass().remove("mensaje-error");
        mensaje.getStyleClass().add("mensaje-exito");
    }

    /**
     * Botonera auxiliar con la misma familia de estilos.
     */
    public static HBox filaDeBotones(Button... botones) {
        HBox fila = new HBox(8);
        fila.setAlignment(Pos.CENTER_RIGHT);
        for (Button boton : botones) {
            fila.getChildren().add(boton);
        }
        return fila;
    }
}
