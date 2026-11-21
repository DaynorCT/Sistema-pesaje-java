package bo.firmadigital.jacobitus.pesaje.ui.comun;

import java.net.URL;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;

/**
 * Aplica las mismas hojas de estilo que la ventana principal a modales
 * JavaFX (Dialog, Alert, Stage), que por defecto no las heredan.
 */
public final class EstilosModal {

    private EstilosModal() {
    }

    public static void aplicarEscena(Scene escena) {
        if (escena == null) {
            return;
        }
        agregarHoja(escena, "/jacobitus.css");
        agregarHoja(escena, "/pesaje.css");
        String perfil = System.getProperty("jacobitus.profile", "").trim();
        if (!perfil.isEmpty() && escena.getRoot() != null
                && !escena.getRoot().getStyleClass().contains("prerelease-theme")) {
            escena.getRoot().getStyleClass().add("prerelease-theme");
        }
    }

    public static void aplicarDialogo(Dialog<?> dialogo) {
        javafx.event.EventHandler<DialogEvent> anterior = dialogo.getOnShowing();
        dialogo.setOnShowing(evento -> {
            if (anterior != null) {
                anterior.handle(evento);
            }
            Dialog<?> fuente = (Dialog<?>) evento.getSource();
            aplicarEscena(fuente.getDialogPane().getScene());
        });
    }

    public static void aplicarAlerta(Alert alerta) {
        javafx.event.EventHandler<DialogEvent> anterior = alerta.getOnShowing();
        alerta.setOnShowing(evento -> {
            if (anterior != null) {
                anterior.handle(evento);
            }
            Scene escena = alerta.getDialogPane().getScene();
            aplicarEscena(escena);
        });
    }

    private static void agregarHoja(Scene escena, String recurso) {
        URL url = EstilosModal.class.getResource(recurso);
        if (url == null) {
            return;
        }
        String externo = url.toExternalForm();
        if (!escena.getStylesheets().contains(externo)) {
            escena.getStylesheets().add(externo);
        }
    }
}