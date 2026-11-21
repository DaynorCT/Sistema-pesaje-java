package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.balanza.ConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.balanza.MetodoLectura;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.ui.comun.PanelAbm;

public final class BalanzaPane extends VBox {

    private final RepositorioConfigBalanza repo = new RepositorioConfigBalanza();
    private final TextField puerto = new TextField();
    private final TextField baud = new TextField();
    private final TextField dataBits = new TextField();
    private final TextField paridad = new TextField();
    private final TextField stopBits = new TextField();
    private final TextField flow = new TextField();
    private final TextField timeout = new TextField();
    private final TextField posInicio = new TextField();
    private final TextField posLongitud = new TextField();
    private final ComboBox<MetodoLectura> metodo = new ComboBox<>(
            FXCollections.observableArrayList(MetodoLectura.values()));
    private final Label mensaje = new Label();

    public BalanzaPane() {
        setPadding(new Insets(16));
        setSpacing(12);
        getStyleClass().add("tarjeta");
        ConfigBalanza cfg = repo.obtenerActiva();
        repo.cargarId();
        rellenar(cfg);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        int f = 0;
        agregar(grid, f++, "Puerto COM", puerto);
        agregar(grid, f++, "Baudrate", baud);
        agregar(grid, f++, "Data bits", dataBits);
        agregar(grid, f++, "Paridad", paridad);
        agregar(grid, f++, "Stop bits", stopBits);
        agregar(grid, f++, "Flow control", flow);
        agregar(grid, f++, "Timeout (ms)", timeout);
        agregar(grid, f++, "Método lectura", metodo);
        agregar(grid, f++, "Posición inicio", posInicio);
        agregar(grid, f, "Longitud", posLongitud);
        Button guardar = new Button("Guardar");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());
        mensaje.getStyleClass().add("subtitulo");
        getChildren().addAll(new Label("Configuración de balanza"), grid, guardar, mensaje);
    }

    private void rellenar(ConfigBalanza cfg) {
        puerto.setText(cfg.getPuertoCom());
        baud.setText(String.valueOf(cfg.getBaudRate()));
        dataBits.setText(String.valueOf(cfg.getDataBits()));
        paridad.setText(cfg.getParidad());
        stopBits.setText(String.valueOf(cfg.getStopBits()));
        flow.setText(cfg.getFlowControl());
        timeout.setText(String.valueOf(cfg.getTimeoutMs()));
        metodo.getSelectionModel().select(cfg.getMetodoLectura());
        posInicio.setText(String.valueOf(cfg.getPosicionInicio()));
        posLongitud.setText(String.valueOf(cfg.getPosicionLongitud()));
    }

    private void guardar() {
        try {
            ConfigBalanza cfg = new ConfigBalanza();
            cfg.setPuertoCom(puerto.getText().trim());
            cfg.setBaudRate(PanelAbm.parsearEntero(baud.getText(), "Baudrate"));
            cfg.setDataBits(PanelAbm.parsearEntero(dataBits.getText(), "Data bits"));
            cfg.setParidad(paridad.getText().trim());
            cfg.setStopBits(PanelAbm.parsearEntero(stopBits.getText(), "Stop bits"));
            cfg.setFlowControl(flow.getText().trim());
            cfg.setTimeoutMs(PanelAbm.parsearEntero(timeout.getText(), "Timeout"));
            cfg.setMetodoLectura(metodo.getValue());
            cfg.setPosicionInicio(PanelAbm.parsearEntero(posInicio.getText(), "Posición inicio"));
            cfg.setPosicionLongitud(PanelAbm.parsearEntero(posLongitud.getText(), "Longitud"));
            repo.guardar(cfg);
            mensaje.setText("Configuración de balanza guardada.");
        } catch (RuntimeException e) {
            mensaje.setText(e.getMessage());
        }
    }

    private void agregar(GridPane grid, int fila, String etiqueta, javafx.scene.Node campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }
}
