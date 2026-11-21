package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import bo.firmadigital.jacobitus.pesaje.balanza.ConfigBalanza;
import bo.firmadigital.jacobitus.pesaje.balanza.MetodoLectura;

public final class RepositorioConfigBalanza {

    private final Sql sql = Database.obtenerInstancia().sql();
    private Integer idCache;

    public ConfigBalanza obtenerActiva() {
        List<ConfigBalanza> filas = sql.consultar(
                "SELECT * FROM config_balanza ORDER BY id DESC LIMIT 1", RepositorioConfigBalanza::mapear);
        if (filas.isEmpty()) {
            idCache = null;
            return new ConfigBalanza();
        }
        return filas.get(0);
    }

    public void cargarId() {
        List<Integer> ids = sql.consultar(
                "SELECT id FROM config_balanza ORDER BY id DESC LIMIT 1", r -> r.getInt("id"));
        idCache = ids.isEmpty() ? null : ids.get(0);
    }

    public void guardar(ConfigBalanza cfg) {
        if (idCache == null) {
            cargarId();
        }
        String fecha = Fechas.ahora();
        String metodo = cfg.getMetodoLectura() != null ? cfg.getMetodoLectura().texto() : MetodoLectura.POR_DEFECTO.texto();
        if (idCache == null || idCache <= 0) {
            long nuevo = sql.insertar(
                    "INSERT INTO config_balanza (puerto_com, baudrate, databits, paridad, stopbits,"
                            + " flowcontrol, timeout_ms, metodo_lectura, posicion_inicio, posicion_longitud,"
                            + " fecha_modificacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    cfg.getPuertoCom(), cfg.getBaudRate(), cfg.getDataBits(), cfg.getParidad(),
                    cfg.getStopBits(), cfg.getFlowControl(), cfg.getTimeoutMs(), metodo,
                    cfg.getPosicionInicio(), cfg.getPosicionLongitud(), fecha);
            idCache = (int) nuevo;
            return;
        }
        sql.actualizar(
                "UPDATE config_balanza SET puerto_com = ?, baudrate = ?, databits = ?, paridad = ?,"
                        + " stopbits = ?, flowcontrol = ?, timeout_ms = ?, metodo_lectura = ?,"
                        + " posicion_inicio = ?, posicion_longitud = ?, fecha_modificacion = ? WHERE id = ?",
                cfg.getPuertoCom(), cfg.getBaudRate(), cfg.getDataBits(), cfg.getParidad(),
                cfg.getStopBits(), cfg.getFlowControl(), cfg.getTimeoutMs(), metodo,
                cfg.getPosicionInicio(), cfg.getPosicionLongitud(), fecha, idCache);
    }

    private static final class FilaCfg extends ConfigBalanza {
        int id;
    }

    private static ConfigBalanza mapear(ResultSet r) throws SQLException {
        FilaCfg cfg = new FilaCfg();
        cfg.id = r.getInt("id");
        cfg.setPuertoCom(Sql.texto(r, "puerto_com"));
        Integer baud = Sql.entero(r, "baudrate");
        if (baud != null) {
            cfg.setBaudRate(baud);
        }
        Integer data = Sql.entero(r, "databits");
        if (data != null) {
            cfg.setDataBits(data);
        }
        cfg.setParidad(Sql.texto(r, "paridad"));
        Integer stop = Sql.entero(r, "stopbits");
        if (stop != null) {
            cfg.setStopBits(stop);
        }
        cfg.setFlowControl(Sql.texto(r, "flowcontrol"));
        Integer timeout = Sql.entero(r, "timeout_ms");
        if (timeout != null) {
            cfg.setTimeoutMs(timeout);
        }
        cfg.setMetodoLectura(MetodoLectura.desdeTexto(Sql.texto(r, "metodo_lectura")));
        Integer ini = Sql.entero(r, "posicion_inicio");
        if (ini != null) {
            cfg.setPosicionInicio(ini);
        }
        Integer lon = Sql.entero(r, "posicion_longitud");
        if (lon != null) {
            cfg.setPosicionLongitud(lon);
        }
        return cfg;
    }
}
