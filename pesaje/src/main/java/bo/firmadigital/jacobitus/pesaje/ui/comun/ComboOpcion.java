package bo.firmadigital.jacobitus.pesaje.ui.comun;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;

/** Elemento de combo con id de base de datos. */
public final class ComboOpcion {

    private final int id;
    private final String etiqueta;

    public ComboOpcion(int id, String etiqueta) {
        this.id = id;
        this.etiqueta = etiqueta;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static ComboBox<ComboOpcion> crear(List<ComboOpcion> opciones) {
        ComboBox<ComboOpcion> combo = new ComboBox<>(FXCollections.observableArrayList(opciones));
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    public static Integer idSeleccionado(ComboBox<ComboOpcion> combo) {
        ComboOpcion op = combo.getSelectionModel().getSelectedItem();
        return op != null ? op.getId() : null;
    }

    public static List<ComboOpcion> vacio() {
        return new ArrayList<>();
    }
}
