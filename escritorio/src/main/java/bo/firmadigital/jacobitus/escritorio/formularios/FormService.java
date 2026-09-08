package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import bo.firmadigital.jacobitus.comun.InfoCertificado;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.comun.ECA;
import bo.firmadigital.jacobitus.escritorio.comun.TokenSelected;
import bo.firmadigital.jacobitus.firmador.FirmadorJws;
import bo.firmadigital.jacobitus.firmador.FirmadorPdf;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.pkcs11.CK_TOKEN_INFO;
import bo.firmadigital.jacobitus.token.GestorSlot;
import bo.firmadigital.jacobitus.token.IToken;
import bo.firmadigital.jacobitus.token.Slot;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

public class FormService extends Stage {
    private final ObservableList<InfoCertificado> listaInfoCertificado;
    private final TokenSelected tokenSelected;
    private final String format;
    private final PasswordField passwordField;
    private final Button button;
    private final ChoiceBox<InfoCertificado> cbInfoCertificado;
    private final Label estado;
    private final ProgressBar progressBar;
    private final Button buttonFirmar;
    private final Label message;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    private String resolverDigest(String digest) {
        if (digest == null) {
            return "SHA256";
        }
        switch (digest.toLowerCase()) {
            case "sha1":
                return "SHA1";
            case "sha224":
                return "SHA224";
            case "sha256":
                return "SHA256";
            case "sha384":
                return "SHA384";
            default:
                return "SHA512";
        }
    }

    public FormService(Stage parent, TokenSelected tokenSelected, String format) {
        setTitle("Pin del token");
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        tokenSelected.setAlias(null);
        tokenSelected.setPin(null);
        this.tokenSelected = tokenSelected;
        this.format = format;
        GridPane root = new GridPane();
        root.setHgap(5);
        root.setVgap(5);
        root.setPadding(new Insets(5, 5, 5, 5));
        Scene scene = new Scene(root, 300, 175 + (tokenSelected.getSlot() == null ? 27 : 0));
        setScene(scene);
        int r = 0;
        if (tokenSelected.getSlot() == null) {
            ObservableList<DetalleToken> tokens = FXCollections.observableArrayList();
            for (Slot slot : tokenSelected.getSlots()) {
                tokens.add(new DetalleToken(slot.detalleToken()));
            }
            ChoiceBox<DetalleToken> tokensChoiceBox = new ChoiceBox<DetalleToken>(tokens);
            tokensChoiceBox.prefWidthProperty().bind(root.widthProperty());
            tokensChoiceBox.setPrefHeight(27);
            tokensChoiceBox.getSelectionModel().selectedIndexProperty()
                    .addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
                        GestorSlot gestorSlot = GestorSlot.getInstance();
                        gestorSlot.setConfigFirmador(this.getConfigFirmador());
                        tokenSelected.setSlot(gestorSlot.obtenerSlot(tokens.get(ov.getValue().intValue()).getSlot()));
                    });
            tokensChoiceBox.getSelectionModel().selectFirst();
            root.add(tokensChoiceBox, 0, 0, 2, 1);
            r++;
        }
        passwordField = new PasswordField();
        passwordField.setPromptText("Su contraseña");
        root.add(passwordField, 0, 0 + r, 1, 1);
        button = new Button("Actualizar");
        AnchorPane anchorPane = new AnchorPane();
        AnchorPane.setTopAnchor(button, 0d);
        AnchorPane.setLeftAnchor(button, 0d);
        AnchorPane.setBottomAnchor(button, 0d);
        AnchorPane.setRightAnchor(button, 0d);
        anchorPane.getChildren().add(button);
        root.add(anchorPane, 1, 0 + r, 1, 1);
        listaInfoCertificado = FXCollections.observableArrayList();
        cbInfoCertificado = new ChoiceBox<InfoCertificado>(listaInfoCertificado);
        cbInfoCertificado.setConverter(new StringConverter<InfoCertificado>() {
            @Override
            public String toString(InfoCertificado infoCertificado) {
                if (infoCertificado == null) {
                    return "";
                }

                if (infoCertificado.getInfoSujeto().getCargo().equals("")) {
                    return infoCertificado.getInfoSujeto().getNombreComun();
                } else {
                    return infoCertificado.getInfoSujeto().getNombreComun() + "\n" + infoCertificado.getInfoSujeto().getCargo();
                }
            }

            @Override
            public InfoCertificado fromString(String string) {
                return null;
            }
        });
        cbInfoCertificado.prefWidthProperty().bind(root.widthProperty());
        cbInfoCertificado.setPrefHeight(55);
        root.add(cbInfoCertificado, 0, 1 + r, 2, 1);
        estado = new Label("Archivos: 0 de " + (
            (tokenSelected.getFiles() == null ? 0 : tokenSelected.getFiles().length()) + 
            (tokenSelected.getFilesJson() == null ? 0 : tokenSelected.getFilesJson().length()) +
            (tokenSelected.getHashArray() == null ? 0 : tokenSelected.getHashArray().length())
        ));
        root.add(estado, 0, 2 + r, 2, 1);
        progressBar = new ProgressBar();
        progressBar.prefWidthProperty().bind(root.widthProperty());
        root.add(progressBar, 0, 3 + r, 2, 1);
        buttonFirmar = new Button("Firmar");
        buttonFirmar.setDisable(true);
        buttonFirmar.prefWidthProperty().bind(root.widthProperty());
        root.add(buttonFirmar, 0, 4 + r, 2, 1);
        message = new Label("");
        root.add(message, 0, 5 + r, 2, 1);

        passwordField.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                this.mostrarCertificados();
            }
        });
        button.setOnAction(t -> {
            this.mostrarCertificados();
        });
        buttonFirmar.setOnAction(t -> {
            this.aplicarFirma();
        });
        FormService window = this;
        this.setOnShown((WindowEvent t) -> {
            window.setAlwaysOnTop(true);
            passwordField.requestFocus();
        });
    }

    public TokenSelected getDatos() {
        return tokenSelected;
    }

    public Task<Boolean> firmar() {
        progressBar.progressProperty().unbind();
        Task<Boolean> task = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                final JSONArray filesPdf = tokenSelected.getFiles() == null ? new JSONArray()
                        : tokenSelected.getFiles();
                final JSONArray filesJson = tokenSelected.getFilesJson() == null ? new JSONArray()
                        : tokenSelected.getFilesJson();
                final JSONArray hashArray = tokenSelected.getHashArray() == null ? new JSONArray()
                        : tokenSelected.getHashArray();
                if (filesPdf.length() + filesJson.length() + hashArray.length() == 0) {
                    updateProgress(100, 100);
                } else {
                    GestorSlot gestorSlot = GestorSlot.getInstance();
                    gestorSlot.setConfigFirmador(getConfigFirmador());
                    IToken token = gestorSlot.obtenerSlot(tokenSelected.getSlot().getSlotID()).getToken();
                    token.iniciar(tokenSelected.getPin());
                    if (tokenSelected.getCI() != null) {
                        InfoCertificado infoCertificado = new InfoCertificado(tokenSelected.getAlias(),
                                token.obtenerCertificado(tokenSelected.getAlias()));
                        String numDocumento = infoCertificado.getInfoSujeto().getNumeroDocumento();
                        if (!infoCertificado.getInfoSujeto().getComplemento().equals("")) {
                            numDocumento += "-" + infoCertificado.getInfoSujeto().getComplemento();
                        }
                        if (!tokenSelected.getCI().equals(numDocumento)) {
                            token.salir();
                            throw new CustomException("El CI del token no corresponde con el CI de la persona para la que se solicitó la firma.");
                        }
                    }
                    if (format.equals("pades_hash")) {
                        X509Certificate certificado = token.obtenerCertificado(tokenSelected.getAlias());
                        tokenSelected.setCertificate(Base64.getEncoder().encodeToString(certificado.getEncoded()));
                        List<String> cadena = new ArrayList<>();
                        for (Certificate c : token.getCertificateChain(tokenSelected.getAlias())) {
                            cadena.add(Base64.getEncoder().encodeToString(c.getEncoded()));
                        }
                        tokenSelected.setCertificateChain(cadena);
                        tokenSelected.setEncryptionAlgorithm(certificado.getPublicKey().getAlgorithm());
                    }
                    if (format == "both") {
                        JSONArray arr = new JSONArray();
                        int i;
                        for (i = 0; i < filesPdf.length(); i++) {
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                String[] base64 = filesPdf.getJSONObject(i).getString("pdf").split("base64,");
                                byte[] file = Base64.getDecoder().decode(base64.length == 2 ? base64[1] : base64[0]);
                                FirmadorPdf.firmar(new ByteArrayInputStream(file), out, false, token,
                                        tokenSelected.getAlias());
                                JSONObject obj = new JSONObject();
                                if (!filesPdf.getJSONObject(i).isNull("pdf")) {
                                    obj.put("id", filesPdf.getJSONObject(i).getString("id"));
                                    obj.put("pdf_firmado", Base64.getEncoder().encodeToString(out.toByteArray()));
                                }
                                arr.put(obj);
                                final int a = i + 1;
                                Platform.runLater(() -> {
                                    estado.setText("Archivos: " + a + " de " + (filesPdf.length() + filesJson.length()));
                                });
                                updateProgress(a, filesPdf.length() + filesJson.length());
                            } catch (RuntimeException ex) {
                                token.salir();
                                throw new CustomException(ex.getMessage());
                            }
                        }
                        tokenSelected.setFiles(arr);

                        JSONArray arrJson = new JSONArray();
                        for (int j = 0; j < filesJson.length(); j++) {
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                String[] base64 = filesJson.getJSONObject(j).getString("json").split("base64,");
                                byte[] file = Base64.getDecoder().decode(base64.length == 2 ? base64[1] : base64[0]);
                                FirmadorJws.firmar(new ByteArrayInputStream(file), out, false, token,
                                        tokenSelected.getAlias());
                                JSONObject obj = new JSONObject();
                                obj.put("id", filesJson.getJSONObject(j).getString("id"));
                                obj.put("json_firmado", Base64.getEncoder().encodeToString(out.toByteArray()));
                                arrJson.put(obj);
                                final int a = i + j + 1;
                                Platform.runLater(() -> {
                                    estado.setText("Archivos: " + a + " de " + (filesPdf.length() + filesJson.length()));
                                });
                                updateProgress(a, filesPdf.length() + filesJson.length());
                            } catch (RuntimeException ex) {
                                token.salir();
                                throw new CustomException(ex.getMessage());
                            }
                        }
                        tokenSelected.setFilesJson(arrJson);
                    } else {
                        /* Compatibilidad Firmatic */
                        JSONArray filesFirmados = new JSONArray();
                        JSONArray files = new JSONArray();
                        switch (format) {
                            case "pades":
                                files = filesPdf;
                                break;
                            case "jws":
                                files = filesJson;
                                break;
                            case "pades_hash":
                                files = hashArray;
                                break;
                            default:
                                throw new JacobitusException(String.format("Formato %s no admitido.", format));                            
                        }
                        int i;
                        for (i = 0; i < files.length(); i++) {
                            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                                if(format.equals("pades_hash")) {
                                    String dateString = files.getJSONObject(i).getString("date");
                                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ");
                                    try {
                                        OffsetDateTime.parse(dateString, dateFormatter);
                                    } catch (DateTimeParseException ex) {
                                        throw new CustomException("Formato de fecha inválido. Formato esperado: yyyy-MM-dd HH:mm:ss.SSSZ");
                                    }
                                }

                                String[] base64 = files.getJSONObject(i).getString("base64").split("base64,");
                                byte[] file = Base64.getDecoder().decode(base64.length == 2 ? base64[1] : base64[0]);
                                String digestName = format.equals("pades_hash") ? resolverDigest(tokenSelected.getDigest()) : null;
                                switch (format) {
                                    case "pades":
                                        FirmadorPdf.firmar(new ByteArrayInputStream(file), out, false, token,
                                                tokenSelected.getAlias());
                                        break;
                                    case "jws":
                                        FirmadorJws.firmar(new ByteArrayInputStream(file), out, false, token,
                                                tokenSelected.getAlias());
                                        break;
                                    case "pades_hash":
                                        Signature signature = Signature.getInstance(digestName + "withRSA");
                                        PrivateKey pk = token.obtenerClavePrivada(tokenSelected.getAlias());
                                        if (pk == null) {
                                            token.salir();
                                            throw new KeyStoreException("No se encontró la clave con alias: " + tokenSelected.getAlias());
                                        }
                                        signature.initSign(pk);
                                        signature.update(file);
                                        byte[] signed = signature.sign();
                                        out.write(signed);
                                        break;
                                    default:
                                        throw new JacobitusException(String.format("Formato %s no admitido.", format));
                                }
                                JSONObject obj = new JSONObject();
                                if (!files.getJSONObject(i).isNull("base64")) {
                                    if (format.equals("pades_hash")) {
                                        obj.put("value", Base64.getEncoder().encodeToString(out.toByteArray()));
                                        obj.put("algorithm", "RSA_" + digestName);
                                    } else {
                                        obj.put("name", files.getJSONObject(i).getString("name"));
                                        obj.put("base64", Base64.getEncoder().encodeToString(out.toByteArray()));
                                    }
                                }
                                filesFirmados.put(obj);
                                final int a = i + 1;
                                Platform.runLater(() -> {
                                    estado.setText("Archivos: " + a + " de " + (filesPdf.length() + filesJson.length() + hashArray.length()));
                                });
                                updateProgress(a, filesPdf.length() + filesJson.length() + hashArray.length());
                            } catch (IllegalArgumentException ex) {
                                token.salir();
                                throw new CustomException("Uno o más parámetros tienen problemas.");
                            } catch (RuntimeException ex) {
                                token.salir();
                                throw new CustomException(ex.getMessage());
                            }
                        }
                        tokenSelected.setFiles(filesFirmados);
                        /* FIN - Compatibilidad Firmatic */
                    }
                    token.salir();
                }
                return true;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnFailed((WorkerStateEvent evt) -> {
            tokenSelected.setAlias(null);
            tokenSelected.setPin(null);
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(this);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            alert.showAndWait();
            if (task.getException() instanceof CustomException) {
                close();
            }
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            close();
        });
        return task;
    }

    private void mostrarCertificados() {
        message.setText("");
        IToken token = tokenSelected.getSlot().getToken();
        try {
            token.iniciar(passwordField.getText());
            List<String> listaAlias = token.listarIdentificadorClaves();
            listaInfoCertificado.clear();
            for (String alias : listaAlias) {
                InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                if (ECA.esValida(infoCertificado) && ECA.esPublica(infoCertificado)) {
                    listaInfoCertificado.add(infoCertificado);
                } else {
                    message.setText("Certificado no emitido por la ECP.");
                    buttonFirmar.setDisable(true);
                }
            }
            token.salir();
            if (listaInfoCertificado.size() > 0) {
                cbInfoCertificado.getSelectionModel().selectFirst();
                message.setText("");
                buttonFirmar.setDisable(false);
                buttonFirmar.requestFocus();
            }
        } catch (GeneralSecurityException ex) {
            try {
                String mensaje = ex.getMessage();
                if (ex.getCause() instanceof IOException) {
                    if (ex.getCause().getMessage() != null && ex.getCause().getMessage().equals("PKCS12 key store mac invalid - wrong password or corrupted file.")) {
                        mensaje = "Pin incorrecto, intente nuevamente.";
                    }
                }
                if (ex instanceof java.security.cert.CertificateExpiredException) {
                    mensaje = "El certificado se encuentra expirado.";
                }
                if (ex instanceof java.security.cert.CertificateNotYetValidException) {
                    mensaje = "El certificado aún no está vigente.";
                }
                if (ex.getCause() instanceof java.security.UnrecoverableKeyException) {
                    if (ex.getCause().getCause() instanceof javax.security.auth.login.FailedLoginException) {
                        mensaje = "Por favor verifique el pin.";
                    }
                }
                if (ex.getCause() instanceof javax.security.auth.login.LoginException) {
                    if (ex.getCause().getCause() != null && ex.getCause().getCause().getMessage() != null && ex.getCause().getCause().getMessage().equals("CKR_PIN_LOCKED")) {
                        mensaje = "El token criptográfico se encuentra bloqueado por demasiados intentos fallidos al ingresar el PIN.";
                    }
                }
                message.setText(mensaje);
            } catch (Exception ex1) {
                message.setText(ex1.getMessage());
            }
        }
    }

    private void aplicarFirma() {
        if (cbInfoCertificado.getValue() instanceof InfoCertificado) {
            tokenSelected.setPin(passwordField.getText());
            tokenSelected.setAlias(((InfoCertificado) cbInfoCertificado.getValue()).getAlias());
            new Thread(firmar()).start();
        }
    }

    private class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }

    private class DetalleToken {
        private final CK_TOKEN_INFO info;

        public DetalleToken(CK_TOKEN_INFO info) {
            this.info = info;
        }

        public long getSlot() {
            return info.getSlot();
        }

        @Override
        public String toString() {
            return info.getLabel();
        }
    }
}
