package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroChofer;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioChofer;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class ChoferesPane extends PanelAbm<RegistroChofer> {

    private final RepositorioChofer repo = new RepositorioChofer();
    private final TextField nombre = new TextField();
    private final TextField apellidoP = new TextField();
    private final TextField apellidoM = new TextField();
    private final TextField ci = new TextField();
    private final TextField telefono = new TextField();
    private final TextField licencia = new TextField();
    private final javafx.scene.control.ComboBox<String> estado = PanelAbm.comboEstado();

    public ChoferesPane() {
        super(() -> { });
        TableColumn<RegistroChofer, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombreCompleto()));
        colNombre.setPrefWidth(180);
        TableColumn<RegistroChofer, String> colCi = new TableColumn<>("CI");
        colCi.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCi() != null ? c.getValue().getCi() : ""));
        colCi.setPrefWidth(100);
        TableColumn<RegistroChofer, String> colTel = new TableColumn<>("Teléfono");
        colTel.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : ""));
        colTel.setPrefWidth(110);
        TableColumn<RegistroChofer, String> colLic = new TableColumn<>("Licencia");
        colLic.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getLicencia() != null ? c.getValue().getLicencia() : ""));
        colLic.setPrefWidth(120);
        tabla().getColumns().setAll(colNombre, colCi, colTel, colLic);
        int f = 0;
        agregarCampo(formulario(), f++, "Nombre", nombre);
        agregarCampo(formulario(), f++, "Ap. paterno", apellidoP);
        agregarCampo(formulario(), f++, "Ap. materno", apellidoM);
        agregarCampo(formulario(), f++, "CI", ci);
        agregarCampo(formulario(), f++, "Teléfono", telefono);
        agregarCampo(formulario(), f++, "Licencia", licencia);
        agregarCombo(formulario(), f, "Estado", estado);
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        anexarColumnaEstado(RegistroChofer::getEstado);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    private void rellenar() {
        RegistroChofer r = seleccion();
        if (r == null) {
            return;
        }
        nombre.setText(r.getNombre() != null ? r.getNombre() : "");
        apellidoP.setText(r.getApellidoPaterno() != null ? r.getApellidoPaterno() : "");
        apellidoM.setText(r.getApellidoMaterno() != null ? r.getApellidoMaterno() : "");
        ci.setText(r.getCi() != null ? r.getCi() : "");
        telefono.setText(r.getTelefono() != null ? r.getTelefono() : "");
        licencia.setText(r.getLicencia() != null ? r.getLicencia() : "");
        estado.getSelectionModel().select(r.getEstado());
    }

    private void limpiar() {
        nombre.clear();
        apellidoP.clear();
        apellidoM.clear();
        ci.clear();
        telefono.clear();
        licencia.clear();
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            if (nombre.getText().trim().isEmpty()) {
                mostrarError("El nombre es obligatorio.");
                return;
            }
            if (seleccion() == null) {
                repo.crear(nombre.getText().trim(), apellidoP.getText().trim(), apellidoM.getText().trim(),
                        ci.getText().trim(), telefono.getText().trim(), licencia.getText().trim());
                mostrarExito("Chofer creado.");
            } else {
                repo.actualizar(seleccion().getId(), nombre.getText().trim(), apellidoP.getText().trim(),
                        apellidoM.getText().trim(), ci.getText().trim(), telefono.getText().trim(),
                        licencia.getText().trim(), estado.getValue());
                mostrarExito("Chofer actualizado.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
