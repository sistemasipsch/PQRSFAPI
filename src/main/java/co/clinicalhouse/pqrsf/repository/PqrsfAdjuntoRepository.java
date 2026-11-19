package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.PqrsfAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PqrsfAdjuntoRepository extends JpaRepository<PqrsfAdjunto, Long> {
    List<PqrsfAdjunto> findByPqrsfId(Long pqrsfId);
    java.util.Optional<PqrsfAdjunto> findByNombreAlmacenado(String nombreAlmacenado);
}
