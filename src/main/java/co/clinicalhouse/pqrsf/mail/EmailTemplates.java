package co.clinicalhouse.pqrsf.mail;

import co.clinicalhouse.pqrsf.model.Pqrsf;
import co.clinicalhouse.pqrsf.sla.SlaPolicy;
import org.springframework.web.util.HtmlUtils;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Plantillas HTML de los correos institucionales SIAU.
 * Paleta corporativa: blanco (predominante), gris, negro y azul.
 * Todo el contenido proveniente del usuario se escapa para evitar inyección de HTML.
 */
public final class EmailTemplates {

    private static final String AZUL = "#1062EA";
    private static final String NEGRO = "#111827";
    private static final String GRIS_TEXTO = "#4B5563";
    private static final String GRIS_SUAVE = "#6B7280";
    private static final String GRIS_BORDE = "#E5E7EB";
    private static final String GRIS_FONDO = "#F3F4F6";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private EmailTemplates() {}

    // =====================================================================
    // 1. CORREO DE VERIFICACIÓN / CONSTANCIA AL CIUDADANO
    // =====================================================================
    public static String reciboCiudadano(Pqrsf p, String urlPublica) {
        String cuerpo =
                parrafo("Apreciado(a) <b>" + esc(nombreFamiliar(p)) + "</b>,") +
                parrafo("Hemos <b>recibido y radicado</b> su solicitud en el Servicio de Información y Atención al Usuario (SIAU) de IPS Clinical House. " +
                        "Conserve este número: le permitirá hacer seguimiento y lo citaremos en nuestra respuesta.") +
                destacado("Número de radicado", p.getConsecutivo(), "Radicado el " + p.getFechaRadicacion().format(FECHA)) +
                seccion("Resumen de su solicitud") +
                tabla(
                        fila("Tipo de solicitud", esc(tipoLimpio(p.getTipoSolicitud()))),
                        fila("Paciente", esc(nombrePaciente(p)) + "<br/><span style=\"color:" + GRIS_SUAVE + ";\">" + esc(p.getPacTipoDocumento()) + " " + esc(p.getPacNumeroDocumento()) + "</span>"),
                        fila("Entidad / EPS", esc(p.getPacEntidad())),
                        fila("Le responderemos por", esc(descripcionMedio(p))),
                        fila("Documentos adjuntos", p.getAdjuntos() == null || p.getAdjuntos().isEmpty() ? "Ninguno" : p.getAdjuntos().size() + " archivo(s)"),
                        fila("Estado actual", pill("Radicado"))
                ) +
                seccion("Descripción de la solicitud") +
                "<div style=\"background:" + GRIS_FONDO + ";border-radius:8px;padding:14px;font-size:13.5px;line-height:1.6;color:" + GRIS_TEXTO + ";white-space:pre-wrap;margin-bottom:18px;\">" + esc(resumir(p.getDescripcion(), 900)) + "</div>" +
                nota("<b>Protección de datos (Ley 1581 de 2012):</b> sus datos se tratan de forma confidencial y solo para gestionar su solicitud.");

        return layout("Recibimos su solicitud", cuerpo, urlPublica);
    }

    // =====================================================================
    // 2. NOTIFICACIÓN DE NUEVA SOLICITUD A LA JEFE SIAU
    // =====================================================================
    public static String notificacionJefe(Pqrsf p, String urlPublica) {
        SlaPolicy.Evaluacion sla = SlaPolicy.evaluar(p);
        String colorRiesgo = switch (sla.riesgo()) {
            case VITAL -> "#B91C1C";
            case PRIORIZADO -> "#B45309";
            default -> AZUL;
        };
        String fondoRiesgo = switch (sla.riesgo()) {
            case VITAL -> "#FEF2F2";
            case PRIORIZADO -> "#FFFBEB";
            default -> "#EFF6FF";
        };

        int nAdjuntos = p.getAdjuntos() == null ? 0 : p.getAdjuntos().size();
        String adjuntos = nAdjuntos == 0
                ? "<span style=\"color:" + GRIS_SUAVE + ";\">Sin documentos adjuntos</span>"
                : p.getAdjuntos().stream()
                    .map(a -> "<li style=\"margin:2px 0;\">" + esc(a.getNombreOriginal()) + " <span style=\"color:" + GRIS_SUAVE + ";\">(" + Math.max(1, a.getTamanoBytes() / 1024) + " KB)</span></li>")
                    .collect(Collectors.joining("", "<ul style=\"margin:0;padding-left:18px;\">", "</ul>"))
                  + "<div style=\"font-size:12.5px;color:" + GRIS_SUAVE + ";margin-top:6px;\">Los archivos van adjuntos a este correo (si no superan 10 MB) y siempre están disponibles en el panel.</div>";

        String cuerpo =
                // Cabecera de alerta: tipo, radicado, riesgo y vencimiento
                "<div style=\"background:" + fondoRiesgo + ";border:1px solid " + colorRiesgo + ";border-left:5px solid " + colorRiesgo + ";border-radius:10px;padding:16px 18px;margin:0 0 22px;\">" +
                "<div style=\"font-size:12px;color:" + GRIS_SUAVE + ";text-transform:uppercase;letter-spacing:.5px;font-weight:600;\">Nueva " + esc(tipoLimpio(p.getTipoSolicitud())) + "</div>" +
                "<div style=\"font-size:24px;font-weight:700;color:" + NEGRO + ";margin:4px 0 8px;letter-spacing:.3px;\">" + esc(p.getConsecutivo()) + "</div>" +
                "<table role=\"presentation\" style=\"border-collapse:collapse;font-size:13.5px;\">" +
                "<tr><td style=\"padding:2px 14px 2px 0;color:" + GRIS_SUAVE + ";\">Nivel</td><td style=\"padding:2px 0;color:" + colorRiesgo + ";font-weight:700;\">" + sla.riesgo().getEtiqueta() + " &middot; " + sla.riesgo().getPlazoHoras() + " h</td></tr>" +
                "<tr><td style=\"padding:2px 14px 2px 0;color:" + GRIS_SUAVE + ";\">Radicada</td><td style=\"padding:2px 0;color:" + NEGRO + ";\">" + p.getFechaRadicacion().format(FECHA) + "</td></tr>" +
                "<tr><td style=\"padding:2px 14px 2px 0;color:" + GRIS_SUAVE + ";\">Vence</td><td style=\"padding:2px 0;color:" + NEGRO + ";font-weight:700;\">" + sla.fechaLimite().format(FECHA) + "</td></tr>" +
                "</table></div>" +
                seccion("Descripción de la solicitud") +
                "<div style=\"background:" + GRIS_FONDO + ";border-radius:8px;padding:14px;font-size:14px;line-height:1.6;color:" + NEGRO + ";white-space:pre-wrap;margin-bottom:20px;\">" + esc(p.getDescripcion()) + "</div>" +
                seccion("Paciente") +
                tabla(
                        fila("Nombre", "<b>" + esc(nombrePaciente(p)) + "</b>"),
                        fila("Documento", esc(p.getPacTipoDocumento()) + " " + esc(p.getPacNumeroDocumento())),
                        fila("EPS / Régimen", esc(p.getPacEntidad()) + " &middot; " + esc(p.getPacRegimen())),
                        fila("Población", esc(p.getPacPoblacion())),
                        fila("Teléfono", esc(p.getPacTelefono())),
                        fila("Correo", enlaceCorreo(p.getPacEmail())),
                        fila("Dirección", esc(p.getPacDireccion()))
                ) +
                seccion("Quien radica (familiar / acompañante)") +
                tabla(
                        fila("Nombre", esc(nombreFamiliar(p))),
                        fila("Documento", esc(p.getFamTipoDocumento()) + " " + esc(p.getFamNumeroDocumento())),
                        fila("Teléfono", esc(p.getFamTelefono())),
                        fila("Correo", enlaceCorreo(p.getFamEmail()))
                ) +
                seccion("Respuesta solicitada por") +
                parrafo("<b>" + esc(descripcionMedio(p)) + "</b>") +
                seccion("Adjuntos (" + nAdjuntos + ")") +
                "<div style=\"font-size:14px;color:" + GRIS_TEXTO + ";margin-bottom:24px;\">" + adjuntos + "</div>" +
                // Un enlace a localhost marca el correo como sospechoso (y no funciona fuera de este equipo):
                // el botón solo se incluye cuando el panel tiene una dirección pública.
                (esUrlPublica(urlPublica)
                        ? "<div style=\"text-align:center;margin:8px 0 4px;\">" + boton("Abrir en el panel SIAU", urlPublica + "/gestion-siau") + "</div>"
                        : "");

        return layout("Nueva PQRSF por gestionar", cuerpo, urlPublica);
    }

    // =====================================================================
    // 3. CAMBIO DE ESTADO (CIUDADANO)
    // =====================================================================
    public static String cambioEstado(Pqrsf p, String estadoAnterior, String nuevoEstado, String observacion, String urlPublica) {
        String cuerpo =
                parrafo("Apreciado(a) usuario(a), su solicitud <b>" + esc(p.getConsecutivo()) + "</b> tiene una actualización:") +
                tabla(
                        fila("Estado anterior", esc(capitalizar(estadoAnterior))),
                        fila("Nuevo estado", pill(capitalizar(nuevoEstado)))
                ) +
                (observacion != null && !observacion.isBlank()
                        ? seccion("Observación") + "<div style=\"background:" + GRIS_FONDO + ";border-radius:8px;padding:14px;font-size:14px;line-height:1.6;color:" + NEGRO + ";white-space:pre-wrap;\">" + esc(observacion) + "</div>"
                        : "");
        return layout("Actualización de su solicitud", cuerpo, urlPublica);
    }

    // =====================================================================
    // 4. RESPUESTA OFICIAL AL CIUDADANO (ENVIADA DESDE EL PANEL)
    // =====================================================================
    public static String respuestaCiudadano(Pqrsf p, String mensaje, String funcionario, String cargo, String urlPublica) {
        String cuerpo =
                parrafo("Apreciado(a) <b>" + esc(p.getFamPrimerNombre() + " " + p.getFamPrimerApellido()) + "</b>,") +
                parrafo("En atención a su solicitud con radicado <b>" + esc(p.getConsecutivo()) + "</b> (" + esc(p.getTipoSolicitud()) + "), le damos respuesta en los siguientes términos:") +
                "<div style=\"border-left:3px solid " + AZUL + ";padding:4px 0 4px 16px;margin:18px 0;font-size:14.5px;line-height:1.7;color:" + NEGRO + ";white-space:pre-wrap;\">" + esc(mensaje) + "</div>" +
                parrafo("Cordialmente,") +
                "<div style=\"font-size:14px;color:" + NEGRO + ";font-weight:600;\">" + esc(funcionario) + "</div>" +
                "<div style=\"font-size:13px;color:" + GRIS_SUAVE + ";margin-bottom:20px;\">" + esc(cargo) + " &middot; SIAU IPS Clinical House</div>" +
                nota("Si tiene inquietudes adicionales, responda a este correo citando su número de radicado.");
        return layout("Respuesta a su solicitud", cuerpo, urlPublica);
    }

    // =====================================================================
    // 5. CÓDIGO DE RECUPERACIÓN DE CONTRASEÑA
    // =====================================================================
    public static String codigoRecuperacion(String nombre, String codigo, String urlPublica) {
        String cuerpo =
                parrafo("Hola <b>" + esc(nombre != null ? nombre : "Funcionario SIAU") + "</b>,") +
                parrafo("Recibimos una solicitud para restablecer la contraseña de su cuenta del panel SIAU. Su código de seguridad es:") +
                "<div style=\"text-align:center;margin:24px 0;\"><span style=\"display:inline-block;background:" + NEGRO + ";color:#fff;font-size:30px;font-weight:700;letter-spacing:8px;padding:12px 26px;border-radius:10px;\">" + esc(codigo) + "</span></div>" +
                nota("El código vence en <b>15 minutos</b>. Si usted no hizo esta solicitud, ignore este correo y avise al administrador del sistema.");
        return layout("Código de seguridad", cuerpo, urlPublica);
    }

    // =====================================================================
    // BLOQUES REUTILIZABLES
    // =====================================================================
    /** Content-ID del logo que EmailServiceImpl adjunta en línea (se ve aunque el servidor no sea público). */
    public static final String CID_LOGO = "logoClinicalHouse";

    private static String layout(String titulo, String cuerpo, String urlPublica) {
        String logo = "<img src=\"cid:" + CID_LOGO + "\" alt=\"IPS Clinical House\" height=\"44\" style=\"display:block;height:44px;border:0;\"/>";
        return "<!DOCTYPE html><html lang=\"es\"><body style=\"margin:0;padding:24px 12px;background:" + GRIS_FONDO + ";font-family:'Segoe UI',Arial,sans-serif;\">" +
                "<div style=\"max-width:620px;margin:0 auto;background:#ffffff;border:1px solid " + GRIS_BORDE + ";border-radius:14px;overflow:hidden;\">" +
                "<div style=\"padding:20px 28px;border-bottom:1px solid " + GRIS_BORDE + ";\">" + logo + "</div>" +
                "<div style=\"height:3px;background:" + AZUL + ";\"></div>" +
                "<div style=\"padding:28px;\">" +
                "<h1 style=\"margin:0 0 18px;font-size:20px;font-weight:700;color:" + NEGRO + ";\">" + esc(titulo) + "</h1>" +
                cuerpo +
                "</div>" +
                "<div style=\"padding:16px 28px;background:" + GRIS_FONDO + ";font-size:12px;color:" + GRIS_SUAVE + ";text-align:center;\">" +
                "IPS Clinical House &middot; Servicio de Información y Atención al Usuario (SIAU)<br/>siau@clinicalhouse.co &middot; clinicalhouse.co" +
                "</div></div></body></html>";
    }

    private static String parrafo(String html) {
        return "<p style=\"margin:0 0 14px;font-size:14.5px;line-height:1.6;color:" + GRIS_TEXTO + ";\">" + html + "</p>";
    }

    private static String nota(String html) {
        return "<p style=\"margin:14px 0 0;font-size:12.5px;line-height:1.5;color:" + GRIS_SUAVE + ";\">" + html + "</p>";
    }

    private static String seccion(String titulo) {
        return "<div style=\"font-size:12px;font-weight:600;color:" + GRIS_SUAVE + ";text-transform:uppercase;letter-spacing:.5px;margin:0 0 8px;\">" + esc(titulo) + "</div>";
    }

    private static String destacado(String etiqueta, String valor, String sub) {
        return "<div style=\"border:1px solid " + GRIS_BORDE + ";border-radius:12px;padding:18px;margin:18px 0;text-align:center;\">" +
                "<div style=\"font-size:12px;color:" + GRIS_SUAVE + ";text-transform:uppercase;letter-spacing:.5px;\">" + esc(etiqueta) + "</div>" +
                "<div style=\"font-size:26px;font-weight:700;color:" + AZUL + ";margin:6px 0;letter-spacing:.5px;\">" + esc(valor) + "</div>" +
                "<div style=\"font-size:13px;color:" + GRIS_SUAVE + ";\">" + esc(sub) + "</div></div>";
    }

    private static String tabla(String... filas) {
        return "<table role=\"presentation\" style=\"width:100%;border-collapse:collapse;margin:0 0 18px;font-size:14px;\">" + String.join("", filas) + "</table>";
    }

    private static String fila(String etiqueta, String valorHtml) {
        return "<tr><td style=\"padding:9px 0;border-bottom:1px solid " + GRIS_BORDE + ";color:" + GRIS_SUAVE + ";width:42%;vertical-align:top;\">" + esc(etiqueta) + "</td>" +
                "<td style=\"padding:9px 0;border-bottom:1px solid " + GRIS_BORDE + ";color:" + NEGRO + ";\">" + valorHtml + "</td></tr>";
    }

    private static String pill(String texto) {
        return "<span style=\"display:inline-block;background:#EAF2FE;color:" + AZUL + ";font-size:12.5px;font-weight:600;padding:2px 10px;border-radius:999px;\">" + esc(texto) + "</span>";
    }

    private static boolean esUrlPublica(String url) {
        if (url == null || url.isBlank()) return false;
        String u = url.toLowerCase();
        return !(u.contains("localhost") || u.contains("127.0.0.1") || u.contains("0.0.0.0") || u.contains("192.168.") || u.contains("10.0."));
    }

    private static String boton(String texto, String url) {
        return "<a href=\"" + url + "\" style=\"display:inline-block;background:" + AZUL + ";color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;padding:11px 20px;border-radius:8px;\">" + esc(texto) + "</a>";
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================
    public static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s, "UTF-8");
    }

    private static String nombrePaciente(Pqrsf p) {
        return p.getPacPrimerNombre() + " " + (p.getPacSegundoNombre() != null ? p.getPacSegundoNombre() + " " : "")
                + p.getPacPrimerApellido() + " " + (p.getPacSegundoApellido() != null ? p.getPacSegundoApellido() : "");
    }

    private static String nombreFamiliar(Pqrsf p) {
        return (p.getFamPrimerNombre() + " " + (p.getFamSegundoNombre() != null && !p.getFamSegundoNombre().isBlank() ? p.getFamSegundoNombre() + " " : "")
                + p.getFamPrimerApellido() + " " + (p.getFamSegundoApellido() != null ? p.getFamSegundoApellido() : "")).trim();
    }

    /** "(Q) Queja" → "Queja". */
    private static String tipoLimpio(String tipo) {
        return tipo == null ? "solicitud" : tipo.replaceFirst("^\\([A-Z]\\)\\s*", "");
    }

    private static String resumir(String texto, int max) {
        String t = texto == null ? "" : texto.trim();
        return t.length() > max ? t.substring(0, max) + "…" : t;
    }

    private static String enlaceCorreo(String correo) {
        if (correo == null || correo.isBlank()) return "—";
        return "<a href=\"mailto:" + esc(correo) + "\" style=\"color:" + AZUL + ";text-decoration:none;\">" + esc(correo) + "</a>";
    }

    /** Versión en texto plano (algunos clientes y filtros anti-spam la requieren). */
    public static String aTextoPlano(String html) {
        if (html == null) return "";
        String t = html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|tr|li|h1|h2|ol|ul|table)>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("(?i)</td>", "  ")
                .replaceAll("<[^>]+>", "")
                .replace("&middot;", "·").replace("&nbsp;", " ");
        t = HtmlUtils.htmlUnescape(t);
        return t.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static String descripcionMedio(Pqrsf p) {
        String medio = p.getMedioRespuesta() == null ? "C" : p.getMedioRespuesta();
        return switch (medio) {
            case "A" -> "Domicilio: " + p.getRespDireccion() + ", " + p.getRespBarrio() + ", " + p.getRespMunicipio();
            case "B" -> "Teléfono: " + p.getRespCelular() + (p.getRespTelefonoFijo() != null && !p.getRespTelefonoFijo().isBlank() ? " / " + p.getRespTelefonoFijo() : "");
            default -> "Correo electrónico: " + (p.getRespCorreo() != null ? p.getRespCorreo() : p.getFamEmail());
        };
    }

    private static String capitalizar(String s) {
        if (s == null || s.isBlank()) return "N/A";
        String t = s.replace('_', ' ').trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
