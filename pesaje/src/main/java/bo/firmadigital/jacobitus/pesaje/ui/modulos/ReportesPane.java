package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import bo.firmadigital.jacobitus.pesaje.datos.FilaPesaje;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;
import bo.firmadigital.jacobitus.pesaje.reportes.BoletaPdf;
import bo.firmadigital.jacobitus.pesaje.reportes.ReportePdf;
import bo.firmadigital.jacobitus.pesaje.ui.comun.DialogoVistaPreviaPdf;

/**
 * Reportes de pesaje: listado, reporte en PDF y boleta por pesaje.
 *
 * Los filtros son los del sistema original: rango de fechas y placa.
 */
public final class ReportesPane extends BorderPane {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RepositorioPesaje repo = new RepositorioPesaje();
    private final TableView<FilaPesaje> tabla = new TableView<>();
    private final TextField desde = new TextField();
    private final TextField hasta = new TextField();
    private final TextField placa = new TextField();
    private final Label mensaje = new Label();

    public ReportesPane() {
        setPadding(new Insets(8));
        tabla.getStyleClass().add("tabla-modulo");
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
        tabla.getColumns().setAll(colGuia, colVehiculo, colChofer, colProveedor, colBruto,
                colTara, colNeto, colEstado, colFecha);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        desde.setPromptText("Desde (yyyy-MM-dd)");
        hasta.setPromptText("Hasta (yyyy-MM-dd)");
        placa.setPromptText("Placa");
        mensaje.getStyleClass().add("subtitulo");

        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("boton-secundario");
        actualizar.setOnAction(e -> recargar());

        Button reporte = new Button("Reporte PDF");
        reporte.getStyleClass().add("boton-primario");
        reporte.setOnAction(e -> exportarReporte());

        Button boleta = new Button("Boleta del pesaje");
        boleta.getStyleClass().add("boton-primario");
        boleta.setOnAction(e -> exportarBoleta());

        HBox barra = new HBox(8, desde, hasta, placa, actualizar, reporte, boleta, mensaje);
        barra.setPadding(new Insets(0, 0, 8, 0));
        setTop(barra);
        setCenter(tabla);
        recargar();
    }

    private void recargar() {
        tabla.setItems(FXCollections.observableArrayList(repo.listarListado(200)));
    }

    private void exportarReporte() {
        try {
            Path destino = carpetaReportes().resolve("reporte-pesaje-"
                    + LocalDate.now().format(FORMATO_FECHA) + ".pdf");
            ReportePdf.guardar(desde.getText().trim(), hasta.getText().trim(),
                    placa.getText().trim(), destino.toFile());
            mensaje.setText("Reporte generado: " + destino);
            DialogoVistaPreviaPdf.mostrar(ventanaDeEstaPantalla(), "Reporte de pesaje",
                    destino.toFile());
        } catch (Exception e) {
            mensaje.setText("No se pudo generar el reporte: " + e.getMessage());
        }
    }

    private void exportarBoleta() {
        FilaPesaje seleccion = tabla.getSelectionModel().getSelectedItem();
        if (seleccion == null) {
            mensaje.setText("Seleccione un pesaje de la lista.");
            return;
        }
        try {
            Path destino = carpetaReportes().resolve("boleta-" + seleccion.getGuia() + ".pdf");
            BoletaPdf.guardar(seleccion.getId(), destino.toFile());
            mensaje.setText("Boleta generada: " + destino);
            DialogoVistaPreviaPdf.mostrar(ventanaDeEstaPantalla(), "Boleta de pesaje",
                    destino.toFile());
        } catch (Exception e) {
            mensaje.setText("No se pudo generar la boleta: " + e.getMessage());
        }
    }

    /**
     * Ventana donde esta esta pantalla.
     *
     * @return la ventana propietaria del dialogo, o null si la pantalla
     *         todavia no esta en escena
     */
    private Window ventanaDeEstaPantalla() {
        return getScene() != null ? getScene().getWindow() : null;
    }

    /**
     * Carpeta donde se guardan los PDF.
     *
     * @return carpeta de reportes
     */
    public static Path carpetaReportes() {
        Path base = Paths.get(System.getProperty("user.home", "."),
                "Documents", "SistemaPesaje");
        try {
            java.nio.file.Files.createDirectories(base);
        } catch (java.io.IOException e) {
            base = Paths.get(System.getProperty("java.io.tmpdir", "."));
        }
        return base;
    }
}
