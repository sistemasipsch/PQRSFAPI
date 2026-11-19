package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.Pqrsf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PqrsfRepository extends JpaRepository<Pqrsf, Long>, JpaSpecificationExecutor<Pqrsf> {

    Optional<Pqrsf> findByConsecutivo(String consecutivo);

    long countByEstadoIgnoreCase(String estado);

    @Query("SELECT COUNT(p) FROM Pqrsf p")
    long countTotal();

    @Query(value = "SELECT consecutivo FROM pqrsf ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLastConsecutivo();

    java.util.List<Pqrsf> findByResponsableAsignadoIgnoreCase(String responsableAsignado);

    java.util.List<Pqrsf> findByAreaIgnoreCase(String area);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Pqrsf p SET p.responsableAsignado = 'Sin asignar', p.fechaActualizacion = CURRENT_TIMESTAMP WHERE LOWER(p.responsableAsignado) = LOWER(:nombre)")
    int reasignarResponsableASinAsignar(@org.springframework.data.repository.query.Param("nombre") String nombre);
}
