package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.PermisoCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermisoCargoRepository extends JpaRepository<PermisoCargo, Long> {
    List<PermisoCargo> findByCargoIgnoreCase(String cargo);

    @Modifying
    @Query("delete from PermisoCargo p where lower(p.cargo) = lower(:cargo)")
    void eliminarDeCargo(@Param("cargo") String cargo);

    @Modifying
    @Query("update PermisoCargo p set p.cargo = :nuevo where lower(p.cargo) = lower(:anterior)")
    void renombrarCargo(@Param("anterior") String anterior, @Param("nuevo") String nuevo);
}
