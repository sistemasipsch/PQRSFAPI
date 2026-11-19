package co.clinicalhouse.pqrsf.model;

import jakarta.persistence.*;

/** Funcionalidad habilitada para un cargo del catálogo. El SUPER_ADMIN siempre tiene todas. */
@Entity
@Table(name = "permisos_cargo", uniqueConstraints = @UniqueConstraint(columnNames = {"cargo", "permiso"}))
public class PermisoCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String cargo;

    @Column(nullable = false, length = 60)
    private String permiso;

    public PermisoCargo() {}

    public PermisoCargo(String cargo, String permiso) {
        this.cargo = cargo;
        this.permiso = permiso;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getPermiso() { return permiso; }
    public void setPermiso(String permiso) { this.permiso = permiso; }
}
