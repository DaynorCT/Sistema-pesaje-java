package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Fila de la grilla de pesajes: los datos del pesaje con los nombres de
 * las entidades relacionadas ya resueltos.
 */
public final class FilaPesaje {

    private final int id;
    private final String guia;
    private final String lote;
    private final String nombreChofer;
    private final String placaVehiculo;
    private final String licenciaChofer;
    private final String tipoVehiculo;
    private final String nombreProveedor;
    private final String nombreProducto;
    private final String origen;
    private final String destino;
    private final Integer costoBs;
    private final Integer fleteBsPendiente;
    private final String fechaCreacion;
    private final Integer pesoBruto;
    private final Integer tara;
    private final Integer pesoNeto;
    private final String estado;
    private final String estadoBalanza;

    public FilaPesaje(int id, String guia, String lote, String nombreChofer, String placaVehiculo,
            String licenciaChofer, String tipoVehiculo, String nombreProveedor, String nombreProducto,
            String origen, String destino, Integer costoBs, Integer fleteBsPendiente,
            String fechaCreacion, Integer pesoBruto, Integer tara, Integer pesoNeto,
            String estado, String estadoBalanza) {
        this.id = id;
        this.guia = guia;
        this.lote = lote;
        this.nombreChofer = nombreChofer;
        this.placaVehiculo = placaVehiculo;
        this.licenciaChofer = licenciaChofer;
        this.tipoVehiculo = tipoVehiculo;
        this.nombreProveedor = nombreProveedor;
        this.nombreProducto = nombreProducto;
        this.origen = origen;
        this.destino = destino;
        this.costoBs = costoBs;
        this.fleteBsPendiente = fleteBsPendiente;
        this.fechaCreacion = fechaCreacion;
        this.pesoBruto = pesoBruto;
        this.tara = tara;
        this.pesoNeto = pesoNeto;
        this.estado = estado;
        this.estadoBalanza = estadoBalanza;
    }

    public int getId() {
        return id;
    }

    public String getGuia() {
        return guia;
    }

    public String getLote() {
        return lote;
    }

    public String getNombreChofer() {
        return nombreChofer;
    }

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public String getLicenciaChofer() {
        return licenciaChofer;
    }

    public String getTipoVehiculo() {
        return tipoVehiculo;
    }

    public String getNombreProveedor() {
        return nombreProveedor;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public Integer getCostoBs() {
        return costoBs;
    }

    public Integer getFleteBsPendiente() {
        return fleteBsPendiente;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public String getFecha() {
        return Fechas.soloFecha(fechaCreacion);
    }

    public String getHora() {
        return Fechas.soloHora(fechaCreacion);
    }

    public Integer getPesoBruto() {
        return pesoBruto;
    }

    public Integer getTara() {
        return tara;
    }

    public Integer getPesoNeto() {
        return pesoNeto;
    }

    /** Estado del registro (ACTIVO / INACTIVO). */
    public String getEstado() {
        return estado;
    }

    /** Estado frente a la báscula (EN_PROCESO / FINALIZADO). */
    public String getEstadoBalanza() {
        return estadoBalanza;
    }

    static FilaPesaje deFila(ResultSet r) throws SQLException {
        return new FilaPesaje(
                r.getInt("id"),
                Sql.texto(r, "guia"),
                Sql.texto(r, "lote"),
                Sql.texto(r, "chofer"),
                Sql.texto(r, "placa"),
                Sql.texto(r, "licencia"),
                Sql.texto(r, "tipo"),
                Sql.texto(r, "proveedor"),
                Sql.texto(r, "producto"),
                Sql.texto(r, "origen"),
                Sql.texto(r, "destino"),
                Sql.entero(r, "costo_bs"),
                Sql.entero(r, "flete_bs_pendiente"),
                Sql.texto(r, "fecha_creacion"),
                Sql.entero(r, "peso_bruto"),
                Sql.entero(r, "tara"),
                Sql.entero(r, "peso_neto"),
                Sql.texto(r, "estado"),
                Sql.texto(r, "estado_balanza"));
    }
}