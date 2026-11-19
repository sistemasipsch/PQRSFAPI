package co.clinicalhouse.pqrsf.controller;

import co.clinicalhouse.pqrsf.dto.ActualizarPerfilDTO;
import co.clinicalhouse.pqrsf.dto.CrearUsuarioRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginRequestDTO;
import co.clinicalhouse.pqrsf.dto.LoginResponseDTO;
import co.clinicalhouse.pqrsf.model.Usuario;
import co.clinicalhouse.pqrsf.security.AccesoUsuariosService;
import co.clinicalhouse.pqrsf.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AccesoUsuariosService accesoUsuarios;
    private final co.clinicalhouse.pqrsf.repository.UsuarioRepository usuarioRepository;

    @Autowired
    public AuthController(AuthService authService, AccesoUsuariosService accesoUsuarios,
                          co.clinicalhouse.pqrsf.repository.UsuarioRepository usuarioRepository) {
        this.authService = authService;
        this.accesoUsuarios = accesoUsuarios;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Inicio de sesión para la Jefe de SIAU y Super Admin.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {

        String ipOrigen = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            LoginResponseDTO response = authService.autenticar(request, ipOrigen, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage(), "error", "Unauthorized"));
        }
    }

    /**
     * Paso 1: Solicitar código de recuperación de contraseña.
     */
    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<?> solicitarRecuperacion(
            @RequestBody Map<String, String> payload,
            HttpServletRequest httpRequest) {

        String correo = payload.get("correo");
        if (correo == null || correo.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El correo es obligatorio"));
        }

        String ipOrigen = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            authService.solicitarRecuperacion(correo, ipOrigen, userAgent);
            return ResponseEntity.ok(Map.of(
                    "message", "Si el correo corresponde a una cuenta activa, recibirá un código de seguridad de 6 dígitos. Revise también la carpeta de spam."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Paso 2: Confirmar código y actualizar contraseña.
     */
    @PostMapping("/recuperar/confirmar")
    public ResponseEntity<?> confirmarRecuperacion(
            @RequestBody Map<String, String> payload,
            HttpServletRequest httpRequest) {

        String correo = payload.get("correo");
        String codigo = payload.get("codigo");
        String nuevoPassword = payload.get("nuevoPassword");

        if (correo == null || codigo == null || nuevoPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Todos los campos son obligatorios"));
        }

        String ipOrigen = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        try {
            authService.confirmarRecuperacion(correo, codigo, nuevoPassword, ipOrigen, userAgent);
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente. Ya puede iniciar sesión."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Datos y permisos vigentes de la sesión. El panel lo consulta al cargar para aplicar
     * cambios de permisos hechos por el Super Admin sin obligar a cerrar sesión.
     */
    @GetMapping("/sesion")
    public ResponseEntity<LoginResponseDTO> sesionActual(Authentication auth) {
        return ResponseEntity.ok(authService.sesionActual(auth.getName()));
    }

    /**
     * Funcionarios que pueden ser responsables de una PQRSF: usuarios activos del sistema.
     * Disponible para cualquier funcionario autenticado.
     */
    @GetMapping("/responsables")
    public ResponseEntity<List<Map<String, Object>>> listarResponsables() {
        return ResponseEntity.ok(usuarioRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(u -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nombre", u.getNombre());
                    m.put("correo", u.getCorreo());
                    m.put("cargo", u.getCargo());
                    m.put("area", u.getArea());
                    m.put("rol", u.getRol());
                    return m;
                })
                .toList());
    }

    // =====================================================================
    // MÓDULO DE USUARIOS: protegido por credenciales de Super Administrador
    // =====================================================================

    /**
     * Habilita el módulo de Usuarios. Solo funciona con el correo y la contraseña de un
     * Super Administrador activo; devuelve una autorización temporal (header X-Acceso-Usuarios).
     */
    @PostMapping("/usuarios/acceso")
    public ResponseEntity<Map<String, Object>> accederUsuarios(
            @RequestBody Map<String, String> credenciales,
            Authentication auth,
            HttpServletRequest request) {
        return ResponseEntity.ok(accesoUsuarios.autorizar(
                credenciales.get("correo"), credenciales.get("password"),
                auth.getName(), extraerIp(request), request.getHeader("User-Agent")));
    }

    /** Listar usuarios (requiere la autorización del módulo). */
    @GetMapping("/usuarios")
    public ResponseEntity<List<Usuario>> listarUsuarios(
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth) {
        accesoUsuarios.verificar(acceso, auth.getName());
        return ResponseEntity.ok(authService.listarUsuarios());
    }

    /** Crear usuario (requiere la autorización del módulo). */
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(
            @RequestBody CrearUsuarioRequestDTO nuevo,
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth,
            HttpServletRequest request) {
        String superAdmin = accesoUsuarios.verificar(acceso, auth.getName());
        Usuario creado = authService.crearUsuario(nuevo, superAdmin, auth.getName(), extraerIp(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Actualizar el perfil del usuario autenticado (nombre, correo, cargo, contraseña).
     */
    @PutMapping("/perfil")
    public ResponseEntity<LoginResponseDTO> actualizarPerfil(
            @RequestBody ActualizarPerfilDTO dto,
            Authentication auth,
            HttpServletRequest request) {

        LoginResponseDTO sesion = authService.actualizarPerfil(
                auth.getName(), dto, extraerIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(sesion);
    }

    /** Activar o desactivar un usuario (requiere la autorización del módulo). */
    @PutMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> payload,
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth,
            HttpServletRequest request) {
        String superAdmin = accesoUsuarios.verificar(acceso, auth.getName());

        Boolean activo = payload.get("activo");
        if (activo == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El parámetro 'activo' es obligatorio."));
        }
        String firma = superAdmin.equalsIgnoreCase(auth.getName()) ? superAdmin : superAdmin + " (sesión: " + auth.getName() + ")";
        return ResponseEntity.ok(authService.cambiarEstadoUsuario(id, activo, firma, extraerIp(request)));
    }

    /** Editar un usuario (nombre, correo, cargo, área, rol) (requiere la autorización del módulo). */
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> editarUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth,
            HttpServletRequest request) {
        String superAdmin = accesoUsuarios.verificar(acceso, auth.getName());
        String firma = superAdmin.equalsIgnoreCase(auth.getName()) ? superAdmin : superAdmin + " (sesión: " + auth.getName() + ")";
        try {
            Usuario actualizado = authService.editarUsuario(
                    id,
                    payload.get("nombre"),
                    payload.get("correo"),
                    payload.get("cargo"),
                    payload.get("area"),
                    payload.get("rol"),
                    firma,
                    extraerIp(request)
            );
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Cambiar la contraseña de un usuario (requiere la autorización del módulo). */
    @PutMapping("/usuarios/{id}/password")
    public ResponseEntity<Usuario> cambiarPasswordUsuario(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth,
            HttpServletRequest request) {
        String superAdmin = accesoUsuarios.verificar(acceso, auth.getName());
        String firma = superAdmin.equalsIgnoreCase(auth.getName()) ? superAdmin : superAdmin + " (sesión: " + auth.getName() + ")";
        return ResponseEntity.ok(authService.cambiarPasswordUsuario(id, payload.get("nuevoPassword"), firma, extraerIp(request)));
    }

    /** Eliminar usuario permanentemente (requiere la autorización del módulo). */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = AccesoUsuariosService.HEADER, required = false) String acceso,
            Authentication auth,
            HttpServletRequest request) {
        String superAdmin = accesoUsuarios.verificar(acceso, auth.getName());
        try {
            authService.eliminarUsuario(id, superAdmin, auth.getName(), extraerIp(request));
            return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String extraerIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
