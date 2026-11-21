package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.balanza.ConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.balanza.ServicioBalanza;
import bo.firmadigital.jacobitus.pesaje.datos.FilaPesaje;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroCatalogo;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroChofer;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroProveedor;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroVehiculo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioCatalogo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioChofer;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioProveedor;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioVehiculo;
import bo.firmadigital.jacobitus.pesaje.modelo.CalculoPesaje;
import bo.firmadigital.jacobitus.pesaje.modelo.EstadoBalanza;
import bo.firmadigital.jacobitus.pesaje.modelo.Pesaje;
import bo.firmadigital.jacobitus.pesaje.ui.comun.ComboOpcion;
import bo.firmadigital.jacobitus.pesaje.ui.comun.Cerrable;

/**
 * Pantalla operativa de pesaje.
 *
 * Replica el flujo del sistema original: captura de la guia y el lote,
 * eleccion de vehiculo, chofer, proveedor, producto, origen y destino,
 * peso bruto y tara con el neto calculado, y una grilla con los pesajes
 * registrados. La diferencia relevante es que el peso puede venir de la
 * bascula conectada en vivo, no de un valor simulado.
 */
public final class PesajePane extends BorderPane implements Cerrable {

    private final UsuarioSesion usuario;
    private final RepositorioPesaje repo = new RepositorioPesaje();
    private final RepositorioVehiculo repoVehiculo = new RepositorioVehiculo();
    private final RepositorioChofer repoChofer = new RepositorioChofer();
    private final RepositorioProveedor repoProveedor = new RepositorioProveedor();

    private final TextField guia = new TextField();
    private final TextField lote = new TextField();
    private final TextField bruto = new TextField();
    private final TextField tara = new TextField();
    private final TextField neto = new TextField();
    private final TextField costo = new TextField("0");
    private final TextField flete = new TextField("0");

    private final Label pesoEnVivo = new Label("—");
    private final Label mensaje = new Label();
    private final Button botonConectar = new Button("Conectar balanza");
    private final Button botonUsarPeso = new Button("Usar peso en vivo");

    private final TableView<FilaPesaje> grilla = new TableView<>();
    private ComboBox<ComboOpcion> comboVehiculo;
    private ComboBox<ComboOpcion> comboChofer;
    private ComboBox<ComboOpcion> comboProveedor;
    private ComboBox<ComboOpcion> comboProducto;
    private ComboBox<ComboOpcion> comboOrigen;
    private ComboBox<ComboOpcion> comboDestino;

    private ServicioBalanza servicioBalanza;

    public PesajePane(UsuarioSesion usuario) {
        this.usuario = usuario;
        setPadding(new Insets(8));
        setCenter(construirCentro());
        setRight(construirFormulario());
        setBottom(construirGrilla());
        recargarGrilla();
    }

    // ───────────────────────────────────────────────────────────────────
    // Construccion de la pantalla
    // ───────────────────────────────────────────────────────────────────

    private VBox construirCentro() {
        pesoEnVivo.getStyleClass().add("valor-peso");
        pesoEnVivo.setMinWidth(220);
        pesoEnVivo.setAlignment(Pos.CENTER);

        botonConectar.getStyleClass().add("boton-secundario");
        botonUsarPeso.getStyleClass().add("boton-secundario");
        botonUsarPeso.setDisable(true);

        botonConectar.setOnAction(e -> alternarBalanza());
        botonUsarPeso.setOnAction(e -> {
            String valor = pesoEnVivo.getText();
            try {
                int peso = Integer.parseInt(valor.trim());
                bruto.setText(String.valueOf(peso));
                recalcularNeto();
            } catch (NumberFormatException ex) {
                mensaje.setText("Todavía no hay una lectura de peso válida.");
            }
        });

        mensaje.getStyleClass().add("subtitulo");

        VBox panel = new VBox(8, pesoEnVivo, new HBox(8, botonConectar, botonUsarPeso), mensaje);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("tarjeta");
        panel.setPrefWidth(300);
        return panel;
    }

    private VBox construirFormulario() {
        cargarCombos();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        int f = 0;
        agregar(grid, f++, "Guía", guia);
        agregar(grid, f++, "Lote", lote);
        agregar(grid, f++, "Vehículo", comboVehiculo);
        agregar(grid, f++, "Chofer", comboChofer);
        agregar(grid, f++, "Proveedor", comboProveedor);
        agregar(grid, f++, "Producto", comboProducto);
        agregar(grid, f++, "Origen", comboOrigen);
        agregar(grid, f++, "Destino", comboDestino);
        agregar(grid, f++, "Peso bruto (kg)", bruto);
        agregar(grid, f++, "Tara (kg)", tara);
        agregar(grid, f++, "Peso neto (kg)", neto);
        agregar(grid, f++, "Costo Bs", costo);
        agregar(grid, f, "Flete pendiente Bs", flete);
        neto.setEditable(false);

        bruto.textProperty().addListener((o, a, b) -> recalcularNeto());
        tara.textProperty().addListener((o, a, b) -> recalcularNeto());

        Button nuevoVehiculo = new Button("+ Vehículo");
        nuevoVehiculo.getStyleClass().add("boton-secundario");
        nuevoVehiculo.setOnAction(e -> {
            if (crearVehiculoRapido()) {
                recargarCombos();
                recargarGrilla();
            }
        });
        Button nuevoChofer = new Button("+ Chofer");
        nuevoChofer.getStyleClass().add("boton-secundario");
        nuevoChofer.setOnAction(e -> {
            if (crearChoferRapido()) {
                recargarCombos();
                recargarGrilla();
            }
        });
        Button nuevoProveedor = new Button("+ Proveedor");
        nuevoProveedor.getStyleClass().add("boton-secundario");
        nuevoProveedor.setOnAction(e -> {
            if (crearProveedorRapido()) {
                recargarCombos();
                recargarGrilla();
            }
        });

        Button guardar = new Button("Guardar pesaje");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());

        Button usarTara = new Button("Usar tara del vehículo");
        usarTara.getStyleClass().add("boton-secundario");
        usarTara.setOnAction(e -> aplicarTaraVehiculo());

        HBox rapidos = new HBox(8, nuevoVehiculo, nuevoChofer, nuevoProveedor);
        HBox acciones = new HBox(8, usarTara, guardar);

        VBox panel = new VBox(8, grid, rapidos, acciones);
        panel.setPadding(new Insets(12));
        panel.getStyleClass().add("tarjeta");
        panel.setPrefWidth(380);
        return panel;
    }

    private TableView<FilaPesaje> construirGrilla() {
        grilla.getStyleClass().add("tabla-modulo");
        grilla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<FilaPesaje, String> colGuia = new TableColumn<>("Guía");
        colGuia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuia()));
        TableColumn<FilaPesaje, String> colVehiculo = new TableColumn<>("Vehículo");
        colVehiculo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPlacaVehiculo()));
        TableColumn<FilaPesaje, String> colChofer = new TableColumn<>("Chofer");
        colChofer.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreChofer()));
        TableColumn<FilaPesaje, String> colProveedor = new TableColumn<>("Proveedor");
        colProveedor.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreProveedor()));
        TableColumn<FilaPesaje, String> colBruto = new TableColumn<>("Bruto");
        colBruto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPesoBruto() != null ? String.valueOf(c.getValue().getPesoBruto()) : ""));
        TableColumn<FilaPesaje, String> colTara = new TableColumn<>("Tara");
        colTara.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTara() != null ? String.valueOf(c.getValue().getTara()) : ""));
        TableColumn<FilaPesaje, String> colNeto = new TableColumn<>("Neto");
        colNeto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPesoNeto() != null ? String.valueOf(c.getValue().getPesoNeto()) : ""));
        TableColumn<FilaPesaje, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstadoBalanza()));
        TableColumn<FilaPesaje, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaCreacion()));
        grilla.getColumns().setAll(colGuia, colVehiculo, colChofer, colProveedor, colBruto,
                colTara, colNeto, colEstado, colFecha);
        return grilla;
    }

    // ───────────────────────────────────────────────────────────────────
    // Combos y grilla
    // ───────────────────────────────────────────────────────────────────

    private void cargarCombos() {
        comboVehiculo = ComboOpcion.crear(mapearVehiculos());
        comboChofer = ComboOpcion.crear(mapearChoferes());
        comboProveedor = ComboOpcion.crear(mapearProveedores());
        comboProducto = ComboOpcion.crear(mapearCatalogo("productos"));
        comboOrigen = ComboOpcion.crear(mapearCatalogo("origenes"));
        comboDestino = ComboOpcion.crear(mapearCatalogo("destinos"));
    }

    private void recargarCombos() {
        List<ComboOpcion> vehiculos = mapearVehiculos();
        List<ComboOpcion> choferes = mapearChoferes();
        List<ComboOpcion> proveedores = mapearProveedores();
        comboVehiculo.setItems(FXCollections.observableArrayList(vehiculos));
        comboChofer.setItems(FXCollections.observableArrayList(choferes));
        comboProveedor.setItems(FXCollections.observableArrayList(proveedores));
        if (!vehiculos.isEmpty()) {
            comboVehiculo.getSelectionModel().selectFirst();
        }
        if (!choferes.isEmpty()) {
            comboChofer.getSelectionModel().selectFirst();
        }
        if (!proveedores.isEmpty()) {
            comboProveedor.getSelectionModel().selectFirst();
        }
    }

    private void recargarGrilla() {
        grilla.setItems(FXCollections.observableArrayList(repo.listarListado(50)));
    }

    private List<ComboOpcion> mapearVehiculos() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroVehiculo v : repoVehiculo.listarActivos()) {
            lista.add(new ComboOpcion(v.getId(), v.etiqueta()));
        }
        return lista;
    }

    private List<ComboOpcion> mapearChoferes() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroChofer c : repoChofer.listarActivos()) {
            lista.add(new ComboOpcion(c.getId(), c.nombreCompleto()));
        }
        return lista;
    }

    private List<ComboOpcion> mapearProveedores() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroProveedor p : repoProveedor.listarActivos()) {
            lista.add(new ComboOpcion(p.getId(), p.etiqueta()));
        }
        return lista;
    }

    private List<ComboOpcion> mapearCatalogo(String tabla) {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroCatalogo r : new RepositorioCatalogo(tabla).listarActivos()) {
            lista.add(new ComboOpcion(r.getId(), r.getNombre()));
        }
        return lista;
    }

    // ───────────────────────────────────────────────────────────────────
    // Balanza en vivo
    // ───────────────────────────────────────────────────────────────────

    private void alternarBalanza() {
        if (servicioBalanza != null && servicioBalanza.estaConectado()) {
            servicioBalanza.desconectar();
            botonConectar.setText("Conectar balanza");
            botonUsarPeso.setDisable(true);
            return;
        }
        ConfigBalanza cfg = new RepositorioConfigBalanza().obtenerActiva();
        servicioBalanza = new ServicioBalanza(cfg);
        servicioBalanza.agregarObservador(new ServicioBalanza.Observador() {
            @Override
            public void pesoActualizado(int peso) {
                Platform.runLater(() -> {
                    pesoEnVivo.setText(String.valueOf(peso));
                    botonUsarPeso.setDisable(false);
                });
            }

            @Override
            public void estadoCambiado(boolean conectado, String texto) {
                Platform.runLater(() -> {
                    mensaje.setText(texto);
                    botonConectar.setText(conectado ? "Desconectar balanza" : "Conectar balanza");
                    if (!conectado) {
                        botonUsarPeso.setDisable(true);
                    }
                });
            }
        });
        if (!servicioBalanza.conectar()) {
            mensaje.setText(servicioBalanza.ultimoError());
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Acciones
    // ───────────────────────────────────────────────────────────────────

    private void aplicarTaraVehiculo() {
        Integer id = ComboOpcion.idSeleccionado(comboVehiculo);
        if (id == null) {
            mensaje.setText("Seleccione un vehículo.");
            return;
        }
        for (RegistroVehiculo v : repoVehiculo.listarActivos()) {
            if (v.getId() == id) {
                tara.setText(String.valueOf(v.getTara()));
                recalcularNeto();
                return;
            }
        }
    }

    private void recalcularNeto() {
        try {
            int b = Integer.parseInt(bruto.getText().trim());
            int t = tara.getText().trim().isEmpty() ? 0 : Integer.parseInt(tara.getText().trim());
            neto.setText(String.valueOf(CalculoPesaje.neto(b, t)));
        } catch (Exception ignored) {
            neto.clear();
        }
    }

    private void guardar() {
        try {
            Integer vehiculoId = ComboOpcion.idSeleccionado(comboVehiculo);
            if (vehiculoId == null) {
                mensaje.setText("Seleccione un vehículo.");
                return;
            }
            int b = Integer.parseInt(bruto.getText().trim());
            int t = tara.getText().trim().isEmpty() ? 0 : Integer.parseInt(tara.getText().trim());
            CalculoPesaje.validar(b, t);
            Pesaje p = new Pesaje();
            p.setGuia(guia.getText().trim());
            p.setLote(lote.getText().trim());
            p.setVehiculoId(vehiculoId);
            p.setChoferId(ComboOpcion.idSeleccionado(comboChofer));
            p.setProveedorId(ComboOpcion.idSeleccionado(comboProveedor));
            p.setProductoId(ComboOpcion.idSeleccionado(comboProducto));
            p.setOrigenId(ComboOpcion.idSeleccionado(comboOrigen));
            p.setDestinoId(ComboOpcion.idSeleccionado(comboDestino));
            p.setPesoBruto(b);
            p.setTara(t);
            p.setCostoBs(enteroOCero(costo.getText()));
            p.setFleteBsPendiente(enteroOCero(flete.getText()));
            p.setPesadorId(usuario.getPersonaId());
            p.setEstadoBalanza(EstadoBalanza.FINALIZADO);
            repo.guardar(p, usuario.getId());
            mensaje.setText("Pesaje guardado correctamente.");
            guia.clear();
            lote.clear();
            bruto.clear();
            tara.clear();
            neto.clear();
            recargarGrilla();
        } catch (Exception e) {
            mensaje.setText(e.getMessage());
        }
    }

    private static int enteroOCero(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(texto.trim());
    }

    // ───────────────────────────────────────────────────────────────────
    // Alta rapida desde la propia pantalla
    // ───────────────────────────────────────────────────────────────────

    private boolean crearVehiculoRapido() {
        DialogoRapido dialogo = new DialogoRapido("Nuevo vehículo",
                new String[] {"Placa", "Tipo", "Tara (kg)"}, new String[] {"", "", "0"});
        if (!dialogo.mostrar()) {
            return false;
        }
        String[] valores = dialogo.valores();
        repoVehiculo.crear(valores[0].trim(), valores[1].trim(), enteroOCero(valores[2]));
        mensaje.setText("Vehículo creado.");
        return true;
    }

    private boolean crearChoferRapido() {
        DialogoRapido dialogo = new DialogoRapido("Nuevo chofer",
                new String[] {"Nombre", "Apellido paterno", "Licencia", "Teléfono"},
                new String[] {"", "", "", ""});
        if (!dialogo.mostrar()) {
            return false;
        }
        String[] valores = dialogo.valores();
        repoChofer.crear(valores[0].trim(), valores[1].trim(), null, null, valores[3].trim(),
                valores[2].trim());
        mensaje.setText("Chofer creado.");
        return true;
    }

    private boolean crearProveedorRapido() {
        DialogoRapido dialogo = new DialogoRapido("Nuevo proveedor",
                new String[] {"Nombre", "Apellido paterno", "Empresa", "Teléfono"},
                new String[] {"", "", "", ""});
        if (!dialogo.mostrar()) {
            return false;
        }
        String[] valores = dialogo.valores();
        repoProveedor.crear(valores[0].trim(), valores[1].trim(), null, null, valores[3].trim(),
                valores[2].trim(), null);
        mensaje.setText("Proveedor creado.");
        return true;
    }

    /**
     * Cierra la conexion con la balanza y detiene el hilo de lectura.
     * Lo invoca el shell al navegar a otro modulo.
     */
    @Override
    public void cerrar() {
        if (servicioBalanza != null) {
            servicioBalanza.close();
            servicioBalanza = null;
        }
    }

    private void agregar(GridPane grid, int fila, String etiqueta, javafx.scene.Node campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }
}
