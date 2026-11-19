package co.clinicalhouse.pqrsf.service.impl;

import co.clinicalhouse.pqrsf.dto.AuditoriaResponseDTO;
import co.clinicalhouse.pqrsf.model.AuditoriaPqrsf;
import co.clinicalhouse.pqrsf.repository.AuditoriaPqrsfRepository;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaPqrsfRepository auditoriaRepository;

    @Autowired
    public AuditoriaServiceImpl(AuditoriaPqrsfRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    /**
     * Se guarda en una transacción propia (REQUIRES_NEW) para que el evento quede registrado
     * aunque la operación que lo origina falle y haga rollback (p. ej. LOGIN_FALLIDO).
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditoriaPqrsf registrar(String entidad, String entidadId, String accion, 
                                     String usuario, String ipOrigen, String navegadorAgente, String detalles) {
        AuditoriaPqrsf audit = new AuditoriaPqrsf(
                entidad,
                entidadId,
                accion,
                usuario != null ? usuario : "ANONIMO",
                ipOrigen,
                navegadorAgente,
                detalles
        );
        return auditoriaRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaResponseDTO> obtenerUltimosEventos() {
        return auditoriaRepository.findTop50ByOrderByFechaEventoDesc().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> obtenerTodos(Pageable pageable) {
        return auditoriaRepository.findAllByOrderByFechaEventoDesc(pageable)
                .map(this::convertirADTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> obtenerPorDia(java.time.LocalDate dia, Pageable pageable) {
        java.time.LocalDateTime inicio = dia.atStartOfDay();
        return auditoriaRepository
                .findByFechaEventoGreaterThanEqualAndFechaEventoLessThanOrderByFechaEventoDesc(inicio, inicio.plusDays(1), pageable)
                .map(this::convertirADTO);
    }

    private AuditoriaResponseDTO convertirADTO(AuditoriaPqrsf a) {
        return new AuditoriaResponseDTO(
                a.getId(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getAccion(),
                a.getUsuario(),
                a.getIpOrigen(),
                a.getNavegadorAgente(),
                a.getDetalles(),
                a.getFechaEvento()
        );
    }
}
