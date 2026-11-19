package co.clinicalhouse.pqrsf.security;

import co.clinicalhouse.pqrsf.model.Usuario;
import co.clinicalhouse.pqrsf.repository.UsuarioRepository;
import co.clinicalhouse.pqrsf.security.AccesoUsuariosException.Motivo;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puerta de entrada al módulo de Usuarios.
 * Solo se habilita con las credenciales de un Super Administrador activo; se emite una
 * autorización temporal ligada a la sesión del funcionario que la pidió.
 */
@Service
public class AccesoUsuariosService {

    public static final String HEADER = "X-Acceso-Usuarios";
    private static final long MINUTOS_ACCESO = 15;
    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(5);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditoriaService auditoriaService;

    /** Intentos fallidos por sesión: [cantidad, bloqueado hasta (epoch ms)]. */
    private final Map<String, long[]> intentos = new ConcurrentHashMap<>();

    public AccesoUsuariosService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                 JwtTokenProvider tokenProvider, AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditoriaService = auditoriaService;
    }

    /** Valida credenciales de Super Admin y devuelve la autorización temporal del módulo. */
    public Map<String, Object> autorizar(String correo, String password, String sesionCorreo, String ip, String agente) {
        long[] estado = intentos.computeIfAbsent(sesionCorreo, k -> new long[2]);
        long ahora = Instant.now().toEpochMilli();
        if (estado[1] > ahora) {
            long min = Math.max(1, (estado[1] - ahora) / 60_000 + 1);
            throw new AccesoUsuariosException(Motivo.BLOQUEADO,
                    "Demasiados intentos fallidos. Intente de nuevo en " + min + " minuto(s).");
        }

        String correoLimpio = correo == null ? "" : correo.trim().toLowerCase();
        Usuario u = usuarioRepository.findByCorreo(correoLimpio).orElse(null);
        boolean passwordOk = u != null && Boolean.TRUE.equals(u.getActivo())
                && password != null && passwordEncoder.matches(password, u.getPassword());

        if (!passwordOk) {
            registrarFallo(estado, ahora);
            auditar("ACCESO_USUARIOS_RECHAZADO", sesionCorreo, ip, agente,
                    "{\"motivo\":\"Credenciales inválidas\",\"correoIndicado\":\"" + correoLimpio + "\"}");
            throw new AccesoUsuariosException(Motivo.CREDENCIALES_INVALIDAS, "Correo o contraseña incorrectos.");
        }

        if (!"SUPER_ADMIN".equalsIgnoreCase(u.getRol())) {
            registrarFallo(estado, ahora);
            auditar("ACCESO_USUARIOS_RECHAZADO", sesionCorreo, ip, agente,
                    "{\"motivo\":\"La cuenta no es Super Administrador\",\"correoIndicado\":\"" + correoLimpio + "\"}");
            throw new AccesoUsuariosException(Motivo.NO_SUPER_ADMIN,
                    "No puede ingresar a este módulo. Solo los Super Administradores tienen acceso a Usuarios.");
        }

        intentos.remove(sesionCorreo);
        auditar("ACCESO_USUARIOS", sesionCorreo, ip, agente, "{\"autorizadoPor\":\"" + u.getCorreo() + "\"}");

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("token", tokenProvider.generarTokenAccesoUsuarios(u.getCorreo(), sesionCorreo, MINUTOS_ACCESO));
        r.put("superAdminCorreo", u.getCorreo());
        r.put("superAdminNombre", u.getNombre());
        r.put("expiraEnMinutos", MINUTOS_ACCESO);
        return r;
    }

    /**
     * Verifica la autorización del módulo enviada en el header {@value #HEADER}.
     * @return correo del Super Administrador que autorizó.
     */
    public String verificar(String tokenAcceso, String sesionCorreo) {
        Claims c = tokenAcceso == null ? null : tokenProvider.leerClaims(tokenAcceso).orElse(null);
        boolean valido = c != null
                && JwtTokenProvider.TIPO_ACCESO_USUARIOS.equals(c.get("tipo", String.class))
                && sesionCorreo != null && sesionCorreo.equalsIgnoreCase(c.get("sesion", String.class));
        if (!valido) {
            throw new AccesoUsuariosException(Motivo.ACCESO_REQUERIDO,
                    "Ingrese las credenciales de un Super Administrador para acceder a Usuarios.");
        }
        // El Super Admin debe seguir activo y con el rol
        Usuario admin = usuarioRepository.findByCorreo(c.getSubject()).orElse(null);
        if (admin == null || !Boolean.TRUE.equals(admin.getActivo()) || !"SUPER_ADMIN".equalsIgnoreCase(admin.getRol())) {
            throw new AccesoUsuariosException(Motivo.ACCESO_REQUERIDO, "La autorización ya no es válida. Ingrese de nuevo.");
        }
        return admin.getCorreo();
    }

    private void registrarFallo(long[] estado, long ahora) {
        estado[0]++;
        if (estado[0] >= MAX_INTENTOS) {
            estado[0] = 0;
            estado[1] = ahora + BLOQUEO.toMillis();
        }
    }

    private void auditar(String accion, String sesion, String ip, String agente, String detalles) {
        auditoriaService.registrar("USUARIO", "MODULO_USUARIOS", accion, sesion, ip, agente, detalles);
    }
}
