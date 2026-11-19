package co.clinicalhouse.pqrsf.repository;

import co.clinicalhouse.pqrsf.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);

    /** Funcionarios que pueden ser responsables de una PQRSF (activos y sin rol de Super Admin). */
    java.util.List<Usuario> findByActivoTrueAndRolNotOrderByNombreAsc(String rol);

    /** Todos los funcionarios y usuarios activos del sistema para asignación oficial. */
    java.util.List<Usuario> findByActivoTrueOrderByNombreAsc();
}
