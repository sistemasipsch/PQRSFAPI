package co.clinicalhouse.pqrsf.service;

import co.clinicalhouse.pqrsf.dto.AuditoriaResponseDTO;
import co.clinicalhouse.pqrsf.model.AuditoriaPqrsf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuditoriaService {
    AuditoriaPqrsf registrar(String entidad, String entidadId, String accion, 
                             String usuario, String ipOrigen, String navegadorAgente, String detalles);
    List<AuditoriaResponseDTO> obtenerUltimosEventos();
    Page<AuditoriaResponseDTO> obtenerTodos(Pageable pageable);

    /** Eventos de un día calendario (00:00 a 23:59:59). */
    Page<AuditoriaResponseDTO> obtenerPorDia(java.time.LocalDate dia, Pageable pageable);
}
