package bo.firmadigital.jacobitus.pesaje.ui;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import bo.firmadigital.jacobitus.pesaje.ui.comun.Cerrable;

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
        cabecera.getStyleClass().add("tarjeta");

        tituloModulo.getStyleClass().add("titulo-modulo");
        HBox.setHgrow(tituloModulo, javafx.scene.layout.Priority.ALWAYS);

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
        VBox menu = new VBox(2);
        menu.setPadding(new Insets(8));
        menu.getStyleClass().add("tarjeta");
        menu.setVisible(false);

        Label item = new Label(texto);
        item.getStyleClass().add("item-menu");
        item.setOnMouseClicked(event -> {
            StackPane padre = (StackPane) menu.getParent();
            for (Node nodo : padre.getChildren()) {
                nodo.setVisible(nodo == menu);
            }
        });
        menu.getChildren().add(item);

        if (acciones.length > 0) {
            menu.getChildren().add(separador());
        }
        for (Runnable accion : acciones) {
            Label opcion = new Label(opcionDe(accion));
            opcion.getStyleClass().add("item-menu");
            opcion.setOnMouseClicked(event -> {
                menu.setVisible(false);
                accion.run();
            });
            menu.getChildren().add(opcion);
        }

        StackPane contenedor = new StackPane(menu);
        menu.setOnMouseExited(event -> menu.setVisible(false));

        Label boton = new Label(texto);
        boton.getStyleClass().add("item-menu");
        boton.setOnMouseClicked(event -> menu.setVisible(!menu.isVisible()));

        cabeceraUsuario.getChildren().addAll(contenedor, boton);
    }

    private String opcionDe(Runnable accion) {
        if (accion == alCerrarSesion) {
            return "Cerrar sesión";
        }
        if (accion == alVolverAPesaje) {
            return "Volver a pesaje";
        }
        return "Acción";
    }

    private Label separador() {
        Label separador = new Label();
        separador.setMaxWidth(Double.MAX_VALUE);
        separador.setMinHeight(1);
        separador.setStyle("-fx-background-color: #e0e0e0;");
        return separador;
    }
}
