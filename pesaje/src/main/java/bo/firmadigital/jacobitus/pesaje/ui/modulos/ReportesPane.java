package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;
import bo.firmadigital.jacobitus.pesaje.modelo.Pesaje;

public final class ReportesPane extends BorderPane {

    private final RepositorioPesaje repo = new RepositorioPesaje();
    private final TableView<Pesaje> tabla = new TableView<>();
    private final Label mensaje = new Label();

    public ReportesPane() {
        setPadding(new Insets(8));
        tabla.getStyleClass().add("tabla-modulo");
        TableColumn<Pesaje, String> colGuia = new TableColumn<>("Guía");
        colGuia.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getGuia()));
        TableColumn<Pesaje, String> colLote = new TableColumn<>("Lote");
        colLote.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLote()));
        TableColumn<Pesaje, String> colNeto = new TableColumn<>("Neto (kg)");
        colNeto.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPesoNeto() != null ? String.valueOf(c.getValue().getPesoNeto()) : ""));
        TableColumn<Pesaje, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaCreacion()));
        tabla.getColumns().setAll(colGuia, colLote, colNeto, colFecha);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recargar();
        Button actualizar = new Button("Actualizar");
        actualizar.getStyleClass().add("boton-secundario");
        actualizar.setOnAction(e -> recargar());
        Button pdf = new Button("Exportar PDF");
        pdf.getStyleClass().add("boton-primario");
        pdf.setOnAction(e -> mensaje.setText("La exportación PDF se completará en T10."));
        mensaje.getStyleClass().add("subtitulo");
        HBox barra = new HBox(8, actualizar, pdf, mensaje);
        setTop(barra);
        setCenter(tabla);
    }

    private void recargar() {
        tabla.setItems(FXCollections.observableArrayList(repo.listarRecientes(100)));
    }
}
