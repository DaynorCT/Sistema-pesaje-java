package bo.firmadigital.jacobitus.escritorio.formularios;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.MessageDigest;
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

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DLSet;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertID;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificate;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.IssuerSerial;
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
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class FormService extends Stage {
    private static final String TITULO_VENTANA_PIN = "Pin del token";

    private final ObservableList<InfoCertificado> listaInfoCertificado;
    private final TokenSelected tokenSelected;
    private final String format;
    private final boolean software;
    private final PasswordField passwordField;
    private final Button button;
    private final ChoiceBox<InfoCertificado> cbInfoCertificado;
    private Label estadoToken;
    private final Label estado;
    private final ProgressBar progressBar;
    private final Button buttonFirmar;
    private final Button buttonCancelar;
    private final Label message;
    private boolean firmaEnCurso = false;
    private boolean cancelacionFirmaSolicitada = false;
    private Task<Boolean> tareaFirma;
    private PauseTransition esperaToken;

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    public static String resolverDigest(String digest) {
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

    private static String oidDigest(String digestName) {
        switch (digestName) {
            case "SHA224":
                return "2.16.840.1.101.3.4.2.4";
            case "SHA384":
                return "2.16.840.1.101.3.4.2.2";
            default:
                return "2.16.840.1.101.3.4.2.3";
        }
    }

    public static byte[] firmarHash(IToken token, String alias, byte[] hashBytes, String digestName) throws GeneralSecurityException {
        byte[] toSign = hashBytes;
        try (ASN1InputStream asn1InputStream = new ASN1InputStream(hashBytes)) {
            DLSet dlSet = (DLSet) asn1InputStream.readObject();
            AttributeTable signedAttribute = new AttributeTable(dlSet);

            X509Certificate certificadoFirmante = token.obtenerCertificado(alias);
            org.bouncycastle.asn1.x509.Certificate bcCertificadoFirmante =
                    org.bouncycastle.asn1.x509.Certificate.getInstance(certificadoFirmante.getEncoded());
            IssuerSerial issuerSerial = new IssuerSerial(
                    new GeneralNames(new GeneralName(bcCertificadoFirmante.getIssuer())),
                    certificadoFirmante.getSerialNumber());

            ASN1ObjectIdentifier attributeOid;
            org.bouncycastle.asn1.ASN1Encodable signingCertificateAttribute;
            if ("SHA1".equals(digestName)) {
                byte[] certHash = MessageDigest.getInstance("SHA-1").digest(certificadoFirmante.getEncoded());
                attributeOid = PKCSObjectIdentifiers.id_aa_signingCertificate;
                signingCertificateAttribute = new SigningCertificate(new ESSCertID(certHash, issuerSerial));
            } else if ("SHA256".equals(digestName)) {
                byte[] certHash = MessageDigest.getInstance("SHA-256").digest(certificadoFirmante.getEncoded());
                attributeOid = PKCSObjectIdentifiers.id_aa_signingCertificateV2;
                signingCertificateAttribute = new SigningCertificateV2(new ESSCertIDv2(certHash, issuerSerial));
            } else {
                byte[] certHash = MessageDigest.getInstance(digestName.replace("SHA", "SHA-"))
                        .digest(certificadoFirmante.getEncoded());
                AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(
                        new ASN1ObjectIdentifier(oidDigest(digestName)), DERNull.INSTANCE);
                attributeOid = PKCSObjectIdentifiers.id_aa_signingCertificateV2;
                signingCertificateAttribute = new SigningCertificateV2(
                        new ESSCertIDv2(algorithmIdentifier, certHash, issuerSerial));
            }

            AttributeTable updatedAttribute = signedAttribute.add(
                    attributeOid, signingCertificateAttribute);

            DERSet signedAttributesData = new DERSet(updatedAttribute.toASN1EncodableVector());
            toSign = signedAttributesData.getEncoded();
        } catch (Exception ex) {
            // No es una estructura ASN.1 SignedAttributes: se firma tal cual llegó
        }

        Signature signature = Signature.getInstance(digestName + "withRSA");
        PrivateKey pk = token.obtenerClavePrivada(alias);
        if (pk == null) {
            token.salir();
            throw new KeyStoreException("Llaves no encontradas en token");
        }
        signature.initSign(pk);
        signature.update(toSign);
        return signature.sign();
    }

    public FormService(Stage parent, TokenSelected tokenSelected, String format) {
        this(parent, tokenSelected, format, false);
    }

    public FormService(Stage parent, TokenSelected tokenSelected, String format, boolean software) {
        IconosVentana.aplicarAplicacion(this);
        setTitle(TITULO_VENTANA_PIN);
        initOwner(parent);
        initModality(Modality.APPLICATION_MODAL);
        tokenSelected.setAlias(null);
        tokenSelected.setPin(null);
        tokenSelected.setErrorMessage(null);
        this.tokenSelected = tokenSelected;
        this.format = format;
        this.software = software;

        VBox root = new VBox(10);
        root.setPrefWidth(430);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setStyle("-fx-background-color: #f7f8fa;");

        Label titulo = new Label("Solicitud de firma digital");
        titulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2933;");

        Label descripcion = new Label("Un servicio solicitó firmar digitalmente " + totalDocumentosTexto() + ".");
        descripcion.setWrapText(true);
        descripcion.setStyle("-fx-text-fill: #3d4852;");
        root.getChildren().addAll(titulo, descripcion, new Separator());

        root.getChildren().add(crearEtiquetaSeccion("Token"));
        if (tokenSelected.getSlot() == null && tokenSelected.getSlots() != null) {
            ObservableList<DetalleToken> tokens = FXCollections.observableArrayList();
            for (Slot slot : tokenSelected.getSlots()) {
                tokens.add(new DetalleToken(slot.detalleToken()));
            }
            ChoiceBox<DetalleToken> tokensChoiceBox = new ChoiceBox<DetalleToken>(tokens);
            tokensChoiceBox.maxWidthProperty().bind(root.widthProperty());
            tokensChoiceBox.setPrefHeight(27);
            tokensChoiceBox.getSelectionModel().selectedIndexProperty()
                    .addListener((ObservableValue<? extends Number> ov, Number t, Number t1) -> {
                        GestorSlot gestorSlot = GestorSlot.getInstance();
                        gestorSlot.setConfigFirmador(this.getConfigFirmador());
                        tokenSelected.setSlot(gestorSlot.obtenerSlot(tokens.get(ov.getValue().intValue()).getSlot()));
                    });
            tokensChoiceBox.getSelectionModel().selectFirst();
            root.getChildren().add(tokensChoiceBox);
        } else if (tokenSelected.getSlot() != null) {
            Label tokenDetalle = new Label(tokenSelected.getSlot().detalleToken().getLabel());
            tokenDetalle.setWrapText(true);
            tokenDetalle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1f2933;");
            root.getChildren().add(tokenDetalle);
        } else {
            estadoToken = new Label("Detectando token...");
            estadoToken.setWrapText(true);
            estadoToken.setStyle("-fx-text-fill: #52616f;");
            root.getChildren().add(estadoToken);
        }

        passwordField = new PasswordField();
        passwordField.setPromptText("Ingrese el PIN");
        if (tokenSelected.getPinInicial() != null) {
            passwordField.setText(tokenSelected.getPinInicial());
        }
        button = new Button("Buscar certificados");
        button.setMinWidth(145);
        button.setDisable(passwordField.getText().isEmpty());
        passwordField.textProperty().addListener((observable, anterior, nuevo) ->
                button.setDisable(nuevo == null || nuevo.isEmpty()));
        HBox pinBox = new HBox(8, passwordField, button);
        pinBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
        passwordField.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().addAll(crearEtiquetaSeccion("PIN del token"), pinBox);

        Label avisoBloqMayus = IndicadorTeclado.crearIndicadorBloqMayus(passwordField);
        root.getChildren().add(avisoBloqMayus);

        listaInfoCertificado = FXCollections.observableArrayList();
        cbInfoCertificado = new ChoiceBox<InfoCertificado>(listaInfoCertificado);
        cbInfoCertificado.setConverter(new StringConverter<InfoCertificado>() {
            @Override
            public String toString(InfoCertificado infoCertificado) {
                if (infoCertificado == null) {
                    return "";
                }

                String nombre = infoCertificado.getInfoSujeto().getNombreComun();
                String alias = infoCertificado.getAlias();
                String serie = infoCertificado.getX509certificado().getSerialNumber().toString();
                String identificador = alias == null || alias.isEmpty() ? serie : alias;
                return nombre + " · " + identificador;
            }

            @Override
            public InfoCertificado fromString(String string) {
                return null;
            }
        });
        cbInfoCertificado.maxWidthProperty().bind(root.widthProperty());
        cbInfoCertificado.setPrefHeight(32);
        cbInfoCertificado.setDisable(true);
        root.getChildren().addAll(crearEtiquetaSeccion("Certificado"), cbInfoCertificado);

        estado = new Label("Documentos: 0 de " + totalDocumentos());
        estado.setStyle("-fx-text-fill: #3d4852;");
        root.getChildren().add(estado);

        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        progressBar.maxWidthProperty().bind(root.widthProperty());
        root.getChildren().add(progressBar);

        message = new Label("Detectando el token conectado...");
        message.setWrapText(true);
        message.setTextFill(Color.web("#5f6c7b"));
        root.getChildren().add(message);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        buttonCancelar = new Button("Cancelar");
        buttonCancelar.setOnAction(t -> {
            if (firmaEnCurso) {
                solicitarCancelacionFirma();
            } else {
                close();
            }
        });
        buttonFirmar = new Button("Firmar documento");
        buttonFirmar.setDisable(true);
        buttonFirmar.setMinWidth(125);
        HBox acciones = new HBox(8, espacio, buttonCancelar, buttonFirmar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(new Separator(), acciones);

        Scene scene = new Scene(root);
        setScene(scene);
        setMinWidth(430);
        setResizable(false);

        passwordField.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                if (!passwordField.getText().isEmpty()) {
                    this.mostrarCertificados(false);
                }
            }
        });
        button.setOnAction(t -> {
            this.mostrarCertificados(false);
        });
        buttonFirmar.setOnAction(t -> {
            this.aplicarFirma();
        });
        FormService window = this;
        this.setOnShown((WindowEvent t) -> {
            window.setAlwaysOnTop(true);

            PauseTransition esperaRegistroWm = new PauseTransition(Duration.millis(100));
            esperaRegistroWm.setOnFinished(e -> {
                ActivadorVentanaX11.activarPorTitulo(TITULO_VENTANA_PIN);
                window.sizeToScene();
                passwordField.requestFocus();
                if (tokenSelected.getSlot() == null && tokenSelected.getSlots() == null) {
                    iniciarBusquedaToken(true);
                }
            });

            esperaRegistroWm.play();
        });
        this.setOnCloseRequest(t -> {
            if (firmaEnCurso) {
                solicitarCancelacionFirma();
                t.consume();
            }
        });
        if (tokenSelected.getSlot() != null && tokenSelected.getPinInicial() != null) {
            try {
                this.mostrarCertificados(true);
            } catch (RuntimeException ex) {
                message.setText("No se pudo validar el PIN automáticamente. Inténtelo manualmente.");
                message.setTextFill(Color.RED);
            }
        }
    }

    private void iniciarBusquedaToken(boolean busquedaInicial) {
        if (busquedaInicial) {
            passwordField.setDisable(true);
            button.setDisable(true);
            message.setText("Detectando el token conectado...");
            message.setTextFill(Color.web("#5f6c7b"));
            if (estadoToken != null) {
                estadoToken.setText("Detectando token...");
                estadoToken.setStyle("-fx-text-fill: #52616f;");
            }
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }

        Task<Slot> task = new Task<Slot>() {
            @Override
            protected Slot call() throws GeneralSecurityException {
                GestorSlot gestorSlot = GestorSlot.getInstance();
                gestorSlot.setConfigFirmador(getConfigFirmador());
                Slot[] slots = gestorSlot.listarSlots(software);
                if (slots.length > 1) {
                    throw new GeneralSecurityException("Por favor conecte solo un token.");
                }
                return slots.length == 1 ? slots[0] : null;
            }
        };
        task.setOnSucceeded(event -> {
            Slot slot = task.getValue();
            if (slot != null) {
                tokenSelected.setSlot(slot);
                progressBar.setProgress(0);
                if (estadoToken != null) {
                    estadoToken.setText("Token detectado: " + slot.detalleToken().getLabel());
                    estadoToken.setStyle("-fx-text-fill: #1b6e3c; -fx-font-weight: bold;");
                }
                passwordField.setDisable(false);
                button.setDisable(passwordField.getText().isEmpty());
                message.setText("Ingrese el PIN para buscar certificados disponibles en el token.");
                if (tokenSelected.getPinInicial() != null) {
                    mostrarCertificados(true);
                }
            } else {
                progressBar.setProgress(0);
                if (estadoToken != null) {
                    estadoToken.setText("No conectado");
                    estadoToken.setStyle("-fx-text-fill: #8a5a00;");
                }
                message.setText("Conecte un token para habilitar el ingreso del PIN.");
                programarBusquedaToken();
            }
        });
        task.setOnFailed(event -> {
            progressBar.setProgress(0);
            String error = task.getException() == null ? null : task.getException().getMessage();
            if ("No se encontró ningún token conectado.".equals(error)) {
                message.setText("Conecte un token para continuar.");
                message.setTextFill(Color.web("#5f6c7b"));
                if (estadoToken != null) {
                    estadoToken.setText("Esperando conexión...");
                    estadoToken.setStyle("-fx-text-fill: #8a5a00;");
                }
            } else {
                message.setText(error == null ? "No se pudo detectar el token." : error);
                message.setTextFill(Color.RED);
                if (estadoToken != null) {
                    estadoToken.setText("No se puede continuar");
                    estadoToken.setStyle("-fx-text-fill: #b42318; -fx-font-weight: bold;");
                }
            }
            programarBusquedaToken();
        });
        new Thread(task).start();
    }

    private void programarBusquedaToken() {
        if (!isShowing()) {
            return;
        }
        esperaToken = new PauseTransition(Duration.seconds(1));
        esperaToken.setOnFinished(event -> {
            if (isShowing() && tokenSelected.getSlot() == null) {
                iniciarBusquedaToken(false);
            }
        });
        esperaToken.play();
    }

    private Label crearEtiquetaSeccion(String texto) {
        Label label = new Label(texto);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #52616f;");
        return label;
    }

    private int totalDocumentos() {
        return (tokenSelected.getFiles() == null ? 0 : tokenSelected.getFiles().length())
                + (tokenSelected.getFilesJson() == null ? 0 : tokenSelected.getFilesJson().length())
                + (tokenSelected.getHashArray() == null ? 0 : tokenSelected.getHashArray().length());
    }

    private String totalDocumentosTexto() {
        int total = totalDocumentos();
        if (total == 1) {
            return "1 documento";
        }
        return total + " documentos";
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
                    try {
                        if (format.equals("pades_hash") && tokenSelected.getCI() != null) {
                            InfoCertificado infoCertificado = new InfoCertificado(tokenSelected.getAlias(),
                                    token.obtenerCertificado(tokenSelected.getAlias()));
                            String numDocumento = infoCertificado.getInfoSujeto().getNumeroDocumento();
                            if (!infoCertificado.getInfoSujeto().getComplemento().equals("")) {
                                numDocumento += "-" + infoCertificado.getInfoSujeto().getComplemento();
                            }
                            if (!tokenSelected.getCI().equals(numDocumento)) {
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
                        if ("both".equals(format)) {
                            JSONArray arr = new JSONArray();
                            int i;
                            for (i = 0; i < filesPdf.length(); i++) {
                                if (cancelacionFirmaSolicitada) {
                                    return false;
                                }
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
                                        estado.setText("Documentos: " + a + " de " + (filesPdf.length() + filesJson.length()));
                                    });
                                    updateProgress(a, filesPdf.length() + filesJson.length());
                                } catch (RuntimeException ex) {
                                    throw new CustomException(ex.getMessage());
                                }
                            }
                            tokenSelected.setFiles(arr);

                            JSONArray arrJson = new JSONArray();
                            for (int j = 0; j < filesJson.length(); j++) {
                                if (cancelacionFirmaSolicitada) {
                                    return false;
                                }
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
                                        estado.setText("Documentos: " + a + " de " + (filesPdf.length() + filesJson.length()));
                                    });
                                    updateProgress(a, filesPdf.length() + filesJson.length());
                                } catch (RuntimeException ex) {
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
                                if (cancelacionFirmaSolicitada) {
                                    return false;
                                }
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
                                            try {
                                                out.write(firmarHash(token, tokenSelected.getAlias(), file, digestName));
                                            } catch (GeneralSecurityException ex) {
                                                throw new CustomException(ex.getMessage());
                                            }
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
                                        estado.setText("Documentos: " + a + " de " + (filesPdf.length() + filesJson.length() + hashArray.length()));
                                    });
                                    updateProgress(a, filesPdf.length() + filesJson.length() + hashArray.length());
                                } catch (IllegalArgumentException ex) {
                                    throw new CustomException("Uno o más parámetros tienen problemas.");
                                } catch (RuntimeException ex) {
                                    throw new CustomException(ex.getMessage());
                                }
                            }
                            tokenSelected.setFiles(filesFirmados);
                            /* FIN - Compatibilidad Firmatic */
                        }
                    } finally {
                        token.salir();
                    }
                }
                return true;
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnFailed((WorkerStateEvent evt) -> {
            firmaEnCurso = false;
            cancelacionFirmaSolicitada = false;
            tareaFirma = null;
            resetearProgreso();
            tokenSelected.setAlias(null);
            tokenSelected.setPin(null);
            tokenSelected.setErrorMessage(task.getException().getMessage());
            passwordField.setDisable(false);
            button.setDisable(passwordField.getText().isEmpty());
            buttonFirmar.setDisable(false);
            cbInfoCertificado.setDisable(false);
            buttonCancelar.setDisable(false);
            Alert alert = new Alert(AlertType.WARNING, task.getException().getMessage(), ButtonType.OK);
            alert.initOwner(this);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.setTitle("Jacobitus");
            IconosVentana.aplicarModal(alert);
            alert.showAndWait();
            if (task.getException() instanceof CustomException) {
                close();
            }
        });
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            firmaEnCurso = false;
            if (!Boolean.TRUE.equals(task.getValue())) {
                tokenSelected.setAlias(null);
                tokenSelected.setPin(null);
            }
            cancelacionFirmaSolicitada = false;
            tareaFirma = null;
            close();
        });
        return task;
    }

    private void mostrarCertificados(boolean autoFirmarSiUnico) {
        message.setText("Buscando certificados en el token...");
        message.setTextFill(Color.web("#5f6c7b"));
        listaInfoCertificado.clear();
        buttonFirmar.setDisable(true);
        cbInfoCertificado.setDisable(true);
        passwordField.setDisable(true);
        button.setDisable(true);
        progressBar.progressProperty().unbind();

        String pin = passwordField.getText();
        Task<ResultadoCertificados> task = new Task<ResultadoCertificados>() {
            @Override
            protected ResultadoCertificados call() throws GeneralSecurityException {
                if (tokenSelected.getSlot() == null) {
                    GestorSlot gestorSlot = GestorSlot.getInstance();
                    gestorSlot.setConfigFirmador(getConfigFirmador());
                    Slot[] slots = gestorSlot.listarSlots(software);
                    if (slots.length != 1) {
                        if (slots.length == 0) {
                            throw new GeneralSecurityException("No se encontró ningún token conectado.");
                        }
                        throw new GeneralSecurityException("Por favor conecte solo un token.");
                    }
                    tokenSelected.setSlot(slots[0]);
                }
                IToken token = tokenSelected.getSlot().getToken();
                token.iniciar(pin);
                try {
                    List<InfoCertificado> encontrados = new ArrayList<>();
                    String mensaje = null;
                    List<String> listaAlias = token.listarIdentificadorClaves();
                    for (String alias : listaAlias) {
                        InfoCertificado infoCertificado = new InfoCertificado(alias, token.obtenerCertificado(alias));
                        if (ECA.esValida(infoCertificado) && ECA.esPublica(infoCertificado)) {
                            if (format.equals("pades_hash")) {
                                encontrados.add(infoCertificado);
                            } else {
                                String numDocumento = infoCertificado.getInfoSujeto().getNumeroDocumento();
                                if (!infoCertificado.getInfoSujeto().getComplemento().equals("")) {
                                    numDocumento += "-" + infoCertificado.getInfoSujeto().getComplemento();
                                }
                                if (tokenSelected.getCI() == null || tokenSelected.getCI().equals(numDocumento)) {
                                    encontrados.add(infoCertificado);
                                } else {
                                    mensaje = "No se encontró ningún certificado para el ci: " + tokenSelected.getCI();
                                }
                            }
                        } else {
                            mensaje = "Certificado no emitido por la ECP.";
                        }
                    }
                    return new ResultadoCertificados(encontrados, mensaje);
                } finally {
                    token.salir();
                }
            }
        };
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded((WorkerStateEvent evt) -> {
            resetearProgreso();
            ResultadoCertificados resultado = task.getValue();
            listaInfoCertificado.addAll(resultado.certificados);
            if (!listaInfoCertificado.isEmpty()) {
                cbInfoCertificado.getSelectionModel().selectFirst();
                message.setText("Seleccione el certificado y confirme la firma.");
                message.setTextFill(Color.web("#5f6c7b"));
                buttonFirmar.requestFocus();
                if (autoFirmarSiUnico && listaInfoCertificado.size() == 1) {
                    PauseTransition delay = new PauseTransition(Duration.millis(1000));
                    delay.setOnFinished(e -> this.aplicarFirma());
                    delay.play();
                } else {
                    buttonFirmar.setDisable(false);
                    cbInfoCertificado.setDisable(false);
                }
            } else {
                message.setText(resultado.mensaje == null ? "" : resultado.mensaje);
                message.setTextFill(Color.RED);
                passwordField.setDisable(false);
                    button.setDisable(passwordField.getText().isEmpty());
            }
        });
        task.setOnFailed((WorkerStateEvent evt) -> {
            resetearProgreso();
            try {
                Throwable ex = task.getException();
                String mensaje = ex.getMessage();
                if (ex.getCause() instanceof IOException) {
                    if (ex.getCause().getMessage() != null && ex.getCause().getMessage().equals("PKCS12 key store mac invalid - wrong password or corrupted file.")) {
                        mensaje = "El PIN es incorrecto. Inténtelo nuevamente.";
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
                        mensaje = "Por favor verifique el PIN.";
                    }
                }
                if (ex.getCause() instanceof javax.security.auth.login.LoginException) {
                    if (ex.getCause().getCause() != null && ex.getCause().getCause().getMessage() != null && ex.getCause().getCause().getMessage().equals("CKR_PIN_LOCKED")) {
                        mensaje = "El token criptográfico se encuentra bloqueado por demasiados intentos fallidos al ingresar el PIN.";
                    }
                }
                message.setText(mensaje);
                message.setTextFill(Color.RED);
            } catch (Exception ex1) {
                message.setText(ex1.getMessage());
                message.setTextFill(Color.RED);
            }
            passwordField.setDisable(false);
            button.setDisable(passwordField.getText().isEmpty());
        });
        new Thread(task).start();
    }

    private void aplicarFirma() {
        if (cbInfoCertificado.getValue() instanceof InfoCertificado) {
            message.setText("Firmando documentos...");
            message.setTextFill(Color.web("#5f6c7b"));
            passwordField.setDisable(true);
            button.setDisable(true);
            buttonFirmar.setDisable(true);
            buttonCancelar.setDisable(false);
            cbInfoCertificado.setDisable(true);
            firmaEnCurso = true;
            cancelacionFirmaSolicitada = false;
            tokenSelected.setPin(passwordField.getText());
            tokenSelected.setAlias(((InfoCertificado) cbInfoCertificado.getValue()).getAlias());
            tareaFirma = firmar();
            new Thread(tareaFirma).start();
        }
    }

    private void solicitarCancelacionFirma() {
        if (tareaFirma != null && firmaEnCurso) {
            message.setText("Cancelando firma, espere a que finalice el documento en curso...");
            message.setTextFill(Color.web("#5f6c7b"));
            buttonCancelar.setDisable(true);
            cancelacionFirmaSolicitada = true;
        }
    }

    private void resetearProgreso() {
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
    }

    private class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }

    private static class ResultadoCertificados {
        private final List<InfoCertificado> certificados;
        private final String mensaje;

        ResultadoCertificados(List<InfoCertificado> certificados, String mensaje) {
            this.certificados = certificados;
            this.mensaje = mensaje;
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
