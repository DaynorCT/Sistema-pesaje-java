package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Registro de los modulos de la aplicacion.
 *
 * Cada modulo se registra con la fabrica que construye su panel. Asi el
 * shell no cambia cuando se agrega un modulo: la tarea que lo implementa
 * se limita a registrarlo.
 */
public final class Modulos {

    private static final Map<Navegacion, Supplier<Node>> REGISTRO = new EnumMap<>(Navegacion.class);

    private Modulos() {
    }

    /**
     * Registra el panel de un modulo.
     *
     * @param modulo modulo del menu
     * @param fabrica fabrica de su panel
     */
    public static void registrar(Navegacion modulo, Supplier<Node> fabrica) {
        REGISTRO.put(modulo, fabrica);
    }

    /**
     * Construye el panel de un modulo. Si el modulo todavia no esta
     * implementado se muestra un aviso, sin romper la navegacion.
     *
     * @param modulo modulo del menu
     * @return panel del modulo
     */
    public static Node crear(Navegacion modulo) {
        Supplier<Node> fabrica = REGISTRO.get(modulo);
        if (fabrica == null) {
            StackPane aviso = new StackPane();
            Label etiqueta = new Label("El módulo " + modulo.getTitulo() + " todavía no está disponible.");
            etiqueta.getStyleClass().add("subtitulo");
            aviso.getChildren().add(etiqueta);
            return aviso;
        }
        return fabrica.get();
    }

    /**
     * @param modulo modulo del menu
     * @return true si el modulo ya tiene panel implementado
     */
    public static boolean implementado(Navegacion modulo) {
        return REGISTRO.containsKey(modulo);
    }
}
