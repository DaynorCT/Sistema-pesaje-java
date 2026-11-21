package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 * Ventana principal del sistema: barra lateral con los modulos, barra
 * superior con el modulo activo y el menu de usuario, y el area de
 * contenido donde se muestra el modulo.
 *
 * El recorrido y los nombres de los modulos son los del sistema original.
 */
public class ShellPane extends BorderPane {

    private final StackPane contenido = new StackPane();
    private final Label tituloModulo = new Label();
    private final Map<Navegacion, Label> itemsMenu = new EnumMap<>(Navegacion.class);
    private final Map<String, VBox> subMenus = new HashMap<>();
    private final VBox lateral = new VBox();
    private final HBox cabeceraUsuario = new HBox(8);
    private final MenuFlotante menuUsuario = new MenuFlotante();

    private Navegacion moduloActual = Navegacion.INICIO;
    private Runnable alCerrarSesion;
    private Runnable alVolverAPesaje;

    public ShellPane() {
        setLeft(construirLateral());
        setTop(construirCabecera());
        setCenter(contenido);
        navegar(Navegacion.INICIO);
    }

    /**
     * Define la accion que se ejecuta al cerrar sesion.
     *
     * @param accion accion a ejecutar
     */
    public void setAlCerrarSesion(Runnable accion) {
        this.alCerrarSesion = accion;
    }

    /**
     * Define la accion que se ejecuta al volver a la pantalla de pesaje.
     *
     * @param accion accion a ejecutar
     */
    public void setAlVolverAPesaje(Runnable accion) {
        this.alVolverAPesaje = accion;
    }

    /**
     * Muestra un modulo en el area de contenido.
     *
     * @param modulo modulo a mostrar
     */
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
     * Marca visualmente el modulo activo en el menu.
     *
     * @param modulo modulo activo
     */
    private void marcarActivo(Navegacion modulo) {
        itemsMenu.forEach((clave, item) -> {
            boolean activo = clave == modulo;
            if (activo && !item.getStyleClass().contains("activo")) {
                item.getStyleClass().add("activo");
            } else if (!activo && item.getStyleClass().contains("activo")) {
                item.getStyleClass().remove("activo");
            }
        });
    }

    /**
     * @return el modulo que se esta mostrando
     */
    public Navegacion getModuloActual() {
        return moduloActual;
    }

    private VBox construirLateral() {
        lateral.setPadding(new Insets(12, 8, 12, 8));
        lateral.setSpacing(2);
        lateral.setPrefWidth(232);
        lateral.getStyleClass().add("barra-lateral");

        ImageView logo = new ImageView(new Image("/logo-agetic.png"));
        logo.setFitWidth(180);
        logo.setPreserveRatio(true);
        logo.setTranslateX(10);
        VBox cabecera = new VBox(logo);
        cabecera.setPadding(new Insets(0, 0, 12, 0));

        lateral.getChildren().add(cabecera);

        for (Navegacion modulo : Navegacion.principales()) {
            Label item = crearItem(modulo);
            lateral.getChildren().add(item);
            if (!Navegacion.deGrupo(modulo.getTitulo()).isEmpty()) {
                VBox sub = construirSubMenu(modulo.getTitulo());
                subMenus.put(modulo.getTitulo(), sub);
                lateral.getChildren().add(sub);
                sub.setVisible(false);
            }
        }
        return lateral;
    }

    private Label crearItem(Navegacion modulo) {
        Label item = new Label(modulo.getTitulo());
        item.getStyleClass().add("item-menu");
        item.setMaxWidth(Double.MAX_VALUE);
        item.setOnMouseClicked(event -> {
            if (Navegacion.deGrupo(modulo.getTitulo()).isEmpty()) {
                navegar(modulo);
            } else {
                alternarSubMenu(modulo.getTitulo());
            }
        });
        itemsMenu.put(modulo, item);
        return item;
    }

    private VBox construirSubMenu(String grupo) {
        VBox sub = new VBox(2);
        sub.setPadding(new Insets(0, 0, 6, 16));
        for (Navegacion modulo : Navegacion.deGrupo(grupo)) {
            Label item = new Label("  " + modulo.getTitulo());
            item.getStyleClass().add("item-menu");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnMouseClicked(event -> navegar(modulo));
            itemsMenu.put(modulo, item);
            sub.getChildren().add(item);
        }
        return sub;
    }

    private void alternarSubMenu(String grupo) {
        VBox sub = subMenus.get(grupo);
        if (sub == null) {
            return;
        }
        boolean visible = !sub.isVisible();
        subMenus.values().forEach(otro -> otro.setVisible(false));
        sub.setVisible(visible);
    }

    private HBox construirCabecera() {
        HBox cabecera = new HBox(12);
        cabecera.setPadding(new Insets(10, 16, 10, 16));
        cabecera.setAlignment(Pos.CENTER_LEFT);
        cabecera.getStyleClass().add("barra-superior");

        tituloModulo.getStyleClass().add("titulo-modulo");
        HBox.setHgrow(tituloModulo, Priority.ALWAYS);

        cabecera.getChildren().add(tituloModulo);
        cabecera.getChildren().add(cabeceraUsuario);
        return cabecera;
    }

    /**
     * Agrega el boton de usuario al final de la cabecera.
     *
     * @param texto texto del boton (nombre del usuario)
     * @param acciones acciones del menu de usuario
     */
    public void agregarBotonUsuario(String texto, Runnable... acciones) {
        Button menu = new Button(texto);
        menu.getStyleClass().add("boton-secundario");
        menu.setOnAction(event -> alternarMenuUsuario(menu, texto, acciones));
        cabeceraUsuario.getChildren().setAll(menu);
    }

    private void alternarMenuUsuario(Button ancla, String nombre, Runnable... acciones) {
        MenuFlotante.Entrada[] entradas = new MenuFlotante.Entrada[2 + acciones.length * 2];
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
