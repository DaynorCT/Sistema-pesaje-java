package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
import bo.firmadigital.jacobitus.pesaje.sync.ServicioSync;
import bo.firmadigital.jacobitus.pesaje.ui.comun.Cerrable;
import bo.firmadigital.jacobitus.pesaje.ui.comun.ComboOpcion;

/**
 * Pantalla operativa de pesaje (layout Lazarus / captura de referencia).
 *
 * Izquierda: display de peso en vivo + captura bruto/tara.
 * Derecha: datos del pesaje en tres columnas.
 * Abajo: grilla de pesajes recientes.
 */
public final class PesajePane extends BorderPane implements Cerrable {

    private final UsuarioSesion usuario;
    private final ServicioSync servicioSync;
    private final RepositorioPesaje repo = new RepositorioPesaje();
    private final RepositorioVehiculo repoVehiculo = new RepositorioVehiculo();
    private final RepositorioChofer repoChofer = new RepositorioChofer();
    private final RepositorioProveedor repoProveedor = new RepositorioProveedor();

    private final Label pesoEnVivo = new Label("0 kg");
    private final Label lblValBruto = new Label("0");
    private final Label lblValTara = new Label("0");
    private final Label lblValNeto = new Label("0");
    private final Label mensaje = new Label();

    private final Button botonConectar = new Button("Conectar");
    private final Button botonCapPeso = new Button("Cap. peso");
    private final Button botonCapTara = new Button("Cap. tara");

    private final TextField guia = new TextField();
    private final TextField lote = new TextField();
    private final TextField licencia = new TextField();
    private final TextField tipoVehiculo = new TextField();
    private final TextField tara = new TextField("0");
    private final TextField costo = new TextField("0");
    private final TextField flete = new TextField("0");
    private final CheckBox taraManual = new CheckBox("Tara manual");

    private final TableView<FilaPesaje> grilla = new TableView<>();
    private ComboBox<ComboOpcion> comboVehiculo;
    private ComboBox<ComboOpcion> comboChofer;
    private ComboBox<ComboOpcion> comboProveedor;
    private ComboBox<ComboOpcion> comboProducto;
    private ComboBox<ComboOpcion> comboOrigen;
    private ComboBox<ComboOpcion> comboDestino;

    private int pesoBruto = 0;
    private int pesoTara = 0;
    private ServicioBalanza servicioBalanza;

    public PesajePane(UsuarioSesion usuario, ServicioSync servicioSync) {
        this.usuario = usuario;
        this.servicioSync = servicioSync;
        getStyleClass().add("panel-pesaje");
        setPadding(new Insets(12));

        cargarCombos();
        VBox medio = new VBox(12, construirMedio());
        setTop(medio);
        setCenter(construirGrillaCard());
        recargarGrilla();
        actualizarResumenPesos();
    }

    private HBox construirMedio() {
        VBox izq = construirRegistroPeso();
        VBox der = construirDatosPesaje();
        HBox.setHgrow(der, Priority.ALWAYS);
        izq.setPrefWidth(320);
        izq.setMinWidth(280);
        HBox medio = new HBox(16, izq, der);
        medio.getStyleClass().add("panel-pesaje-medio");
        return medio;
    }

    private VBox construirRegistroPeso() {
        Label titulo = new Label("Registro de peso");
        titulo.getStyleClass().add("titulo-tarjeta");

        Region sep1 = new Region();
        sep1.getStyleClass().add("separador-horizontal");

        pesoEnVivo.getStyleClass().add("valor-peso");
        pesoEnVivo.setMaxWidth(Double.MAX_VALUE);
        pesoEnVivo.setAlignment(Pos.CENTER);
        StackPane display = new StackPane(pesoEnVivo);
        display.getStyleClass().add("display-peso");
        display.setPrefHeight(110);

        Region sep2 = new Region();
        sep2.getStyleClass().add("separador-horizontal");

        botonConectar.getStyleClass().add("boton-secundario");
        botonCapPeso.getStyleClass().add("boton-primario");
        botonCapTara.getStyleClass().add("boton-info");
        botonCapPeso.setDisable(true);
        botonCapTara.setDisable(true);
        botonConectar.setOnAction(e -> alternarBalanza());
        botonCapPeso.setOnAction(e -> capturarPeso());
        botonCapTara.setOnAction(e -> capturarTara());

        VBox colConexion = new VBox(2, botonConectar, etiquetaMini("Conexión"));
        colConexion.setAlignment(Pos.CENTER);
        HBox acciones = new HBox(8, colConexion, botonCapPeso, botonCapTara);
        acciones.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(botonCapTara, Priority.ALWAYS);
        botonCapPeso.setMaxWidth(Double.MAX_VALUE);
        botonCapTara.setMaxWidth(Double.MAX_VALUE);

        HBox resumen = new HBox(10,
                cajaResumen("Peso Bruto", lblValBruto),
                cajaResumen("Peso tara", lblValTara),
                cajaResumen("Peso Neto", lblValNeto));
        HBox.setHgrow(resumen.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(resumen.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(resumen.getChildren().get(2), Priority.ALWAYS);

        mensaje.getStyleClass().add("subtitulo");
        mensaje.setWrapText(true);

        VBox card = new VBox(10, titulo, sep1, display, sep2, acciones, resumen, mensaje);
        card.getStyleClass().add("tarjeta");
        return card;
    }

    private VBox cajaResumen(String titulo, Label valor) {
        Label t = etiquetaMini(titulo);
        valor.getStyleClass().add("valor-caja-peso");
        valor.setMaxWidth(Double.MAX_VALUE);
        valor.setAlignment(Pos.CENTER);
        VBox caja = new VBox(4, t, valor);
        caja.getStyleClass().add("caja-peso-resumen");
        caja.setAlignment(Pos.CENTER);
        caja.setMaxWidth(Double.MAX_VALUE);
        return caja;
    }

    private static Label etiquetaMini(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("etiqueta-mini");
        return l;
    }

    private VBox construirDatosPesaje() {
        Label titulo = new Label("Datos del Pesaje");
        titulo.getStyleClass().add("titulo-tarjeta");

        Region sep1 = new Region();
        sep1.getStyleClass().add("separador-horizontal");

        licencia.setEditable(false);
        tipoVehiculo.setEditable(false);
        licencia.setPromptText("—");
        tipoVehiculo.setPromptText("—");

        Button plusChofer = botonMas(() -> {
            if (crearChoferRapido()) {
                recargarCombos();
            }
        });
        Button plusVehiculo = botonMas(() -> {
            if (crearVehiculoRapido()) {
                recargarCombos();
            }
        });
        Button plusProveedor = botonMas(() -> {
            if (crearProveedorRapido()) {
                recargarCombos();
            }
        });

        comboChofer.valueProperty().addListener((o, a, b) -> alCambiarChofer());
        comboVehiculo.valueProperty().addListener((o, a, b) -> alCambiarVehiculo());
        taraManual.selectedProperty().addListener((o, a, b) -> {
            tara.setEditable(b);
            if (!b) {
                alCambiarVehiculo();
            }
        });
        tara.setEditable(false);
        tara.textProperty().addListener((o, a, b) -> {
            if (taraManual.isSelected()) {
                try {
                    pesoTara = Integer.parseInt(b.trim());
                } catch (NumberFormatException ex) {
                    pesoTara = 0;
                }
                actualizarResumenPesos();
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        ColumnConstraints c2 = new ColumnConstraints();
        ColumnConstraints c3 = new ColumnConstraints();
        c1.setPercentWidth(33.3);
        c2.setPercentWidth(33.3);
        c3.setPercentWidth(33.4);
        c1.setHgrow(Priority.ALWAYS);
        c2.setHgrow(Priority.ALWAYS);
        c3.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2, c3);

        int f = 0;
        grid.add(campo("Guía", guia), 0, f);
        grid.add(campo("Lote", lote), 1, f);
        f++;
        grid.add(campoConBoton("Chofer", comboChofer, plusChofer), 0, f);
        grid.add(campoConBoton("Placa *", comboVehiculo, plusVehiculo), 1, f);
        grid.add(campo("Licencia", licencia), 2, f);
        f++;
        grid.add(campo("Tipo vehículo", tipoVehiculo), 0, f);
        grid.add(campoConBoton("Proveedor", comboProveedor, plusProveedor), 1, f);
        grid.add(campo("Producto", comboProducto), 2, f);
        f++;
        grid.add(campo("Origen", comboOrigen), 0, f);
        grid.add(campo("Destino", comboDestino), 1, f);
        grid.add(campo("Costo (Bs)", costo), 2, f);
        f++;
        grid.add(campo("Flete pend. (Bs)", flete), 0, f);
        VBox taraBox = campo("Tara (kg)", tara);
        taraBox.getChildren().add(0, taraManual);
        grid.add(taraBox, 1, f);

        Region sep2 = new Region();
        sep2.getStyleClass().add("separador-horizontal");

        Button guardar = new Button("Registrar Pesaje");
        guardar.getStyleClass().add("boton-primario");
        guardar.setPrefWidth(170);
        guardar.setOnAction(e -> guardar());
        HBox acciones = new HBox(guardar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(10, titulo, sep1, grid, sep2, acciones);
        card.getStyleClass().add("tarjeta");
        VBox.setVgrow(grid, Priority.ALWAYS);
        return card;
    }

    private static Button botonMas(Runnable accion) {
        Button b = new Button("+");
        b.getStyleClass().add("boton-mas");
        b.setOnAction(e -> accion.run());
        return b;
    }

    private static VBox campo(String etiqueta, javafx.scene.Node control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("etiqueta-mini");
        if (control instanceof Region) {
            ((Region) control).setMaxWidth(Double.MAX_VALUE);
        }
        VBox box = new VBox(4, l, control);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static VBox campoConBoton(String etiqueta, ComboBox<?> combo, Button plus) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("etiqueta-mini");
        combo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(combo, Priority.ALWAYS);
        HBox fila = new HBox(4, combo, plus);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(4, l, fila);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private VBox construirGrillaCard() {
        grilla.getStyleClass().add("tabla-modulo");
        grilla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<FilaPesaje, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        TableColumn<FilaPesaje, String> colChofer = new TableColumn<>("Chofer");
        colChofer.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getNombreChofer())));
        TableColumn<FilaPesaje, String> colPlaca = new TableColumn<>("Placa");
        colPlaca.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getPlacaVehiculo())));
        TableColumn<FilaPesaje, String> colProveedor = new TableColumn<>("Proveedor");
        colProveedor.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getNombreProveedor())));
        TableColumn<FilaPesaje, String> colProducto = new TableColumn<>("Producto");
        colProducto.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getNombreProducto())));
        TableColumn<FilaPesaje, String> colOrigen = new TableColumn<>("Origen");
        colOrigen.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getOrigen())));
        TableColumn<FilaPesaje, String> colDestino = new TableColumn<>("Destino");
        colDestino.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getDestino())));
        TableColumn<FilaPesaje, String> colBruto = new TableColumn<>("P.Bruto");
        colBruto.setCellValueFactory(c -> new SimpleStringProperty(entero(c.getValue().getPesoBruto())));
        TableColumn<FilaPesaje, String> colTara = new TableColumn<>("P.Tara");
        colTara.setCellValueFactory(c -> new SimpleStringProperty(entero(c.getValue().getTara())));
        TableColumn<FilaPesaje, String> colNeto = new TableColumn<>("P.Neto");
        colNeto.setCellValueFactory(c -> new SimpleStringProperty(entero(c.getValue().getPesoNeto())));
        TableColumn<FilaPesaje, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getEstadoBalanza())));
        TableColumn<FilaPesaje, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(texto(c.getValue().getFechaCreacion())));
        grilla.getColumns().setAll(colId, colChofer, colPlaca, colProveedor, colProducto,
                colOrigen, colDestino, colBruto, colTara, colNeto, colEstado, colFecha);

        VBox card = new VBox(grilla);
        card.getStyleClass().addAll("tarjeta", "tarjeta-sin-padding");
        VBox.setVgrow(grilla, Priority.ALWAYS);
        BorderPane.setMargin(card, new Insets(12, 0, 0, 0));
        return card;
    }

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
            lista.add(new ComboOpcion(v.getId(), v.getPlaca()));
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

    private void alCambiarChofer() {
        Integer id = ComboOpcion.idSeleccionado(comboChofer);
        if (id == null) {
            licencia.clear();
            return;
        }
        for (RegistroChofer c : repoChofer.listarActivos()) {
            if (c.getId() == id) {
                licencia.setText(c.getLicencia() != null ? c.getLicencia() : "");
                return;
            }
        }
    }

    private void alCambiarVehiculo() {
        Integer id = ComboOpcion.idSeleccionado(comboVehiculo);
        if (id == null) {
            tipoVehiculo.clear();
            return;
        }
        for (RegistroVehiculo v : repoVehiculo.listarActivos()) {
            if (v.getId() == id) {
                tipoVehiculo.setText(v.getTipo() != null ? v.getTipo() : "");
                if (!taraManual.isSelected()) {
                    pesoTara = v.getTara();
                    tara.setText(String.valueOf(pesoTara));
                    actualizarResumenPesos();
                }
                return;
            }
        }
    }

    private void alternarBalanza() {
        if (servicioBalanza != null && servicioBalanza.estaConectado()) {
            servicioBalanza.desconectar();
            botonConectar.setText("Conectar");
            botonCapPeso.setDisable(true);
            botonCapTara.setDisable(true);
            return;
        }
        ConfigBalanza cfg = new RepositorioConfigBalanza().obtenerActiva();
        servicioBalanza = new ServicioBalanza(cfg);
        servicioBalanza.agregarObservador(new ServicioBalanza.Observador() {
            @Override
            public void pesoActualizado(int peso) {
                Platform.runLater(() -> {
                    pesoEnVivo.setText(peso + " kg");
                    botonCapPeso.setDisable(false);
                    botonCapTara.setDisable(false);
                });
            }

            @Override
            public void estadoCambiado(boolean conectado, String texto) {
                Platform.runLater(() -> {
                    mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
                    mensaje.setText(texto);
                    botonConectar.setText(conectado ? "Desconectar" : "Conectar");
                    if (!conectado) {
                        botonCapPeso.setDisable(true);
                        botonCapTara.setDisable(true);
                    }
                });
            }
        });
        if (!servicioBalanza.conectar()) {
            mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
            mensaje.getStyleClass().add("mensaje-error");
            mensaje.setText(servicioBalanza.ultimoError());
        }
    }

    private void capturarPeso() {
        Integer lectura = leerPesoDisplay();
        if (lectura == null) {
            mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
            mensaje.getStyleClass().add("mensaje-error");
            mensaje.setText("Todavía no hay una lectura de peso válida.");
            return;
        }
        pesoBruto = lectura;
        actualizarResumenPesos();
    }

    private void capturarTara() {
        Integer lectura = leerPesoDisplay();
        if (lectura == null) {
            mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
            mensaje.getStyleClass().add("mensaje-error");
            mensaje.setText("Todavía no hay una lectura de peso válida.");
            return;
        }
        pesoTara = lectura;
        taraManual.setSelected(true);
        tara.setEditable(true);
        tara.setText(String.valueOf(pesoTara));
        actualizarResumenPesos();
    }

    private Integer leerPesoDisplay() {
        String valor = pesoEnVivo.getText().replace("kg", "").trim();
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void actualizarResumenPesos() {
        lblValBruto.setText(String.valueOf(pesoBruto));
        lblValTara.setText(String.valueOf(pesoTara));
        try {
            lblValNeto.setText(String.valueOf(CalculoPesaje.neto(pesoBruto, pesoTara)));
        } catch (Exception ex) {
            lblValNeto.setText("0");
        }
    }

    private void guardar() {
        try {
            Integer vehiculoId = ComboOpcion.idSeleccionado(comboVehiculo);
            if (vehiculoId == null) {
                mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
                mensaje.getStyleClass().add("mensaje-error");
                mensaje.setText("Seleccione un vehículo (placa).");
                return;
            }
            if (taraManual.isSelected()) {
                pesoTara = enteroOCero(tara.getText());
            }
            CalculoPesaje.validar(pesoBruto, pesoTara);
            Pesaje p = new Pesaje();
            p.setGuia(guia.getText().trim());
            p.setLote(lote.getText().trim());
            p.setVehiculoId(vehiculoId);
            p.setChoferId(ComboOpcion.idSeleccionado(comboChofer));
            p.setProveedorId(ComboOpcion.idSeleccionado(comboProveedor));
            p.setProductoId(ComboOpcion.idSeleccionado(comboProducto));
            p.setOrigenId(ComboOpcion.idSeleccionado(comboOrigen));
            p.setDestinoId(ComboOpcion.idSeleccionado(comboDestino));
            p.setPesoBruto(pesoBruto);
            p.setTara(pesoTara);
            p.setCostoBs(enteroOCero(costo.getText()));
            p.setFleteBsPendiente(enteroOCero(flete.getText()));
            p.setPesadorId(usuario.getPersonaId());
            p.setEstadoBalanza(EstadoBalanza.FINALIZADO);
            repo.guardar(p, usuario.getId());
            if (servicioSync != null) {
                servicioSync.encolarPesaje(p);
            }
            mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
            mensaje.getStyleClass().add("mensaje-exito");
            mensaje.setText("Pesaje guardado correctamente.");
            guia.clear();
            lote.clear();
            pesoBruto = 0;
            actualizarResumenPesos();
            recargarGrilla();
        } catch (Exception e) {
            mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
            mensaje.getStyleClass().add("mensaje-error");
            mensaje.setText(e.getMessage());
        }
    }

    private static int enteroOCero(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(texto.trim());
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }

    private static String entero(Integer valor) {
        return valor != null ? String.valueOf(valor) : "";
    }

    private boolean crearVehiculoRapido() {
        DialogoRapido dialogo = new DialogoRapido("Nuevo vehículo",
                new String[] {"Placa", "Tipo", "Tara (kg)"}, new String[] {"", "", "0"});
        if (!dialogo.mostrar()) {
            return false;
        }
        String[] valores = dialogo.valores();
        repoVehiculo.crear(valores[0].trim(), valores[1].trim(), enteroOCero(valores[2]));
        mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
        mensaje.getStyleClass().add("mensaje-exito");
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
        mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
        mensaje.getStyleClass().add("mensaje-exito");
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
        mensaje.getStyleClass().removeAll("mensaje-error", "mensaje-exito");
        mensaje.getStyleClass().add("mensaje-exito");
        mensaje.setText("Proveedor creado.");
        return true;
    }

    @Override
    public void cerrar() {
        if (servicioBalanza != null) {
            servicioBalanza.close();
            servicioBalanza = null;
        }
    }
}
