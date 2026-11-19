package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findAllByOrderByNombreAsc();
    Optional<Area> findByNombreIgnoreCase(String nombre);
}
