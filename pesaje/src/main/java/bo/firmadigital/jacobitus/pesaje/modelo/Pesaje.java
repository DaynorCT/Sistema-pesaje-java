package bo.firmadigital.jacobitus.pesaje.modelo;

/**
 * Un pesaje registrado.
 *
 * Refleja la tabla `pesajes` del modelo de datos: la misma que usa el
 * sistema original, para que la sincronizacion con el servidor web
 * pueda leer y escribir los mismos campos.
 */
public class Pesaje {

    private Integer id;
    private String guia;
    private String lote;
    private Integer vehiculoId;
    private Integer choferId;
    private Integer proveedorId;
    private Integer productoId;
    private Integer origenId;
    private Integer destinoId;
    private Integer pesoBruto;
    private Integer tara;
    private Integer pesoNeto;
    private Integer costoBs;
    private Integer fleteBsPendiente;
    private Integer pesadorId;
    private Estado estado = Estado.ACTIVO;
    private EstadoBalanza estadoBalanza = EstadoBalanza.EN_PROCESO;
    private boolean sincronizado;
    private String syncEventId;
    private String origen = "ESCRITORIO";
    private Integer webId;
    private String syncError;
    private String fechaCreacion;
    private String fechaModificacion;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGuia() {
        return guia;
    }

    public void setGuia(String guia) {
        this.guia = guia;
    }

    public String getLote() {
        return lote;
    }

    public void setLote(String lote) {
        this.lote = lote;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Integer getChoferId() {
        return choferId;
    }

    public void setChoferId(Integer choferId) {
        this.choferId = choferId;
    }

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public Integer getOrigenId() {
        return origenId;
    }

    public void setOrigenId(Integer origenId) {
        this.origenId = origenId;
    }

    public Integer getDestinoId() {
        return destinoId;
    }

    public void setDestinoId(Integer destinoId) {
        this.destinoId = destinoId;
    }

    public Integer getPesoBruto() {
        return pesoBruto;
    }

    public void setPesoBruto(Integer pesoBruto) {
        this.pesoBruto = pesoBruto;
    }

    public Integer getTara() {
        return tara;
    }

    public void setTara(Integer tara) {
        this.tara = tara;
    }

    public Integer getPesoNeto() {
        return pesoNeto;
    }

    public void setPesoNeto(Integer pesoNeto) {
        this.pesoNeto = pesoNeto;
    }

    public Integer getCostoBs() {
        return costoBs;
    }

    public void setCostoBs(Integer costoBs) {
        this.costoBs = costoBs;
    }

    public Integer getFleteBsPendiente() {
        return fleteBsPendiente;
    }

    public void setFleteBsPendiente(Integer fleteBsPendiente) {
        this.fleteBsPendiente = fleteBsPendiente;
    }

    public Integer getPesadorId() {
        return pesadorId;
    }

    public void setPesadorId(Integer pesadorId) {
        this.pesadorId = pesadorId;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado == null ? Estado.ACTIVO : estado;
    }

    public EstadoBalanza getEstadoBalanza() {
        return estadoBalanza;
    }

    public void setEstadoBalanza(EstadoBalanza estadoBalanza) {
        this.estadoBalanza = estadoBalanza == null ? EstadoBalanza.EN_PROCESO : estadoBalanza;
    }

    public boolean isSincronizado() {
        return sincronizado;
    }

    public void setSincronizado(boolean sincronizado) {
        this.sincronizado = sincronizado;
    }

    public String getSyncEventId() {
        return syncEventId;
    }

    public void setSyncEventId(String syncEventId) {
        this.syncEventId = syncEventId;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Integer getWebId() {
        return webId;
    }

    public void setWebId(Integer webId) {
        this.webId = webId;
    }

    public String getSyncError() {
        return syncError;
    }

    public void setSyncError(String syncError) {
        this.syncError = syncError;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(String fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
