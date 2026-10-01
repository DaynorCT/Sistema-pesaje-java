package bo.firmadigital.jacobitus.escritorio.utilidades;

import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;

public final class MenuContextualHelper {

    private MenuContextualHelper() {
    }

    /**
     * Cierra el menú cuando se presiona cualquier punto de la escena propietaria.
     * El ContextMenu vive en una ventana emergente aparte, por lo que los clics en
     * esta escena siempre ocurren fuera del menú.
     */
    public static void cerrarAlHacerClicFuera(ContextMenu menu, Scene escenaPropietaria) {
        menu.setAutoHide(true);
        menu.setConsumeAutoHidingEvents(false);
        escenaPropietaria.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (menu.isShowing()) {
                menu.hide();
            }
        });
    }
}
