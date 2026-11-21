package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.datos.RegistroUsuario;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioUsuario;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class UsuariosPane extends PanelAbm<RegistroUsuario> {

    private final RepositorioUsuario repo = new RepositorioUsuario();
    private final TextField nombre = new TextField();
    private final TextField apellidoP = new TextField();
    private final TextField apellidoM = new TextField();
    private final TextField ci = new TextField();
    private final TextField telefono = new TextField();
    private final TextField correo = new TextField();
    private final PasswordField clave = new PasswordField();
    private final ComboBox<String> rol = new ComboBox<>(FXCollections.observableArrayList(
            "usuario", "administrador"));
    private final ComboBox<String> estado = PanelAbm.comboEstado();

    public UsuariosPane() {
        super(() -> { });
        TableColumn<RegistroUsuario, String> col = new TableColumn<>("Correo");
        col.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        TableColumn<RegistroUsuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRol()));
        tabla().getColumns().setAll(col, colRol);
        int f = 0;
        agregarCampo(formulario(), f++, "Nombre", nombre);
        agregarCampo(formulario(), f++, "Ap. paterno", apellidoP);
        agregarCampo(formulario(), f++, "Ap. materno", apellidoM);
        agregarCampo(formulario(), f++, "CI", ci);
        agregarCampo(formulario(), f++, "Teléfono", telefono);
        agregarCampo(formulario(), f++, "Correo", correo);
        agregarCampo(formulario(), f++, "Contraseña", clave);
        agregarCombo(formulario(), f++, "Rol", rol);
        agregarCombo(formulario(), f, "Estado", estado);
        rol.getSelectionModel().selectFirst();
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    private void cargar() {
        setItems(FXCollections.observableArrayList(repo.listarTodos()));
    }

    private void rellenar() {
        RegistroUsuario r = seleccion();
        if (r == null) {
            return;
        }
        nombre.setText(r.getNombre() != null ? r.getNombre() : "");
        apellidoP.setText(r.getApellidoPaterno() != null ? r.getApellidoPaterno() : "");
        telefono.setText(r.getTelefono() != null ? r.getTelefono() : "");
        correo.setText(r.getEmail());
        rol.getSelectionModel().select(r.getRol());
        estado.getSelectionModel().select(r.getEstado());
        clave.clear();
    }

    private void limpiar() {
        nombre.clear();
        apellidoP.clear();
        apellidoM.clear();
        ci.clear();
        telefono.clear();
        correo.clear();
        clave.clear();
        rol.getSelectionModel().selectFirst();
        estado.getSelectionModel().select("ACTIVO");
    }

    private void guardar() {
        try {
            if (correo.getText().trim().isEmpty()) {
                mostrarError("El correo es obligatorio.");
                return;
            }
            if (seleccion() == null) {
                repo.crear(nombre.getText().trim(), apellidoP.getText().trim(), apellidoM.getText().trim(),
                        ci.getText().trim(), telefono.getText().trim(), correo.getText().trim(),
                        rol.getValue(), clave.getText());
                mostrarExito("Usuario creado.");
            } else {
                String nuevaClave = clave.getText();
                repo.actualizar(seleccion().getId(), nombre.getText().trim(), apellidoP.getText().trim(),
                        apellidoM.getText().trim(), ci.getText().trim(), telefono.getText().trim(),
                        correo.getText().trim(), rol.getValue(), estado.getValue(),
                        nuevaClave.isEmpty() ? null : nuevaClave);
                mostrarExito("Usuario actualizado.");
            }
            cargar();
            limpiar();
            tabla().getSelectionModel().clearSelection();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
