package co.clinicalhouse.pqrsf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Respuesta oficial que la Jefe SIAU envía al ciudadano por correo desde el panel. */
public class RespuestaCorreoDTO {

    /** Correo destino. Si se omite se usa el correo de respuesta o el del remitente. */
    private String correoDestino;

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 200, message = "El asunto no puede superar 200 caracteres")
    private String asunto;

    @NotBlank(message = "El mensaje de respuesta es obligatorio")
    @Size(min = 10, message = "El mensaje debe tener al menos 10 caracteres")
    private String mensaje;

    /** Si es true, la solicitud pasa automáticamente a estado "solucionado". */
    private boolean marcarSolucionado;

    public String getCorreoDestino() { return correoDestino; }
    public void setCorreoDestino(String correoDestino) { this.correoDestino = correoDestino; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean isMarcarSolucionado() { return marcarSolucionado; }
    public void setMarcarSolucionado(boolean marcarSolucionado) { this.marcarSolucionado = marcarSolucionado; }
}
