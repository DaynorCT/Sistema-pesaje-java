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
        TableColumn<RegistroEmpresa, String> colEmpresa = new TableColumn<>("Empresa");
        colEmpresa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreEmpresa()));
        colEmpresa.setPrefWidth(180);
        TableColumn<RegistroEmpresa, String> colActividad = new TableColumn<>("Actividad");
        colActividad.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getActividad() != null ? c.getValue().getActividad() : ""));
        colActividad.setPrefWidth(160);
        TableColumn<RegistroEmpresa, String> colCorreo = new TableColumn<>("Correo");
        colCorreo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCorreo() != null ? c.getValue().getCorreo() : ""));
        colCorreo.setPrefWidth(180);
        TableColumn<RegistroEmpresa, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : ""));
        colTelefono.setPrefWidth(110);
        tabla().getColumns().setAll(colEmpresa, colActividad, colCorreo, colTelefono);
        anexarColumnaEstado(RegistroEmpresa::getEstado);
        agregarCampo(formulario(), 0, "Nombre", nombre, true);
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
            if (!confirmarContextoGuardar()) {
                return;
            }
            PanelAbm.validarObligatorio(nombre.getText(), "Nombre");
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
            finalizarFormulario();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
