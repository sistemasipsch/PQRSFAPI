package co.clinicalhouse.pqrsf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Convierte las excepciones en respuestas JSON {"message": "..."} que el frontend muestra al usuario.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .collect(Collectors.joining(" "));
        return error(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Datos inválidos." : msg);
    }

    /** Parámetros con formato inválido (p. ej. un id que no es número). */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametro(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Parámetro inválido: " + e.getName() + ".");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> argumento(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Rechazos del módulo de Usuarios: 403 + motivo (nunca 401, que cerraría la sesión del funcionario). */
    @ExceptionHandler(co.clinicalhouse.pqrsf.security.AccesoUsuariosException.class)
    public ResponseEntity<Map<String, String>> accesoUsuarios(co.clinicalhouse.pqrsf.security.AccesoUsuariosException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "message", e.getMessage(),
                "motivo", e.getMotivo().name(),
                "error", "Forbidden"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> seguridad(SecurityException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> estado(IllegalStateException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tamano(MaxUploadSizeExceededException e) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Los adjuntos superan el máximo permitido de 20 MB por solicitud.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> general(RuntimeException e) {
        log.error("Error no controlado", e);
        // Solo los mensajes propios de la aplicación (RuntimeException directa) se muestran al usuario.
        // Los de librerías (SQL, JPA, NullPointer...) pueden revelar detalles internos: mensaje genérico.
        boolean propio = e.getClass() == RuntimeException.class && e.getMessage() != null;
        if (propio && e.getMessage().toLowerCase().contains("no se encontr")) {
            return error(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, propio ? e.getMessage()
                : "Ocurrió un error interno. Intente de nuevo; si persiste, contacte al administrador.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message == null ? "" : message, "error", status.getReasonPhrase()));
    }
}
