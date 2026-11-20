package bo.firmadigital.jacobitus.pesaje.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Helpers finos sobre la conexion SQLite.
 *
 * Todas las operaciones usan sentencias preparadas: ningun valor se
 * concatena en el texto SQL (decision D3, en contraste con el sistema
 * original Lazarus, que armaba el SQL por concatenacion).
 *
 * Sobre el uso desde hilos: la conexion SQLite no es segura para uso
 * concurrente. Cada operacion de esta clase toma un cerrojo reentrante
 * compartido con {@link Database}, de modo que el acceso queda serializado.
 * Aun asi, la capa de datos no debe usarse desde hilos de fondo: el
 * sincronizador (T11) debera resolver ese escenario por su cuenta.
 *
 * Para leer y escribir valores nulos sin ambiguedad de tipos estan
 * {@link #nulo()}, {@link #nulo(int)} (escritura) y {@link #texto(ResultSet, String)},
 * {@link #entero(ResultSet, String)}, {@link #enteroLargo(ResultSet, String)} y
 * {@link #bandera(ResultSet, String)} (lectura): un NULL de SQL se lee como
 * {@code null} y nunca como 0 ni como cadena vacia.
 */
public final class Sql {

    /**
     * Mapea una fila del resultado a un objeto del dominio.
     *
     * @param <T> tipo del objeto
     */
    @FunctionalInterface
    public interface Mapeador<T> {
        /**
         * Construye el objeto a partir de la fila actual.
         *
         * @param resultado resultado con la posicion sobre la fila
         * @return objeto mapeado
         * @throws SQLException si la lectura de la fila falla
         */
        T mapear(ResultSet resultado) throws SQLException;
    }

    /**
     * Accion que se ejecuta dentro de una transaccion.
     *
     * @param <T> tipo de resultado
     */
    @FunctionalInterface
    public interface Funcion<T> {
        /**
         * Ejecuta la accion con el acceso a SQL de la transaccion en curso.
         *
         * @param sql helpers de SQL de la transaccion
         * @return resultado de la accion
         * @throws SQLException si la operacion falla
         */
        T ejecutar(Sql sql) throws SQLException;
    }

    /**
     * Valor SQL NULL con tipo JDBC explicito.
     */
    public static final class Nulo {
        private final int tipoSql;

        private Nulo(int tipoSql) {
            this.tipoSql = tipoSql;
        }

        /**
         * @return tipo JDBC con el que se enlaza el NULL
         */
        public int tipoSql() {
            return tipoSql;
        }
    }

    private final Database base;

    Sql(Database base) {
        this.base = base;
    }

    /**
     * Consulta filas y las convierte con el mapeador indicado.
     *
     * @param <T>        tipo de los objetos devueltos
     * @param sentencia  SQL con marcadores de posicion
     * @param mapeador   conversion fila -&gt; objeto
     * @param parametros valores de los marcadores
     * @return lista de objetos (vacia si no hay filas)
     */
    public <T> List<T> consultar(String sentencia, Mapeador<T> mapeador, Object... parametros) {
        List<T> resultados = new ArrayList<>();
        base.cerrojo().lock();
        try (PreparedStatement preparacion = base.conexion().prepareStatement(sentencia)) {
            vincular(preparacion, parametros);
            try (ResultSet resultado = preparacion.executeQuery()) {
                while (resultado.next()) {
                    resultados.add(mapeador.mapear(resultado));
                }
            }
            return resultados;
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo ejecutar la consulta: " + sentencia, e);
        } finally {
            base.cerrojo().unlock();
        }
    }

    /**
     * Consulta el resultado de un {@code SELECT COUNT(*)}.
     *
     * @param sentencia  SQL con marcadores de posicion
     * @param parametros valores de los marcadores
     * @return cantidad de filas (0 si no hay resultados)
     */
    public long contar(String sentencia, Object... parametros) {
        List<Long> valores = consultar(sentencia, resultado -> resultado.getLong(1), parametros);
        return valores.isEmpty() ? 0L : valores.get(0);
    }

    /**
     * Ejecuta un INSERT y devuelve el id generado.
     *
     * @param sentencia  SQL con marcadores de posicion
     * @param parametros valores de los marcadores
     * @return id de la fila insertada
     */
    public long insertar(String sentencia, Object... parametros) {
        base.cerrojo().lock();
        try (PreparedStatement preparacion =
                     base.conexion().prepareStatement(sentencia, Statement.RETURN_GENERATED_KEYS)) {
            vincular(preparacion, parametros);
            preparacion.executeUpdate();
            try (ResultSet claves = preparacion.getGeneratedKeys()) {
                if (claves != null && claves.next()) {
                    return claves.getLong(1);
                }
            }
            // Respaldo: algunos escenarios no devuelven claves generadas.
            return ultimoIdInsertado();
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo ejecutar el INSERT: " + sentencia, e);
        } finally {
            base.cerrojo().unlock();
        }
    }

    /**
     * Ejecuta un UPDATE o un DELETE.
     *
     * @param sentencia  SQL con marcadores de posicion
     * @param parametros valores de los marcadores
     * @return cantidad de filas afectadas
     */
    public int actualizar(String sentencia, Object... parametros) {
        base.cerrojo().lock();
        try (PreparedStatement preparacion = base.conexion().prepareStatement(sentencia)) {
            vincular(preparacion, parametros);
            return preparacion.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo ejecutar la actualizacion: " + sentencia, e);
        } finally {
            base.cerrojo().unlock();
        }
    }

    /**
     * Ejecuta una sentencia sin parametros (DDL o sentencia de control).
     *
     * @param sentencia SQL a ejecutar
     */
    public void ejecutar(String sentencia) {
        base.cerrojo().lock();
        try (Statement statement = base.conexion().createStatement()) {
            statement.execute(sentencia);
        } catch (SQLException e) {
            throw new ErrorDeBaseDeDatos("No se pudo ejecutar la sentencia: " + sentencia, e);
        } finally {
            base.cerrojo().unlock();
        }
    }

    /**
     * Ejecuta una accion dentro de una transaccion: se confirma si termina
     * bien y se revierte si lanza una excepcion.
     *
     * @param <T>    tipo de resultado
     * @param accion accion a ejecutar
     * @return resultado de la accion
     */
    public <T> T enTransaccion(Funcion<T> accion) {
        base.cerrojo().lock();
        Connection conexion = base.conexion();
        boolean autocommit;
        try {
            autocommit = conexion.getAutoCommit();
            conexion.setAutoCommit(false);
        } catch (SQLException e) {
            base.cerrojo().unlock();
            throw new ErrorDeBaseDeDatos("No se pudo iniciar la transaccion", e);
        }
        try {
            T resultado = accion.ejecutar(this);
            conexion.commit();
            return resultado;
        } catch (SQLException e) {
            revertir(conexion);
            throw new ErrorDeBaseDeDatos("La transaccion no pudo completarse", e);
        } catch (RuntimeException | Error e) {
            revertir(conexion);
            throw e;
        } finally {
            try {
                conexion.setAutoCommit(autocommit);
            } catch (SQLException e) {
                // La conexion ya esta cerrando: no hay nada que recuperar.
            }
            base.cerrojo().unlock();
        }
    }

    // ───────────────────────────────────────────────────────────────────
    // Valores nulos
    // ───────────────────────────────────────────────────────────────────

    /**
     * Valor NULL sin tipo JDBC especifico.
     *
     * @return marcador de NULL
     */
    public static Nulo nulo() {
        return new Nulo(Types.NULL);
    }

    /**
     * Valor NULL con tipo JDBC explicito (por ejemplo {@link Types#VARCHAR}).
     *
     * @param tipoSql tipo JDBC con el que se enlaza el NULL
     * @return marcador de NULL
     */
    public static Nulo nulo(int tipoSql) {
        return new Nulo(tipoSql);
    }

    /**
     * Lee un texto; un NULL de SQL se devuelve como {@code null}.
     *
     * @param resultado resultado de la consulta
     * @param columna   nombre de la columna
     * @return valor de la columna o {@code null}
     * @throws SQLException si la lectura falla
     */
    public static String texto(ResultSet resultado, String columna) throws SQLException {
        String valor = resultado.getString(columna);
        return resultado.wasNull() ? null : valor;
    }

    /**
     * Lee un entero; un NULL de SQL se devuelve como {@code null} y no como 0.
     *
     * @param resultado resultado de la consulta
     * @param columna   nombre de la columna
     * @return valor de la columna o {@code null}
     * @throws SQLException si la lectura falla
     */
    public static Integer entero(ResultSet resultado, String columna) throws SQLException {
        int valor = resultado.getInt(columna);
        return resultado.wasNull() ? null : valor;
    }

    /**
     * Lee un entero largo; un NULL de SQL se devuelve como {@code null}.
     *
     * @param resultado resultado de la consulta
     * @param columna   nombre de la columna
     * @return valor de la columna o {@code null}
     * @throws SQLException si la lectura falla
     */
    public static Long enteroLargo(ResultSet resultado, String columna) throws SQLException {
        long valor = resultado.getLong(columna);
        return resultado.wasNull() ? null : valor;
    }

    /**
     * Lee un valor booleano guardado como 0/1; un NULL de SQL se lee como false.
     *
     * @param resultado resultado de la consulta
     * @param columna   nombre de la columna
     * @return true si el valor es distinto de cero
     * @throws SQLException si la lectura falla
     */
    public static boolean bandera(ResultSet resultado, String columna) throws SQLException {
        int valor = resultado.getInt(columna);
        return !resultado.wasNull() && valor != 0;
    }

    // ───────────────────────────────────────────────────────────────────
    // Internos
    // ───────────────────────────────────────────────────────────────────

    private long ultimoIdInsertado() throws SQLException {
        try (Statement statement = base.conexion().createStatement();
             ResultSet resultado = statement.executeQuery("SELECT last_insert_rowid()")) {
            return resultado.next() ? resultado.getLong(1) : 0L;
        }
    }

    private void revertir(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            // Se conserva el error original: el rollback es best effort.
        }
    }

    /**
     * Enlaza los parametros de la sentencia preparada.
     *
     * @param preparacion sentencia preparada
     * @param parametros  valores a enlazar
     * @throws SQLException si el enlace falla
     */
    private void vincular(PreparedStatement preparacion, Object... parametros) throws SQLException {
        if (parametros == null) {
            return;
        }
        for (int i = 0; i < parametros.length; i++) {
            Object valor = parametros[i];
            int indice = i + 1;
            if (valor == null) {
                preparacion.setNull(indice, Types.NULL);
            } else if (valor instanceof Nulo) {
                preparacion.setNull(indice, ((Nulo) valor).tipoSql());
            } else if (valor instanceof Integer) {
                preparacion.setInt(indice, (Integer) valor);
            } else if (valor instanceof Long) {
                preparacion.setLong(indice, (Long) valor);
            } else if (valor instanceof Short) {
                preparacion.setInt(indice, ((Short) valor).intValue());
            } else if (valor instanceof Byte) {
                preparacion.setInt(indice, ((Byte) valor).intValue());
            } else if (valor instanceof Double) {
                preparacion.setDouble(indice, (Double) valor);
            } else if (valor instanceof Float) {
                preparacion.setDouble(indice, ((Float) valor).doubleValue());
            } else if (valor instanceof Boolean) {
                preparacion.setInt(indice, ((Boolean) valor) ? 1 : 0);
            } else if (valor instanceof String) {
                preparacion.setString(indice, (String) valor);
            } else if (valor instanceof byte[]) {
                preparacion.setBytes(indice, (byte[]) valor);
            } else if (valor instanceof Date) {
                preparacion.setTimestamp(indice, new Timestamp(((Date) valor).getTime()));
            } else if (valor instanceof LocalDateTime) {
                preparacion.setString(indice, valor.toString());
            } else if (valor instanceof LocalDate) {
                preparacion.setString(indice, valor.toString());
            } else {
                preparacion.setObject(indice, valor);
            }
        }
    }
}
