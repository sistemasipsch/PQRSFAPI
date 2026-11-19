package co.clinicalhouse.pqrsf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_pqrsf")
public class AuditoriaPqrsf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String entidad; // 'PQRSF', 'USUARIO', 'ADJUNTO', 'SISTEMA'

    @Column(name = "entidad_id", length = 50)
    private String entidadId; // ID numérico o consecutivo 'pqrs-ch-0001'

    @Column(nullable = false, length = 50)
    private String accion; // 'RADICACION', 'CAMBIO_ESTADO', 'LOGIN_EXITOSO', 'LOGIN_FALLIDO', 'DESCARGA_ADJUNTO'

    @Column(nullable = false, length = 150)
    private String usuario; // 'siau@clinicalhouse.co' o 'PACIENTE_WEB'

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    @Column(name = "navegador_agente", length = 255)
    private String navegadorAgente;

    @Column(columnDefinition = "TEXT")
    private String detalles; // JSON o texto explicativo con snapshot

    @Column(name = "fecha_evento", nullable = false, updatable = false)
    private LocalDateTime fechaEvento = LocalDateTime.now();

    public AuditoriaPqrsf() {}

    public AuditoriaPqrsf(String entidad, String entidadId, String accion, 
                          String usuario, String ipOrigen, String navegadorAgente, String detalles) {
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.usuario = usuario;
        this.ipOrigen = ipOrigen;
        this.navegadorAgente = navegadorAgente;
        this.detalles = detalles;
        this.fechaEvento = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getEntidadId() { return entidadId; }
    public void setEntidadId(String entidadId) { this.entidadId = entidadId; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getIpOrigen() { return ipOrigen; }
    public void setIpOrigen(String ipOrigen) { this.ipOrigen = ipOrigen; }

    public String getNavegadorAgente() { return navegadorAgente; }
    public void setNavegadorAgente(String navegadorAgente) { this.navegadorAgente = navegadorAgente; }

    public String getDetalles() { return detalles; }
    public void setDetalles(String detalles) { this.detalles = detalles; }

    public LocalDateTime getFechaEvento() { return fechaEvento; }
    public void setFechaEvento(LocalDateTime fechaEvento) { this.fechaEvento = fechaEvento; }
}
