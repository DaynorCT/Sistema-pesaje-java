package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modulos del sistema y su orden en el menu.
 *
 * El orden y las etiquetas siguen al del sistema original (y al del modulo
 * web). En el escritorio Java el menu principal va en la barra superior.
 */
public enum Navegacion {

    INICIO(0, "Inicio", null),
    USUARIOS(10, "Usuarios", null),
    EMPRESAS(2, "Empresas", null),
    CHOFERES(3, "Choferes", null),
    PROVEEDORES(4, "Proveedores", null),
    PESAJE(1, "Pesaje", null),
    CATALOGO(100, "Catálogo", null),
    VEHICULOS(6, "Vehículos", "Catálogo"),
    PRODUCTOS(5, "Productos", "Catálogo"),
    ORIGENES(7, "Orígenes", "Catálogo"),
    DESTINOS(8, "Destinos", "Catálogo"),
    REPORTES(11, "Reportes", null),
    CONFIG(200, "Configuración", null),
    BOLETA(12, "Boleta", "Configuración"),
    BALANZA(13, "Balanza", "Configuración");

    private final int tag;
    private final String titulo;
    private final String grupo;

    Navegacion(int tag, String titulo, String grupo) {
        this.tag = tag;
        this.titulo = titulo;
        this.grupo = grupo;
    }

    public int getTag() {
        return tag;
    }

    public String getTitulo() {
        return titulo;
    }

    /** @return el grupo del submenu al que pertenece, o null si es de primer nivel */
    public String getGrupo() {
        return grupo;
    }

    public boolean esSubMenu() {
        return grupo != null;
    }

    /**
     * Modulos que aparecen directamente en la barra lateral.
     */
    public static List<Navegacion> principales() {
        List<Navegacion> lista = new ArrayList<>();
        for (Navegacion modulo : values()) {
            if (!modulo.esSubMenu()) {
                lista.add(modulo);
            }
        }
        return lista;
    }

    /**
     * Modulos de un submenu, en el orden en que se muestran.
     */
    public static List<Navegacion> deGrupo(String grupo) {
        List<Navegacion> lista = new ArrayList<>();
        for (Navegacion modulo : values()) {
            if (grupo.equals(modulo.getGrupo())) {
                lista.add(modulo);
            }
        }
        return lista;
    }

    /**
     * Busca un modulo por su etiqueta de navegacion.
     */
    public static Navegacion porTag(int tag) {
        for (Navegacion modulo : values()) {
            if (modulo.tag == tag) {
                return modulo;
            }
        }
        return null;
    }

    /**
     * Orden del menu lateral, con los submenus intercalados en su lugar.
     */
    public static List<Navegacion> ordenMenu() {
        return Arrays.asList(INICIO, USUARIOS, EMPRESAS, CHOFERES, PROVEEDORES, PESAJE,
                CATALOGO, VEHICULOS, PRODUCTOS, ORIGENES, DESTINOS, REPORTES, CONFIG,
                BOLETA, BALANZA);
    }
}
