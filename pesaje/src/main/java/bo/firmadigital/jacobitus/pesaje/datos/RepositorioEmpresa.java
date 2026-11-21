package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.List;

public final class RepositorioEmpresa {

    private final Sql sql = Database.obtenerInstancia().sql();

    public List<RegistroEmpresa> listarTodos() {
        return sql.consultar(
                "SELECT id, nombre_empresa, actividad_economica, correo_electronico, telefono,"
                        + " logo, estado FROM empresas ORDER BY id DESC",
                RegistroEmpresa::deFila);
    }

    public long crear(String nombre, String actividad, String correo, String telefono, String logo) {
        String fecha = Fechas.ahora();
        return sql.insertar(
                "INSERT INTO empresas (nombre_empresa, actividad_economica, correo_electronico,"
                        + " telefono, logo, estado, fecha_creacion, fecha_modificacion)"
                        + " VALUES (?, ?, ?, ?, ?, 'ACTIVO', ?, ?)",
                nombre, actividad, correo, telefono, textoLogo(logo), fecha, fecha);
    }

    public void actualizar(int id, String nombre, String actividad, String correo,
            String telefono, String estado, String logo) {
        sql.actualizar(
                "UPDATE empresas SET nombre_empresa = ?, actividad_economica = ?,"
                        + " correo_electronico = ?, telefono = ?, logo = ?, estado = ?,"
                        + " fecha_modificacion = ? WHERE id = ?",
                nombre, actividad, correo, telefono, textoLogo(logo), estado, Fechas.ahora(), id);
    }

    /** Baja lógica: solo cambia {@code estado}. */
    public void cambiarEstado(int id, String nuevoEstado) {
        sql.actualizar(
                "UPDATE empresas SET estado = ?, fecha_modificacion = ? WHERE id = ?",
                nuevoEstado, Fechas.ahora(), id);
    }

    /**
     * Logo de la ultima empresa activa, como data URL.
     *
     * Es la misma regla que usa la boleta en PDF: se toma la empresa
     * activa mas reciente (mayor id). Si no hay empresas, o la empresa
     * activa no tiene logo, se devuelve una cadena vacia.
     *
     * @return data URL del logo, o cadena vacia
     */
    public String logoActivo() {
        try {
            List<String> logos = sql.consultar(
                    "SELECT logo FROM empresas WHERE estado = 'ACTIVO' ORDER BY id DESC LIMIT 1",
                    fila -> fila.getString("logo"));
            if (logos.isEmpty()) {
                return "";
            }
            String logo = logos.get(0);
            return logo == null ? "" : logo;
        } catch (RuntimeException e) {
            // El logo es un dato opcional de la cabecera: si la base no
            // responde, se sigue mostrando el sistema sin logo.
            return "";
        }
    }

    /**
     * Normaliza el logo: sin logo se guarda una cadena vacia.
     *
     * @param logo logo de la empresa
     * @return el logo o una cadena vacia
     */
    private static String textoLogo(String logo) {
        return logo == null ? "" : logo;
    }
}
