package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class RepositorioBoleta {

    private final Sql sql = Database.obtenerInstancia().sql();

    public BoletaFila obtenerActiva() {
        List<BoletaFila> filas = sql.consultar(
                "SELECT * FROM boleta_config ORDER BY id DESC LIMIT 1", BoletaFila::deFila);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public void guardar(BoletaFila fila) {
        String fecha = Fechas.ahora();
        if (fila.id == null || fila.id <= 0) {
            sql.insertar(
                    "INSERT INTO boleta_config (salida, direccion, celular1, celular2, ciudad,"
                            + " titulo_superior, marca, titulo_documento, acreditacion, fecha_modificacion)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    fila.salida, fila.direccion, fila.celular1, fila.celular2, fila.ciudad,
                    fila.tituloSuperior, fila.marca, fila.tituloDocumento, fila.acreditacion, fecha);
            return;
        }
        sql.actualizar(
                "UPDATE boleta_config SET salida = ?, direccion = ?, celular1 = ?, celular2 = ?,"
                        + " ciudad = ?, titulo_superior = ?, marca = ?, titulo_documento = ?,"
                        + " acreditacion = ?, fecha_modificacion = ? WHERE id = ?",
                fila.salida, fila.direccion, fila.celular1, fila.celular2, fila.ciudad,
                fila.tituloSuperior, fila.marca, fila.tituloDocumento, fila.acreditacion, fecha, fila.id);
    }

    public static final class BoletaFila {
        public Integer id;
        public String salida;
        public String direccion;
        public String celular1;
        public String celular2;
        public String ciudad;
        public String tituloSuperior;
        public String marca;
        public String tituloDocumento;
        public String acreditacion;

        static BoletaFila deFila(ResultSet r) throws SQLException {
            BoletaFila f = new BoletaFila();
            f.id = Sql.entero(r, "id");
            f.salida = Sql.texto(r, "salida");
            f.direccion = Sql.texto(r, "direccion");
            f.celular1 = Sql.texto(r, "celular1");
            f.celular2 = Sql.texto(r, "celular2");
            f.ciudad = Sql.texto(r, "ciudad");
            f.tituloSuperior = Sql.texto(r, "titulo_superior");
            f.marca = Sql.texto(r, "marca");
            f.tituloDocumento = Sql.texto(r, "titulo_documento");
            f.acreditacion = Sql.texto(r, "acreditacion");
            return f;
        }
    }
}
