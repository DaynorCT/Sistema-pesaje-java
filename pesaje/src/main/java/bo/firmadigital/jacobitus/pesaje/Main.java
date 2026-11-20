package bo.firmadigital.jacobitus.pesaje;

import java.io.InputStream;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Punto de entrada del Sistema de Pesaje.
 *
 * El shell definitivo (login, navegacion, modulos) se construye en la
 * tarea T5; esta version deja verificable el arranque de JavaFX, la carga
 * del CSS de Jacobitus y el empaquetado del jar.
 */
public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();
        Label label = new Label("Sistema de Pesaje");
        label.getStyleClass().add("version-banner");
        root.getChildren().add(label);

        Scene scene = new Scene(root, 900, 600);
        cargarEstilos(scene);

        stage.setTitle("Sistema de Pesaje");
        stage.setScene(scene);
        stage.show();
    }

    private void cargarEstilos(Scene scene) {
        InputStream css = Main.class.getResourceAsStream("/jacobitus.css");
        if (css != null) {
            scene.getStylesheets().add(getClass().getResource("/jacobitus.css").toExternalForm());
        }
    }
}
