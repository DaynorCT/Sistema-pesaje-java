package bo.firmadigital.jacobitus.pesaje.ui.comun;

import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Menu desplegable con {@code menu-flotante} / {@code item-menu}, mismo
 * aspecto en modo integrado y en el sistema completo.
 */
public final class MenuFlotante {

    private Popup popup;

    /**
     * Abre o cierra el menu anclado al nodo.
     */
    public void alternar(Node ancla, Entrada... entradas) {
        if (popup != null && popup.isShowing()) {
            popup.hide();
            return;
        }
        VBox panel = new VBox(2);
        panel.setPadding(new Insets(8));
        panel.getStyleClass().addAll("tarjeta", "menu-flotante");
        panel.setMinWidth(220);

        for (Entrada entrada : entradas) {
            if (entrada.separador) {
                panel.getChildren().add(lineaSeparador());
                continue;
            }
            if (entrada.encabezado) {
                Label titulo = new Label(entrada.texto);
                titulo.getStyleClass().addAll("subtitulo", "menu-encabezado");
                titulo.setMaxWidth(Double.MAX_VALUE);
                titulo.setPadding(new Insets(4, 14, 4, 14));
                panel.getChildren().add(titulo);
                continue;
            }
            Label item = new Label(entrada.texto);
            item.getStyleClass().add("item-menu");
            item.setMaxWidth(Double.MAX_VALUE);
            Runnable accion = entrada.accion;
            if (accion != null) {
                item.setOnMouseClicked(e -> {
                    ocultar();
                    accion.run();
                });
            }
            panel.getChildren().add(item);
        }

        popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(panel);
        panel.sceneProperty().addListener((obs, anterior, scene) -> {
            if (scene != null) {
                EstilosModal.aplicarEscena(scene);
            }
        });
        Bounds bounds = ancla.localToScreen(ancla.getBoundsInLocal());
        double y = bounds.getMaxY() + 4;
        double anchoPanel = 220;
        double x = Math.max(bounds.getMinX(), bounds.getMaxX() - anchoPanel);
        popup.show(ancla, x, y);
    }

    public void ocultar() {
        if (popup != null && popup.isShowing()) {
            popup.hide();
        }
    }

    public static Entrada opcion(String texto, Runnable accion) {
        return new Entrada(texto, accion, false, false);
    }

    public static Entrada encabezado(String texto) {
        return new Entrada(texto, null, true, false);
    }

    public static Entrada separador() {
        return new Entrada(null, null, false, true);
    }

    private static Region lineaSeparador() {
        Region linea = new Region();
        linea.getStyleClass().add("separador-menu");
        linea.setMaxWidth(Double.MAX_VALUE);
        return linea;
    }

    public static final class Entrada {
        private final String texto;
        private final Runnable accion;
        private final boolean encabezado;
        private final boolean separador;

        private Entrada(String texto, Runnable accion, boolean encabezado, boolean separador) {
            this.texto = texto;
            this.accion = accion;
            this.encabezado = encabezado;
            this.separador = separador;
        }
    }
}
