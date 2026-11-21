package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroCatalogo;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroChofer;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroProveedor;
import bo.firmadigital.jacobitus.pesaje.datos.RegistroVehiculo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioCatalogo;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioChofer;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioProveedor;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioVehiculo;
import bo.firmadigital.jacobitus.pesaje.modelo.CalculoPesaje;
import bo.firmadigital.jacobitus.pesaje.modelo.EstadoBalanza;
import bo.firmadigital.jacobitus.pesaje.modelo.Pesaje;
import bo.firmadigital.jacobitus.pesaje.ui.comun.ComboOpcion;

public final class PesajePane extends VBox {

    private final UsuarioSesion usuario;
    private final RepositorioPesaje repo = new RepositorioPesaje();
    private final TextField guia = new TextField();
    private final TextField lote = new TextField();
    private final TextField bruto = new TextField();
    private final TextField tara = new TextField();
    private final TextField neto = new TextField();
    private final TextField costo = new TextField("0");
    private final TextField flete = new TextField("0");
    private final Label mensaje = new Label();
    private ComboBox<ComboOpcion> comboVehiculo;
    private ComboBox<ComboOpcion> comboChofer;
    private ComboBox<ComboOpcion> comboProveedor;
    private ComboBox<ComboOpcion> comboProducto;
    private ComboBox<ComboOpcion> comboOrigen;
    private ComboBox<ComboOpcion> comboDestino;

    public PesajePane(UsuarioSesion usuario) {
        this.usuario = usuario;
        setPadding(new Insets(16));
        setSpacing(12);
        getStyleClass().add("tarjeta");
        Label titulo = new Label("Registro de pesaje");
        titulo.getStyleClass().add("titulo-modulo");
        cargarCombos();
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
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
        neto.setEditable(false);
        bruto.textProperty().addListener((o, a, b) -> recalcularNeto());
        tara.textProperty().addListener((o, a, b) -> recalcularNeto());
        agregar(grid, f++, "Costo Bs", costo);
        agregar(grid, f, "Flete pendiente Bs", flete);
        Button usarTara = new Button("Usar tara del vehículo");
        usarTara.getStyleClass().add("boton-secundario");
        usarTara.setOnAction(e -> aplicarTaraVehiculo());
        Button guardar = new Button("Guardar pesaje");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());
        mensaje.getStyleClass().add("subtitulo");
        HBox acciones = new HBox(8, usarTara, guardar);
        getChildren().addAll(titulo, grid, acciones, mensaje);
    }

    private void cargarCombos() {
        comboVehiculo = ComboOpcion.crear(mapearVehiculos());
        comboChofer = ComboOpcion.crear(mapearChoferes());
        comboProveedor = ComboOpcion.crear(mapearProveedores());
        comboProducto = ComboOpcion.crear(mapearCatalogo("productos"));
        comboOrigen = ComboOpcion.crear(mapearCatalogo("origenes"));
        comboDestino = ComboOpcion.crear(mapearCatalogo("destinos"));
    }

    private List<ComboOpcion> mapearVehiculos() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroVehiculo v : new RepositorioVehiculo().listarActivos()) {
            lista.add(new ComboOpcion(v.getId(), v.etiqueta()));
        }
        return lista;
    }

    private List<ComboOpcion> mapearChoferes() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroChofer c : new RepositorioChofer().listarActivos()) {
            lista.add(new ComboOpcion(c.getId(), c.nombreCompleto()));
        }
        return lista;
    }

    private List<ComboOpcion> mapearProveedores() {
        List<ComboOpcion> lista = new ArrayList<>();
        for (RegistroProveedor p : new RepositorioProveedor().listarActivos()) {
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

    private void agregar(GridPane grid, int fila, String etiqueta, javafx.scene.Node campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }

    private void aplicarTaraVehiculo() {
        Integer id = ComboOpcion.idSeleccionado(comboVehiculo);
        if (id == null) {
            mensaje.setText("Seleccione un vehículo.");
            return;
        }
        for (RegistroVehiculo v : new RepositorioVehiculo().listarActivos()) {
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
            mensaje.setText("");
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
            p.setCostoBs(Integer.parseInt(costo.getText().trim().isEmpty() ? "0" : costo.getText().trim()));
            p.setFleteBsPendiente(Integer.parseInt(flete.getText().trim().isEmpty() ? "0" : flete.getText().trim()));
            p.setPesadorId(usuario.getPersonaId());
            p.setEstadoBalanza(EstadoBalanza.FINALIZADO);
            repo.guardar(p, usuario.getId());
            mensaje.setText("Pesaje guardado correctamente.");
            guia.clear();
            lote.clear();
            bruto.clear();
            tara.clear();
            neto.clear();
        } catch (Exception e) {
            mensaje.setText(e.getMessage());
        }
    }
}
