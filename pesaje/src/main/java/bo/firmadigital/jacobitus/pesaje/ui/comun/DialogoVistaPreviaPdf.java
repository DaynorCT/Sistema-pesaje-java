package bo.firmadigital.jacobitus.pesaje.ui.comun;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import bo.firmadigital.jacobitus.pesaje.reportes.PrevisualizadorPdf;

/**
 * Vista previa de un PDF generado por el sistema (boleta o reporte).
 *
 * Muestra la primera pagina renderizada y deja que el operador abra el
 * archivo con el visor del sistema o cierre el dialogo. El visor no se
 * abre solo: abrirlo al vuelo le roba el foco a la pantalla de pesaje.
 *
 * El render y la apertura viven en {@link PrevisualizadorPdf}, sin
 * JavaFX, para poder probarlos por separado.
 */
public final class DialogoVistaPreviaPdf {

    /** Ancho maximo de la vista previa, en pixeles. */
    private static final double ANCHO_MAXIMO = 860;

    private DialogoVistaPreviaPdf() {
    }

    /**
     * Muestra la vista previa de un PDF.
     *
     * @param ventana ventana propietaria del dialogo; puede ser null
     * @param titulo  titulo del dialogo
     * @param pdf     archivo PDF a previsualizar
     */
    public static void mostrar(Window ventana, String titulo, File pdf) {
        BufferedImage imagen = PrevisualizadorPdf.renderizarPrimeraPagina(pdf);
        if (imagen == null) {
            DialogoAviso.error(ventana, titulo, "No se pudo generar la vista previa del PDF.");
            return;
        }

        Dialog<Void> dialogo = crearDialogo(ventana, titulo);

        ImageView vista = crearVistaPrevia(imagen);

        Label ruta = new Label(pdf.getAbsolutePath());
        ruta.getStyleClass().add("subtitulo");
        ruta.setWrapText(true);
        ruta.setMaxWidth(ANCHO_MAXIMO);

        Label estado = new Label();
        estado.getStyleClass().add("mensaje-error");
        estado.setWrapText(true);
        estado.setVisible(false);

        Button botonAbrir = new Button("Abrir con el visor");
        botonAbrir.getStyleClass().add("boton-primario");
        botonAbrir.setOnAction(e -> abrirEnVisor(dialogo, pdf, estado));

        Button botonCerrar = new Button("Cerrar");
        botonCerrar.getStyleClass().add("boton-secundario");
        botonCerrar.setOnAction(e -> cerrar(dialogo));

        HBox botones = new HBox(8, botonCerrar, botonAbrir);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox(12, vista, ruta, estado, botones);
        contenido.setPadding(new Insets(8));
        dialogo.getDialogPane().setContent(contenido);
        prepararAlMostrar(dialogo);
        dialogo.showAndWait();
    }

    /**
     * Abre el PDF con el visor del sistema.
     *
     * Si el visor no abre el archivo, el dialogo sigue abierto y el
     * error se muestra en la etiqueta de estado.
     *
     * @param dialogo dialogo a cerrar si la apertura funciona
     * @param pdf     archivo PDF a abrir
     * @param estado  etiqueta donde se informa el error
     */
    private static void abrirEnVisor(Dialog<Void> dialogo, File pdf, Label estado) {
        if (PrevisualizadorPdf.abrirConVisor(pdf)) {
            cerrar(dialogo);
            return;
        }
        estado.setText("No se pudo abrir el PDF con el visor del sistema.");
        estado.setVisible(true);
    }

    /**
     * Crea la vista previa de la pagina renderizada.
     *
     * @param imagen imagen de la primera pagina
     * @return la vista previa, ajustada al ancho maximo y con la
     *         proporcion de la pagina
     */
    private static ImageView crearVistaPrevia(BufferedImage imagen) {
        ImageView vista = new ImageView(imagenFx(imagen));
        vista.setPreserveRatio(true);
        double escala = imagen.getWidth() > ANCHO_MAXIMO
                ? ANCHO_MAXIMO / imagen.getWidth()
                : 1.0;
        vista.setFitWidth(imagen.getWidth() * escala);
        vista.setFitHeight(imagen.getHeight() * escala);
        return vista;
    }

    /**
     * Convierte la imagen renderizada en una imagen de JavaFX.
     *
     * @param imagen imagen de la primera pagina
     * @return la imagen de JavaFX, o null si no se puede codificar
     */
    private static Image imagenFx(BufferedImage imagen) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return new Image(new ByteArrayInputStream(salida.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Crea el dialogo base, con el estilo del sistema.
     *
     * @param ventana ventana propietaria; puede ser null
     * @param titulo  titulo del dialogo
     * @return el dialogo, todavia sin mostrar
     */
    private static Dialog<Void> crearDialogo(Window ventana, String titulo) {
        Dialog<Void> dialogo = new Dialog<>();
        EstilosModal.aplicarDialogo(dialogo);
        if (ventana != null) {
            dialogo.initOwner(ventana);
        }
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.setResizable(false);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialogo.setResultConverter(boton -> null);
        return dialogo;
    }

    private static void cerrar(Dialog<Void> dialogo) {
        dialogo.setResult(null);
        dialogo.close();
    }

    /**
     * Oculta la barra de botones del sistema: el dialogo usa botones
     * propios.
     *
     * @param dialogo dialogo a preparar
     */
    private static void prepararAlMostrar(Dialog<Void> dialogo) {
        javafx.event.EventHandler<DialogEvent> previo = dialogo.getOnShowing();
        dialogo.setOnShowing(evento -> {
            if (previo != null) {
                previo.handle(evento);
            }
            Node barra = dialogo.getDialogPane().lookup(".button-bar");
            if (barra != null) {
                barra.setVisible(false);
                barra.setManaged(false);
            }
            Button cancelSistema = (Button) dialogo.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelSistema != null) {
                cancelSistema.setVisible(false);
                cancelSistema.setManaged(false);
                cancelSistema.setOnAction(e -> cerrar(dialogo));
            }
            Region region = (Region) dialogo.getDialogPane().lookup(".content");
            if (region != null) {
                region.setPadding(new Insets(12));
            }
        });
    }
}
