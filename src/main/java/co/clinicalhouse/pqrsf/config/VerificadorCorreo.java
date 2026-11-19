package co.clinicalhouse.pqrsf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Al arrancar comprueba la conexión con el servidor de correo (sin enviar nada), para que una
 * contraseña inválida se vea en la consola del backend antes del primer envío.
 */
@Component
public class VerificadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(VerificadorCorreo.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:true}")
    private boolean habilitado;

    @Value("${app.mail.siau-chief-email:}")
    private String destinoJefe;

    public VerificadorCorreo(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verificar() {
        if (!habilitado || !(mailSender instanceof JavaMailSenderImpl impl)) return;
        // En otro hilo para no demorar el arranque
        new Thread(() -> {
            try {
                impl.testConnection();
                log.info("CORREO OK: conectado como {} · solicitudes nuevas → {}", impl.getUsername(), destinoJefe);
            } catch (Exception e) {
                log.error("CORREO NO FUNCIONA: Gmail rechazó la cuenta {}. Configure una contraseña de aplicación "
                        + "(https://myaccount.google.com/apppasswords) en spring.mail.password. Detalle: {}",
                        impl.getUsername(), e.getMessage());
            }
        }, "verificador-correo").start();
    }
}
