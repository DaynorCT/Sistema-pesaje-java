package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.servicios;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import bo.firmadigital.jacobitus.comun.InfoCertificado;
import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.comun.TokenSelected;
import bo.firmadigital.jacobitus.escritorio.formularios.FormAplicacion;
import bo.firmadigital.jacobitus.escritorio.formularios.FormService;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.ErrorRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashItemDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashItemRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaHashModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.FirmaPdfItemRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.dtos.comun.IRespuestaDto;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.token.GestorSlot;
import bo.firmadigital.jacobitus.token.IToken;
import bo.firmadigital.jacobitus.token.Slot;

public class FirmadorServicio {

    public FirmadorServicio() {
    }

    private ConfiguracionFirmador getConfigFirmador() {
        Config config = Config.getInstance();
        ConfiguracionFirmador configFirmador = new ConfiguracionFirmador();
        configFirmador.setControlador(config.getDriver());
        configFirmador.setSoftoken(config.getToken());
        configFirmador.setDirectorioControladores(config.getDirectorioControladores());
        configFirmador.setDispositivosCompatibles(config.getDispositivosCompatibles());
        return configFirmador;
    }

    public FirmaModoSeguroRespuestaDto firmarModoSeguro(FirmaModoSeguroDto objetoDto) throws Exception {
        FirmaModoSeguroRespuestaDto respuesta = new FirmaModoSeguroRespuestaDto();
        try {
            ObjectMapper om = new ObjectMapper();
            String ci = objetoDto.getCi();
            String format = objetoDto.getFormat();
            JSONArray archivo = new JSONArray(om.writeValueAsString(objetoDto.getArchivo()));

            boolean software = false;
            if (objetoDto.getSoftware() != null) {
                software = objetoDto.getSoftware();
            }
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(this.getConfigFirmador());
            Slot[] slots = gestorSlot.listarSlots(software);
            if (slots.length != 1) {
                throw new JacobitusException("Por favor conecte solo un token.");
            }
            TokenSelected dt;
            if (ci != null) {
                if (format.equals("jws")) {
                    dt = FormAplicacion.serviceJWS(slots[0], ci, archivo);
                } else {
                    dt = FormAplicacion.service(slots[0], ci, archivo);
                }
            } else {
                if (format.equals("jws")) {
                    dt = FormAplicacion.serviceJWS(slots[0], null, archivo);
                } else {
                    dt = FormAplicacion.service(slots[0], null, archivo);
                }
            }
            if (dt.getAlias() != null && dt.getPin() != null) {
                List<FirmaPdfItemRespuestaDto> lista = Arrays.asList(om.readValue(dt.getFiles().toString(), FirmaPdfItemRespuestaDto[].class));
                respuesta.setMessage("Se firmó correctamente");
                respuesta.setFiles(lista);
                return respuesta;
            } else {
                respuesta.setMessage("Se canceló la firma del documento");
                respuesta.setFiles(Collections.emptyList());
                return respuesta;
            }
        } catch (JSONException | RuntimeException ex) {
            respuesta.setMessage("No se pudo firmar el documento");
            respuesta.setError(ex.getMessage());
            return respuesta;
        }
    }

    public IRespuestaDto firmarHashModoSeguro(FirmaHashModoSeguroDto objetoDto) throws Exception {
        FirmaHashModoSeguroRespuestaDto respuestaDto = new FirmaHashModoSeguroRespuestaDto();
        ErrorRespuestaDto errorDto = new ErrorRespuestaDto();

        try {
            ObjectMapper om = new ObjectMapper();

            String format = objetoDto.getFormat();
            if (format == null || !format.equals("pades_hash")) {
                throw new JacobitusException("Formato no soportado.");
            }

            String digest = objetoDto.getDigest();
            if (digest != null && !Arrays.asList("sha1", "sha224", "sha256", "sha384", "sha512").contains(digest.toLowerCase())) {
                throw new JacobitusException("El campo digest no es válido. Valores permitidos: sha1, sha224, sha256, sha384, sha512.");
            }

            String language = objetoDto.getLanguage();
            if (language != null && !language.equals("es")) {
                throw new JacobitusException("El campo language no es válido. Valor permitido: es.");
            }

            List<FirmaHashItemDto> archivoItems = objetoDto.getArchivo();
            if (archivoItems == null || archivoItems.isEmpty()) {
                throw new JacobitusException("El campo archivo es obligatorio.");
            }
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ");
            for (FirmaHashItemDto item : archivoItems) {
                if (item.getBase64() == null || item.getBase64().isEmpty()) {
                    throw new JacobitusException("El campo archivo.base64 o archivo[].base64 es obligatorio.");
                }
                if (item.getDate() == null) {
                    throw new JacobitusException("El campo archivo.date o archivo[].date es obligatorio.");
                }
                try {
                    OffsetDateTime.parse(item.getDate(), dateFormatter);
                } catch (DateTimeParseException ex) {
                    throw new JacobitusException("Formato de fecha inválido. Formato esperado: yyyy-MM-dd HH:mm:ss.SSSZ");
                }
            }

            String ci = objetoDto.getCi();
            JSONArray archivo = new JSONArray(om.writeValueAsString(objetoDto.getArchivo()));

            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(this.getConfigFirmador());
            Slot[] slots = gestorSlot.listarSlots(false);
            if (slots.length != 1) {
                throw new JacobitusException("Por favor conecte solo un token.");
            }

            if (objetoDto.getPin() != null && objetoDto.getAlias() != null) {
                return firmarHashSinInterfaz(objetoDto, archivoItems, slots[0]);
            }

            TokenSelected dt;
            if (ci != null) {
                dt = FormAplicacion.serviceHash(slots[0], ci, archivo, objetoDto.getDigest(), objetoDto.getPin());
            } else {
                dt = FormAplicacion.serviceHash(slots[0], null, archivo, objetoDto.getDigest(), objetoDto.getPin());
            }
            if (dt.getAlias() != null && dt.getPin() != null) {
                List<FirmaHashItemRespuestaDto> lista = Arrays.asList(om.readValue(dt.getFiles().toString(), FirmaHashItemRespuestaDto[].class));
                respuestaDto.setFiles(lista);
                respuestaDto.setCertificate(dt.getCertificate());
                respuestaDto.setCertificateChain(dt.getCertificateChain());
                respuestaDto.setEncryptionAlgorithm(dt.getEncryptionAlgorithm());
                return respuestaDto;
            } else if (dt.getErrorMessage() != null) {
                errorDto.setMessage("No se pudo firmar el documento");
                errorDto.setError(dt.getErrorMessage());
                return errorDto;
            } else {
                errorDto.setMessage("Se canceló la firma del documento");
                errorDto.setError("El usuario canceló el proceso antes de completar la firma.");
                return errorDto;
            }
        } catch (JSONException | RuntimeException ex) {
            String detalle = ex.getMessage();
            if (detalle != null && detalle.startsWith("Ya se tiene una solicitud de firma pendiente")) {
                errorDto.setMessage("Ya se está ejecutando un proceso de firma");
            } else {
                errorDto.setMessage("No se pudo firmar el documento");
            }
            errorDto.setError(detalle);
            return errorDto;
        }
    }

    private IRespuestaDto firmarHashSinInterfaz(FirmaHashModoSeguroDto objetoDto, List<FirmaHashItemDto> archivoItems, Slot slot) {
        FirmaHashModoSeguroRespuestaDto respuestaDto = new FirmaHashModoSeguroRespuestaDto();
        ErrorRespuestaDto errorDto = new ErrorRespuestaDto();
        IToken token = slot.getToken();
        String alias = objetoDto.getAlias();

        try {
            token.iniciar(objetoDto.getPin());
        } catch (GeneralSecurityException ex) {
            errorDto.setMessage("No se pudo firmar el documento");
            errorDto.setError(ex.getMessage());
            return errorDto;
        }

        try {
            X509Certificate certificado = token.obtenerCertificado(alias);
            if (certificado == null) {
                errorDto.setMessage("No se pudo firmar el documento");
                errorDto.setError("No se encontró ningún certificado en el token para el alias: " + alias);
                return errorDto;
            }

            String ci = objetoDto.getCi();
            if (ci != null) {
                InfoCertificado infoCertificado = new InfoCertificado(alias, certificado);
                String numDocumento = infoCertificado.getInfoSujeto().getNumeroDocumento();
                if (!infoCertificado.getInfoSujeto().getComplemento().equals("")) {
                    numDocumento += "-" + infoCertificado.getInfoSujeto().getComplemento();
                }
                if (!ci.equals(numDocumento)) {
                    errorDto.setMessage("No se pudo firmar el documento");
                    errorDto.setError("El CI del token no corresponde con el CI de la persona para la que se solicitó la firma.");
                    return errorDto;
                }
            }

            String digestName = FormService.resolverDigest(objetoDto.getDigest());
            List<FirmaHashItemRespuestaDto> lista = new ArrayList<>();
            for (FirmaHashItemDto item : archivoItems) {
                byte[] hashBytes = Base64.getDecoder().decode(item.getBase64());
                byte[] signed = FormService.firmarHash(token, alias, hashBytes, digestName);
                FirmaHashItemRespuestaDto itemRespuesta = new FirmaHashItemRespuestaDto();
                itemRespuesta.setValue(Base64.getEncoder().encodeToString(signed));
                itemRespuesta.setAlgorithm("RSA_" + digestName);
                lista.add(itemRespuesta);
            }

            List<String> cadena = new ArrayList<>();
            for (Certificate c : token.getCertificateChain(alias)) {
                cadena.add(Base64.getEncoder().encodeToString(c.getEncoded()));
            }

            respuestaDto.setFiles(lista);
            respuestaDto.setCertificate(Base64.getEncoder().encodeToString(certificado.getEncoded()));
            respuestaDto.setCertificateChain(cadena);
            respuestaDto.setEncryptionAlgorithm(certificado.getPublicKey().getAlgorithm());
            return respuestaDto;
        } catch (GeneralSecurityException ex) {
            errorDto.setMessage("No se pudo firmar el documento");
            errorDto.setError(ex.getMessage());
            return errorDto;
        } finally {
            token.salir();
        }
    }
}
