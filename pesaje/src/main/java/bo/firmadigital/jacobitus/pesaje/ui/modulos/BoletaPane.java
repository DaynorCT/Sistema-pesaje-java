package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.datos.RepositorioBoleta;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioBoleta.BoletaFila;

public final class BoletaPane extends VBox {

    private final RepositorioBoleta repo = new RepositorioBoleta();
    private final BoletaFila fila = new BoletaFila();
    private final TextField salida = new TextField();
    private final TextField direccion = new TextField();
    private final TextField celular1 = new TextField();
    private final TextField celular2 = new TextField();
    private final TextField ciudad = new TextField();
    private final TextField tituloSuperior = new TextField();
    private final TextField marca = new TextField();
    private final TextField tituloDocumento = new TextField();
    private final TextField acreditacion = new TextField();
    private final Label mensaje = new Label();

    public BoletaPane() {
        setPadding(new Insets(16));
        setSpacing(12);
        getStyleClass().add("tarjeta");
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(8);
        int f = 0;
        agregar(grid, f++, "Salida", salida);
        agregar(grid, f++, "Dirección", direccion);
        agregar(grid, f++, "Celular 1", celular1);
        agregar(grid, f++, "Celular 2", celular2);
        agregar(grid, f++, "Ciudad", ciudad);
        agregar(grid, f++, "Título superior", tituloSuperior);
        agregar(grid, f++, "Marca", marca);
        agregar(grid, f++, "Título documento", tituloDocumento);
        agregar(grid, f, "Acreditación", acreditacion);
        cargar();
        Button guardar = new Button("Guardar");
        guardar.getStyleClass().add("boton-primario");
        guardar.setOnAction(e -> guardar());
        mensaje.getStyleClass().add("subtitulo");
        getChildren().addAll(new Label("Configuración de boleta"), grid, guardar, mensaje);
    }

    private void cargar() {
        BoletaFila db = repo.obtenerActiva();
        if (db == null) {
            return;
        }
        fila.id = db.id;
        salida.setText(db.salida);
        direccion.setText(db.direccion);
        celular1.setText(db.celular1);
        celular2.setText(db.celular2);
        ciudad.setText(db.ciudad);
        tituloSuperior.setText(db.tituloSuperior);
        marca.setText(db.marca);
        tituloDocumento.setText(db.tituloDocumento);
        acreditacion.setText(db.acreditacion != null ? db.acreditacion : "");
    }

    private void guardar() {
        fila.salida = salida.getText().trim();
        fila.direccion = direccion.getText().trim();
        fila.celular1 = celular1.getText().trim();
        fila.celular2 = celular2.getText().trim();
        fila.ciudad = ciudad.getText().trim();
        fila.tituloSuperior = tituloSuperior.getText().trim();
        fila.marca = marca.getText().trim();
        fila.tituloDocumento = tituloDocumento.getText().trim();
        fila.acreditacion = acreditacion.getText().trim();
        repo.guardar(fila);
        cargar();
        mensaje.setText("Configuración de boleta guardada.");
    }

    private void agregar(GridPane grid, int fila, String etiqueta, TextField campo) {
        grid.add(new Label(etiqueta), 0, fila);
        grid.add(campo, 1, fila);
        GridPane.setHgrow(campo, Priority.ALWAYS);
    }
}
