package bo.firmadigital.jacobitus.pesaje.sync;

/**
 * Que hacer con una fila de catalogo que llega del servidor.
 */
public enum AccionVersion {

    NADA(0),
    APLICAR(1),
    CONFLICTO(2);

    private final int codigo;

    AccionVersion(int codigo) {
        this.codigo = codigo;
    }

    public int getCodigo() {
        return codigo;
    }
}
