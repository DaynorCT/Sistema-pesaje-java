package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.File;
import java.util.List;
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

public final class AlertaEstructuraPdf {

  private static final int COUNTDOWN_SEGUNDOS = 10;

  private AlertaEstructuraPdf() {}

  public enum Resultado {
    CANCELAR,
    REPARAR
  }

  public static void mostrarBloqueoFirma(Stage owner, String detalleAdvertencias) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText("No se puede firmar todavía");
    alert.setContentText(null);
    configurarContenido(
        alert,
        detalleAdvertencias,
        "• Use Archivo → Analizar y reparar PDF para continuar.");
    alert.getButtonTypes().setAll(ButtonType.OK);
    alert.showAndWait();
  }

  public static void mostrarSinProblemas(Stage owner) {
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText(null);
    alert.setContentText("No se detectaron problemas de estructura en los PDF de la lista.");
    alert.getButtonTypes().setAll(ButtonType.OK);
    alert.showAndWait();
  }

  public static Resultado mostrarAnalizarReparar(Stage owner, String detalleAdvertencias) {
    StringBuilder pie = new StringBuilder();
    pie.append("• Se guardará una copia .reparado.pdf en la carpeta que elija; el original no se modifica.\n");
    pie.append("• Puede reparar o cancelar para revisar los documentos manualmente.");
    boolean reparar = mostrarConCountdown(owner, "Se requiere reparar", detalleAdvertencias, pie.toString(), "Reparar");
    return reparar ? Resultado.REPARAR : Resultado.CANCELAR;
  }

  public static void mostrarFalloReparacion(Stage owner, String detalleFallos) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText("No se pudo reparar");
    alert.setContentText(null);
    configurarContenido(alert, detalleFallos, "• Obtenga una nueva copia del documento e intente nuevamente.");
    alert.getButtonTypes().setAll(ButtonType.OK);
    alert.showAndWait();
  }

  public static void mostrarReparacionExitosa(Stage owner, File directorioDestino, List<String> documentosConFirmasPerdidas) {
    StringBuilder mensaje = new StringBuilder();
    mensaje.append("• Los documentos reparados se guardaron en:\n  ").append(directorioDestino.getAbsolutePath());
    mensaje.append("\n• La lista fue actualizada con las copias .reparado.pdf.");
    Alert alert = new Alert(AlertType.INFORMATION);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText(null);
    alert.setContentText(null);
    configurarContenido(alert, mensaje.toString().trim(), "");
    alert.getButtonTypes().setAll(ButtonType.OK);
    alert.showAndWait();
  }

  private static void configurarContenido(Alert alert, String detalle, String textoPie) {
    TextArea areaDetalle = new TextArea(detalle);
    areaDetalle.setEditable(false);
    areaDetalle.setWrapText(true);
    int lineas = detalle.split("\n", -1).length;
    areaDetalle.setPrefRowCount(Math.min(10, Math.max(3, lineas)));
    areaDetalle.setPrefColumnCount(72);
    areaDetalle.setMaxWidth(520);

    Label pie = new Label(textoPie);
    pie.setWrapText(true);
    pie.setMaxWidth(Double.MAX_VALUE);

    if (textoPie == null || textoPie.isEmpty()) {
      alert.getDialogPane().setContent(areaDetalle);
    } else {
      alert.getDialogPane().setContent(new javafx.scene.layout.VBox(12, areaDetalle, pie));
    }
  }

  private static boolean mostrarConCountdown(
      Stage owner, String headerText, String detalle, String textoPie, String textoBotonPrincipal) {
    Alert alert = new Alert(AlertType.WARNING);
    alert.initOwner(owner);
    alert.initModality(Modality.APPLICATION_MODAL);
    alert.setTitle("Jacobitus");
    alert.setHeaderText(headerText);
    alert.setContentText(null);
    configurarContenido(alert, detalle, textoPie);

    DialogPane dialogPane = alert.getDialogPane();
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
