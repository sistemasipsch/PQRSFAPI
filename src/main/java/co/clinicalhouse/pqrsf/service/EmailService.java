package co.clinicalhouse.pqrsf.service;

import co.clinicalhouse.pqrsf.model.Pqrsf;

public interface EmailService {
    /** Correo de verificación / constancia al ciudadano (remitente y paciente). */
    void enviarReciboRadicacionUsuario(Pqrsf pqrsf);

    /** Notificación de nueva solicitud a la Jefe SIAU. */
    void enviarNotificacionTrazaJefeSiau(Pqrsf pqrsf);

    void enviarNotificacionCambioEstado(Pqrsf pqrsf, String estadoAnterior, String nuevoEstado, String observacion);

    void enviarCodigoRecuperacion(String destinoCorreo, String nombreUsuario, String codigo);

    /**
     * Envía de forma síncrona la respuesta oficial al ciudadano.
     * @return true si se envió por SMTP, false si el correo está deshabilitado (modo simulación).
     * @throws IllegalStateException si el servidor de correo rechaza el envío.
     */
    boolean enviarRespuestaCiudadano(Pqrsf pqrsf, String destino, String asunto, String mensaje,
                                     String funcionario, String cargo);
}
