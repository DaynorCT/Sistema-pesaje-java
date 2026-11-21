package bo.firmadigital.jacobitus.pesaje.ui.comun;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bo.firmadigital.jacobitus.pesaje.auth.AuthService;
import bo.firmadigital.jacobitus.pesaje.auth.ResultadoAutenticacion;

/**
 * Confirma la contrasena del usuario antes de cambiar de modo de
 * operacion, con el mismo estilo que {@link DialogoAviso}.
 */
public final class DialogoContrasena {

    private DialogoContrasena() {
    }

    /**
     * @return true si la contrasena es correcta
     */
    public static boolean confirmar(Window ventana, String titulo, AuthService authService,
            String correo) {
        Dialog<Void> dialogo = new Dialog<>();
        EstilosModal.aplicarDialogo(dialogo);
        if (ventana != null) {
            dialogo.initOwner(ventana);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.setResizable(false);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> null);

        PasswordField campo = new PasswordField();
        campo.setPromptText("Contraseña");
        Label ayuda = new Label("Ingrese su contraseña para continuar.");
        ayuda.getStyleClass().add("subtitulo");
        ayuda.setWrapText(true);

        Button aceptar = new Button("Aceptar");
        aceptar.getStyleClass().add("boton-primario");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        HBox botones = new HBox(8, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        final boolean[] confirmado = {false};
        final boolean[] aceptarOk = {false};

        aceptar.setOnAction(e -> {
            ResultadoAutenticacion auth = authService.autenticar(correo, campo.getText());
            if (!auth.esExito()) {
                DialogoAviso.error(ventana, titulo, "La contraseña no es correcta.");
                return;
            }
            aceptarOk[0] = true;
            confirmado[0] = true;
            dialogo.setResult(null);
            dialogo.close();
        });
        cancelar.setOnAction(e -> {
            dialogo.setResult(null);
            dialogo.close();
        });

        VBox cuerpo = new VBox(10, ayuda, campo, botones);
        cuerpo.setPadding(new Insets(8));
        dialogo.getDialogPane().setContent(cuerpo);
        prepararAlMostrar(dialogo);

        dialogo.showAndWait();
        return confirmado[0] && aceptarOk[0];
    }

    private static void prepararAlMostrar(Dialog<Void> dialogo) {
        javafx.event.EventHandler<DialogEvent> previo = dialogo.getOnShowing();
        dialogo.setOnShowing(evento -> {
            if (previo != null) {
                previo.handle(evento);
            }
            Node barra = dialogo.getDialogPane().lookup(".button-bar");
            if (barra != null) {
                barra.setVisible(false);
                barra.setManaged(false);
            }
            Region region = (Region) dialogo.getDialogPane().lookup(".content");
            if (region != null) {
                region.setPadding(new Insets(12));
            }
        });
    }
}
