package bo.firmadigital.jacobitus.escritorio.formularios;

import java.awt.Toolkit;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Indicadores de estado del teclado para campos de credenciales. */
public final class IndicadorTeclado {
    private IndicadorTeclado() {
    }

    public static Label crearIndicadorBloqMayus(PasswordField... campos) {
        Label indicador = new Label("Bloq Mayús activado");
        indicador.setStyle("-fx-font-size: 11px; -fx-text-fill: #9a3412;");
        indicador.setMinHeight(16);
        indicador.setVisible(false);
        // Reservar el espacio evita que la ventana cambie de tamaño al mostrar el aviso.
        indicador.setManaged(true);

        boolean[] activado = {leerEstadoBloqMayus()};
        for (PasswordField campo : campos) {
            campo.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.CAPS) {
                    activado[0] = !activado[0];
                    actualizar(indicador, activado[0]);
                }
            });
            campo.focusedProperty().addListener((observable, anterior, enfocado) -> {
                if (enfocado) {
                    Platform.runLater(() -> {
                        boolean estadoSistema = leerEstadoBloqMayus();
                        if (estadoSistema != activado[0]) {
                            activado[0] = estadoSistema;
                            actualizar(indicador, activado[0]);
                        }
                    });
                }
            });
        }
        actualizar(indicador, activado[0]);
        return indicador;
    }

    private static boolean leerEstadoBloqMayus() {
        try {
            return Toolkit.getDefaultToolkit().getLockingKeyState(java.awt.event.KeyEvent.VK_CAPS_LOCK);
        } catch (UnsupportedOperationException ex) {
            return false;
        }
    }

    private static void actualizar(Label indicador, boolean activado) {
        indicador.setVisible(activado);
    }
}
