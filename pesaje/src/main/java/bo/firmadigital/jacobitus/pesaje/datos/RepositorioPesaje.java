package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import bo.firmadigital.jacobitus.pesaje.modelo.CalculoPesaje;
import bo.firmadigital.jacobitus.pesaje.modelo.Estado;
import bo.firmadigital.jacobitus.pesaje.modelo.EstadoBalanza;
import bo.firmadigital.jacobitus.pesaje.modelo.Pesaje;

public final class RepositorioPesaje {

    private final Sql sql = Database.obtenerInstancia().sql();

    public List<Pesaje> listarRecientes(int limite) {
        return sql.consultar(
                "SELECT * FROM pesajes ORDER BY id DESC LIMIT ?",
                RepositorioPesaje::mapear,
                limite);
    }

    public long guardar(Pesaje pesaje, Integer usuarioId) {
        CalculoPesaje.aplicar(pesaje);
        String fecha = Fechas.ahora();
        String evento = pesaje.getSyncEventId();
        if (evento == null || evento.isEmpty()) {
            evento = UUID.randomUUID().toString();
            pesaje.setSyncEventId(evento);
        }
        if (pesaje.getId() != null) {
            sql.actualizar(
                    "UPDATE pesajes SET guia = ?, lote = ?, vehiculo_id = ?, chofer_id = ?,"
                            + " proveedor_id = ?, producto_id = ?, id_origen = ?, id_destino = ?,"
                            + " peso_bruto = ?, tara = ?, peso_neto = ?, costo_bs = ?,"
                            + " flete_bs_pendiente = ?, estado = ?, estado_balanza = ?,"
                            + " usuario_modificacion = ?, fecha_modificacion = ? WHERE id = ?",
                    pesaje.getGuia(), pesaje.getLote(), pesaje.getVehiculoId(), pesaje.getChoferId(),
                    pesaje.getProveedorId(), pesaje.getProductoId(), pesaje.getOrigenId(),
                    pesaje.getDestinoId(), pesaje.getPesoBruto(), pesaje.getTara(), pesaje.getPesoNeto(),
                    pesaje.getCostoBs(), pesaje.getFleteBsPendiente(),
                    pesaje.getEstado().name(), pesaje.getEstadoBalanza().name(),
                    usuarioId, fecha, pesaje.getId());
            return pesaje.getId();
        }
        long id = sql.insertar(
                "INSERT INTO pesajes (guia, lote, vehiculo_id, chofer_id, proveedor_id, producto_id,"
                        + " id_origen, id_destino, peso_bruto, tara, peso_neto, costo_bs,"
                        + " flete_bs_pendiente, pesador_id, estado, estado_balanza, sync_event_id,"
                        + " usuario_creacion, usuario_modificacion, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                pesaje.getGuia(), pesaje.getLote(), pesaje.getVehiculoId(), pesaje.getChoferId(),
                pesaje.getProveedorId(), pesaje.getProductoId(), pesaje.getOrigenId(), pesaje.getDestinoId(),
                pesaje.getPesoBruto(), pesaje.getTara(), pesaje.getPesoNeto(), pesaje.getCostoBs(),
                pesaje.getFleteBsPendiente(), pesaje.getPesadorId(),
                pesaje.getEstado().name(), pesaje.getEstadoBalanza().name(), evento,
                usuarioId, usuarioId, fecha, fecha);
        pesaje.setId((int) id);
        return id;
    }

    public long contarHoy() {
        return sql.contar("SELECT COUNT(*) FROM pesajes WHERE date(fecha_creacion) = date('now', 'localtime')");
    }

    public long contarTotal() {
        return sql.contar("SELECT COUNT(*) FROM pesajes");
    }

    private static Pesaje mapear(ResultSet fila) throws SQLException {
        Pesaje p = new Pesaje();
        p.setId(Sql.entero(fila, "id"));
        p.setGuia(Sql.texto(fila, "guia"));
        p.setLote(Sql.texto(fila, "lote"));
        p.setVehiculoId(Sql.entero(fila, "vehiculo_id"));
        p.setChoferId(Sql.entero(fila, "chofer_id"));
        p.setProveedorId(Sql.entero(fila, "proveedor_id"));
        p.setProductoId(Sql.entero(fila, "producto_id"));
        p.setOrigenId(Sql.entero(fila, "id_origen"));
        p.setDestinoId(Sql.entero(fila, "id_destino"));
        p.setPesoBruto(Sql.entero(fila, "peso_bruto"));
        p.setTara(Sql.entero(fila, "tara"));
        p.setPesoNeto(Sql.entero(fila, "peso_neto"));
        p.setCostoBs(Sql.entero(fila, "costo_bs"));
        p.setFleteBsPendiente(Sql.entero(fila, "flete_bs_pendiente"));
        String estado = Sql.texto(fila, "estado");
        if (estado != null) {
            p.setEstado(Estado.valueOf(estado));
        }
        String balanza = Sql.texto(fila, "estado_balanza");
        if (balanza != null) {
            p.setEstadoBalanza(EstadoBalanza.valueOf(balanza));
        }
        p.setFechaCreacion(Sql.texto(fila, "fecha_creacion"));
        return p;
    }
}
