package co.clinicalhouse.pqrsf.mail;

import co.clinicalhouse.pqrsf.model.Pqrsf;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Genera vistas previas HTML de los correos con datos de ejemplo (target/vista-previa-correos).
 * Útil para revisar el diseño sin enviar nada.
 */
class VistaPreviaCorreosTest {

    @Test
    void generarVistasPrevias() throws Exception {
        Pqrsf p = new Pqrsf();
        p.setConsecutivo("PQRSF-CH-0042");
        p.setFechaRadicacion(LocalDateTime.now().minusMinutes(3));
        p.setTipoSolicitud("(Q) Queja");
        p.setEstado("radicado");
        p.setDescripcion("El día de ayer mi madre esperó más de 4 horas en urgencias sin recibir información sobre su estado. "
                + "Solicitamos que se revise el proceso de atención y se nos informe qué medidas se tomarán.");
        p.setPacTipoDocumento("CC");
        p.setPacNumeroDocumento("1.020.304.050");
        p.setPacPrimerNombre("Rosa");
        p.setPacPrimerApellido("Gómez");
        p.setPacSegundoApellido("Pérez");
        p.setPacEntidad("Nueva EPS");
        p.setPacRegimen("Contributivo");
        p.setPacTelefono("3001234567");
        p.setPacEmail("paciente.ejemplo@correo.com");
        p.setFamTipoDocumento("CC");
        p.setFamNumeroDocumento("1.098.765.432");
        p.setFamPrimerNombre("Laura");
        p.setFamPrimerApellido("Gómez");
        p.setFamTelefono("3109876543");
        p.setFamEmail("familiar.ejemplo@correo.com");

        File dir = new File("target/vista-previa-correos");
        dir.mkdirs();
        Path logo = new File("src/main/resources/mail/logo-clinical-house.png").toPath().toAbsolutePath();
        String cid = "cid:" + EmailTemplates.CID_LOGO;
        String url = "http://localhost:5173";

        escribir(new File(dir, "1-correo-paciente.html"), EmailTemplates.reciboCiudadano(p, url).replace(cid, logo.toUri().toString()));
        escribir(new File(dir, "2-correo-jefe.html"), EmailTemplates.notificacionJefe(p, url).replace(cid, logo.toUri().toString()));
        assertTrue(new File(dir, "2-correo-jefe.html").length() > 0);
    }

    private static void escribir(File f, String html) throws Exception {
        Files.writeString(f.toPath(), html, StandardCharsets.UTF_8);
    }
}
