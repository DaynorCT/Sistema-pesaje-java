package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;

public final class InicioPane extends VBox {

    public InicioPane() {
        setPadding(new Insets(16));
        setSpacing(12);
        getStyleClass().add("tarjeta");
        Label titulo = new Label("Resumen del sistema");
        titulo.getStyleClass().add("titulo-modulo");
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(8);
        RepositorioPesaje pesajes = new RepositorioPesaje();
        var sql = Database.obtenerInstancia().sql();
        agregar(grid, 0, "Pesajes totales", String.valueOf(pesajes.contarTotal()));
        agregar(grid, 1, "Pesajes hoy", String.valueOf(pesajes.contarHoy()));
        agregar(grid, 2, "Vehículos activos", String.valueOf(sql.contar(
                "SELECT COUNT(*) FROM vehiculos WHERE estado = 'ACTIVO'")));
        agregar(grid, 3, "Productos activos", String.valueOf(sql.contar(
                "SELECT COUNT(*) FROM productos WHERE estado = 'ACTIVO'")));
        agregar(grid, 4, "Choferes activos", String.valueOf(sql.contar(
                "SELECT COUNT(*) FROM choferes WHERE estado = 'ACTIVO'")));
        agregar(grid, 5, "Proveedores activos", String.valueOf(sql.contar(
                "SELECT COUNT(*) FROM proveedores WHERE estado = 'ACTIVO'")));
        getChildren().addAll(titulo, grid);
    }

    private void agregar(GridPane grid, int fila, String etiqueta, String valor) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("subtitulo");
        Label v = new Label(valor);
        v.getStyleClass().add("titulo-tarjeta");
        grid.add(l, 0, fila);
        grid.add(v, 1, fila);
    }
}
