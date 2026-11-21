package bo.firmadigital.jacobitus.pesaje.datos;

import java.util.List;

public final class RepositorioVehiculo {

    private final Sql sql = Database.obtenerInstancia().sql();

    public List<RegistroVehiculo> listarActivos() {
        return sql.consultar(
                "SELECT id, placa, tipo_vehiculo, tara, estado FROM vehiculos WHERE estado = 'ACTIVO'"
                        + " ORDER BY placa",
                RegistroVehiculo::deFila);
    }

    public List<RegistroVehiculo> listarTodos() {
        return sql.consultar(
                "SELECT id, placa, tipo_vehiculo, tara, estado FROM vehiculos ORDER BY id DESC",
                RegistroVehiculo::deFila);
    }

    public long crear(String placa, String tipo, int tara) {
        String fecha = Fechas.ahora();
        return sql.insertar(
                "INSERT INTO vehiculos (placa, tipo_vehiculo, tara, estado, pendiente_push,"
                        + " fecha_creacion, fecha_modificacion) VALUES (?, ?, ?, 'ACTIVO', 1, ?, ?)",
                placa, tipo, tara, fecha, fecha);
    }

    public void actualizar(int id, String placa, String tipo, int tara, String estado) {
        sql.actualizar(
                "UPDATE vehiculos SET placa = ?, tipo_vehiculo = ?, tara = ?, estado = ?,"
                        + " pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                placa, tipo, tara, estado, Fechas.ahora(), id);
    }

    public void cambiarEstado(int id, String nuevoEstado) {
        sql.actualizar(
                "UPDATE vehiculos SET estado = ?, pendiente_push = 1, fecha_modificacion = ? WHERE id = ?",
                nuevoEstado, Fechas.ahora(), id);
    }
}
