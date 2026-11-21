package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroUsuario;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioUsuario;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class UsuariosPane extends PanelAbm<RegistroUsuario> {

    private final RepositorioUsuario repo = new RepositorioUsuario();
    private final UsuarioSesion sesion;
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

    public UsuariosPane(UsuarioSesion sesion) {
        super(() -> { });
        this.sesion = sesion;
        TableColumn<RegistroUsuario, String> colCi = new TableColumn<>("Nro. documento");
        colCi.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCi() != null ? c.getValue().getCi() : ""));
        TableColumn<RegistroUsuario, String> colNombre = new TableColumn<>("Nombres");
        colNombre.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getNombreCompleto()));
        colNombre.setPrefWidth(220);
        TableColumn<RegistroUsuario, String> colCorreo = new TableColumn<>("Correo electrónico");
        colCorreo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colCorreo.setPrefWidth(200);
        TableColumn<RegistroUsuario, String> colTel = new TableColumn<>("Teléfono");
        colTel.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTelefono() != null ? c.getValue().getTelefono() : ""));
        TableColumn<RegistroUsuario, String> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRol()));
        tabla().getColumns().setAll(colCi, colNombre, colCorreo, colTel, colRol);
        anexarColumnaEstado(RegistroUsuario::getEstado);
        int f = 0;
        agregarCampo(formulario(), f++, "Nombre", nombre, true);
        agregarCampo(formulario(), f++, "Ap. paterno", apellidoP);
        agregarCampo(formulario(), f++, "Ap. materno", apellidoM);
        agregarCampo(formulario(), f++, "CI", ci);
        agregarCampo(formulario(), f++, "Teléfono", telefono);
        agregarCampo(formulario(), f++, "Correo", correo, true);
        agregarCampo(formulario(), f++, "Contraseña", clave, true);
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
        apellidoM.setText(r.getApellidoMaterno() != null ? r.getApellidoMaterno() : "");
        ci.setText(r.getCi() != null ? r.getCi() : "");
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
            if (!confirmarContextoGuardar()) {
                return;
            }
            PanelAbm.validarObligatorio(nombre.getText(), "Nombre");
            PanelAbm.validarObligatorio(correo.getText(), "Correo");
            if (seleccion() == null) {
                PanelAbm.validarObligatorio(clave.getText(), "Contraseña");
                repo.crear(nombre.getText().trim(), apellidoP.getText().trim(), apellidoM.getText().trim(),
                        ci.getText().trim(), telefono.getText().trim(), correo.getText().trim(),
                        rol.getValue(), clave.getText());
                mostrarExito("Usuario creado.");
            } else {
                if (seleccion().getId() == sesion.getId()
                        && "INACTIVO".equalsIgnoreCase(estado.getValue())) {
                    mostrarError("No puede inactivar su propio usuario.");
                    return;
                }
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
            finalizarFormulario();
        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }
}
