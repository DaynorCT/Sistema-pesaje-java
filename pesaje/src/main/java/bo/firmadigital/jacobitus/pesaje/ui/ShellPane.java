package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.EnumMap;
import java.util.List;
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
    private final MenuFlotante menuUsuario = new MenuFlotante();
    private final MenuFlotante menuGrupo = new MenuFlotante();

    private Navegacion moduloActual = Navegacion.INICIO;
    private Runnable alCerrarSesion;
    private Runnable alVolverAPesaje;

    public ShellPane() {
        setTop(construirCabecera());
        setCenter(contenido);
        navegar(Navegacion.INICIO);
    }

    public void setAlCerrarSesion(Runnable accion) {
        this.alCerrarSesion = accion;
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

        ImageView logo = new ImageView(new Image("/logo-agetic.png"));
        logo.setFitHeight(32);
        logo.setPreserveRatio(true);

        HBox modulos = construirMenuHorizontal();
        HBox.setHgrow(modulos, Priority.ALWAYS);

        filaPrincipal.getChildren().addAll(logo, modulos, cabeceraUsuario);

        tituloModulo.getStyleClass().add("titulo-modulo");
        HBox filaTitulo = new HBox(tituloModulo);
        filaTitulo.setPadding(new Insets(0, 16, 10, 16));

        cabecera.getChildren().addAll(filaPrincipal, filaTitulo);
        return cabecera;
    }

    private HBox construirMenuHorizontal() {
        HBox fila = new HBox(4);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("barra-navegacion");

        for (Navegacion modulo : Navegacion.principales()) {
            Label item = crearItemHorizontal(modulo);
            fila.getChildren().add(item);
        }

        ScrollPane scroll = new ScrollPane(fila);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-menu-superior");
        scroll.setPannable(true);

        HBox contenedor = new HBox(scroll);
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

    public void agregarBotonUsuario(String texto, Runnable... acciones) {
        Button menu = new Button(texto);
        menu.getStyleClass().add("boton-secundario");
        menu.setOnAction(event -> alternarMenuUsuario(menu, texto, acciones));
        cabeceraUsuario.getChildren().setAll(menu);
    }

    private void alternarMenuUsuario(Button ancla, String nombre, Runnable... acciones) {
        MenuFlotante.Entrada[] entradas = new MenuFlotante.Entrada[2 + acciones.length];
        int i = 0;
        entradas[i++] = MenuFlotante.encabezado(nombre);
        entradas[i++] = MenuFlotante.separador();
        for (Runnable accion : acciones) {
            entradas[i++] = MenuFlotante.opcion(opcionDe(accion), accion);
        }
        MenuFlotante.Entrada[] compactas = new MenuFlotante.Entrada[i];
        System.arraycopy(entradas, 0, compactas, 0, i);
        menuUsuario.alternar(ancla, compactas);
    }

    private String opcionDe(Runnable accion) {
        if (accion == alCerrarSesion) {
            return "Cerrar sesión";
        }
        if (accion == alVolverAPesaje) {
            return "Volver al modo báscula";
        }
        return "Acción";
    }
}
