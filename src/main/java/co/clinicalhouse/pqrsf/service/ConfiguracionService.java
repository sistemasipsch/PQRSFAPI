package co.clinicalhouse.pqrsf.service;

import co.clinicalhouse.pqrsf.model.Area;
import co.clinicalhouse.pqrsf.model.Cargo;
import co.clinicalhouse.pqrsf.model.ConfigSla;
import co.clinicalhouse.pqrsf.model.Pqrsf;
import co.clinicalhouse.pqrsf.model.Usuario;
import co.clinicalhouse.pqrsf.repository.AreaRepository;
import co.clinicalhouse.pqrsf.repository.CargoRepository;
import co.clinicalhouse.pqrsf.repository.ConfigSlaRepository;
import co.clinicalhouse.pqrsf.repository.PqrsfRepository;
import co.clinicalhouse.pqrsf.repository.UsuarioRepository;
import co.clinicalhouse.pqrsf.sla.SlaPolicy;
import co.clinicalhouse.pqrsf.sla.SlaPolicy.NivelRiesgo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Catálogos y parámetros editables desde el panel: tiempos de respuesta (SLA), áreas y cargos. */
@Service
public class ConfiguracionService {

    private static final String CLAVE_UMBRAL = "UMBRAL";
    private static final long MAX_HORAS = 24L * 90; // 90 días

    private static final List<String> AREAS_INICIALES = List.of(
            "Urgencias", "Hospitalización", "Consulta externa", "Cirugía", "Laboratorio clínico",
            "Imágenes diagnósticas", "Farmacia", "Facturación", "Admisiones", "SIAU");

    private final ConfigSlaRepository slaRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PqrsfRepository pqrsfRepository;
    private final AuditoriaService auditoriaService;
    private final co.clinicalhouse.pqrsf.security.PermisoService permisoService;

    public ConfiguracionService(ConfigSlaRepository slaRepository, AreaRepository areaRepository,
                                CargoRepository cargoRepository, UsuarioRepository usuarioRepository,
                                PqrsfRepository pqrsfRepository, AuditoriaService auditoriaService,
                                co.clinicalhouse.pqrsf.security.PermisoService permisoService) {
        this.permisoService = permisoService;
        this.slaRepository = slaRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pqrsfRepository = pqrsfRepository;
        this.auditoriaService = auditoriaService;
    }

    /** Arranque: siembra catálogos vacíos y aplica los tiempos de respuesta guardados. */
    @Transactional
    public void inicializar() {
        if (areaRepository.count() == 0) {
            AREAS_INICIALES.forEach(n -> areaRepository.save(new Area(n, null)));
        }
        if (cargoRepository.count() == 0) {
            // Se parte de los cargos que ya tienen los usuarios existentes
            usuarioRepository.findAll().stream()
                    .map(Usuario::getCargo)
                    .filter(Objects::nonNull).map(String::trim).filter(c -> !c.isEmpty())
                    .distinct()
                    .forEach(c -> cargoRepository.save(new Cargo(c, null)));
            if (cargoRepository.findByNombreIgnoreCase("Funcionario SIAU").isEmpty()) {
                cargoRepository.save(new Cargo("Funcionario SIAU", null));
            }
        }
        aplicarSla();
    }

    // =====================================================================
    // TIEMPOS DE RESPUESTA (SLA)
    // =====================================================================

    private void aplicarSla() {
        Map<NivelRiesgo, Long> plazos = new EnumMap<>(NivelRiesgo.class);
        Double umbral = null;
        for (ConfigSla c : slaRepository.findAll()) {
            if (CLAVE_UMBRAL.equals(c.getClave())) {
                umbral = c.getValor() / 100.0;
            } else {
                try {
                    plazos.put(NivelRiesgo.valueOf(c.getClave()), c.getValor());
                } catch (IllegalArgumentException ignorado) {
                    // clave desconocida
                }
            }
        }
        SlaPolicy.configurar(plazos, umbral);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerSla() {
        List<Map<String, Object>> niveles = new ArrayList<>();
        for (NivelRiesgo n : NivelRiesgo.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nivel", n.name());
            m.put("etiqueta", n.getEtiqueta());
            m.put("plazoHoras", n.getPlazoHoras());
            m.put("plazoPredeterminadoHoras", n.getPlazoPredeterminadoHoras());
            niveles.add(m);
        }
        ConfigSla ultima = slaRepository.findAll().stream()
                .filter(c -> c.getActualizadoEl() != null)
                .max((a, b) -> a.getActualizadoEl().compareTo(b.getActualizadoEl()))
                .orElse(null);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("niveles", niveles);
        r.put("umbralAlertaPorcentaje", Math.round(SlaPolicy.getUmbralAlerta() * 100));
        r.put("actualizadoPor", ultima != null ? ultima.getActualizadoPor() : null);
        r.put("actualizadoEl", ultima != null ? ultima.getActualizadoEl() : null);
        return r;
    }

    @Transactional
    public Map<String, Object> guardarSla(Map<String, Long> plazosHoras, Long umbralPorcentaje, String usuario, String ip) {
        StringBuilder detalle = new StringBuilder("{");
        for (NivelRiesgo n : NivelRiesgo.values()) {
            Long horas = plazosHoras == null ? null : plazosHoras.get(n.name());
            if (horas == null) continue;
            if (horas < 1 || horas > MAX_HORAS) {
                throw new IllegalArgumentException("El plazo de " + n.getEtiqueta().toLowerCase()
                        + " debe estar entre 1 y " + MAX_HORAS + " horas.");
            }
            guardarClave(n.name(), horas, usuario);
            detalle.append('"').append(n.name()).append("\":").append(horas).append(',');
        }
        if (umbralPorcentaje != null) {
            if (umbralPorcentaje < 10 || umbralPorcentaje > 95) {
                throw new IllegalArgumentException("El umbral de alerta debe estar entre 10% y 95%.");
            }
            guardarClave(CLAVE_UMBRAL, umbralPorcentaje, usuario);
            detalle.append("\"umbralAlerta\":").append(umbralPorcentaje).append(',');
        }
        if (detalle.length() > 1) detalle.setLength(detalle.length() - 1);
        detalle.append('}');

        aplicarSla();
        auditoriaService.registrar("SISTEMA", "SLA", "CONFIGURACION_TIEMPOS", usuario, ip, null, detalle.toString());
        return obtenerSla();
    }

    private void guardarClave(String clave, Long valor, String usuario) {
        ConfigSla c = slaRepository.findById(clave).orElseGet(() -> new ConfigSla(clave, valor));
        c.setValor(valor);
        c.setActualizadoPor(usuario);
        c.setActualizadoEl(LocalDateTime.now());
        slaRepository.save(c);
    }

    // =====================================================================
    // ÁREAS
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Area> listarAreas() {
        return areaRepository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Area guardarArea(Long id, String nombre, String descripcion, Boolean activo, String usuario, String ip) {
        String limpio = validarNombre(nombre, "área");
        areaRepository.findByNombreIgnoreCase(limpio)
                .filter(a -> !a.getId().equals(id))
                .ifPresent(a -> { throw new IllegalArgumentException("Ya existe un área llamada «" + a.getNombre() + "»."); });

        Area area = id == null ? new Area() : areaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
        String anterior = area.getNombre();
        area.setNombre(limpio);
        area.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
        if (activo != null) area.setActivo(activo);
        Area guardada = areaRepository.save(area);

        // Al renombrar se actualiza en usuarios y solicitudes para no dejar valores huérfanos
        if (anterior != null && !anterior.equals(limpio)) {
            usuarioRepository.findAll().stream().filter(u -> anterior.equalsIgnoreCase(u.getArea()))
                    .forEach(u -> { u.setArea(limpio); usuarioRepository.save(u); });
            pqrsfRepository.findByAreaIgnoreCase(anterior)
                    .forEach(p -> { p.setArea(limpio); pqrsfRepository.save(p); });
        }

        auditoriaService.registrar("SISTEMA", "AREA-" + guardada.getId(), id == null ? "CREACION_AREA" : "EDICION_AREA",
                usuario, ip, null, "{\"area\":\"" + limpio.replace("\"", "'") + "\",\"activo\":" + guardada.getActivo() + "}");
        return guardada;
    }

    @Transactional
    public void eliminarArea(Long id, String usuario, String ip) {
        Area area = areaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Área no encontrada."));
        long enUsuarios = usuarioRepository.findAll().stream().filter(u -> area.getNombre().equalsIgnoreCase(u.getArea())).count();
        List<Pqrsf> enPqrsf = pqrsfRepository.findByAreaIgnoreCase(area.getNombre());
        if (enUsuarios > 0 || !enPqrsf.isEmpty()) {
            throw new IllegalArgumentException("El área «" + area.getNombre() + "» está en uso (" + enUsuarios + " usuario(s), "
                    + enPqrsf.size() + " solicitud(es)). Desactívela en lugar de eliminarla.");
        }
        areaRepository.delete(area);
        auditoriaService.registrar("SISTEMA", "AREA-" + id, "ELIMINACION_AREA", usuario, ip, null,
                "{\"area\":\"" + area.getNombre().replace("\"", "'") + "\"}");
    }

    // =====================================================================
    // CARGOS
    // =====================================================================

    @Transactional(readOnly = true)
    public List<Cargo> listarCargos() {
        return cargoRepository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Cargo guardarCargo(Long id, String nombre, String descripcion, Boolean activo, String usuario, String ip) {
        String limpio = validarNombre(nombre, "cargo");
        cargoRepository.findByNombreIgnoreCase(limpio)
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new IllegalArgumentException("Ya existe un cargo llamado «" + c.getNombre() + "»."); });

        Cargo cargo = id == null ? new Cargo() : cargoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado."));
        String anterior = cargo.getNombre();
        cargo.setNombre(limpio);
        cargo.setDescripcion(descripcion != null && !descripcion.isBlank() ? descripcion.trim() : null);
        if (activo != null) cargo.setActivo(activo);
        Cargo guardado = cargoRepository.save(cargo);

        if (anterior != null && !anterior.equals(limpio)) {
            usuarioRepository.findAll().stream().filter(u -> anterior.equalsIgnoreCase(u.getCargo()))
                    .forEach(u -> { u.setCargo(limpio); usuarioRepository.save(u); });
            permisoService.renombrarCargo(anterior, limpio); // los permisos siguen al cargo
        }
        if (anterior == null) {
            permisoService.crearCargo(limpio); // permisos iniciales del cargo nuevo
        }

        auditoriaService.registrar("SISTEMA", "CARGO-" + guardado.getId(), id == null ? "CREACION_CARGO" : "EDICION_CARGO",
                usuario, ip, null, "{\"cargo\":\"" + limpio.replace("\"", "'") + "\",\"activo\":" + guardado.getActivo() + "}");
        return guardado;
    }

    @Transactional
    public void eliminarCargo(Long id, String usuario, String ip) {
        Cargo cargo = cargoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Cargo no encontrado."));
        long enUso = usuarioRepository.findAll().stream().filter(u -> cargo.getNombre().equalsIgnoreCase(u.getCargo())).count();
        if (enUso > 0) {
            throw new IllegalArgumentException("El cargo «" + cargo.getNombre() + "» lo tienen " + enUso
                    + " usuario(s). Cámbieles el cargo o desactívelo en lugar de eliminarlo.");
        }
        cargoRepository.delete(cargo);
        permisoService.eliminarCargo(cargo.getNombre());
        auditoriaService.registrar("SISTEMA", "CARGO-" + id, "ELIMINACION_CARGO", usuario, ip, null,
                "{\"cargo\":\"" + cargo.getNombre().replace("\"", "'") + "\"}");
    }

    /** Verifica que el valor exista en el catálogo de áreas (activa). Null/vacío = sin área. */
    @Transactional(readOnly = true)
    public String validarArea(String area) {
        if (area == null || area.isBlank()) return null;
        return areaRepository.findByNombreIgnoreCase(area.trim())
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .map(Area::getNombre)
                .orElseThrow(() -> new IllegalArgumentException("El área «" + area + "» no existe o está inactiva."));
    }

    /** Verifica que el valor exista en el catálogo de cargos (activo). */
    @Transactional(readOnly = true)
    public String validarCargo(String cargo) {
        if (cargo == null || cargo.isBlank()) return null;
        return cargoRepository.findByNombreIgnoreCase(cargo.trim())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .map(Cargo::getNombre)
                .orElseThrow(() -> new IllegalArgumentException("El cargo «" + cargo + "» no existe o está inactivo."));
    }

    private static String validarNombre(String nombre, String que) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() < 2 || limpio.length() > 100) {
            throw new IllegalArgumentException("El nombre del " + que + " debe tener entre 2 y 100 caracteres.");
        }
        return limpio;
    }
}
