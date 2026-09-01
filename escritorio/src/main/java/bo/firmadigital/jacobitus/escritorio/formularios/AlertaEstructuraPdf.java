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

  public enum Resultado {
    CANCELAR,
    REPARAR_Y_FIRMAR
  }

  // "Reparar y firmar" solo se habilita tras el countdown.
  public static Resultado mostrar(Stage owner, String detalleAdvertencias) {
    boolean confirmado = mostrarConCountdown(
        owner,
        "Revise estos documentos antes de firmar",
        detalleAdvertencias,
        "La firma podría no ser válida o el documento podría alterarse después.",
        "Reparar y firmar");
    return confirmado ? Resultado.REPARAR_Y_FIRMAR : Resultado.CANCELAR;
  }

  // Respaldo cuando la reparación automática no dejó todo íntegro.
  public static boolean mostrarFalloReparacion(Stage owner, String detalleFallos) {
    return mostrarConCountdown(
        owner,
        "No se pudo reparar completamente",
        detalleFallos,
        "Puede cancelar para revisar los documentos, o firmar de todos modos bajo su responsabilidad.",
        "Firmar de todos modos");
  }

  private static boolean mostrarConCountdown(
      Stage owner, String headerText, String detalle, String textoPie, String textoBotonPrincipal) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText(headerText);

    TextArea areaDetalle = new TextArea(detalle);
    areaDetalle.setEditable(false);
    areaDetalle.setWrapText(true);
    areaDetalle.setPrefRowCount(Math.min(12, detalle.split("\n", -1).length + 1));
    areaDetalle.setMaxWidth(Double.MAX_VALUE);

    Label pie = new Label(textoPie);
    pie.setWrapText(true);
    pie.setMaxWidth(Double.MAX_VALUE);

    DialogPane dialogPane = alert.getDialogPane();
    dialogPane.setContent(new javafx.scene.layout.VBox(12, areaDetalle, pie));

    ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
    ButtonType botonPrincipalTipo =
        new ButtonType(textoBotonPrincipal + " (" + COUNTDOWN_SEGUNDOS + "s)", ButtonBar.ButtonData.OK_DONE);
    dialogPane.getButtonTypes().setAll(cancelar, botonPrincipalTipo);

    Button botonPrincipal = (Button) dialogPane.lookupButton(botonPrincipalTipo);
    botonPrincipal.setDisable(true);

    final int[] segundosRestantes = {COUNTDOWN_SEGUNDOS};
    Timeline countdown = new Timeline(
        new KeyFrame(
            Duration.seconds(1),
            event -> {
              segundosRestantes[0]--;
              if (segundosRestantes[0] <= 0) {
                botonPrincipal.setDisable(false);
                botonPrincipal.setText(textoBotonPrincipal);
              } else {
                botonPrincipal.setText(textoBotonPrincipal + " (" + segundosRestantes[0] + "s)");
              }
            }));
    countdown.setCycleCount(COUNTDOWN_SEGUNDOS);

    alert.setOnShown(event -> countdown.play());
    alert.setOnHidden(event -> countdown.stop());

    Optional<ButtonType> resultado = alert.showAndWait();
    countdown.stop();
    return resultado.isPresent() && resultado.get() == botonPrincipalTipo && !botonPrincipal.isDisable();
  }
}
