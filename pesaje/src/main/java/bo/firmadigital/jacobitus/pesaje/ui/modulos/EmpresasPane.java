package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroEmpresa;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioEmpresa;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class EmpresasPane extends PanelAbm<RegistroEmpresa> {

    private final RepositorioEmpresa repo = new RepositorioEmpresa();
    private final TextField nombre = new TextField();
    private final TextField actividad = new TextField();
    private final TextField correo = new TextField();
    private final TextField telefono = new TextField();
    private final javafx.scene.control.ComboBox<String> estado = PanelAbm.comboEstado();

    public EmpresasPane() {
        super(() -> { });
        TableColumn<RegistroEmpresa, String> col = new TableColumn<>("Empresa");
        col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreEmpresa()));
        TableColumn<RegistroEmpresa, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstado()));
        tabla().getColumns().setAll(col, colEstado);
        agregarCampo(formulario(), 0, "Nombre", nombre);
        agregarCampo(formulario(), 1, "Actividad", actividad);
        agregarCampo(formulario(), 2, "Correo", correo);
        agregarCampo(formulario(), 3, "Teléfono", telefono);
        agregarCombo(formulario(), 4, "Estado", estado);
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    private void rellenar() {
        RegistroEmpresa r = seleccion();
        if (r == null) {
            return;
        }
        nombre.setText(r.getNombreEmpresa());
        actividad.setText(r.getActividad() != null ? r.getActividad() : "");
        correo.setText(r.getCorreo() != null ? r.getCorreo() : "");
        telefono.setText(r.getTelefono() != null ? r.getTelefono() : "");
        estado.getSelectionModel().select(r.getEstado());
    }

    private void limpiar() {
        nombre.clear();
        actividad.clear();
        correo.clear();
        telefono.clear();
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            if (nombre.getText().trim().isEmpty()) {
                mostrarError("El nombre de la empresa es obligatorio.");
                return;
            }
            if (seleccion() == null) {
                repo.crear(nombre.getText().trim(), actividad.getText().trim(),
                        correo.getText().trim(), telefono.getText().trim());
                mostrarExito("Empresa creada.");
            } else {
                repo.actualizar(seleccion().getId(), nombre.getText().trim(), actividad.getText().trim(),
                        correo.getText().trim(), telefono.getText().trim(), estado.getValue());
                mostrarExito("Empresa actualizada.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
