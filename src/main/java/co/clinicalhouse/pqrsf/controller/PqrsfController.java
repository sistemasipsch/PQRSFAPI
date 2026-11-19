package co.clinicalhouse.pqrsf.controller;

import co.clinicalhouse.pqrsf.dto.DashboardStatsDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfRequestDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfResponseDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfStatusChangeDTO;
import co.clinicalhouse.pqrsf.dto.ReporteSiauDTO;
import co.clinicalhouse.pqrsf.dto.RespuestaCorreoDTO;
import co.clinicalhouse.pqrsf.security.Permiso;
import co.clinicalhouse.pqrsf.security.PermisoService;
import co.clinicalhouse.pqrsf.service.PqrsfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pqrsf")
public class PqrsfController {

    private final PqrsfService pqrsfService;
    private final PermisoService permisos;

    @Autowired
    public PqrsfController(PqrsfService pqrsfService, PermisoService permisos) {
        this.pqrsfService = pqrsfService;
        this.permisos = permisos;
    }

    /**
     * Radicar nueva solicitud PQRSF (Formulario Público).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PqrsfResponseDTO> radicar(
            @Valid @ModelAttribute PqrsfRequestDTO requestDTO,
            @RequestParam(value = "archivos", required = false) List<MultipartFile> archivos,
            HttpServletRequest request) {

        String ipOrigen = extraerIp(request);
        String userAgent = request.getHeader("User-Agent");

        PqrsfResponseDTO response = pqrsfService.radicar(requestDTO, archivos, ipOrigen, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Listar radicados con filtros por estado, responsable o término de búsqueda (Panel Jefe SIAU).
     */
    @GetMapping
    public ResponseEntity<Page<PqrsfResponseDTO>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String responsable,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String termino,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Authentication auth) {

        permisos.exigir(auth, Permiso.VER_PQRSF);
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200));
        Page<PqrsfResponseDTO> resultado = pqrsfService.listar(estado, responsable, area, termino, pageable);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Obtener estadísticas de radicación para tarjetas del dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> obtenerEstadisticas() {
        return ResponseEntity.ok(pqrsfService.obtenerEstadisticas());
    }

    /**
     * Consulta por consecutivo (solo funcionarios con sesión: la respuesta contiene datos personales).
     */
    @GetMapping("/consulta/{consecutivo}")
    public ResponseEntity<PqrsfResponseDTO> consultarPorConsecutivo(@PathVariable String consecutivo, Authentication auth) {
        permisos.exigir(auth, Permiso.VER_PQRSF);
        return ResponseEntity.ok(pqrsfService.obtenerPorConsecutivo(consecutivo));
    }

    /**
     * Detalle de SLA de todas las solicitudes del periodo (exportación CSV desde Reportes).
     */
    @GetMapping("/exportar")
    public ResponseEntity<List<PqrsfResponseDTO>> exportar(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            Authentication auth) {
        permisos.exigir(auth, Permiso.EXPORTAR_REPORTES);
        return ResponseEntity.ok(pqrsfService.exportar(fechaDesde, fechaHasta));
    }

    /**
     * Detalle completo de una PQRSF por ID (Panel Jefe SIAU).
     */
    @GetMapping("/{id}")
    public ResponseEntity<PqrsfResponseDTO> obtenerPorId(
            @PathVariable Long id,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.VER_PQRSF);
        String usuario = auth != null ? auth.getName() : "JEFE_SIAU";
        return ResponseEntity.ok(pqrsfService.obtenerPorId(id, usuario, extraerIp(request)));
    }

    /**
     * Cambiar estado de una PQRSF y registrar traza y auditoría (Panel Jefe SIAU).
     * Una solicitud Solucionada ya no puede cambiar de estado.
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<PqrsfResponseDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody PqrsfStatusChangeDTO dto,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.CAMBIAR_ESTADO);
        PqrsfResponseDTO actualizado = pqrsfService.cambiarEstado(id, dto, auth.getName(), extraerIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Asignar funcionario responsable de la PQRSF (Panel Jefe SIAU).
     */
    @PutMapping("/{id}/responsable")
    public ResponseEntity<PqrsfResponseDTO> asignarResponsable(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.ASIGNAR_RESPONSABLE);
        String responsable = payload.get("responsable");
        String observacion = payload.getOrDefault("observacion", "");

        PqrsfResponseDTO actualizado = pqrsfService.asignarResponsable(id, responsable, observacion,
                auth.getName(), extraerIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Asignar el área / servicio involucrado (catálogo de Áreas).
     */
    @PutMapping("/{id}/area")
    public ResponseEntity<PqrsfResponseDTO> asignarArea(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.ASIGNAR_RESPONSABLE);
        return ResponseEntity.ok(pqrsfService.asignarArea(id, payload.get("area"),
                auth.getName(), extraerIp(request), request.getHeader("User-Agent")));
    }

    /**
     * Enviar respuesta oficial al ciudadano por correo desde el panel (Jefe SIAU).
     */
    @PostMapping("/{id}/respuesta")
    public ResponseEntity<PqrsfResponseDTO> responderPorCorreo(
            @PathVariable Long id,
            @Valid @RequestBody RespuestaCorreoDTO dto,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.RESPONDER_CORREO);
        PqrsfResponseDTO actualizado = pqrsfService.responderPorCorreo(
                id, dto, auth.getName(), extraerIp(request), request.getHeader("User-Agent"));
        return ResponseEntity.ok(actualizado);
    }

    /**
     * Registrar la respuesta enviada por WhatsApp (el mensaje se abre en WhatsApp Web/App desde el navegador).
     */
    @PostMapping("/{id}/whatsapp")
    public ResponseEntity<PqrsfResponseDTO> registrarWhatsapp(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication auth,
            HttpServletRequest request) {

        permisos.exigir(auth, Permiso.RESPONDER_WHATSAPP);
        String telefono = payload.get("telefono") != null ? payload.get("telefono").toString() : null;
        String mensaje = payload.get("mensaje") != null ? payload.get("mensaje").toString() : null;
        boolean solucionado = Boolean.TRUE.equals(payload.get("marcarSolucionado"));
        return ResponseEntity.ok(pqrsfService.registrarRespuestaWhatsapp(id, telefono, mensaje, solucionado,
                auth.getName(), extraerIp(request), request.getHeader("User-Agent")));
    }

    /**
     * Módulo de Reportes Ejecutivos por Tiempos (SLA) y por Funcionario Responsable.
     */
    @GetMapping("/reportes")
    public ResponseEntity<ReporteSiauDTO> obtenerReportes(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            Authentication auth) {
        permisos.exigir(auth, Permiso.VER_REPORTES);
        return ResponseEntity.ok(pqrsfService.generarReporte(fechaDesde, fechaHasta));
    }

    private String extraerIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
