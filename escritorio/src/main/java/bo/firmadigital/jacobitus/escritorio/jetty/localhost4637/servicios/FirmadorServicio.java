package bo.firmadigital.jacobitus.escritorio.jetty.localhost4637.servicios;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.comun.TokenSelected;
import bo.firmadigital.jacobitus.escritorio.formularios.FormAplicacion;
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

    public IRespuestaDto firmarModoSeguro(FirmaModoSeguroDto objetoDto) throws Exception {
        FirmaModoSeguroRespuestaDto respuestaDto = new FirmaModoSeguroRespuestaDto();
        ErrorRespuestaDto errorDto = new ErrorRespuestaDto();

        try {
            ObjectMapper om = new ObjectMapper();

            String format = objetoDto.getFormat();
            if (format == null || !(format.equals("pades") || format.equals("jws"))) {
                throw new JacobitusException("Formato no soportado.");
            }

            String ci = objetoDto.getCi();
            JSONArray archivo = new JSONArray(om.writeValueAsString(objetoDto.getArchivo()));

            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(this.getConfigFirmador());
            Slot[] slots = gestorSlot.listarSlots(false);
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
                respuestaDto.setFiles(lista);
                return respuestaDto;
            } else {
                errorDto.setMessage("Se canceló la firma del documento");
                return errorDto;
            }
        } catch (JSONException | RuntimeException ex) {
            errorDto.setMessage("No se pudo firmar el documento");
            errorDto.setError(ex.getMessage());
            return errorDto;
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

            List<FirmaHashItemDto> archivoItems = objetoDto.getArchivo();
            if (archivoItems == null || archivoItems.isEmpty()) {
                throw new JacobitusException("El campo archivo es obligatorio.");
            }
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ");
            for (FirmaHashItemDto item : archivoItems) {
                if (item.getBase64() == null || item.getBase64().isEmpty()) {
                    throw new JacobitusException("El campo archivo[].base64 es obligatorio.");
                }
                if (item.getDate() == null) {
                    throw new JacobitusException("El campo archivo[].date es obligatorio.");
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
            TokenSelected dt;
            if (ci != null) {
                dt = FormAplicacion.serviceHash(slots[0], ci, archivo, objetoDto.getDigest());
            } else {
                dt = FormAplicacion.serviceHash(slots[0], null, archivo, objetoDto.getDigest());
            }
            if (dt.getAlias() != null && dt.getPin() != null) {
                List<FirmaHashItemRespuestaDto> lista = Arrays.asList(om.readValue(dt.getFiles().toString(), FirmaHashItemRespuestaDto[].class));
                respuestaDto.setFiles(lista);
                respuestaDto.setCertificate(dt.getCertificate());
                respuestaDto.setCertificateChain(dt.getCertificateChain());
                respuestaDto.setEncryptionAlgorithm(dt.getEncryptionAlgorithm());
                return respuestaDto;
            } else {
                String detalle = dt.getErrorMessage() != null ? dt.getErrorMessage() : "Se canceló la firma del documento";
                errorDto.setMessage(detalle);
                errorDto.setError(detalle);
                return errorDto;
            }
        } catch (JSONException | RuntimeException ex) {
            errorDto.setMessage("No se pudo firmar el documento");
            errorDto.setError(ex.getMessage());
            return errorDto;
        }
    }
}
