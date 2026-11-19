package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.TrazaPqrsf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrazaPqrsfRepository extends JpaRepository<TrazaPqrsf, Long> {
    List<TrazaPqrsf> findByPqrsfIdOrderByFechaRegistroDesc(Long pqrsfId);
}
