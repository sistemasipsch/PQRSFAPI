package co.clinicalhouse.pqrsf.service.impl;

import co.clinicalhouse.pqrsf.dto.ActualizarPerfilDTO;
import co.clinicalhouse.pqrsf.dto.CrearUsuarioRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginResponseDTO;
import co.clinicalhouse.pqrsf.model.Usuario;
import co.clinicalhouse.pqrsf.repository.UsuarioRepository;
import co.clinicalhouse.pqrsf.security.JwtTokenProvider;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import co.clinicalhouse.pqrsf.service.AuthService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.clinicalhouse.pqrsf.model.Pqrsf;
import co.clinicalhouse.pqrsf.model.TrazaPqrsf;
import co.clinicalhouse.pqrsf.repository.PqrsfRepository;
import co.clinicalhouse.pqrsf.repository.TrazaPqrsfRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.security.SecureRandom;

@Service
public class AuthServiceImpl implements AuthService {

    private static final int MAX_INTENTOS_RECUPERACION = 5;
    /** Intentos fallidos de código de recuperación por correo. */
    private final java.util.Map<String, Integer> intentosRecuperacion = new java.util.concurrent.ConcurrentHashMap<>();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditoriaService auditoriaService;
    private final co.clinicalhouse.pqrsf.service.EmailService emailService;
    private final PqrsfRepository pqrsfRepository;
    private final TrazaPqrsfRepository trazaRepository;
    private final co.clinicalhouse.pqrsf.security.PermisoService permisoService;
    private final co.clinicalhouse.pqrsf.service.ConfiguracionService configuracionService;

    @Autowired
    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           AuditoriaService auditoriaService,
                           co.clinicalhouse.pqrsf.service.EmailService emailService,
                           PqrsfRepository pqrsfRepository,
                           TrazaPqrsfRepository trazaRepository,
                           co.clinicalhouse.pqrsf.security.PermisoService permisoService,
                           co.clinicalhouse.pqrsf.service.ConfiguracionService configuracionService) {
        this.permisoService = permisoService;
        this.configuracionService = configuracionService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditoriaService = auditoriaService;
        this.emailService = emailService;
        this.pqrsfRepository = pqrsfRepository;
        this.trazaRepository = trazaRepository;
    }

    @Override
    @PostConstruct
    @Transactional
    public void inicializarUsuarioJefeSiNoExiste() {
        // Las cuentas semilla SOLO se crean si no existen. Nunca se restablece su contraseña,
        // para que los cambios hechos desde "Mi perfil" o "Recuperar contraseña" se conserven.
        // La contraseña inicial sale de una variable de entorno; si no está definida se genera una
        // aleatoria y se muestra UNA vez en el registro del servidor (nunca hay contraseñas fijas en el código).
        if (usuarioRepository.findByCorreo("siau@clinicalhouse.co").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "Jefe SIAU Clinical House",
                    "siau@clinicalhouse.co",
                    passwordEncoder.encode(claveInicial("siau@clinicalhouse.co", claveInicialJefe)),
                    "JEFE_SIAU",
                    "Jefe de Servicio de Información y Atención al Usuario"
            ));
        }

        if (usuarioRepository.findByCorreo("admin@clinicalhouse.co").isEmpty()) {
            usuarioRepository.save(new Usuario(
                    "Super Administrador IPS Clinical House",
                    "admin@clinicalhouse.co",
                    passwordEncoder.encode(claveInicial("admin@clinicalhouse.co", claveInicialAdmin)),
                    "SUPER_ADMIN",
                    "Administrador General del Sistema"
            ));
        }
    }

    @org.springframework.beans.factory.annotation.Value("${ADMIN_PASSWORD_INICIAL:}")
    private String claveInicialAdmin;

    @org.springframework.beans.factory.annotation.Value("${JEFE_PASSWORD_INICIAL:}")
    private String claveInicialJefe;

    private static String claveInicial(String correo, String configurada) {
        if (configurada != null && configurada.trim().length() >= 8) return configurada.trim();
        String alfabeto = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 14; i++) sb.append(alfabeto.charAt(r.nextInt(alfabeto.length())));
        org.slf4j.LoggerFactory.getLogger(AuthServiceImpl.class).warn(
                "Cuenta inicial creada: {} · contraseña temporal: {} · cámbiela en 'Mi perfil' al ingresar.", correo, sb);
        return sb.toString();
    }

    /** Contraseñas guardadas por BCrypt empiezan por $2a$, $2b$ o $2y$. */
    // ---- Bloqueo temporal tras varios intentos fallidos de inicio de sesión (por correo) ----
    private static final int MAX_FALLOS_LOGIN = 5;
    private static final long BLOQUEO_LOGIN_MS = 15 * 60_000L;
    /** correo -> [fallos, bloqueado hasta (epoch ms)] */
    private final java.util.Map<String, long[]> fallosLogin = new java.util.concurrent.ConcurrentHashMap<>();

    private void verificarBloqueoLogin(String correo) {
        long[] f = fallosLogin.get(correo);
        if (f == null) return;
        long restante = f[1] - System.currentTimeMillis();
        if (restante > 0) {
            throw new RuntimeException("Demasiados intentos fallidos. Intente de nuevo en " + Math.max(1, restante / 60_000 + 1) + " minutos.");
        }
        if (f[1] > 0) fallosLogin.remove(correo);
    }

    private void registrarFalloLogin(String correo) {
        long[] f = fallosLogin.computeIfAbsent(correo, k -> new long[2]);
        synchronized (f) {
            f[0]++;
            if (f[0] >= MAX_FALLOS_LOGIN) f[1] = System.currentTimeMillis() + BLOQUEO_LOGIN_MS;
        }
    }

    private static boolean esHashBcrypt(String valor) {
        return valor != null && valor.matches("^\\$2[aby]\\$.{56}$");
    }

    @Override
    @Transactional
    public LoginResponseDTO autenticar(LoginRequestDTO request, String ipOrigen, String userAgent) {
        String correoLimpio = request.getCorreo().trim().toLowerCase();
        verificarBloqueoLogin(correoLimpio);

        Usuario usuario = usuarioRepository.findByCorreo(correoLimpio)
                .orElse(null);

        if (usuario == null || !usuario.getActivo()) {
            auditoriaService.registrar(
                    "USUARIO",
                    correoLimpio,
                    "LOGIN_FALLIDO",
                    correoLimpio,
                    ipOrigen,
                    userAgent,
                    "{\"motivo\":\"Usuario inexistente o inactivo\"}"
            );
            registrarFalloLogin(correoLimpio);
            throw new RuntimeException("Credenciales inválidas. Verifique correo y contraseña.");
        }

        boolean coincide;
        if (esHashBcrypt(usuario.getPassword())) {
            coincide = passwordEncoder.matches(request.getPassword(), usuario.getPassword());
        } else {
            // Migración única de contraseñas heredadas en texto plano (scripts SQL iniciales)
            coincide = request.getPassword().equals(usuario.getPassword());
            if (coincide) {
                usuario.setPassword(passwordEncoder.encode(request.getPassword()));
                usuarioRepository.save(usuario);
            }
        }

        if (!coincide) {
            auditoriaService.registrar(
                    "USUARIO",
                    correoLimpio,
                    "LOGIN_FALLIDO",
                    correoLimpio,
                    ipOrigen,
                    userAgent,
                    "{\"motivo\":\"Contraseña incorrecta\"}"
            );
            registrarFalloLogin(correoLimpio);
            throw new RuntimeException("Credenciales inválidas. Verifique correo y contraseña.");
        }

        fallosLogin.remove(correoLimpio);
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String token = tokenProvider.generarToken(usuario.getCorreo(), usuario.getRol(), usuario.getId());

        auditoriaService.registrar(
                "USUARIO",
                usuario.getId().toString(),
                "LOGIN_EXITOSO",
                usuario.getCorreo(),
                ipOrigen,
                userAgent,
                "{\"rol\":\"" + usuario.getRol() + "\",\"nombre\":\"" + usuario.getNombre() + "\"}"
        );

        return sesion(token, usuario);
    }

    /** Respuesta de sesión con los permisos vigentes de su cargo (o de su rol si el cargo no está configurado). */
    private LoginResponseDTO sesion(String token, Usuario usuario) {
        LoginResponseDTO dto = new LoginResponseDTO(token, usuario.getId(), usuario.getNombre(), usuario.getCorreo(),
                usuario.getRol(), usuario.getCargo());
        dto.setArea(usuario.getArea());
        dto.setPermisos(permisoService.codigosDe(usuario));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO sesionActual(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .orElseThrow(() -> new SecurityException("La cuenta de la sesión no existe o está inactiva."));
        return sesion(null, usuario);
    }

    @Override
    @Transactional
    public Usuario editarUsuario(Long id, String nombre, String correo, String cargo, String area, String rol, String adminUsuario, String ipOrigen) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));
        StringBuilder cambios = new StringBuilder();

        // 1. Nombre completo
        String nombreAnterior = usuario.getNombre();
        if (nombre != null && !nombre.isBlank() && !nombre.trim().equals(nombreAnterior)) {
            String nuevoNombre = nombre.trim();
            if (nuevoNombre.length() < 3) {
                throw new IllegalArgumentException("El nombre completo debe tener al menos 3 caracteres.");
            }
            usuario.setNombre(nuevoNombre);
            cambios.append("nombre,");

            // Reasignar nombre en las solicitudes PQRSF para mantener consistencia
            List<Pqrsf> asignadas = pqrsfRepository.findByResponsableAsignadoIgnoreCase(nombreAnterior);
            for (Pqrsf p : asignadas) {
                p.setResponsableAsignado(nuevoNombre);
                p.setFechaActualizacion(LocalDateTime.now());
                TrazaPqrsf traza = new TrazaPqrsf(
                        p,
                        p.getEstado(),
                        p.getEstado(),
                        "Actualización de nombre del funcionario asignado: " + nombreAnterior + " → " + nuevoNombre,
                        adminUsuario
                );
                trazaRepository.save(traza);
            }
            if (!asignadas.isEmpty()) {
                pqrsfRepository.saveAll(asignadas);
            }
        }

        // 2. Correo institucional
        if (correo != null && !correo.isBlank()) {
            String correoLimpio = correo.trim().toLowerCase();
            if (!correoLimpio.equalsIgnoreCase(usuario.getCorreo())) {
                if ("admin@clinicalhouse.co".equalsIgnoreCase(usuario.getCorreo())) {
                    throw new IllegalArgumentException("No se puede modificar el correo de la cuenta principal del Super Administrador.");
                }
                if (!correoLimpio.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
                    throw new IllegalArgumentException("El formato del correo institucional es inválido.");
                }
                if (usuarioRepository.existsByCorreo(correoLimpio)) {
                    throw new IllegalArgumentException("Ya existe otro usuario registrado con el correo: " + correoLimpio);
                }
                usuario.setCorreo(correoLimpio);
                cambios.append("correo,");
            }
        }

        // 3. Cargo
        if (cargo != null) {
            String c = configuracionService.validarCargo(cargo);
            if (c != null && !c.equals(usuario.getCargo())) { usuario.setCargo(c); cambios.append("cargo,"); }
        }

        // 4. Área
        if (area != null) {
            String a = configuracionService.validarArea(area); // "" → sin área
            if (!java.util.Objects.equals(a, usuario.getArea())) { usuario.setArea(a); cambios.append("area,"); }
        }

        // 5. Rol
        if (rol != null && !rol.isBlank()) {
            String r = rol.trim().toUpperCase();
            if (!co.clinicalhouse.pqrsf.security.PermisoService.ROLES_VALIDOS.contains(r)) {
                throw new IllegalArgumentException("Rol no válido.");
            }
            if (!r.equals(usuario.getRol())) {
                if ("admin@clinicalhouse.co".equalsIgnoreCase(usuario.getCorreo())) {
                    throw new IllegalArgumentException("No se puede cambiar el rol de la cuenta principal del Super Administrador.");
                }
                if ("SUPER_ADMIN".equals(usuario.getRol())) {
                    long otrosAdmins = usuarioRepository.findAll().stream()
                            .filter(u -> "SUPER_ADMIN".equals(u.getRol()) && Boolean.TRUE.equals(u.getActivo()) && !u.getId().equals(id))
                            .count();
                    if (otrosAdmins < 1) throw new IllegalArgumentException("Debe quedar al menos un Super Administrador activo.");
                }
                usuario.setRol(r);
                cambios.append("rol,");
            }
        }

        if (cambios.length() == 0) return usuario;
        usuario.setActualizadoEl(LocalDateTime.now());
        Usuario guardado = usuarioRepository.save(usuario);
        auditoriaService.registrar("USUARIO", guardado.getId().toString(), "EDICION_USUARIO", adminUsuario, ipOrigen, null,
                "{\"usuario\":\"" + guardado.getCorreo() + "\",\"camposModificados\":\"" + cambios.substring(0, cambios.length() - 1)
                        + "\",\"rol\":\"" + guardado.getRol() + "\"}");
        return guardado;
    }

    @Override
    @Transactional
    public String solicitarRecuperacion(String correo, String ipOrigen, String userAgent) {
        String correoLimpio = correo.trim().toLowerCase();
        // No se revela si el correo tiene cuenta: la respuesta al usuario es la misma en ambos casos
        Usuario usuario = usuarioRepository.findByCorreo(correoLimpio).orElse(null);
        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return null;
        }
        intentosRecuperacion.remove(correoLimpio);

        // Generar código numérico de 6 dígitos
        String codigo = String.format("%06d", new SecureRandom().nextInt(1000000));
        usuario.setCodigoRecuperacion(codigo);
        usuario.setCodigoExpiracion(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "USUARIO",
                usuario.getId().toString(),
                "SOLICITUD_RECUPERACION_PASSWORD",
                correoLimpio,
                ipOrigen,
                userAgent,
                "{\"mensaje\":\"Código de recuperación emitido y enviado por correo para restablecimiento de contraseña\"}"
        );

        // Envío de correo electrónico al usuario con el código de seguridad
        try {
            emailService.enviarCodigoRecuperacion(usuario.getCorreo(), usuario.getNombre(), codigo);
        } catch (Exception e) {
            // Se registra pero no se bloquea
        }

        return codigo;
    }

    @Override
    @Transactional
    public void confirmarRecuperacion(String correo, String codigo, String nuevoPassword, 
                                      String ipOrigen, String userAgent) {
        String correoLimpio = correo.trim().toLowerCase();
        Usuario usuario = usuarioRepository.findByCorreo(correoLimpio)
                .orElseThrow(() -> new RuntimeException("El código de recuperación ingresado es inválido."));

        if (usuario.getCodigoRecuperacion() == null || !usuario.getCodigoRecuperacion().equals(codigo.trim())) {
            // Tras varios intentos fallidos el código se anula (evita adivinarlo probando combinaciones)
            int fallos = intentosRecuperacion.merge(correoLimpio, 1, Integer::sum);
            if (fallos >= MAX_INTENTOS_RECUPERACION && usuario.getCodigoRecuperacion() != null) {
                usuario.setCodigoRecuperacion(null);
                usuario.setCodigoExpiracion(null);
                usuarioRepository.save(usuario);
                intentosRecuperacion.remove(correoLimpio);
                throw new RuntimeException("Se superó el número de intentos. Solicite un código nuevo.");
            }
            throw new RuntimeException("El código de recuperación ingresado es inválido.");
        }
        intentosRecuperacion.remove(correoLimpio);

        if (usuario.getCodigoExpiracion() == null || usuario.getCodigoExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El código de recuperación ha expirado. Por favor solicite uno nuevo.");
        }

        if (nuevoPassword == null || nuevoPassword.trim().length() < 6) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevoPassword.trim()));
        usuario.setCodigoRecuperacion(null);
        usuario.setCodigoExpiracion(null);
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "USUARIO",
                usuario.getId().toString(),
                "RESTABLECIMIENTO_PASSWORD_EXITOSO",
                correoLimpio,
                ipOrigen,
                userAgent,
                "{\"mensaje\":\"Contraseña restablecida satisfactoriamente con código de seguridad\"}"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional
    public Usuario crearUsuario(CrearUsuarioRequestDTO request, String autorizadoPor, String usuarioSesion, String ipOrigen) {
        // La autorización (credenciales de Super Admin) ya fue verificada por AccesoUsuariosService.
        String adminUsuario = AuthServiceImpl.firmaAuditoria(autorizadoPor, usuarioSesion);

        Usuario nuevo = new Usuario();
        nuevo.setNombre(request.getNombre());
        nuevo.setCorreo(request.getCorreo());
        nuevo.setCargo(request.getCargo());
        nuevo.setRol(request.getRol());
        nuevo.setPassword(request.getPassword());

        if (nuevo.getNombre() == null || nuevo.getNombre().trim().length() < 3) {
            throw new IllegalArgumentException("El nombre completo es obligatorio y debe tener al menos 3 caracteres.");
        }

        String correoLimpio = nuevo.getCorreo() != null ? nuevo.getCorreo().trim().toLowerCase() : "";
        if (!correoLimpio.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")) {
            throw new IllegalArgumentException("El formato del correo institucional es inválido.");
        }

        if (usuarioRepository.existsByCorreo(correoLimpio)) {
            throw new IllegalArgumentException("Ya existe un usuario registrado con el correo: " + correoLimpio);
        }

        if (nuevo.getPassword() == null || nuevo.getPassword().length() < 6) {
            throw new IllegalArgumentException("La contraseña inicial debe tener al menos 6 caracteres.");
        }

        String rol = nuevo.getRol() != null ? nuevo.getRol().trim().toUpperCase() : "JEFE_SIAU";
        if (!co.clinicalhouse.pqrsf.security.PermisoService.ROLES_VALIDOS.contains(rol)) {
            throw new IllegalArgumentException("El rol seleccionado no es válido.");
        }

        nuevo.setNombre(nuevo.getNombre().trim());
        nuevo.setCorreo(correoLimpio);
        nuevo.setRol(rol);
        String cargo = configuracionService.validarCargo(request.getCargo());
        nuevo.setCargo(cargo != null ? cargo : "Funcionario SIAU");
        nuevo.setArea(configuracionService.validarArea(request.getArea()));
        nuevo.setPassword(passwordEncoder.encode(nuevo.getPassword()));
        nuevo.setActivo(true);
        nuevo.setActualizadoEl(LocalDateTime.now());
        nuevo.setCreadoEl(LocalDateTime.now());

        Usuario guardado = usuarioRepository.save(nuevo);

        auditoriaService.registrar(
                "USUARIO",
                guardado.getId().toString(),
                "CREACION_USUARIO",
                adminUsuario,
                ipOrigen,
                null,
                "{\"nuevoUsuario\":\"" + guardado.getCorreo() + "\",\"rol\":\"" + guardado.getRol() + "\",\"nombre\":\"" + guardado.getNombre() + "\"}"
        );

        return guardado;
    }

    @Override
    @Transactional
    public Usuario cambiarEstadoUsuario(Long id, boolean activo, String adminUsuario, String ipOrigen) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));

        // Evitar que el super admin se desactive a sí mismo
        if (!activo && "admin@clinicalhouse.co".equalsIgnoreCase(usuario.getCorreo())) {
            throw new IllegalArgumentException("No es posible desactivar la cuenta principal del Super Administrador.");
        }

        usuario.setActivo(activo);
        usuario.setActualizadoEl(LocalDateTime.now());
        Usuario actualizado = usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "USUARIO",
                actualizado.getId().toString(),
                "CAMBIO_ESTADO_USUARIO",
                adminUsuario,
                ipOrigen,
                null,
                "{\"usuario\":\"" + actualizado.getCorreo() + "\",\"nuevoEstadoActivo\":" + activo + "}"
        );

        return actualizado;
    }

    @Override
    @Transactional
    public void eliminarUsuario(Long id, String superAdmin, String usuarioSesion, String ipOrigen) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        // 1. Proteger cuentas semilla del sistema
        if ("admin@clinicalhouse.co".equalsIgnoreCase(usuario.getCorreo())) {
            throw new IllegalArgumentException("No es posible eliminar la cuenta principal del Super Administrador.");
        }
        if ("siau@clinicalhouse.co".equalsIgnoreCase(usuario.getCorreo())) {
            throw new IllegalArgumentException("No es posible eliminar la cuenta principal de la Jefe SIAU.");
        }

        // 2. No se puede auto-eliminar la cuenta en sesión ni la que autorizó el módulo
        if (usuario.getCorreo().equalsIgnoreCase(usuarioSesion) || usuario.getCorreo().equalsIgnoreCase(superAdmin)) {
            throw new IllegalArgumentException("No puede eliminar la cuenta con la que ha iniciado sesión o autorizado este módulo.");
        }

        // 3. Proteger que quede al menos un Super Administrador activo
        if ("SUPER_ADMIN".equalsIgnoreCase(usuario.getRol())) {
            long adminsActivos = usuarioRepository.findAll().stream()
                    .filter(u -> "SUPER_ADMIN".equalsIgnoreCase(u.getRol()) && !u.getId().equals(usuario.getId()) && Boolean.TRUE.equals(u.getActivo()))
                    .count();
            if (adminsActivos < 1) {
                throw new IllegalArgumentException("No se puede eliminar el único Super Administrador activo del sistema.");
            }
        }

        String adminUsuario = firmaAuditoria(superAdmin, usuarioSesion);

        // 4. Reasignar todas las solicitudes asignadas a este funcionario a "Sin asignar"
        List<Pqrsf> solicitudesAsignadas = pqrsfRepository.findByResponsableAsignadoIgnoreCase(usuario.getNombre());
        for (Pqrsf p : solicitudesAsignadas) {
            p.setResponsableAsignado("Sin asignar");
            p.setFechaActualizacion(LocalDateTime.now());
            TrazaPqrsf traza = new TrazaPqrsf(
                    p,
                    p.getEstado(),
                    p.getEstado(),
                    "Reasignado automáticamente a 'Sin asignar' por eliminación del funcionario: " + usuario.getNombre(),
                    adminUsuario
            );
            trazaRepository.save(traza);
        }
        if (!solicitudesAsignadas.isEmpty()) {
            pqrsfRepository.saveAll(solicitudesAsignadas);
        }

        // 5. Eliminar el usuario
        usuarioRepository.delete(usuario);

        // 6. Registrar en auditoría
        auditoriaService.registrar(
                "USUARIO",
                usuario.getId().toString(),
                "ELIMINACION_USUARIO",
                adminUsuario,
                ipOrigen,
                null,
                String.format("{\"usuarioEliminado\":\"%s\",\"nombre\":\"%s\",\"solicitudesReasignadas\":%d}",
                        usuario.getCorreo(), usuario.getNombre(), solicitudesAsignadas.size())
        );
    }

    @Override
    @Transactional
    public LoginResponseDTO actualizarPerfil(String correoActual, ActualizarPerfilDTO dto, String ipOrigen, String userAgent) {
        Usuario usuario = usuarioRepository.findByCorreo(correoActual)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la cuenta de la sesión actual."));

        if (dto.getPasswordActual() == null || !passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new SecurityException("La contraseña actual es incorrecta.");
        }

        StringBuilder cambios = new StringBuilder();

        if (dto.getNombre() != null && !dto.getNombre().isBlank() && !dto.getNombre().trim().equals(usuario.getNombre())) {
            if (dto.getNombre().trim().length() < 3) {
                throw new IllegalArgumentException("El nombre debe tener al menos 3 caracteres.");
            }
            usuario.setNombre(dto.getNombre().trim());
            cambios.append("nombre,");
        }

        if (dto.getCargo() != null && !dto.getCargo().isBlank() && !dto.getCargo().trim().equals(usuario.getCargo())) {
            usuario.setCargo(configuracionService.validarCargo(dto.getCargo()));
            cambios.append("cargo,");
        }

        if (dto.getCorreo() != null && !dto.getCorreo().isBlank()) {
            String nuevoCorreo = dto.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equals(usuario.getCorreo())) {
                if (!nuevoCorreo.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    throw new IllegalArgumentException("El formato del correo es inválido.");
                }
                if (usuarioRepository.existsByCorreo(nuevoCorreo)) {
                    throw new IllegalArgumentException("Ya existe otra cuenta con el correo " + nuevoCorreo + ".");
                }
                usuario.setCorreo(nuevoCorreo);
                cambios.append("correo,");
            }
        }

        if (dto.getNuevoPassword() != null && !dto.getNuevoPassword().isBlank()) {
            if (dto.getNuevoPassword().length() < 8) {
                throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
            }
            usuario.setPassword(passwordEncoder.encode(dto.getNuevoPassword()));
            cambios.append("password,");
        }

        if (cambios.length() == 0) {
            throw new IllegalArgumentException("No hay cambios para guardar.");
        }

        usuario.setActualizadoEl(LocalDateTime.now());
        usuarioRepository.save(usuario);

        auditoriaService.registrar(
                "USUARIO",
                usuario.getId().toString(),
                "ACTUALIZACION_PERFIL",
                correoActual,
                ipOrigen,
                userAgent,
                "{\"camposModificados\":\"" + cambios.substring(0, cambios.length() - 1) + "\"}"
        );

        // Se emite un token nuevo (el correo es el sujeto del JWT)
        String token = tokenProvider.generarToken(usuario.getCorreo(), usuario.getRol(), usuario.getId());
        return sesion(token, usuario);
    }

    @Override
    @Transactional
    public Usuario cambiarPasswordUsuario(Long id, String nuevoPassword, String adminUsuario, String ipOrigen) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + id));

        if (nuevoPassword == null || nuevoPassword.length() < 8) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 8 caracteres.");
        }
        if (nuevoPassword.length() > 72) {
            throw new IllegalArgumentException("La contraseña no puede superar 72 caracteres.");
        }
        if (passwordEncoder.matches(nuevoPassword, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }

        usuario.setPassword(passwordEncoder.encode(nuevoPassword));
        // Invalida cualquier código de recuperación pendiente
        usuario.setCodigoRecuperacion(null);
        usuario.setCodigoExpiracion(null);
        usuario.setActualizadoEl(LocalDateTime.now());
        Usuario actualizado = usuarioRepository.save(usuario);

        // Nunca se registra la contraseña, solo a quién se le cambió
        auditoriaService.registrar(
                "USUARIO",
                actualizado.getId().toString(),
                "CAMBIO_PASSWORD_USUARIO",
                adminUsuario,
                ipOrigen,
                null,
                "{\"usuario\":\"" + actualizado.getCorreo() + "\"}"
        );
        return actualizado;
    }

    /** "admin@x (sesión: jefe@x)" cuando quien autoriza no es quien tiene la sesión. Máx. 150 caracteres. */
    static String firmaAuditoria(String autorizadoPor, String usuarioSesion) {
        String firma = autorizadoPor == null || autorizadoPor.equalsIgnoreCase(usuarioSesion)
                ? usuarioSesion
                : autorizadoPor + " (sesión: " + usuarioSesion + ")";
        return firma != null && firma.length() > 150 ? firma.substring(0, 150) : firma;
    }
}
