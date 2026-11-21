package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class RegistroVehiculo {

    private final int id;
    private final String placa;
    private final String tipo;
    private final int tara;
    private final String estado;

    public RegistroVehiculo(int id, String placa, String tipo, int tara, String estado) {
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.tara = tara;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public String getTipo() {
        return tipo;
    }

    public int getTara() {
        return tara;
    }

    public String getEstado() {
        return estado;
    }

    public String etiqueta() {
        return placa + (tipo != null && !tipo.isEmpty() ? " (" + tipo + ")" : "");
    }

    static RegistroVehiculo deFila(ResultSet fila) throws SQLException {
        Integer tara = Sql.entero(fila, "tara");
        return new RegistroVehiculo(
                fila.getInt("id"),
                Sql.texto(fila, "placa"),
                Sql.texto(fila, "tipo_vehiculo"),
                tara != null ? tara : 0,
                Sql.texto(fila, "estado"));
    }
}
