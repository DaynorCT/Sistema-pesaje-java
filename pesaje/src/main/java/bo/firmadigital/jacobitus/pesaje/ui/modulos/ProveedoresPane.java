package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroProveedor;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioProveedor;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class ProveedoresPane extends PanelAbm<RegistroProveedor> {

    private final RepositorioProveedor repo = new RepositorioProveedor();
    private final TextField nombre = new TextField();
    private final TextField apellidoP = new TextField();
    private final TextField apellidoM = new TextField();
    private final TextField ci = new TextField();
    private final TextField telefono = new TextField();
    private final TextField empresa = new TextField();
    private final TextField descripcion = new TextField();
    private final javafx.scene.control.ComboBox<String> estado = PanelAbm.comboEstado();

    public ProveedoresPane() {
        super(() -> { });
        TableColumn<RegistroProveedor, String> col = new TableColumn<>("Proveedor");
        col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().etiqueta()));
        tabla().getColumns().setAll(col);
        int f = 0;
        agregarCampo(formulario(), f++, "Nombre", nombre);
        agregarCampo(formulario(), f++, "Ap. paterno", apellidoP);
        agregarCampo(formulario(), f++, "Ap. materno", apellidoM);
        agregarCampo(formulario(), f++, "CI", ci);
        agregarCampo(formulario(), f++, "Teléfono", telefono);
        agregarCampo(formulario(), f++, "Empresa", empresa);
        agregarCampo(formulario(), f++, "Descripción", descripcion);
        agregarCombo(formulario(), f, "Estado", estado);
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    private void rellenar() {
        RegistroProveedor r = seleccion();
        if (r == null) {
            return;
        }
        nombre.setText(r.getNombre() != null ? r.getNombre() : "");
        apellidoP.setText(r.getApellidoPaterno() != null ? r.getApellidoPaterno() : "");
        telefono.setText(r.getTelefono() != null ? r.getTelefono() : "");
        empresa.setText(r.getNombreEmpresa() != null ? r.getNombreEmpresa() : "");
        descripcion.setText(r.getDescripcion() != null ? r.getDescripcion() : "");
        estado.getSelectionModel().select(r.getEstado());
    }

    private void limpiar() {
        nombre.clear();
        apellidoP.clear();
        apellidoM.clear();
        ci.clear();
        telefono.clear();
        empresa.clear();
        descripcion.clear();
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            if (seleccion() == null) {
                repo.crear(nombre.getText().trim(), apellidoP.getText().trim(), apellidoM.getText().trim(),
                        ci.getText().trim(), telefono.getText().trim(), empresa.getText().trim(),
                        descripcion.getText().trim());
                mostrarExito("Proveedor creado.");
            } else {
                repo.actualizar(seleccion().getId(), nombre.getText().trim(), apellidoP.getText().trim(),
                        apellidoM.getText().trim(), ci.getText().trim(), telefono.getText().trim(),
                        empresa.getText().trim(), descripcion.getText().trim(), estado.getValue());
                mostrarExito("Proveedor actualizado.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
