package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroVehiculo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioVehiculo;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class VehiculosPane extends PanelAbm<RegistroVehiculo> {

    private final RepositorioVehiculo repo = new RepositorioVehiculo();
    private final TextField placa = new TextField();
    private final TextField tipo = new TextField();
    private final TextField tara = new TextField("0");
    private final javafx.scene.control.ComboBox<String> estado = PanelAbm.comboEstado();

    public VehiculosPane() {
        super(() -> { });
        TableColumn<RegistroVehiculo, String> col = new TableColumn<>("Placa");
        col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlaca()));
        TableColumn<RegistroVehiculo, String> colTara = new TableColumn<>("Tara");
        colTara.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getTara())));
        tabla().getColumns().setAll(col, colTara);
        agregarCampo(formulario(), 0, "Placa", placa);
        agregarCampo(formulario(), 1, "Tipo", tipo);
        agregarCampo(formulario(), 2, "Tara (kg)", tara);
        agregarCombo(formulario(), 3, "Estado", estado);
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    private void rellenar() {
        RegistroVehiculo r = seleccion();
        if (r == null) {
            return;
        }
        placa.setText(r.getPlaca());
        tipo.setText(r.getTipo() != null ? r.getTipo() : "");
        tara.setText(String.valueOf(r.getTara()));
        estado.getSelectionModel().select(r.getEstado());
    }

    private void limpiar() {
        placa.clear();
        tipo.clear();
        tara.setText("0");
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            int taraKg = PanelAbm.parsearEntero(tara.getText(), "Tara");
            if (placa.getText().trim().isEmpty()) {
                mostrarError("La placa es obligatoria.");
                return;
            }
            if (seleccion() == null) {
                repo.crear(placa.getText().trim(), tipo.getText().trim(), taraKg);
                mostrarExito("Vehículo creado.");
            } else {
                repo.actualizar(seleccion().getId(), placa.getText().trim(), tipo.getText().trim(),
                        taraKg, estado.getValue());
                mostrarExito("Vehículo actualizado.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
