package co.clinicalhouse.pqrsf.controller;

import co.clinicalhouse.pqrsf.model.Area;
import co.clinicalhouse.pqrsf.model.Cargo;
import co.clinicalhouse.pqrsf.security.Permiso;
import co.clinicalhouse.pqrsf.security.PermisoService;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import co.clinicalhouse.pqrsf.service.ConfiguracionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración del sistema desde el panel.
 *  - Tiempos de respuesta: lectura para todos; edición con el permiso CONFIGURAR_TIEMPOS.
 *  - Áreas, cargos y permisos: lectura de catálogos para todos; edición SOLO Super Administrador.
 */
@RestController
@RequestMapping("/api/config")
public class ConfiguracionController {

    private final ConfiguracionService config;
    private final PermisoService permisos;
    private final AuditoriaService auditoria;

    public ConfiguracionController(ConfiguracionService config, PermisoService permisos, AuditoriaService auditoria) {
        this.config = config;
        this.permisos = permisos;
        this.auditoria = auditoria;
    }

    // ------------------------------------------------------------------ Tiempos de respuesta
    @GetMapping("/sla")
    public ResponseEntity<Map<String, Object>> obtenerSla() {
        return ResponseEntity.ok(config.obtenerSla());
    }

    @PutMapping("/sla")
    public ResponseEntity<Map<String, Object>> guardarSla(@RequestBody Map<String, Object> body, Authentication auth,
                                                          HttpServletRequest request) {
        permisos.exigir(auth, Permiso.CONFIGURAR_TIEMPOS);
        Map<String, Long> plazos = new LinkedHashMap<>();
        Object raw = body.get("plazosHoras");
        if (raw instanceof Map<?, ?> m) {
            m.forEach((k, v) -> plazos.put(String.valueOf(k), aLong(v, "plazo de " + k)));
        }
        Long umbral = body.get("umbralAlertaPorcentaje") == null ? null : aLong(body.get("umbralAlertaPorcentaje"), "umbral de alerta");
        return ResponseEntity.ok(config.guardarSla(plazos, umbral, auth.getName(), ip(request)));
    }

    // ------------------------------------------------------------------ Áreas
    @GetMapping("/areas")
    public ResponseEntity<List<Area>> listarAreas() {
        return ResponseEntity.ok(config.listarAreas());
    }

    @PostMapping("/areas")
    public ResponseEntity<Area> crearArea(@RequestBody Map<String, Object> body, Authentication auth, HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(config.guardarArea(null, texto(body, "nombre"),
                texto(body, "descripcion"), booleano(body, "activo"), auth.getName(), ip(request)));
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<Area> editarArea(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth,
                                           HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        return ResponseEntity.ok(config.guardarArea(id, texto(body, "nombre"), texto(body, "descripcion"),
                booleano(body, "activo"), auth.getName(), ip(request)));
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<Map<String, String>> eliminarArea(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        config.eliminarArea(id, auth.getName(), ip(request));
        return ResponseEntity.ok(Map.of("message", "Área eliminada."));
    }

    // ------------------------------------------------------------------ Cargos
    @GetMapping("/cargos")
    public ResponseEntity<List<Cargo>> listarCargos() {
        return ResponseEntity.ok(config.listarCargos());
    }

    @PostMapping("/cargos")
    public ResponseEntity<Cargo> crearCargo(@RequestBody Map<String, Object> body, Authentication auth, HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        return ResponseEntity.status(HttpStatus.CREATED).body(config.guardarCargo(null, texto(body, "nombre"),
                texto(body, "descripcion"), booleano(body, "activo"), auth.getName(), ip(request)));
    }

    @PutMapping("/cargos/{id}")
    public ResponseEntity<Cargo> editarCargo(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth,
                                             HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        return ResponseEntity.ok(config.guardarCargo(id, texto(body, "nombre"), texto(body, "descripcion"),
                booleano(body, "activo"), auth.getName(), ip(request)));
    }

    @DeleteMapping("/cargos/{id}")
    public ResponseEntity<Map<String, String>> eliminarCargo(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        config.eliminarCargo(id, auth.getName(), ip(request));
        return ResponseEntity.ok(Map.of("message", "Cargo eliminado."));
    }

    // ------------------------------------------------------------------ Permisos por cargo
    @GetMapping("/permisos")
    public ResponseEntity<Map<String, Object>> matrizPermisos(Authentication auth) {
        permisos.exigirSuperAdmin(auth);
        return ResponseEntity.ok(permisos.matriz());
    }

    /** Body: { "cargo": "Gestora de Calidad SIAU", "permisos": ["VER_PQRSF", ...] } */
    @PutMapping("/permisos")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> guardarPermisos(@RequestBody Map<String, Object> body,
                                                               Authentication auth, HttpServletRequest request) {
        permisos.exigirSuperAdmin(auth);
        Object lista = body.get("permisos");
        List<String> codigos = lista instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
        String cargo = permisos.guardar(texto(body, "cargo"), codigos);
        auditoria.registrar("SISTEMA", "PERMISOS-CARGO", "CONFIGURACION_PERMISOS", auth.getName(), ip(request), null,
                "{\"cargo\":\"" + cargo.replace("\"", "'") + "\",\"permisos\":\""
                        + String.join(",", permisos.codigosDeCargo(cargo)) + "\"}");
        return ResponseEntity.ok(permisos.matriz());
    }

    // ------------------------------------------------------------------ utilidades
    private static String texto(Map<String, Object> body, String campo) {
        Object v = body.get(campo);
        return v == null ? null : v.toString();
    }

    private static Boolean booleano(Map<String, Object> body, String campo) {
        Object v = body.get(campo);
        return v == null ? null : Boolean.valueOf(v.toString());
    }

    private static Long aLong(Object v, String campo) {
        try {
            return v == null ? null : Math.round(Double.parseDouble(v.toString()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico inválido para " + campo + ".");
        }
    }

    private static String ip(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null && !xf.isEmpty() ? xf.split(",")[0].trim() : request.getRemoteAddr();
    }
}
