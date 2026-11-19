package co.clinicalhouse.pqrsf.model;

import jakarta.persistence.*;

/** Funcionalidad habilitada para un rol (JEFE_SIAU, FUNCIONARIO_SIAU). El SUPER_ADMIN siempre tiene todas. */
@Entity
@Table(name = "permisos_rol", uniqueConstraints = @UniqueConstraint(columnNames = {"rol", "permiso"}))
public class PermisoRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String rol;

    @Column(nullable = false, length = 60)
    private String permiso;

    public PermisoRol() {}

    public PermisoRol(String rol, String permiso) {
        this.rol = rol;
        this.permiso = permiso;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getPermiso() { return permiso; }
    public void setPermiso(String permiso) { this.permiso = permiso; }
}
