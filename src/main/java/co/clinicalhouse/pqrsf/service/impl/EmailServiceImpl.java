package co.clinicalhouse.pqrsf.service.impl;

import co.clinicalhouse.pqrsf.mail.EmailTemplates;
import co.clinicalhouse.pqrsf.model.Pqrsf;
import co.clinicalhouse.pqrsf.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import co.clinicalhouse.pqrsf.model.PqrsfAdjunto;
import co.clinicalhouse.pqrsf.sla.SlaPolicy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final ClassPathResource LOGO = new ClassPathResource("mail/logo-clinical-house.png");
    /** Gmail/Outlook rechazan mensajes de más de ~25 MB; se deja margen por la codificación base64. */
    private static final long MAX_ADJUNTOS_BYTES = 10L * 1024 * 1024;

    private final JavaMailSender mailSender;

    @Value("${app.mail.siau-chief-email:siau@clinicalhouse.co}")
    private String siauChiefEmail;

    @Value("${app.mail.from:siau@clinicalhouse.co}")
    private String mailFrom;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.public-url:http://localhost:5173}")
    private String urlPublica;

    @Autowired(required = false)
    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void enviarReciboRadicacionUsuario(Pqrsf pqrsf) {
        // Se envía al remitente y, si es distinto, también al paciente
        Set<String> destinos = new LinkedHashSet<>();
        agregarSiValido(destinos, pqrsf.getFamEmail());
        agregarSiValido(destinos, pqrsf.getPacEmail());

        if (destinos.isEmpty()) {
            log.warn("No hay correo para enviar la constancia de {}", pqrsf.getConsecutivo());
            return;
        }

        String asunto = "Recibimos su solicitud · Radicado " + pqrsf.getConsecutivo() + " | IPS Clinical House";
        String html = EmailTemplates.reciboCiudadano(pqrsf, urlPublica);
        destinos.forEach(d -> enviarSilencioso(d, asunto, html, List.of()));
    }

    @Override
    @Async
    public void enviarNotificacionTrazaJefeSiau(Pqrsf pqrsf) {
        SlaPolicy.Evaluacion sla = SlaPolicy.evaluar(pqrsf);
        String tipo = pqrsf.getTipoSolicitud() == null ? "PQRSF" : pqrsf.getTipoSolicitud().replaceFirst("^\\([A-Z]\\)\\s*", "");
        // Asunto escaneable desde la bandeja: tipo, radicado y riesgo
        String asunto = "[SIAU] " + (sla.riesgo() == SlaPolicy.NivelRiesgo.VITAL ? "URGENTE · " : "")
                + (tipo.toLowerCase().startsWith("reclamo") ? "Nuevo " : "Nueva ") + tipo + " " + pqrsf.getConsecutivo()
                + " · " + sla.riesgo().getEtiqueta();

        // Adjuntos del ciudadano (solo si en total no superan el límite, para que el servidor no rechace el correo)
        List<Adjunto> lista = new ArrayList<>();
        long total = 0;
        if (pqrsf.getAdjuntos() != null) {
            for (PqrsfAdjunto a : pqrsf.getAdjuntos()) {
                File f = a.getRutaAlmacenamiento() != null ? new File(a.getRutaAlmacenamiento()) : null;
                if (f != null && f.isFile()) {
                    total += f.length();
                    lista.add(new Adjunto(a.getNombreOriginal(), f));
                }
            }
        }
        if (total > MAX_ADJUNTOS_BYTES) {
            log.info("Adjuntos de {} suman {} bytes: se envía sin archivos (disponibles en el panel)", pqrsf.getConsecutivo(), total);
            lista.clear();
        }
        enviarSilencioso(siauChiefEmail, asunto, EmailTemplates.notificacionJefe(pqrsf, urlPublica), lista);
    }

    @Override
    @Async
    public void enviarNotificacionCambioEstado(Pqrsf pqrsf, String estadoAnterior, String nuevoEstado, String observacion) {
        String destino = pqrsf.getFamEmail() != null ? pqrsf.getFamEmail() : pqrsf.getPacEmail();
        if (destino == null) return;

        String asunto = "Actualización de su solicitud " + pqrsf.getConsecutivo() + " [" + nuevoEstado.toUpperCase() + "]";
        enviarSilencioso(destino, asunto, EmailTemplates.cambioEstado(pqrsf, estadoAnterior, nuevoEstado, observacion, urlPublica), List.of());
    }

    @Override
    @Async
    public void enviarCodigoRecuperacion(String destinoCorreo, String nombreUsuario, String codigo) {
        if (destinoCorreo == null || destinoCorreo.isBlank()) return;
        String asunto = "Código de seguridad para restablecer su contraseña - SIAU IPS Clinical House";
        enviarSilencioso(destinoCorreo, asunto, EmailTemplates.codigoRecuperacion(nombreUsuario, codigo, urlPublica), List.of());
    }

    @Override
    public boolean enviarRespuestaCiudadano(Pqrsf pqrsf, String destino, String asunto, String mensaje,
                                            String funcionario, String cargo) {
        String html = EmailTemplates.respuestaCiudadano(pqrsf, mensaje, funcionario, cargo, urlPublica);
        if (!mailEnabled || mailSender == null) {
            log.info("[SIMULACIÓN CORREO] Respuesta a {} | Asunto: {}", destino, asunto);
            return false;
        }
        try {
            enviar(destino, asunto, html, List.of());
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar la respuesta a {}: {}", destino, e.getMessage());
            throw new IllegalStateException(explicarError(e));
        }
    }

    // ---------------------------------------------------------------------

    private void enviarSilencioso(String to, String asunto, String html, List<Adjunto> adjuntos) {
        if (!mailEnabled || mailSender == null) {
            log.info("[SIMULACIÓN CORREO] Para: {} | Asunto: {} | Adjuntos: {}", to, asunto, adjuntos.size());
            return;
        }
        // Reintentos ante cortes momentáneos de conexión con el servidor SMTP (p. ej. "Couldn't connect to host")
        for (int intento = 1; intento <= MAX_INTENTOS_ENVIO; intento++) {
            try {
                enviar(to, asunto, html, adjuntos);
                return;
            } catch (Exception e) {
                boolean reintentable = esErrorDeConexion(e) && intento < MAX_INTENTOS_ENVIO;
                if (!reintentable) {
                    // No se propaga para no revertir la transacción de base de datos
                    log.error("No se pudo enviar correo a: {} (intento {}/{}). Error: {}", to, intento, MAX_INTENTOS_ENVIO, e.getMessage());
                    return;
                }
                log.warn("Fallo de conexión SMTP enviando a {} (intento {}/{}); se reintenta. Error: {}", to, intento, MAX_INTENTOS_ENVIO, e.getMessage());
                try {
                    Thread.sleep(3000L * intento);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static final int MAX_INTENTOS_ENVIO = 3;

    private static boolean esErrorDeConexion(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException || t instanceof java.net.ConnectException
                    || t instanceof java.net.UnknownHostException) return true;
            String m = String.valueOf(t.getMessage()).toLowerCase();
            if (m.contains("couldn't connect") || m.contains("connection failed") || m.contains("timed out")) return true;
        }
        return false;
    }

    private void enviar(String to, String asunto, String html, List<Adjunto> adjuntos) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
        helper.setFrom(mailFrom, "SIAU IPS Clinical House");
        helper.setReplyTo(siauChiefEmail);
        helper.setTo(to);
        helper.setSubject(asunto);
        // Texto plano + HTML: mejora la entrega (menos spam) y se lee en cualquier cliente
        helper.setText(EmailTemplates.aTextoPlano(html), html);
        // Logo en línea: se ve aunque el frontend no esté publicado en internet
        if (LOGO.exists()) {
            helper.addInline(EmailTemplates.CID_LOGO, LOGO, "image/png");
        }
        for (Adjunto a : adjuntos) {
            helper.addAttachment(a.nombre(), new FileSystemResource(a.archivo()));
        }
        mailSender.send(message);
        log.info("Correo enviado a: {} | Asunto: {} | Adjuntos: {}", to, asunto, adjuntos.size());
    }

    private record Adjunto(String nombre, File archivo) {}

    /** Traduce los errores SMTP más comunes a un mensaje que el funcionario pueda entender. */
    private String explicarError(Exception e) {
        String m = String.valueOf(e.getMessage()).toLowerCase();
        if (e instanceof org.springframework.mail.MailAuthenticationException
                || m.contains("authentication failed") || m.contains("535") || m.contains("username and password not accepted")) {
            return "No se pudo enviar el correo: Gmail rechazó la contraseña de la cuenta " + mailFrom
                    + ". Debe configurarse una contraseña de aplicación de Google. Avise al administrador del sistema.";
        }
        if (m.contains("connect") || m.contains("timed out") || m.contains("unknownhost")) {
            return "No se pudo enviar el correo: no hay conexión con el servidor de correo. Revise la conexión a internet e intente de nuevo.";
        }
        if (m.contains("invalid address") || m.contains("550") || m.contains("553")) {
            return "No se pudo enviar el correo: la dirección del destinatario no es válida o no existe.";
        }
        return "No se pudo enviar el correo: " + e.getMessage();
    }

    private static void agregarSiValido(Set<String> destinos, String correo) {
        if (correo != null && correo.contains("@")) {
            destinos.add(correo.trim().toLowerCase());
        }
    }
}
