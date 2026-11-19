package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.PermisoRol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisoRolRepository extends JpaRepository<PermisoRol, Long> {
    List<PermisoRol> findByRol(String rol);

    @Modifying
    void deleteByRol(String rol);
}
