package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.ConfigSla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigSlaRepository extends JpaRepository<ConfigSla, String> {
}
