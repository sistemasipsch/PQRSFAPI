package co.clinicalhouse.pqrsf.security;

import co.clinicalhouse.pqrsf.model.Cargo;
import co.clinicalhouse.pqrsf.model.PermisoCargo;
import co.clinicalhouse.pqrsf.model.PermisoRol;
import co.clinicalhouse.pqrsf.model.Usuario;
import co.clinicalhouse.pqrsf.repository.CargoRepository;
import co.clinicalhouse.pqrsf.repository.PermisoCargoRepository;
import co.clinicalhouse.pqrsf.repository.PermisoRolRepository;
import co.clinicalhouse.pqrsf.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Permisos de funcionalidades por CARGO (catálogo de Configuración → Cargos).
 * Se guardan en la tabla permisos_cargo y se cachean en memoria.
 * <ul>
 *   <li>El SUPER_ADMIN siempre tiene todos los permisos.</li>
 *   <li>Un usuario sin cargo, o con un cargo sin configuración, recibe los permisos predeterminados de su rol.</li>
 * </ul>
 */
@Service
public class PermisoService {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final List<String> ROLES_VALIDOS = List.of("SUPER_ADMIN", "JEFE_SIAU", "FUNCIONARIO_SIAU");

    private final PermisoCargoRepository repository;
    private final PermisoRolRepository permisoRolRepository;
    private final CargoRepository cargoRepository;
    private final UsuarioRepository usuarioRepository;

    /** cargo (en minúsculas) → permisos */
    private final Map<String, Set<Permiso>> cache = new ConcurrentHashMap<>();

    public PermisoService(PermisoCargoRepository repository, PermisoRolRepository permisoRolRepository,
                          CargoRepository cargoRepository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.permisoRolRepository = permisoRolRepository;
        this.cargoRepository = cargoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Arranque: cada cargo del catálogo sin configuración recibe permisos iniciales.
     * Si algún usuario con ese cargo es Jefe SIAU (o Super Admin), parte de los permisos del Jefe;
     * si no, de los del Funcionario. Se respeta lo que estuviera guardado antes por rol.
     */
    @Transactional
    public void inicializar() {
        for (Cargo c : cargoRepository.findAll()) {
            if (repository.findByCargoIgnoreCase(c.getNombre()).isEmpty()) {
                sembrar(c.getNombre(), rolSugeridoPara(c.getNombre()));
            }
        }
        recargar();
    }

    /** Permisos iniciales de un cargo nuevo (se llama al crearlo en Configuración → Cargos). */
    @Transactional
    public void crearCargo(String cargo) {
        if (repository.findByCargoIgnoreCase(cargo).isEmpty()) {
            sembrar(cargo, rolSugeridoPara(cargo));
            recargar();
        }
    }

    @Transactional
    public void renombrarCargo(String anterior, String nuevo) {
        repository.renombrarCargo(anterior, nuevo);
        recargar();
    }

    @Transactional
    public void eliminarCargo(String cargo) {
        repository.eliminarDeCargo(cargo);
        recargar();
    }

    private String rolSugeridoPara(String cargo) {
        boolean deJefe = usuarioRepository.findAll().stream()
                .filter(u -> cargo.equalsIgnoreCase(u.getCargo()))
                .anyMatch(u -> "JEFE_SIAU".equals(u.getRol()) || SUPER_ADMIN.equals(u.getRol()));
        if (deJefe) return "JEFE_SIAU";
        String n = cargo.toLowerCase(Locale.ROOT);
        if (n.contains("jefe") || n.contains("coordinador") || n.contains("administrador")) return "JEFE_SIAU";
        return "FUNCIONARIO_SIAU";
    }

    private void sembrar(String cargo, String rol) {
        predeterminadosDeRol(rol).forEach(p -> repository.save(new PermisoCargo(cargo, p.name())));
    }

    /** Permisos de un rol: los que se hubieran guardado antes por rol, o los predeterminados del código. */
    private Set<Permiso> predeterminadosDeRol(String rol) {
        List<PermisoRol> guardados = rol == null ? List.of() : permisoRolRepository.findByRol(rol);
        if (guardados.isEmpty()) return Permiso.predeterminados(rol);
        Set<Permiso> set = EnumSet.noneOf(Permiso.class);
        guardados.forEach(pr -> agregar(set, pr.getPermiso()));
        return set;
    }

    private void recargar() {
        cache.clear();
        for (PermisoCargo pc : repository.findAll()) {
            agregar(cache.computeIfAbsent(clave(pc.getCargo()), k -> EnumSet.noneOf(Permiso.class)), pc.getPermiso());
        }
        // Cargos configurados sin ningún permiso marcado también cuentan (lista vacía ≠ sin configurar)
        for (Cargo c : cargoRepository.findAll()) cache.putIfAbsent(clave(c.getNombre()), EnumSet.noneOf(Permiso.class));
    }

    private static void agregar(Set<Permiso> set, String codigo) {
        try {
            set.add(Permiso.valueOf(codigo));
        } catch (IllegalArgumentException ignorado) {
            // Permiso eliminado del código: se ignora
        }
    }

    private static String clave(String cargo) {
        return cargo == null ? "" : cargo.trim().toLowerCase(Locale.ROOT);
    }

    // =====================================================================
    // CONSULTA
    // =====================================================================

    /** Permisos efectivos de un usuario según su rol y cargo. */
    public Set<Permiso> permisosDe(Usuario u) {
        if (u == null) return EnumSet.noneOf(Permiso.class);
        return permisosDe(u.getRol(), u.getCargo());
    }

    public Set<Permiso> permisosDe(String rol, String cargo) {
        if (SUPER_ADMIN.equalsIgnoreCase(rol)) return EnumSet.allOf(Permiso.class);
        Set<Permiso> deCargo = cargo == null ? null : cache.get(clave(cargo));
        if (deCargo != null) return EnumSet.copyOf((EnumSet<Permiso>) deCargo);
        return Permiso.predeterminados(rol == null ? "" : rol.toUpperCase(Locale.ROOT));
    }

    public List<String> codigosDe(Usuario u) {
        return permisosDe(u).stream().map(Enum::name).toList();
    }

    /** Rol del usuario autenticado (a partir de la autoridad ROLE_X del JWT). */
    public static String rolDe(Authentication auth) {
        if (auth == null) return null;
        Collection<? extends GrantedAuthority> auths = auth.getAuthorities();
        return auths.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst().orElse(null);
    }

    /** Lanza 403 si el usuario autenticado no tiene el permiso (se mira su cargo actual en la base). */
    public void exigir(Authentication auth, Permiso permiso) {
        String rol = rolDe(auth);
        if (SUPER_ADMIN.equalsIgnoreCase(rol)) return;
        String cargo = auth == null ? null : usuarioRepository.findByCorreo(auth.getName())
                .map(Usuario::getCargo).orElse(null);
        if (!permisosDe(rol, cargo).contains(permiso)) {
            throw new AccessDeniedException("Sin permiso: " + permiso.name());
        }
    }

    /** Lanza 403 si el usuario no es Super Administrador. */
    public void exigirSuperAdmin(Authentication auth) {
        if (!SUPER_ADMIN.equalsIgnoreCase(rolDe(auth))) {
            throw new SecurityException("Solo un Super Administrador puede realizar esta acción.");
        }
    }

    // =====================================================================
    // CONFIGURACIÓN
    // =====================================================================

    /** Matriz cargo → permisos para la pantalla de Configuración → Permisos. */
    @Transactional(readOnly = true)
    public Map<String, Object> matriz() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("permisos", Arrays.stream(Permiso.values()).map(p -> Map.of(
                "codigo", p.name(), "descripcion", p.getDescripcion(), "grupo", p.getGrupo())).toList());

        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Map<String, Object>> cargos = new ArrayList<>();
        for (Cargo c : cargoRepository.findAllByOrderByNombreAsc()) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("cargo", c.getNombre());
            fila.put("activo", c.getActivo());
            fila.put("usuarios", usuarios.stream().filter(u -> c.getNombre().equalsIgnoreCase(u.getCargo()))
                    .map(u -> Map.of("nombre", u.getNombre(), "rol", u.getRol())).toList());
            fila.put("permisos", cache.getOrDefault(clave(c.getNombre()), EnumSet.noneOf(Permiso.class))
                    .stream().map(Enum::name).toList());
            cargos.add(fila);
        }
        m.put("cargos", cargos);
        return m;
    }

    /** Reemplaza los permisos de un cargo. Devuelve el nombre del cargo tal como está en el catálogo. */
    @Transactional
    public String guardar(String cargo, List<String> codigos) {
        Cargo c = cargoRepository.findByNombreIgnoreCase(cargo == null ? "" : cargo.trim())
                .orElseThrow(() -> new IllegalArgumentException("El cargo «" + cargo + "» no existe."));
        Set<Permiso> nuevos = EnumSet.noneOf(Permiso.class);
        for (String cod : codigos == null ? List.<String>of() : codigos) {
            try {
                nuevos.add(Permiso.valueOf(cod));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Permiso no reconocido: " + cod);
            }
        }
        repository.eliminarDeCargo(c.getNombre());
        repository.flush();
        nuevos.forEach(p -> repository.save(new PermisoCargo(c.getNombre(), p.name())));
        recargar();
        return c.getNombre();
    }

    public List<String> codigosDeCargo(String cargo) {
        return cache.getOrDefault(clave(cargo), EnumSet.noneOf(Permiso.class)).stream().map(Enum::name).toList();
    }
}
