package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.AuditoriaPqrsf;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaPqrsfRepository extends JpaRepository<AuditoriaPqrsf, Long> {
    List<AuditoriaPqrsf> findTop50ByOrderByFechaEventoDesc();
    Page<AuditoriaPqrsf> findAllByOrderByFechaEventoDesc(Pageable pageable);
    List<AuditoriaPqrsf> findByEntidadIdOrderByFechaEventoDesc(String entidadId);

    /** Eventos de un día: [inicio, fin). */
    Page<AuditoriaPqrsf> findByFechaEventoGreaterThanEqualAndFechaEventoLessThanOrderByFechaEventoDesc(
            java.time.LocalDateTime inicio, java.time.LocalDateTime fin, Pageable pageable);
}
