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
    private final String placaVehiculo;
    private final String nombreChofer;
    private final String nombreProveedor;
    private final String nombreProducto;
    private final String origen;
    private final String destino;
    private final Integer pesoBruto;
    private final Integer tara;
    private final Integer pesoNeto;
    private final String fechaCreacion;
    private final String estadoBalanza;

    public FilaPesaje(int id, String guia, String lote, String placaVehiculo, String nombreChofer,
            String nombreProveedor, String nombreProducto, String origen, String destino,
            Integer pesoBruto, Integer tara, Integer pesoNeto, String fechaCreacion,
            String estadoBalanza) {
        this.id = id;
        this.guia = guia;
        this.lote = lote;
        this.placaVehiculo = placaVehiculo;
        this.nombreChofer = nombreChofer;
        this.nombreProveedor = nombreProveedor;
        this.nombreProducto = nombreProducto;
        this.origen = origen;
        this.destino = destino;
        this.pesoBruto = pesoBruto;
        this.tara = tara;
        this.pesoNeto = pesoNeto;
        this.fechaCreacion = fechaCreacion;
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

    public String getPlacaVehiculo() {
        return placaVehiculo;
    }

    public String getNombreChofer() {
        return nombreChofer;
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

    public Integer getPesoBruto() {
        return pesoBruto;
    }

    public Integer getTara() {
        return tara;
    }

    public Integer getPesoNeto() {
        return pesoNeto;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public String getEstadoBalanza() {
        return estadoBalanza;
    }

    static FilaPesaje deFila(ResultSet r) throws SQLException {
        return new FilaPesaje(
                r.getInt("id"),
                Sql.texto(r, "guia"),
                Sql.texto(r, "lote"),
                Sql.texto(r, "placa"),
                Sql.texto(r, "chofer"),
                Sql.texto(r, "proveedor"),
                Sql.texto(r, "producto"),
                Sql.texto(r, "origen"),
                Sql.texto(r, "destino"),
                Sql.entero(r, "peso_bruto"),
                Sql.entero(r, "tara"),
                Sql.entero(r, "peso_neto"),
                Sql.texto(r, "fecha_creacion"),
                Sql.texto(r, "estado_balanza"));
    }
}
