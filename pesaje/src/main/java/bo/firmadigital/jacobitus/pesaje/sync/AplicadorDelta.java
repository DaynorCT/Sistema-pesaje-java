package bo.firmadigital.jacobitus.pesaje.sync;

import java.util.List;

import org.codehaus.jackson.JsonNode;

import bo.firmadigital.jacobitus.pesaje.datos.Database;
import bo.firmadigital.jacobitus.pesaje.datos.Fechas;
import bo.firmadigital.jacobitus.pesaje.datos.Sql;

/**
 * Aplica en la base local las filas que baja el servidor.
 *
 * La regla de conflicto es la del sistema original: gana el servidor
 * (last write wins). Cuando la fila local tenia un cambio sin enviar y el
 * servidor tambien la movio, el cambio local se registra en
 * {@code sync_conflictos} para que el operador lo vea.
 */
public final class AplicadorDelta {

    private final Sql sql = Database.obtenerInstancia().sql();

    /**
     * Aplica una fila de catalogo que vino del servidor.
     *
     * @param recurso recurso del servidor (vehiculos, productos, ...)
     * @param fila    fila recibida
     * @return true si la fila se aplico en la base local
     */
    public boolean aplicarCatalogo(String recurso, JsonNode fila) {
        Integer id = Json.entero(fila, "id");
        if (id == null || id <= 0) {
            return false;
        }
        Integer versionServidor = Json.entero(fila, "version");
        if (versionServidor == null) {
            versionServidor = 0;
        }
        int versionLocal = versionLocal(recurso, id);
        boolean pendiente = pendienteLocal(recurso, id);

        AccionVersion accion = SyncPolicy.resolverVersion(versionServidor, versionLocal, pendiente);
        if (accion == AccionVersion.NADA) {
            return false;
        }
        if (accion == AccionVersion.CONFLICTO) {
            registrarConflicto(recurso, id,
                    "version local " + versionLocal + " / version servidor " + versionServidor,
                    fila.toString());
        }

        String ahora = Fechas.ahora();
        switch (recurso) {
            case "vehiculos":
                return aplicarVehiculo(id, fila, versionServidor, ahora);
            case "productos":
            case "origenes":
            case "destinos":
                return aplicarCatalogoSimple(recurso, id, fila, versionServidor, ahora);
            case "choferes":
                return aplicarChofer(id, fila, versionServidor, ahora);
            case "proveedores":
                return aplicarProveedor(id, fila, versionServidor, ahora);
            default:
                return false;
        }
    }

    private boolean aplicarVehiculo(int id, JsonNode fila, int version, String ahora) {
        String placa = valor(fila, "placa");
        String tipo = valor(fila, "tipo_vehiculo");
        Integer tara = Json.entero(fila, "tara");
        String estado = estado(fila);
        boolean existe = existeFila("vehiculos", id);
        if (existe) {
            sql.actualizar(
                    "UPDATE vehiculos SET placa = ?, tipo_vehiculo = ?, tara = ?, estado = ?,"
                            + " version = ?, pendiente_push = 0, fecha_modificacion = ? WHERE id = ?",
                    placa, tipo, tara != null ? tara : 0, estado, version, ahora, id);
            return true;
        }
        sql.insertar(
                "INSERT INTO vehiculos (id, placa, tipo_vehiculo, tara, estado, version,"
                        + " pendiente_push, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
                id, placa, tipo, tara != null ? tara : 0, estado, version, ahora, ahora);
        return true;
    }

    private boolean aplicarCatalogoSimple(String tabla, int id, JsonNode fila, int version, String ahora) {
        String nombre = valor(fila, "nombre");
        String descripcion = valor(fila, "descripcion");
        String estado = estado(fila);
        if (existeFila(tabla, id)) {
            sql.actualizar(
                    "UPDATE " + tabla + " SET nombre = ?, descripcion = ?, estado = ?, version = ?,"
                            + " pendiente_push = 0, fecha_modificacion = ? WHERE id = ?",
                    nombre, descripcion, estado, version, ahora, id);
            return true;
        }
        sql.insertar(
                "INSERT INTO " + tabla + " (id, nombre, descripcion, estado, version,"
                        + " pendiente_push, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, 0, ?, ?)",
                id, nombre, descripcion, estado, version, ahora, ahora);
        return true;
    }

    private boolean aplicarChofer(int id, JsonNode fila, int version, String ahora) {
        Integer personaId = resolverPersona(fila, ahora);
        if (personaId == null) {
            return false;
        }
        String licencia = valor(fila, "licencia");
        String telefono = valor(fila, "telefono");
        String estado = estado(fila);
        if (existeFila("choferes", id)) {
            sql.actualizar(
                    "UPDATE choferes SET persona_id = ?, licencia = ?, telefono = ?, estado = ?,"
                            + " version = ?, pendiente_push = 0, fecha_modificacion = ? WHERE id = ?",
                    personaId, licencia, telefono, estado, version, ahora, id);
            return true;
        }
        sql.insertar(
                "INSERT INTO choferes (id, persona_id, licencia, telefono, estado, version,"
                        + " pendiente_push, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
                id, personaId, licencia, telefono, estado, version, ahora, ahora);
        return true;
    }

    private boolean aplicarProveedor(int id, JsonNode fila, int version, String ahora) {
        Integer personaId = resolverPersona(fila, ahora);
        if (personaId == null) {
            return false;
        }
        String nombreEmpresa = valor(fila, "nombre_empresa");
        String descripcion = valor(fila, "descripcion");
        String estado = estado(fila);
        if (existeFila("proveedores", id)) {
            sql.actualizar(
                    "UPDATE proveedores SET persona_id = ?, nombre_empresa = ?, descripcion = ?,"
                            + " estado = ?, version = ?, pendiente_push = 0, fecha_modificacion = ?"
                            + " WHERE id = ?",
                    personaId, nombreEmpresa, descripcion, estado, version, ahora, id);
            return true;
        }
        sql.insertar(
                "INSERT INTO proveedores (id, persona_id, nombre_empresa, descripcion, estado,"
                        + " version, pendiente_push, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
                id, personaId, nombreEmpresa, descripcion, estado, version, ahora, ahora);
        return true;
    }

    /**
     * Aplica un pesaje que vino del servidor.
     *
     * @param fila fila recibida
     * @return true si se aplico
     */
    public boolean aplicarPesaje(JsonNode fila) {
        Integer id = Json.entero(fila, "id");
        if (id == null || id <= 0) {
            return false;
        }
        String clientEventId = valor(fila, "client_event_id");
        String guia = valor(fila, "guia");
        String ahora = Fechas.ahora();

        Integer vehiculoId = Json.entero(fila, "vehiculo_id");
        if (vehiculoId == null || vehiculoId <= 0) {
            // El esquema local exige vehiculo (NOT NULL), igual que el del
            // sistema original. Un pesaje del servidor sin vehiculo no se
            // puede guardar aca: se registra y se sigue, no se corta la
            // sincronizacion entera.
            registrarConflicto("pesajes", id,
                    "el servidor envio un pesaje sin vehiculo: no se pudo guardar aca",
                    fila.toString());
            return false;
        }

        if (clientEventId != null && !clientEventId.isEmpty()) {
            List<Integer> propios = sql.consultar(
                    "SELECT COUNT(*) FROM pesajes WHERE sync_event_id = ?",
                    r -> r.getInt(1), clientEventId);
            if (!propios.isEmpty() && propios.get(0) > 0) {
                return false;
            }
        }

        Integer bruto = Json.entero(fila, "peso_bruto");
        Integer tara = Json.entero(fila, "tara");
        Integer neto = Json.entero(fila, "peso_neto");
        String estadoBalanza = valor(fila, "estado_balanza");

        List<Integer> existentes = sql.consultar(
                "SELECT COUNT(*) FROM pesajes WHERE web_id = ?", r -> r.getInt(1), id);
        if (!existentes.isEmpty() && existentes.get(0) > 0) {
            sql.actualizar(
                    "UPDATE pesajes SET guia = ?, peso_bruto = ?, tara = ?, peso_neto = ?,"
                            + " estado_balanza = ?, fecha_modificacion = ? WHERE web_id = ?",
                    guia, bruto, tara, neto,
                    estadoBalanza != null ? estadoBalanza : "EN_PROCESO", ahora, id);
            return true;
        }

        // Id negativo: los pesajes de la web no pueden chocar con los ids
        // locales del escritorio, que son positivos.
        sql.insertar(
                "INSERT INTO pesajes (id, guia, web_id, sync_event_id, vehiculo_id, chofer_id,"
                        + " proveedor_id, producto_id, id_origen, id_destino, peso_bruto, tara,"
                        + " peso_neto, costo_bs, flete_bs_pendiente, estado, estado_balanza,"
                        + " origen, sincronizado, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVO', ?, 'WEB', 1, ?, ?)",
                -id, guia, id, clientEventId,
                vehiculoId, Json.entero(fila, "chofer_id"),
                Json.entero(fila, "proveedor_id"), Json.entero(fila, "producto_id"),
                Json.entero(fila, "id_origen"), Json.entero(fila, "id_destino"),
                bruto != null ? bruto : 0, tara != null ? tara : 0, neto != null ? neto : 0,
               (Json.entero(fila, "costo_bs") != null ? Json.entero(fila, "costo_bs") : 0),
                (Json.entero(fila, "flete_bs_pendiente") != null
                        ? Json.entero(fila, "flete_bs_pendiente") : 0),
                estadoBalanza != null ? estadoBalanza : "EN_PROCESO", ahora, ahora);
        return true;
    }

    /**
     * Marca un pesaje local como sincronizado y adopta la guia que le
     * asigno el servidor: a partir de ahi los dos lados muestran la misma.
     *
     * @param pesajeId id del pesaje local
     * @param webId    id que le asigno el servidor
     * @param guia     guia asignada por el servidor
     */
    public void actualizarPesajeSincronizado(int pesajeId, Integer webId, String guia) {
        if (guia != null && !guia.isEmpty()) {
            sql.actualizar(
                    "UPDATE pesajes SET sincronizado = 1, sync_error = NULL, guia = ?, web_id = ?,"
                            + " fecha_modificacion = ? WHERE id = ?",
                    guia, webId, Fechas.ahora(), pesajeId);
            return;
        }
        sql.actualizar(
                "UPDATE pesajes SET sincronizado = 1, sync_error = NULL, web_id = ?,"
                        + " fecha_modificacion = ? WHERE id = ?",
                webId, Fechas.ahora(), pesajeId);
    }

    /**
     * Registra un conflicto: el servidor gano y el cambio local quedo sin
     * enviar.
     *
     * @param recurso      recurso afectado
     * @param entidadId    id de la fila
     * @param detalle      descripcion del conflicto
     * @param servidorJson fila del servidor
     */
    public void registrarConflicto(String recurso, Integer entidadId, String detalle,
            String servidorJson) {
        sql.insertar(
                "INSERT INTO sync_conflictos (recurso, entidad_id, cliente_json, servidor_json,"
                        + " detectado_en) VALUES (?, ?, ?, ?, ?)",
                recurso, entidadId, detalle, servidorJson, Fechas.ahora());
    }

    /**
     * Version local de una fila de catalogo, o -1 si no existe.
     *
     * @param recurso recurso del servidor
     * @param id      id de la fila
     * @return version local
     */
    public int versionLocal(String recurso, int id) {
        List<Integer> versiones = sql.consultar(
                "SELECT version FROM " + recurso + " WHERE id = ?", r -> r.getInt(1), id);
        if (versiones.isEmpty()) {
            return -1;
        }
        Integer valor = versiones.get(0);
        return valor != null ? valor : -1;
    }

    /**
     * Indica si la fila local tiene un cambio sin enviar.
     *
     * @param recurso recurso del servidor
     * @param id      id de la fila
     * @return true si hay cambios pendientes de enviar
     */
    public boolean pendienteLocal(String recurso, int id) {
        List<Integer> valores = sql.consultar(
                "SELECT pendiente_push FROM " + recurso + " WHERE id = ?", r -> r.getInt(1), id);
        return !valores.isEmpty() && valores.get(0) != null && valores.get(0) != 0;
    }

    /**
     * Ultimo cursor de sincronizacion de un recurso.
     *
     * @param recurso recurso del servidor
     * @return cursor, o cadena vacia si todavia no se bajo nada
     */
    public String leerCursor(String recurso) {
        List<String> cursores = sql.consultar(
                "SELECT cursor FROM sync_cursor WHERE recurso = ?", r -> r.getString("cursor"), recurso);
        return cursores.isEmpty() || cursores.get(0) == null ? "" : cursores.get(0);
    }

    /**
     * Guarda el cursor de un recurso.
     *
     * @param recurso recurso del servidor
     * @param valor   cursor devuelto por el servidor
     */
    public void guardarCursor(String recurso, String valor) {
        int afectados = sql.actualizar(
                "UPDATE sync_cursor SET cursor = ?, ultima_sync = ? WHERE recurso = ?",
                valor, Fechas.ahora(), recurso);
        if (afectados == 0) {
            sql.insertar(
                    "INSERT INTO sync_cursor (recurso, cursor, ultima_sync) VALUES (?, ?, ?)",
                    recurso, valor, Fechas.ahora());
        }
    }

    private Integer resolverPersona(JsonNode fila, String ahora) {
        String nombre = valor(fila, "nombre");
        if (nombre == null || nombre.isEmpty()) {
            nombre = valor(fila, "nombre_empresa");
        }
        String apellidoP = valor(fila, "apellido_paterno");
        String apellidoM = valor(fila, "apellido_materno");
        String ci = valor(fila, "ci");
        String telefono = valor(fila, "telefono");

        if (nombre != null && !nombre.isEmpty()) {
            List<Integer> personas = sql.consultar(
                    "SELECT id FROM personas WHERE nombre = ?"
                            + " AND IFNULL(apellido_paterno, '') = ?"
                            + " AND IFNULL(apellido_materno, '') = ? LIMIT 1",
                    r -> r.getInt(1), nombre,
                    apellidoP != null ? apellidoP : "", apellidoM != null ? apellidoM : "");
            if (!personas.isEmpty()) {
                return personas.get(0);
            }
        }
        long personaId = sql.insertar(
                "INSERT INTO personas (nombre, apellido_paterno, apellido_materno, ci, telefono,"
                        + " estado, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, 'ACTIVO', ?, ?)",
                nombre, apellidoP, apellidoM, ci, telefono, ahora, ahora);
        return (int) personaId;
    }

    private boolean existeFila(String tabla, int id) {
        List<Integer> filas = sql.consultar(
                "SELECT COUNT(*) FROM " + tabla + " WHERE id = ?", r -> r.getInt(1), id);
        return !filas.isEmpty() && filas.get(0) > 0;
    }

    private static String valor(JsonNode fila, String campo) {
        String texto = Json.texto(fila, campo);
        return texto == null ? "" : texto;
    }

    private static String estado(JsonNode fila) {
        String estado = valor(fila, "estado");
        return estado.isEmpty() ? "ACTIVO" : estado;
    }
}
