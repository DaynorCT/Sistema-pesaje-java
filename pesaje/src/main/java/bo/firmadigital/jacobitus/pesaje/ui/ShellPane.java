package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.auth.UsuarioSesion;
import bo.firmadigital.jacobitus.pesaje.datos.LogoEmpresa;
import bo.firmadigital.jacobitus.pesaje.ui.comun.Cerrable;
import bo.firmadigital.jacobitus.pesaje.ui.comun.MenuFlotante;

/**
 * Ventana principal del sistema: menu horizontal arriba (Inicio, Usuarios,
 * Empresas, Choferes, Pesaje, etc.), usuario a la derecha y area de contenido.
 */
public class ShellPane extends BorderPane {

    private final StackPane contenido = new StackPane();
    private final Label tituloModulo = new Label();
    private final Map<Navegacion, Node> itemsMenu = new EnumMap<>(Navegacion.class);
    private final HBox cabeceraUsuario = new HBox(8);
    private final ImageView logoEmpresa = new ImageView();
    private final MenuFlotante menuUsuario = new MenuFlotante();
    private final MenuFlotante menuGrupo = new MenuFlotante();

    private Navegacion moduloActual = Navegacion.INICIO;
    private Runnable alCerrarSesion;
    private Runnable alIrAlaWeb;
    private Runnable alVolverAPesaje;

    public ShellPane() {
        setTop(construirCabecera());
        setCenter(contenido);
        actualizarLogoSistema();
        navegar(Navegacion.INICIO);
    }

    public void setAlCerrarSesion(Runnable accion) {
        this.alCerrarSesion = accion;
    }

    public void setAlIrAlaWeb(Runnable accion) {
        this.alIrAlaWeb = accion;
    }

    public void setAlVolverAPesaje(Runnable accion) {
        this.alVolverAPesaje = accion;
    }

    public void navegar(Navegacion modulo) {
        Node anterior = contenido.getChildren().isEmpty() ? null : contenido.getChildren().get(0);
        if (anterior instanceof Cerrable) {
            ((Cerrable) anterior).cerrar();
        }
        this.moduloActual = modulo;
        tituloModulo.setText(modulo.getTitulo());
        contenido.getChildren().setAll(Modulos.crear(modulo));
        marcarActivo(modulo);
    }

    /**
     * Muestra el logo del sistema en la cabecera, a la derecha del menu.
     *
     * El logo viene con la aplicacion (el mismo del login y de la
     * boleta); si el recurso no esta, la cabecera queda sin imagen.
     */
    public void actualizarLogoSistema() {
        Image imagen = new Image(LogoEmpresa.RECURSO_SISTEMA);
        if (imagen.isError() || imagen.getWidth() <= 0) {
            logoEmpresa.setVisible(false);
            return;
        }
        logoEmpresa.setImage(imagen);
        logoEmpresa.setVisible(true);
    }

    private void marcarActivo(Navegacion modulo) {
        String grupo = modulo.getGrupo();
        itemsMenu.forEach((clave, item) -> {
            boolean activo = clave == modulo
                    || (grupo != null && grupo.equals(clave.getTitulo()));
            if (!item.getStyleClass().contains("item-menu")) {
                item.getStyleClass().add("item-menu");
            }
            if (activo && !item.getStyleClass().contains("activo")) {
                item.getStyleClass().add("activo");
            } else if (!activo && item.getStyleClass().contains("activo")) {
                item.getStyleClass().remove("activo");
            }
        });
    }

    public Navegacion getModuloActual() {
        return moduloActual;
    }

    private VBox construirCabecera() {
        VBox cabecera = new VBox(6);
        cabecera.getStyleClass().add("barra-superior");

        HBox filaPrincipal = new HBox(12);
        filaPrincipal.setPadding(new Insets(10, 16, 6, 16));
        filaPrincipal.setAlignment(Pos.CENTER_LEFT);

        logoEmpresa.setFitHeight(50);
        logoEmpresa.setPreserveRatio(true);
        logoEmpresa.setVisible(false);

        cabeceraUsuario.setAlignment(Pos.CENTER_RIGHT);
        cabeceraUsuario.minHeightProperty().bind(logoEmpresa.fitHeightProperty());

        HBox modulos = construirMenuHorizontal();
        HBox.setHgrow(modulos, Priority.ALWAYS);
        HBox.setMargin(modulos, new Insets(0, 0, 0, 20));

        filaPrincipal.getChildren().addAll(logoEmpresa, modulos, cabeceraUsuario);

        tituloModulo.getStyleClass().add("titulo-modulo");
        HBox filaTitulo = new HBox(tituloModulo);
        filaTitulo.setPadding(new Insets(0, 16, 10, 16));

        cabecera.getChildren().addAll(filaPrincipal, filaTitulo);
        return cabecera;
    }

    private HBox construirMenuHorizontal() {
        HBox fila = new HBox(6);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.minHeightProperty().bind(logoEmpresa.fitHeightProperty());
        fila.getStyleClass().add("barra-navegacion");

        for (Navegacion modulo : Navegacion.principales()) {
            Label item = crearItemHorizontal(modulo);
            fila.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(fila);
        scroll.setFitToHeight(true);
        scroll.prefHeightProperty().bind(logoEmpresa.fitHeightProperty());
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-menu-superior");
        scroll.setPannable(true);

        HBox contenedor = new HBox(scroll);
        contenedor.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        return contenedor;
    }

    private Label crearItemHorizontal(Navegacion modulo) {
        Label item = new Label(modulo.getTitulo());
        item.getStyleClass().add("item-menu");
        item.setOnMouseClicked(event -> {
            List<Navegacion> hijos = Navegacion.deGrupo(modulo.getTitulo());
            if (!hijos.isEmpty()) {
                alternarMenuGrupo(item, modulo.getTitulo(), hijos);
            } else {
                navegar(modulo);
            }
        });
        itemsMenu.put(modulo, item);
        return item;
    }

    private void alternarMenuGrupo(Label ancla, String tituloGrupo, List<Navegacion> hijos) {
        MenuFlotante.Entrada[] entradas = new MenuFlotante.Entrada[hijos.size() + 2];
        int i = 0;
        entradas[i++] = MenuFlotante.encabezado(tituloGrupo);
        entradas[i++] = MenuFlotante.separador();
        for (Navegacion hijo : hijos) {
            entradas[i++] = MenuFlotante.opcion(hijo.getTitulo(), () -> navegar(hijo));
        }
        menuGrupo.alternar(ancla, entradas);
    }

    /**
     * Boton de usuario en la cabecera y menu con rol, nombre, web y cierre.
     */
    public void configurarMenuUsuario(UsuarioSesion usuario) {
        String nombre = usuario.getNombre();
        Button menu = new Button(nombre);
        menu.getStyleClass().addAll("boton-secundario", "boton-usuario-cabecera");
        menu.setOnAction(event -> alternarMenuUsuario(menu, usuario));
        cabeceraUsuario.getChildren().setAll(menu);
    }

    private void alternarMenuUsuario(Button ancla, UsuarioSesion usuario) {
        menuUsuario.alternar(ancla,
                MenuFlotante.encabezado(usuario.getNombre()),
                MenuFlotante.informacion(etiquetaRol(usuario.getRol())),
                MenuFlotante.separador(),
                MenuFlotante.opcion("Ir al sistema web", this::irAlaWeb),
                MenuFlotante.opcion("Volver al modo báscula", this::volverAlModoBascula),
                MenuFlotante.opcion("Cerrar sesión", this::cerrarSesion));
    }

    private void irAlaWeb() {
        if (alIrAlaWeb != null) {
            alIrAlaWeb.run();
        }
    }

    private void volverAlModoBascula() {
        if (alVolverAPesaje != null) {
            alVolverAPesaje.run();
        }
    }

    private void cerrarSesion() {
        if (alCerrarSesion != null) {
            alCerrarSesion.run();
        }
    }

    private static String etiquetaRol(String rol) {
        if (rol == null || rol.isEmpty()) {
            return "Rol: —";
        }
        String normalizado = rol.trim().toLowerCase(Locale.ROOT);
        if (normalizado.isEmpty()) {
            return "Rol: —";
        }
        return "Rol: " + Character.toUpperCase(normalizado.charAt(0))
                + normalizado.substring(1);
    }
}
