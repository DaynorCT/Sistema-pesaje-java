package bo.firmadigital.jacobitus.pesaje.ui.modulos;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Dialogo breve de captura rapida de un registro.
 *
 * Se usa desde la pantalla de pesaje para dar de alta un vehiculo, un
 * chofer o un proveedor sin salir del flujo, como hacia el sistema
 * original.
 */
public final class DialogoRapido {

    private final TextField[] campos;
    private boolean aceptado;

    /**
     * @param titulo  titulo del dialogo
     * @param etiquetas nombres de los campos, en orden
     * @param valoresIniciales de los campos
     */
    public DialogoRapido(String titulo, String[] etiquetas, String[] valoresIniciales) {
        this.campos = new TextField[etiquetas.length];
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));
        for (int i = 0; i < etiquetas.length; i++) {
            campos[i] = new TextField(valoresIniciales[i] != null ? valoresIniciales[i] : "");
            grid.add(new Label(etiquetas[i]), 0, i);
            grid.add(campos[i], 1, i);
        }

        Button aceptar = new Button("Guardar");
        aceptar.getStyleClass().add("boton-primario");
        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("boton-secundario");

        HBox botones = new HBox(8, aceptar, cancelar);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(10, grid, botones);
        contenido.setPadding(new Insets(8));
        Region region = (Region) dialogo.getDialogPane().lookup(".content");
        if (region != null) {
            region.setPadding(new Insets(0));
        }
        dialogo.getDialogPane().setContent(contenido);

        aceptar.setOnAction(e -> {
            aceptado = true;
            dialogo.close();
        });
        cancelar.setOnAction(e -> dialogo.close());

        dialogo.showAndWait();
    }

    /**
     * @return true si el usuario confirmo el alta
     */
    public boolean mostrar() {
        return aceptado;
    }

    /**
     * @return los valores capturados, en el orden de los campos
     */
    public String[] valores() {
        String[] valores = new String[campos.length];
        for (int i = 0; i < campos.length; i++) {
            valores[i] = campos[i].getText() != null ? campos[i].getText().trim() : "";
        }
        return valores;
    }
}
