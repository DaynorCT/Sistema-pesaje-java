package bo.firmadigital.jacobitus.escritorio.formularios;

import java.util.Optional;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Alerta previa a la firma cuando se detectan problemas de estructura o integridad en el PDF.
 */
public final class AlertaEstructuraPdf {

  private static final int COUNTDOWN_SEGUNDOS = 10;

  private AlertaEstructuraPdf() {}

  /**
   * @return true si el usuario elige firmar de todos modos (tras el countdown); false si cancela.
   */
  public static boolean confirmarFirmaConAdvertencias(Stage owner, String detalleAdvertencias) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText("Revise estos documentos antes de firmar");

    TextArea areaDetalle = new TextArea(detalleAdvertencias);
    areaDetalle.setEditable(false);
    areaDetalle.setWrapText(true);
    areaDetalle.setPrefRowCount(Math.min(12, detalleAdvertencias.split("\n", -1).length + 1));
    areaDetalle.setMaxWidth(Double.MAX_VALUE);

    Label pie = new Label("La firma podría no ser válida o el documento podría alterarse después.");
    pie.setWrapText(true);
    pie.setMaxWidth(Double.MAX_VALUE);

    DialogPane dialogPane = alert.getDialogPane();
    dialogPane.setContent(new javafx.scene.layout.VBox(12, areaDetalle, pie));

    ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType firmarDeTodosModos =
        new ButtonType("Firmar de todos modos (" + COUNTDOWN_SEGUNDOS + "s)", ButtonBar.ButtonData.OK_DONE);
    dialogPane.getButtonTypes().setAll(cancelar, firmarDeTodosModos);

    Button botonFirmar = (Button) dialogPane.lookupButton(firmarDeTodosModos);
    botonFirmar.setDisable(true);

    final int[] segundosRestantes = {COUNTDOWN_SEGUNDOS};
    Timeline countdown = new Timeline(
        new KeyFrame(
            Duration.seconds(1),
            event -> {
              segundosRestantes[0]--;
              if (segundosRestantes[0] <= 0) {
                botonFirmar.setDisable(false);
                botonFirmar.setText("Firmar de todos modos");
              } else {
                botonFirmar.setText("Firmar de todos modos (" + segundosRestantes[0] + "s)");
              }
            }));
    countdown.setCycleCount(COUNTDOWN_SEGUNDOS);

    alert.setOnShown(event -> countdown.play());
    alert.setOnHidden(event -> countdown.stop());

    Optional<ButtonType> resultado = alert.showAndWait();
    countdown.stop();
    return resultado.isPresent() && resultado.get() == firmarDeTodosModos && !botonFirmar.isDisable();
  }
}
