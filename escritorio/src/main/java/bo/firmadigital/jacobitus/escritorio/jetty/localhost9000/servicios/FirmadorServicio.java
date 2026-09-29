package bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.servicios;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.xml.security.algorithms.MessageDigestAlgorithm;
import org.apache.xml.security.signature.XMLSignature;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

import bo.firmadigital.jacobitus.comun.JacobitusException;
import bo.firmadigital.jacobitus.comun.CategoriaErrorFirma;
import bo.firmadigital.jacobitus.escritorio.comun.ClasificadorErroresFirma;
import bo.firmadigital.jacobitus.escritorio.comun.Config;
import bo.firmadigital.jacobitus.escritorio.comun.TokenSelected;
import bo.firmadigital.jacobitus.escritorio.formularios.FormAplicacion;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaHashDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaHashRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaJsonDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaJsonItemRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaJsonRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaLotePdfDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaLotePdfRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaModoSeguroDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaModoSeguroRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPdfDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPdfItemDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPdfItemRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPdfRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPkcs7Dto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaPkcs7RespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaXmlDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.FirmaXmlRespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.dtos.comun.RespuestaDto;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.pojos.CompleteSign;
import bo.firmadigital.jacobitus.escritorio.jetty.localhost9000.pojos.Signs;
import bo.firmadigital.jacobitus.firmador.FirmadorPKCS7;
import bo.firmadigital.jacobitus.firmador.FirmadorPdf;
import bo.firmadigital.jacobitus.firmador.FirmadorXml;
import bo.firmadigital.jacobitus.firmador.base.ConfiguracionFirmador;
import bo.firmadigital.jacobitus.firmador.base.IFirmador;
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

    public RespuestaDto<FirmaPdfRespuestaDto> firmarPdf(FirmaPdfDto objetoDto) throws Exception {
        RespuestaDto<FirmaPdfRespuestaDto> respuesta = new RespuestaDto<FirmaPdfRespuestaDto>();

        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            boolean bloquear = objetoDto.getBloquear();
            byte[] file = Base64.getDecoder().decode(objetoDto.getPdf().getBytes(StandardCharsets.UTF_8));
            Integer x = objetoDto.getPoint() != null ? objetoDto.getPoint().getX() : null;
            Integer y = objetoDto.getPoint() != null ? objetoDto.getPoint().getY() : null;
            String image = objetoDto.getImage() != null ? objetoDto.getImage() : null;
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || file == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("Datos requeridos slot, pin, alias y pdf.");
                return respuesta;
            }
            FirmaPdfRespuestaDto datos = new FirmaPdfRespuestaDto();
            respuesta.setDatos(datos);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FirmadorPdf firmar;
            if (x == null) {
                firmar = FirmadorPdf.getInstance(slot, alias, pin, this.getConfigFirmador());
            } else {
                firmar = FirmadorPdf.getInstance(slot, alias, pin, image, x, y, this.getConfigFirmador());
            }
            firmar.firmar(new ByteArrayInputStream(file), out, bloquear);
            datos.setPdf_firmado(Base64.getEncoder().encodeToString(out.toByteArray()));
            respuesta.setFinalizado(true);
            respuesta.setMensaje("Se firmó el PDF correctamente.");
        } catch (GeneralSecurityException | IOException ex) {
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo firmar el PDF.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (OutOfMemoryError err) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.SEVERE, "Error crítico de memoria", err);
            respuesta.setFinalizado(false);
            respuesta.setMensaje("Error interno del servidor (Memoria insuficiente).");
        }

        return respuesta;
    }

    public RespuestaDto<FirmaPkcs7RespuestaDto> firmarPkcs7(FirmaPkcs7Dto objetoDto) throws Exception {
        RespuestaDto<FirmaPkcs7RespuestaDto> respuesta = new RespuestaDto<FirmaPkcs7RespuestaDto>();

        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            Boolean detached = objetoDto.getDetached();
            byte[] file = Base64.getDecoder().decode(objetoDto.getFile().getBytes(StandardCharsets.UTF_8));
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || file == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("Datos requeridos slot, pin, alias y file.");
                return respuesta;
            }
            FirmaPkcs7RespuestaDto datos = new FirmaPkcs7RespuestaDto();
            respuesta.setDatos(datos);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IFirmador firmar = FirmadorPKCS7.getInstance(slot, alias, pin, this.getConfigFirmador());
            firmar.firmar(new ByteArrayInputStream(file), out, detached);
            datos.setPkcs7(Base64.getEncoder().encodeToString(out.toByteArray()));
            respuesta.setFinalizado(true);
            respuesta.setMensaje("Se firmó el archivo correctamente.");
        } catch (GeneralSecurityException | IOException ex) {
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo firmar el archivo PKCS#7.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (OutOfMemoryError err) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.SEVERE, "Error crítico de memoria", err);
            respuesta.setFinalizado(false);
            respuesta.setMensaje("Error interno del servidor (Memoria insuficiente).");
        }

        return respuesta;
    }

    public RespuestaDto<FirmaXmlRespuestaDto> firmarXml(FirmaXmlDto objetoDto) {
        RespuestaDto<FirmaXmlRespuestaDto> respuesta = new RespuestaDto<FirmaXmlRespuestaDto>();

        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            byte[] file = Base64.getDecoder().decode(objetoDto.getFile().getBytes(StandardCharsets.UTF_8));
            String node = objetoDto.getNode();
            String digest = objetoDto.getDigest();
            String signatureAlgorithm = objetoDto.getSignatureAlgorithm();
            Boolean enveloped = objetoDto.getEnveloped();
            Boolean prefix = objetoDto.getPrefix();
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || file == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("Datos requeridos slot, pin, alias y file.");
                return respuesta;
            }
            FirmaXmlRespuestaDto datos = new FirmaXmlRespuestaDto();
            respuesta.setDatos(datos);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FirmadorXml firmar;
            if (node == null) {
                firmar = FirmadorXml.getInstance(slot, alias, pin, this.getConfigFirmador());
            } else {
                firmar = FirmadorXml.getInstance(slot, alias, pin, node, this.getConfigFirmador());
            }

            if (digest != null) {
                switch (digest.toUpperCase()) {
                    case "SHA1":
                        firmar.setMessageDigestAlgorithm(MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA1);
                        break;
                    case "SHA256":
                        firmar.setMessageDigestAlgorithm(MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA256);
                        break;
                    case "SHA512":
                        firmar.setMessageDigestAlgorithm(MessageDigestAlgorithm.ALGO_ID_DIGEST_SHA512);
                        break;
                    default:
                        throw new JacobitusException(CategoriaErrorFirma.CONFIGURACION_FIRMA,
                                CategoriaErrorFirma.CONFIGURACION_FIRMA.getMensajeUsuario(), null);
                }
            }

            if (signatureAlgorithm != null) {
                switch (signatureAlgorithm.toUpperCase()) {
                    case "SHA1":
                        firmar.setSignatureMethodAlgorithm(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
                        break;
                    case "SHA256":
                        firmar.setSignatureMethodAlgorithm(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);
                        break;
                    case "SHA512":
                        firmar.setSignatureMethodAlgorithm(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512);
                        break;
                    default:
                        throw new JacobitusException(CategoriaErrorFirma.CONFIGURACION_FIRMA,
                                CategoriaErrorFirma.CONFIGURACION_FIRMA.getMensajeUsuario(), null);
                }
            }

            firmar.firmar(new ByteArrayInputStream(file), out, enveloped, prefix);
            datos.setXml(Base64.getEncoder().encodeToString(out.toByteArray()));
            respuesta.setFinalizado(true);
            respuesta.setMensaje("Se firmó el archivo correctamente.");
            return respuesta;
        } catch (GeneralSecurityException | IOException ex) {
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo firmar el XML.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (OutOfMemoryError err) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.SEVERE, "Error crítico de memoria", err);
            respuesta.setFinalizado(false);
            respuesta.setMensaje("Error interno del servidor (Memoria insuficiente).");
        }

        return respuesta;
    }

    public RespuestaDto<FirmaJsonRespuestaDto> firmarJson(FirmaJsonDto objetoDto) {
        RespuestaDto<FirmaJsonRespuestaDto> respuesta = new RespuestaDto<FirmaJsonRespuestaDto>();

        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            byte[] data = Base64.getDecoder().decode(objetoDto.getData().getBytes(StandardCharsets.UTF_8));
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || data == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("El slot " + slot + " no se encuentra disponible.");
                return respuesta;
            }
            Slot oSlot = gestorSlot.obtenerSlot(slot);
            if (oSlot == null || oSlot.getToken() == null) {
                throw new JacobitusException(CategoriaErrorFirma.TOKEN_NO_DISPONIBLE,
                        CategoriaErrorFirma.TOKEN_NO_DISPONIBLE.getMensajeUsuario(), null);
            }
            IToken token = oSlot.getToken();
            token.iniciar(pin);
            boolean sesionAbierta = true;
            try {
            PrivateKey pk = token.obtenerClavePrivada(alias);
            if (pk == null) {
                throw new JacobitusException(CategoriaErrorFirma.CLAVE_PRIVADA,
                        CategoriaErrorFirma.CLAVE_PRIVADA.getMensajeUsuario(), null);
            }

            JWSSigner signer = new RSASSASigner(pk);
            boolean inicial = true;

            CompleteSign enviadoJson;
            try {
                String enviado = new String(data);
                ObjectMapper mapper = new ObjectMapper();
                enviadoJson = (CompleteSign) mapper.readValue(enviado, CompleteSign.class);
                inicial = false;
            } catch (IOException ex) {
                enviadoJson = new CompleteSign();
            }

            JWSObject jwsObject;
            if (inicial) {
                jwsObject = new JWSObject((new JWSHeader.Builder(JWSAlgorithm.RS256)).build(), new Payload(data));
                enviadoJson.setPayload(new String(Base64.getEncoder().encode(data), StandardCharsets.UTF_8));
                enviadoJson.setSignatures(new ArrayList<Signs>());
            } else {
                jwsObject = new JWSObject((new JWSHeader.Builder(JWSAlgorithm.RS256)).build(),
                        new Payload(enviadoJson.getPayload()));
            }

            try {
                jwsObject.sign(signer);
            } catch (JOSEException ex) {
                throw new JacobitusException(CategoriaErrorFirma.OTRO,
                        CategoriaErrorFirma.OTRO.getMensajeUsuario(), ex);
            }

            X509Certificate cert = token.obtenerCertificado(alias);
            if (cert == null) {
                throw new JacobitusException(CategoriaErrorFirma.CERTIFICADO_NO_ECP,
                        CategoriaErrorFirma.CERTIFICADO_NO_ECP.getMensajeUsuario(), null);
            }
            X500Name x500Name = new JcaX509CertificateHolder(cert).getSubject();
            String nombreComun = IETFUtils
                    .valueToString(x500Name.getRDNs(new ASN1ObjectIdentifier("2.5.4.3"))[0].getFirst().getValue());
            String pemCert = Base64.getEncoder().encodeToString(cert.getEncoded());
            sesionAbierta = false;
            token.salir();
            Signs sign = new Signs();
            Map<String, Object> mapa = new HashMap<String, Object>();
            mapa.put("gen", "MEFP-DGSGIF");
            mapa.put("x5c", pemCert.replaceAll("(\r\n|\n)", "").toCharArray());
            sign.setHeader(mapa);
            String serial = jwsObject.serialize();
            String[] partes = serial.split("\\.");
            sign.setProtect(partes[0]);
            sign.setSignature(jwsObject.getSignature().toString());
            enviadoJson.getSignatures().add(sign);
            ObjectMapper mapper = new ObjectMapper();
            byte[] bytes = mapper.writeValueAsBytes(enviadoJson);
            InputStream input = new ByteArrayInputStream(bytes);
            BufferedReader buffer = new BufferedReader(new InputStreamReader(input));
            String resultado = (String) buffer.lines().collect(Collectors.joining("\n"));
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            String fechaFirma = dateFormat.format(new Timestamp(calendar.getTime().getTime()));
            FirmaJsonRespuestaDto datos = new FirmaJsonRespuestaDto();
            datos.setJson_firmado(Base64.getEncoder().encodeToString(resultado.getBytes()));
            datos.setCn(nombreComun);
            datos.setFecha_firma(fechaFirma);
            respuesta.setFinalizado(true);
            respuesta.setMensaje("Se firmó la solicitud correctamente.");
            respuesta.setDatos(datos);
            } finally {
                if (sesionAbierta) {
                    token.salir();
                }
            }
        } catch (IOException | GeneralSecurityException ex) {
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo firmar el JSON.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        }

        return respuesta;
    }

    public RespuestaDto<FirmaLotePdfRespuestaDto> firmarLotePdfs(FirmaLotePdfDto objetoDto) {
        RespuestaDto<FirmaLotePdfRespuestaDto> respuesta = new RespuestaDto<FirmaLotePdfRespuestaDto>();
        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            List<FirmaPdfItemDto> pdfs = objetoDto.getPdfs();
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || pdfs == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("Datos requeridos slot, pin, alias y pdfs.");
                return respuesta;
            }
            IToken token = gestorSlot.obtenerSlot(slot).getToken();
            token.iniciar(pin);
            FirmaLotePdfRespuestaDto datos = new FirmaLotePdfRespuestaDto();
            try {
                if (token.obtenerCertificado(alias) == null) {
                    throw new JacobitusException(CategoriaErrorFirma.CERTIFICADO_NO_ECP,
                            CategoriaErrorFirma.CERTIFICADO_NO_ECP.getMensajeUsuario(), null);
                }
                List<FirmaPdfItemRespuestaDto> pdfsFirmados = new ArrayList<FirmaPdfItemRespuestaDto>();
                for (int i = 0; i < pdfs.size(); i++) {
                    boolean bloquear = pdfs.get(i).getBloquear() != null && pdfs.get(i).getBloquear();
                    byte[] file = Base64.getDecoder().decode(pdfs.get(i).getPdf().getBytes(StandardCharsets.UTF_8));

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    FirmadorPdf.firmar(new ByteArrayInputStream(file), out, bloquear, token, alias);
                    FirmaPdfItemRespuestaDto pdfItem = new FirmaPdfItemRespuestaDto();
                    pdfItem.setId(pdfs.get(i).getId());
                    pdfItem.setPdf_firmado(Base64.getEncoder().encodeToString(out.toByteArray()));
                    pdfsFirmados.add(pdfItem);
                }

                datos.setPdfs_firmados(pdfsFirmados);

                respuesta.setDatos(datos);
                respuesta.setFinalizado(true);
                respuesta.setMensaje("Se firmaron los pdfs correctamente!");
            } catch (RuntimeException ex) {
                Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                        "No se pudo completar una firma PDF del lote.", ex);
                respuesta.setFinalizado(false);
                respuesta.setMensaje(procesarExcepcion(ex));
            }
            token.salir();
        } catch (IOException | GeneralSecurityException ex) {
            String mensaje = this.procesarExcepcion(ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(mensaje);
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo completar la firma del lote PDF.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (OutOfMemoryError err) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.SEVERE, "Error crítico de memoria", err);
            respuesta.setFinalizado(false);
            respuesta.setMensaje("Error interno del servidor (Memoria insuficiente).");
        }

        return respuesta;
        
    }

    public RespuestaDto<FirmaHashRespuestaDto> firmarHash(FirmaHashDto objetoDto) {
        RespuestaDto<FirmaHashRespuestaDto> respuesta = new RespuestaDto<FirmaHashRespuestaDto>();
        try {
            Long slot = objetoDto.getSlot();
            String pin = objetoDto.getPin();
            String alias = objetoDto.getAlias();
            String hash = objetoDto.getHash();
            GestorSlot gestorSlot = GestorSlot.getInstance();
            gestorSlot.setConfigFirmador(getConfigFirmador());
            if (slot == null) {
                Slot[] slots = gestorSlot.listarSlots();
                if (slots.length == 1) {
                    slot = slots[0].getSlotID();
                }
            }
            if (slot == null || pin == null || alias == null || hash == null) {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("Datos requeridos slot, pin, alias y hash.");
                return respuesta;
            }
            IToken token = gestorSlot.obtenerSlot(slot).getToken();
            token.iniciar(pin);
            Slot oSlot = gestorSlot.obtenerSlot(objetoDto.getSlot());
            if (oSlot != null) {
                FirmaHashRespuestaDto datos = new FirmaHashRespuestaDto();
                Signature signature = Signature.getInstance("SHA256withRSA");
                PrivateKey pk = token.obtenerClavePrivada(alias);
                if (pk == null) {
                    token.salir();
                    throw new JacobitusException(CategoriaErrorFirma.CLAVE_PRIVADA,
                            CategoriaErrorFirma.CLAVE_PRIVADA.getMensajeUsuario(), null);
                }
                signature.initSign(pk);
                signature.update(Base64.getDecoder().decode(hash));
                byte[] signed = signature.sign();
                token.salir();
                datos.setFirma(Base64.getEncoder().encodeToString(signed));
                respuesta.setDatos(datos);
                respuesta.setFinalizado(true);
                respuesta.setMensaje("Firma realizada correctamente.");
            } else {
                respuesta.setFinalizado(false);
                respuesta.setMensaje("El slot " + slot + " no se encuentra disponible.");
            } 
        } catch (GeneralSecurityException ex) {
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        } catch (RuntimeException ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo firmar el hash.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        }

        return respuesta;
    }

    public RespuestaDto<FirmaModoSeguroRespuestaDto> firmarModoSeguro(FirmaModoSeguroDto objetoDto) throws Exception {
        RespuestaDto<FirmaModoSeguroRespuestaDto> respuesta = new RespuestaDto<FirmaModoSeguroRespuestaDto>();
        GestorSlot gestorSlot = GestorSlot.getInstance();
        gestorSlot.setConfigFirmador(this.getConfigFirmador());
        Slot[] slots = gestorSlot.listarSlots(true);
        TokenSelected tokenSelected;
        try {
            FirmaModoSeguroRespuestaDto datos = new FirmaModoSeguroRespuestaDto();
            try {
                ObjectMapper om = new ObjectMapper();
                JSONArray pdfs, jsons;
                if (objetoDto.getPdfs() != null) {
                    pdfs = new JSONArray(om.writeValueAsString(objetoDto.getPdfs()));
                } else {
                    pdfs = new JSONArray();
                }
                if (objetoDto.getJsons() != null) {
                    jsons = new JSONArray(om.writeValueAsString(objetoDto.getJsons()));
                } else {
                    jsons = new JSONArray();
                }
                if (pdfs.length() + jsons.length() == 0) {
                    throw new JacobitusException(CategoriaErrorFirma.CONFIGURACION_FIRMA,
                            CategoriaErrorFirma.CONFIGURACION_FIRMA.getMensajeUsuario(), null);
                }
                if (objetoDto.getCi() != null) {
                    tokenSelected = FormAplicacion.service(slots, objetoDto.getCi(), pdfs, jsons);
                } else {
                    tokenSelected = FormAplicacion.service(slots, null, pdfs, jsons);
                }
                if (tokenSelected.getAlias() != null && tokenSelected.getPin() != null) {
                    List<FirmaPdfItemRespuestaDto> listaPdf = Arrays.asList(om.readValue(tokenSelected.getFiles().toString(), FirmaPdfItemRespuestaDto[].class));
                    datos.setPdfs_firmados(listaPdf);
                    List<FirmaJsonItemRespuestaDto> listaJson = Arrays.asList(om.readValue(tokenSelected.getFilesJson().toString(), FirmaJsonItemRespuestaDto[].class));
                    datos.setJsons_firmados(listaJson);
                    respuesta.setDatos(datos);
                    respuesta.setFinalizado(true);
                    respuesta.setMensaje("Se firmaron los documentos correctamente!");
                } else {
                    respuesta.setFinalizado(true);
                    respuesta.setMensaje("Se canceló la firma de los documentos");
                }
            } catch (RuntimeException ex) {
                Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                        "No se pudo completar una firma del modo seguro.", ex);
                respuesta.setFinalizado(false);
                respuesta.setMensaje(procesarExcepcion(ex));
            }
        } catch (Exception ex) {
            Logger.getLogger(FirmadorServicio.class.getName()).log(Level.WARNING,
                    "No se pudo completar la firma en modo seguro.", ex);
            respuesta.setFinalizado(false);
            respuesta.setMensaje(procesarExcepcion(ex));
        }
        return respuesta;
    }

    private String procesarExcepcion(Throwable error) {
        return ClasificadorErroresFirma.clasificar(error).getMensajeUsuario();
    }
}
