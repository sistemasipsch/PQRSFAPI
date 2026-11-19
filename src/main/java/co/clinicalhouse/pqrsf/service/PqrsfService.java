package co.clinicalhouse.pqrsf.service;

import co.clinicalhouse.pqrsf.dto.RespuestaCorreoDTO;

import co.clinicalhouse.pqrsf.dto.DashboardStatsDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfRequestDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfResponseDTO;
import co.clinicalhouse.pqrsf.dto.PqrsfStatusChangeDTO;
import co.clinicalhouse.pqrsf.dto.ReporteSiauDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PqrsfService {
    PqrsfResponseDTO radicar(PqrsfRequestDTO requestDTO, List<MultipartFile> adjuntos, 
                             String ipOrigen, String userAgent);
    /** area: nombre del área, "sin-area" para las que no tienen, o "todos"/null para no filtrar. */
    Page<PqrsfResponseDTO> listar(String estado, String responsable, String area, String termino, Pageable pageable);
    PqrsfResponseDTO obtenerPorId(Long id, String usuarioAuditoria, String ipOrigen);
    PqrsfResponseDTO obtenerPorConsecutivo(String consecutivo);
    PqrsfResponseDTO cambiarEstado(Long id, PqrsfStatusChangeDTO dto, 
                                   String usuarioResponsable, String ipOrigen, String userAgent);
    PqrsfResponseDTO asignarResponsable(Long id, String responsable, String observacion,
                                       String usuarioResponsable, String ipOrigen, String userAgent);
    PqrsfResponseDTO responderPorCorreo(Long id, RespuestaCorreoDTO dto, String usuarioResponsable,
                                        String ipOrigen, String userAgent);
    DashboardStatsDTO obtenerEstadisticas();
    ReporteSiauDTO generarReporte(String fechaDesde, String fechaHasta);

    /** Asigna (o quita, con null/vacío) el área / servicio involucrado. */
    PqrsfResponseDTO asignarArea(Long id, String area, String usuarioResponsable, String ipOrigen, String userAgent);

    /** Deja traza y auditoría de una respuesta enviada por WhatsApp desde el panel. */
    PqrsfResponseDTO registrarRespuestaWhatsapp(Long id, String telefono, String mensaje, boolean marcarSolucionado,
                                               String usuarioResponsable, String ipOrigen, String userAgent);

    /** Todas las solicitudes radicadas en el periodo (para exportar el detalle de SLA). */
    List<PqrsfResponseDTO> exportar(String fechaDesde, String fechaHasta);
}
