package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.InputStream;

import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogEvent;
import javafx.scene.image.Image;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public final class IconosVentana {
    private static final String ICONO_APLICACION = "icon.png";

    private IconosVentana() {
    }

    public static void aplicarAplicacion(Stage stage) {
        aplicar(stage, ICONO_APLICACION);
    }

    public static void aplicarConfiguracion(Stage stage) {
        aplicarAplicacion(stage);
    }

    public static void aplicarModal(Alert alert) {
        EventHandler<DialogEvent> onShowing = alert.getOnShowing();
        alert.setOnShowing(event -> {
            if (onShowing != null) {
                onShowing.handle(event);
            }
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            aplicarAplicacion(stage);
            aplicarEstilos(stage.getScene());
        });
    }

    private static void aplicar(Stage stage, String recurso) {
        Image icono = cargar(recurso);
        if (icono != null) {
            stage.getIcons().setAll(icono);
            stage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> {
                stage.getIcons().setAll(icono);
                aplicarEstilos(stage.getScene());
            });
        } else {
            stage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> aplicarEstilos(stage.getScene()));
        }
    }

    private static void aplicarEstilos(Scene scene) {
        if (scene == null) {
            return;
        }
        String hojaEstilos = IconosVentana.class.getClassLoader()
                .getResource("jacobitus.css").toExternalForm();
        if (!scene.getStylesheets().contains(hojaEstilos)) {
            scene.getStylesheets().add(hojaEstilos);
        }
        if (!System.getProperty("jacobitus.profile", "").trim().isEmpty()
                && !scene.getRoot().getStyleClass().contains("prerelease-theme")) {
            scene.getRoot().getStyleClass().add("prerelease-theme");
        }
    }

    private static Image cargar(String recurso) {
        InputStream stream = IconosVentana.class.getClassLoader().getResourceAsStream(recurso);
        if (stream == null) {
            return null;
        }
        return new Image(stream);
    }
}
