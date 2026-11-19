package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
    List<Cargo> findAllByOrderByNombreAsc();
    Optional<Cargo> findByNombreIgnoreCase(String nombre);
}
