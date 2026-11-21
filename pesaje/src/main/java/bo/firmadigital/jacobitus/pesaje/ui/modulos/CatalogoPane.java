package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroCatalogo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioCatalogo;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

/** ABM de productos, origenes o destinos. */
public final class CatalogoPane extends PanelAbm<RegistroCatalogo> {

    private final RepositorioCatalogo repositorio;
    private final TextField nombre = new TextField();
    private final TextField descripcion = new TextField();
    private final javafx.scene.control.ComboBox<String> estado = PanelAbm.comboEstado();

    public CatalogoPane(String tabla) {
        super(() -> { });
        this.repositorio = new RepositorioCatalogo(tabla);

        TableColumn<RegistroCatalogo, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombre()));
        tabla().getColumns().setAll(colNombre);
        anexarColumnaEstado(RegistroCatalogo::getEstado);

        agregarCampo(formulario(), 0, "Nombre", nombre);
        agregarCampo(formulario(), 1, "Descripción", descripcion);
        agregarCombo(formulario(), 2, "Estado", estado);

        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repositorio.listarTodos()));
    }

    private void rellenar() {
        RegistroCatalogo fila = seleccion();
        if (fila == null) {
            return;
        }
        nombre.setText(fila.getNombre());
        descripcion.setText(fila.getDescripcion() != null ? fila.getDescripcion() : "");
        estado.getSelectionModel().select(fila.getEstado());
    }

    private void limpiar() {
        nombre.clear();
        descripcion.clear();
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            if (nombre.getText().trim().isEmpty()) {
                mostrarError("El nombre es obligatorio.");
                return;
            }
            if (seleccion() == null) {
                repositorio.crear(nombre.getText().trim(), descripcion.getText().trim());
                mostrarExito("Registro creado.");
            } else {
                repositorio.actualizar(seleccion().getId(), nombre.getText().trim(),
                        descripcion.getText().trim(), estado.getValue());
                mostrarExito("Registro actualizado.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
