package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.RepositorioPesaje;
import bo.firmadigital.jacobitus.pesaje.sync.ResultadoSync;
import bo.firmadigital.jacobitus.pesaje.sync.ServicioSync;

/**
 * Resumen del sistema: cifras del dia y estado de la sincronizacion con
 * la web.
 */
public final class InicioPane extends VBox {

    private final ServicioSync servicioSync;
    private final Label estadoSync = new Label();
    private final Label pendientes = new Label();
    private final Label errores = new Label();

    public InicioPane(ServicioSync servicioSync) {
        this.servicioSync = servicioSync;
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

        Label tituloSync = new Label("Sincronización con la web");
        tituloSync.getStyleClass().add("titulo-tarjeta");
        estadoSync.getStyleClass().add("subtitulo");
        pendientes.getStyleClass().add("subtitulo");
        errores.getStyleClass().add("subtitulo");

        Button sincronizar = new Button("Sincronizar ahora");
        sincronizar.getStyleClass().add("boton-secundario");
        sincronizar.setOnAction(e -> sincronizar());

        HBox acciones = new HBox(8, sincronizar);
        acciones.setPadding(new Insets(0, 0, 0, 0));

        getChildren().addAll(titulo, grid, tituloSync, estadoSync, pendientes, errores, acciones);
        refrescarEstado();
    }

    private void sincronizar() {
        if (servicioSync == null) {
            estadoSync.setText("La sincronizacion no esta configurada en este equipo.");
            return;
        }
        estadoSync.setText("Sincronizando...");
        ResultadoSync resultado = servicioSync.sincronizarAhora();
        StringBuilder detalle = new StringBuilder();
        detalle.append("Enviados: ").append(resultado.getEnviados())
                .append(" | Conflictos: ").append(resultado.getConflictos())
                .append(" | Descargados: ").append(resultado.getDescargados());
        if (!resultado.getMensaje().isEmpty()) {
            detalle.append(" | ").append(resultado.getMensaje());
        }
        estadoSync.setText(detalle.toString());
        refrescarEstado();
    }

    private void refrescarEstado() {
        if (servicioSync == null) {
            estadoSync.setText("La sincronizacion no esta configurada en este equipo.");
            return;
        }
        pendientes.setText("Pendientes de enviar: " + servicioSync.pendientes());
        errores.setText("Con error: " + servicioSync.errores());
        ResultadoSync ultimo = servicioSync.getUltimoResultado();
        if (ultimo != null && !ultimo.getMensaje().isEmpty()) {
            estadoSync.setText(ultimo.getMensaje());
        }
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
