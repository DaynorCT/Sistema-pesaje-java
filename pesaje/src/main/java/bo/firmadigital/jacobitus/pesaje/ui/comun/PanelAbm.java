package bo.firmadigital.jacobitus.pesaje.ui.comun;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Panel reutilizable de listado + formulario para ABM simples.
 *
 * @param <T> tipo de fila de la tabla
 */
public class PanelAbm<T> extends BorderPane {

    private final TableView<T> tabla = new TableView<>();
    private final Label mensaje = new Label();
    private final Button botonNuevo = new Button("Nuevo");
    private final Button botonGuardar = new Button("Guardar");
    private final GridPane formulario = new GridPane();
    private final Runnable recargar;

    private T seleccion;

    public PanelAbm(Runnable recargar) {
        this.recargar = recargar;
        setPadding(new Insets(8));
        mensaje.getStyleClass().add("subtitulo");

        botonNuevo.getStyleClass().add("boton-secundario");
        botonGuardar.getStyleClass().add("boton-primario");

        formulario.setHgap(10);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(12));
        formulario.getStyleClass().add("tarjeta");

        tabla.getStyleClass().add("tabla-modulo");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> {
            seleccion = actual;
            if (onSeleccion != null) {
                onSeleccion.run();
            }
        });

        botonNuevo.setOnAction(e -> {
            seleccion = null;
            tabla.getSelectionModel().clearSelection();
            if (onNuevo != null) {
                onNuevo.run();
            }
            limpiarMensaje();
        });
        botonGuardar.setOnAction(e -> {
            if (onGuardar != null) {
                onGuardar.run();
            }
        });

        HBox acciones = new HBox(8, botonNuevo, botonGuardar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox derecha = new VBox(8, formulario, acciones, mensaje);
        derecha.setPrefWidth(360);

        BorderPane centro = new BorderPane(tabla);
        centro.setPadding(new Insets(0, 12, 0, 0));
        BorderPane.setMargin(tabla, new Insets(0));

        setCenter(centro);
        setRight(derecha);
        VBox.setVgrow(formulario, Priority.ALWAYS);
    }

    private Runnable onSeleccion;
    private Runnable onNuevo;
    private Runnable onGuardar;

    public TableView<T> tabla() {
        return tabla;
    }

    public GridPane formulario() {
        return formulario;
    }

    public T seleccion() {
        return seleccion;
    }

    public void setItems(javafx.collections.ObservableList<T> items) {
        tabla.setItems(items);
    }

    public void alSeleccionar(Runnable accion) {
        onSeleccion = accion;
    }

    public void alNuevo(Runnable accion) {
        onNuevo = accion;
    }

    public void alGuardar(Runnable accion) {
        onGuardar = accion;
    }

    public void recargar() {
        recargar.run();
    }

    public void mostrarError(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().removeAll("mensaje-exito");
        if (!mensaje.getStyleClass().contains("mensaje-error")) {
            mensaje.getStyleClass().add("mensaje-error");
        }
    }

    public void mostrarExito(String texto) {
        mensaje.setText(texto);
        mensaje.getStyleClass().removeAll("mensaje-error");
        if (!mensaje.getStyleClass().contains("mensaje-exito")) {
            mensaje.getStyleClass().add("mensaje-exito");
        }
    }

    public void limpiarMensaje() {
        mensaje.setText("");
    }

    public static void agregarCampo(GridPane grid, int fila, String etiqueta, TextField campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }

    public static void agregarCombo(GridPane grid, int fila, String etiqueta, ComboBox<String> combo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(combo, 1, fila);
        GridPane.setHgrow(combo, Priority.ALWAYS);
        combo.setMaxWidth(Double.MAX_VALUE);
    }

    public static ComboBox<String> comboEstado() {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        combo.getSelectionModel().selectFirst();
        return combo;
    }

    public static int parsearEntero(String texto, String campo) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El campo " + campo + " debe ser un numero entero.");
        }
    }
}
