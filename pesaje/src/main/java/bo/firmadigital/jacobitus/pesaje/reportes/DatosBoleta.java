package bo.firmadigital.jacobitus.pesaje.reportes;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.Sql;

/**
 * Datos de un pesaje con todo lo que necesita la boleta en PDF.
 *
 * Es la misma consulta que usaba el sistema original, de modo que la
 * boleta muestre exactamente los mismos datos.
 */
public final class DatosBoleta {

    private final int id;
    private final String guia;
    private final String fecha;
    private final String hora;
    private final String choferNombre;
    private final String choferCi;
    private final String choferLicencia;
    private final String vehiculoPlaca;
    private final String vehiculoTipo;
    private final String proveedorNombre;
    private final String productoNombre;
    private final String origenNombre;
    private final String destinoNombre;
    private final String operador;
    private final int pesoBruto;
    private final int tara;
    private final int pesoNeto;
    private final int costoBs;
    private final int fleteBs;

    public DatosBoleta(int id, String guia, String fecha, String hora, String choferNombre,
            String choferCi, String choferLicencia, String vehiculoPlaca, String vehiculoTipo,
            String proveedorNombre, String productoNombre, String origenNombre,
            String destinoNombre, String operador, int pesoBruto, int tara, int pesoNeto,
            int costoBs, int fleteBs) {
        this.id = id;
        this.guia = guia;
        this.fecha = fecha;
        this.hora = hora;
        this.choferNombre = choferNombre;
        this.choferCi = choferCi;
        this.choferLicencia = choferLicencia;
        this.vehiculoPlaca = vehiculoPlaca;
        this.vehiculoTipo = vehiculoTipo;
        this.proveedorNombre = proveedorNombre;
        this.productoNombre = productoNombre;
        this.origenNombre = origenNombre;
        this.destinoNombre = destinoNombre;
        this.operador = operador;
        this.pesoBruto = pesoBruto;
        this.tara = tara;
        this.pesoNeto = pesoNeto;
        this.costoBs = costoBs;
        this.fleteBs = fleteBs;
    }

    public int getId() {
        return id;
    }

    public String getGuia() {
        return guia;
    }

    public String getFecha() {
        return fecha;
    }

    public String getHora() {
        return hora;
    }

    public String getChoferNombre() {
        return choferNombre;
    }

    public String getChoferCi() {
        return choferCi;
    }

    public String getChoferLicencia() {
        return choferLicencia;
    }

    public String getVehiculoPlaca() {
        return vehiculoPlaca;
    }

    public String getVehiculoTipo() {
        return vehiculoTipo;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public String getOrigenNombre() {
        return origenNombre;
    }

    public String getDestinoNombre() {
        return destinoNombre;
    }

    public String getOperador() {
        return operador;
    }

    public int getPesoBruto() {
        return pesoBruto;
    }

    public int getTara() {
        return tara;
    }

    public int getPesoNeto() {
        return pesoNeto;
    }

    public int getCostoBs() {
        return costoBs;
    }

    public int getFleteBs() {
        return fleteBs;
    }

    private static final String SELECT =
            "SELECT p.id, p.guia, p.fecha_creacion,"
                    + " TRIM(COALESCE(pe.nombre, '') || ' ' || COALESCE(pe.apellido_paterno, '')"
                    + " || ' ' || COALESCE(pe.apellido_materno, '')) AS chofer_nombre,"
                    + " COALESCE(pe.ci, '') AS chofer_ci,"
                    + " COALESCE(ch.licencia, '') AS chofer_licencia,"
                    + " COALESCE(v.placa, '') AS vehiculo_placa,"
                    + " COALESCE(v.tipo_vehiculo, '') AS vehiculo_tipo,"
                    + " TRIM(COALESCE(pr.nombre, '') || ' ' || COALESCE(pr.apellido_paterno, ''))"
                    + " AS proveedor_nombre,"
                    + " COALESCE(prod.nombre, '') AS producto_nombre,"
                    + " COALESCE(o.nombre, '') AS origen_nombre,"
                    + " COALESCE(d.nombre, '') AS destino_nombre,"
                    + " COALESCE(ps.nombre, '') AS operador,"
                    + " COALESCE(p.peso_bruto, 0) AS peso_bruto, COALESCE(p.tara, 0) AS tara,"
                    + " COALESCE(p.peso_neto, 0) AS peso_neto, COALESCE(p.costo_bs, 0) AS costo_bs,"
                    + " COALESCE(p.flete_bs_pendiente, 0) AS flete_bs"
                    + " FROM pesajes p"
                    + " LEFT JOIN vehiculos v ON v.id = p.vehiculo_id"
                    + " LEFT JOIN choferes ch ON ch.id = p.chofer_id"
                    + " LEFT JOIN personas pe ON pe.id = ch.persona_id"
                    + " LEFT JOIN proveedores ppr ON ppr.id = p.proveedor_id"
                    + " LEFT JOIN personas pr ON pr.id = ppr.persona_id"
                    + " LEFT JOIN productos prod ON prod.id = p.producto_id"
                    + " LEFT JOIN origenes o ON o.id = p.id_origen"
                    + " LEFT JOIN destinos d ON d.id = p.id_destino"
                    + " LEFT JOIN personas ps ON ps.id = p.pesador_id"
                    + " WHERE p.id = ?";

    /**
     * Carga los datos de un pesaje para la boleta.
     *
     * @param pesajeId id del pesaje
     * @return datos de la boleta, o null si el pesaje no existe
     */
    public static DatosBoleta cargar(int pesajeId) {
        Sql sql = Database.obtenerInstancia().sql();
        List<DatosBoleta> filas = sql.consultar(SELECT, DatosBoleta::deFila, pesajeId);
        return filas.isEmpty() ? null : filas.get(0);
    }

    /**
     * Carga los pesajes para el reporte, con los filtros indicados.
     *
     * @param fechaDesde fecha inicial (yyyy-MM-dd) o vacia
     * @param fechaHasta fecha final (yyyy-MM-dd) o vacia
     * @param placa     parte de la placa o vacia
     * @return pesajes del reporte
     */
    public static List<DatosBoleta> cargarReporte(String fechaDesde, String fechaHasta,
            String placa) {
        StringBuilder consulta = new StringBuilder(SELECT.replace("WHERE p.id = ?", "WHERE p.estado = 'ACTIVO'"));
        List<Object> parametros = new java.util.ArrayList<>();
        if (fechaDesde != null && !fechaDesde.isEmpty()) {
            consulta.append(" AND p.fecha_creacion >= ?");
            parametros.add(fechaDesde + " 00:00:00");
        }
        if (fechaHasta != null && !fechaHasta.isEmpty()) {
            consulta.append(" AND p.fecha_creacion <= ?");
            parametros.add(fechaHasta + " 23:59:59");
        }
        if (placa != null && !placa.trim().isEmpty()) {
            consulta.append(" AND UPPER(v.placa) LIKE ?");
            parametros.add("%" + placa.trim().toUpperCase() + "%");
        }
        consulta.append(" ORDER BY p.id DESC");
        return Database.obtenerInstancia().sql()
                .consultar(consulta.toString(), DatosBoleta::deFila, parametros.toArray());
    }

    private static DatosBoleta deFila(ResultSet r) throws SQLException {
        String fechaCompleta = Sql.texto(r, "fecha_creacion");
        String fecha = "";
        String hora = "";
        if (fechaCompleta != null && fechaCompleta.length() >= 16) {
            fecha = fechaCompleta.substring(8, 10) + "/" + fechaCompleta.substring(5, 7)
                    + "/" + fechaCompleta.substring(0, 4);
            hora = fechaCompleta.substring(11, 16);
        }
        return new DatosBoleta(
                r.getInt("id"),
                Sql.texto(r, "guia"),
                fecha,
                hora,
                Sql.texto(r, "chofer_nombre"),
                Sql.texto(r, "chofer_ci"),
                Sql.texto(r, "chofer_licencia"),
                Sql.texto(r, "vehiculo_placa"),
                Sql.texto(r, "vehiculo_tipo"),
                Sql.texto(r, "proveedor_nombre"),
                Sql.texto(r, "producto_nombre"),
                Sql.texto(r, "origen_nombre"),
                Sql.texto(r, "destino_nombre"),
                Sql.texto(r, "operador"),
                entero(r, "peso_bruto"),
                entero(r, "tara"),
                entero(r, "peso_neto"),
                entero(r, "costo_bs"),
                entero(r, "flete_bs"));
    }

    /**
     * Lee un entero de la fila, con 0 cuando no hay valor.
     *
     * @param r     fila del resultado
     * @param campo nombre de la columna
     * @return valor entero
     * @throws SQLException si la lectura falla
     */
    private static int entero(ResultSet r, String campo) throws SQLException {
        Integer valor = Sql.entero(r, campo);
        return valor != null ? valor : 0;
    }
}
