package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import bo.firmadigital.jacobitus.pesaje.datos.LogoEmpresa;
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
    private final ImageView vistaLogo = new ImageView();
    private final Button botonCargarLogo = new Button("Cargar logo");
    private final Button botonQuitarLogo = new Button("Quitar");

    /** Logo de la pantalla, como data URL. */
    private String logo;

    /** Aviso para refrescar el logo de la empresa en la cabecera del shell. */
    private final Runnable alGuardarLogo;

    public EmpresasPane() {
        this(null);
    }

    /**
     * Crea la pantalla de empresas.
     *
     * @param alGuardarLogo se avisa despues de guardar, para que la
     *                      cabecera del shell refresque el logo; puede ser null
     */
    public EmpresasPane(Runnable alGuardarLogo) {
        super(() -> { });
        this.alGuardarLogo = alGuardarLogo;
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
        agregarCampo(formulario(), 5, "Logo", filaLogo(), false);
        alSeleccionar(this::rellenar);
        alNuevo(this::limpiar);
        alGuardar(this::guardar);
        cargar();
    }

    /**
     * Fila del formulario con la vista previa del logo y sus botones.
     *
     * @return nodo con el preview y las acciones del logo
     */
    private HBox filaLogo() {
        vistaLogo.setFitHeight(56);
        vistaLogo.setFitWidth(56);
        vistaLogo.setPreserveRatio(true);
        botonCargarLogo.getStyleClass().add("boton-secundario");
        botonQuitarLogo.getStyleClass().add("boton-secundario");
        botonCargarLogo.setOnAction(e -> cargarLogo());
        botonQuitarLogo.setOnAction(e -> quitarLogo());
        HBox fila = new HBox(8, vistaLogo, botonCargarLogo, botonQuitarLogo);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
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
        logo = r.getLogo();
        mostrarLogo();
    }

    private void limpiar() {
        nombre.clear();
        actividad.clear();
        correo.clear();
        telefono.clear();
        estado.getSelectionModel().select("ACTIVO");
        quitarLogo();
    }

    /**
     * Abre el selector de archivos para elegir el logo de la empresa.
     *
     * Si el usuario cancela no se cambia nada; si el archivo no sirve se
     * avisa y se conserva el logo anterior.
     */
    private void cargarLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar logo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imagenes PNG o JPEG", "*.png", "*.jpg", "*.jpeg"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Todos los archivos", "*"));
        File archivo = chooser.showOpenDialog(ventanaPropietaria());
        if (archivo == null) {
            return;
        }
        try {
            String dataUrl = LogoEmpresa.dataUrl(archivo.toPath());
            byte[] bytes = LogoEmpresa.decodificar(dataUrl);
            Image imagen = new Image(new ByteArrayInputStream(bytes));
            logo = dataUrl;
            vistaLogo.setImage(imagen);
        } catch (IOException | RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    /** Quita el logo de la pantalla y la vista previa. */
    private void quitarLogo() {
        logo = null;
        vistaLogo.setImage(null);
    }

    /**
     * Ventana que es propietaria del selector de archivos.
     *
     * @return la ventana de la pantalla, o null si todavia no hay escena
     */
    private Window ventanaPropietaria() {
        Scene escena = botonCargarLogo.getScene();
        return escena == null ? null : escena.getWindow();
    }

    /** Muestra en la vista previa el logo que tiene la pantalla. */
    private void mostrarLogo() {
        byte[] bytes = LogoEmpresa.decodificar(logo);
        if (bytes.length == 0) {
            vistaLogo.setImage(null);
            return;
        }
        try {
            vistaLogo.setImage(new Image(new ByteArrayInputStream(bytes)));
        } catch (RuntimeException e) {
            vistaLogo.setImage(null);
        }
    }

    private void guardar() {
        try {
            if (!confirmarContextoGuardar()) {
                return;
            }
            PanelAbm.validarObligatorio(nombre.getText(), "Nombre");
            if (seleccion() == null) {
                repo.crear(nombre.getText().trim(), actividad.getText().trim(),
                        correo.getText().trim(), telefono.getText().trim(), logo);
                mostrarExito("Empresa creada.");
            } else {
                repo.actualizar(seleccion().getId(), nombre.getText().trim(), actividad.getText().trim(),
                        correo.getText().trim(), telefono.getText().trim(), estado.getValue(), logo);
                mostrarExito("Empresa actualizada.");
            }
            if (alGuardarLogo != null) {
                alGuardarLogo.run();
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
