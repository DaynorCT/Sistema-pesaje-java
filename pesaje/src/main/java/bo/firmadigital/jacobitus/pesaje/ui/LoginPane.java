package bo.firmadigital.jacobitus.pesaje.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.datos.LogoEmpresa;

/**
 * Pantalla de inicio de sesion.
 *
 * Distingue el correo inexistente de la contrasena incorrecta, igual que
 * el sistema original. El logo es el del sistema: el mismo que aparece en
 * la cabecera y en la boleta.
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
        getStyleClass().add("login");

        Label titulo = new Label("Sistema de Pesaje");
        titulo.getStyleClass().add("titulo-modulo");

        ImageView logo = new ImageView(new Image(LogoEmpresa.RECURSO_SISTEMA));
        logo.setFitWidth(320);
        logo.setPreserveRatio(true);
        StackPane cajaLogo = new StackPane(logo);
        cajaLogo.getStyleClass().add("login-logo");

        Label subtitulo = new Label("Ingrese con su usuario local para continuar.");
        subtitulo.getStyleClass().add("subtitulo");

        campoCorreo.setPromptText("Correo electrónico");
        campoClave.setPromptText("Contraseña");
        mensaje.setWrapText(true);
        mensaje.getStyleClass().add("mensaje-error");

        botonEntrar.getStyleClass().add("boton-primario");
        botonEntrar.setDefaultButton(true);
        botonEntrar.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(cajaLogo, titulo, subtitulo, campoCorreo, campoClave, mensaje, botonEntrar);
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
        mensaje.getStyleClass().remove("mensaje-exito");
        mensaje.getStyleClass().add("subtitulo");
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
