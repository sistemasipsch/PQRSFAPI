package co.clinicalhouse.pqrsf.dto;

import java.time.LocalDateTime;

public class AuditoriaResponseDTO {
    private Long id;
    private String entidad;
    private String entidadId;
    private String accion;
    private String usuario;
    private String ipOrigen;
    private String navegadorAgente;
    private String detalles;
    private LocalDateTime fechaEvento;

    public AuditoriaResponseDTO() {}

    public AuditoriaResponseDTO(Long id, String entidad, String entidadId, String accion, 
                                String usuario, String ipOrigen, String navegadorAgente, 
                                String detalles, LocalDateTime fechaEvento) {
        this.id = id;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.usuario = usuario;
        this.ipOrigen = ipOrigen;
        this.navegadorAgente = navegadorAgente;
        this.detalles = detalles;
        this.fechaEvento = fechaEvento;
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
