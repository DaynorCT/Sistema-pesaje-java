package bo.firmadigital.jacobitus.pesaje.ui.comun;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Avisos y confirmaciones con el mismo look que el resto del sistema
 * (botones {@code boton-primario} / {@code boton-secundario}), sin
 * {@link javafx.scene.control.Alert} nativo del SO.
 *
 * <p>JavaFX exige al menos un {@link ButtonType} en el dialogo para que la X
 * y {@link Dialog#close()} terminen {@code showAndWait}; el boton por defecto
 * se oculta y se usan botones propios.
 */
public final class DialogoAviso {

    private DialogoAviso() {
    }

    public static void informar(Window ventana, String titulo, String mensaje) {
        mostrarMensaje(ventana, titulo, mensaje, false);
    }

    public static void error(Window ventana, String titulo, String mensaje) {
        mostrarMensaje(ventana, titulo, mensaje, true);
    }

    /**
     * @return true si el usuario confirma
     */
    public static boolean confirmar(Window ventana, String titulo, String mensaje) {
        Dialog<Void> dialogo = crearDialogo(ventana, titulo);
        Label cuerpo = new Label(mensaje);
        cuerpo.getStyleClass().add("subtitulo");
        cuerpo.setWrapText(true);
        cuerpo.setMaxWidth(360);

        Button aceptar = new Button("Sí");
        aceptar.getStyleClass().add("boton-primario");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        HBox botones = new HBox(8, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        final boolean[] confirmado = {false};
        aceptar.setOnAction(e -> {
            confirmado[0] = true;
            cerrar(dialogo);
        });
        cancelar.setOnAction(e -> cerrar(dialogo));

        armarContenido(dialogo, cuerpo, botones);
        dialogo.showAndWait();
        return confirmado[0];
    }

    private static void mostrarMensaje(Window ventana, String titulo, String mensaje,
            boolean esError) {
        Dialog<Void> dialogo = crearDialogo(ventana, titulo);
        Label cuerpo = new Label(mensaje);
        cuerpo.getStyleClass().add(esError ? "mensaje-error" : "subtitulo");
        cuerpo.setWrapText(true);
        cuerpo.setMaxWidth(360);

        Button aceptar = new Button("Aceptar");
        aceptar.getStyleClass().add("boton-primario");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");
        aceptar.setOnAction(e -> cerrar(dialogo));
        cancelar.setOnAction(e -> cerrar(dialogo));

        HBox botones = new HBox(8, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER_RIGHT);
        armarContenido(dialogo, cuerpo, botones);
        dialogo.showAndWait();
    }

    private static void armarContenido(Dialog<Void> dialogo, Label cuerpo, HBox botones) {
        VBox contenido = new VBox(12, cuerpo, botones);
        contenido.setPadding(new Insets(8));
        dialogo.getDialogPane().setContent(contenido);
        prepararAlMostrar(dialogo);
    }

    private static Dialog<Void> crearDialogo(Window ventana, String titulo) {
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
        return dialogo;
    }

    private static void cerrar(Dialog<Void> dialogo) {
        dialogo.setResult(null);
        dialogo.close();
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
            Button cancelSistema = (Button) dialogo.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelSistema != null) {
                cancelSistema.setVisible(false);
                cancelSistema.setManaged(false);
                cancelSistema.setOnAction(e -> cerrar(dialogo));
            }
            Region region = (Region) dialogo.getDialogPane().lookup(".content");
            if (region != null) {
                region.setPadding(new Insets(12));
            }
        });
    }
}
