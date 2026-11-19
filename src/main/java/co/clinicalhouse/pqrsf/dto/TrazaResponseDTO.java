package co.clinicalhouse.pqrsf.dto;

import java.time.LocalDateTime;

public class TrazaResponseDTO {
    private Long id;
    private Long pqrsfId;
    private String estadoAnterior;
    private String estadoNuevo;
    private String observacion;
    private String usuarioResponsable;
    private LocalDateTime fechaRegistro;

    public TrazaResponseDTO() {}

    public TrazaResponseDTO(Long id, Long pqrsfId, String estadoAnterior, String estadoNuevo, 
                             String observacion, String usuarioResponsable, LocalDateTime fechaRegistro) {
        this.id = id;
        this.pqrsfId = pqrsfId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.observacion = observacion;
        this.usuarioResponsable = usuarioResponsable;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPqrsfId() { return pqrsfId; }
    public void setPqrsfId(Long pqrsfId) { this.pqrsfId = pqrsfId; }

    public String getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(String estadoAnterior) { this.estadoAnterior = estadoAnterior; }

    public String getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(String estadoNuevo) { this.estadoNuevo = estadoNuevo; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getUsuarioResponsable() { return usuarioResponsable; }
    public void setUsuarioResponsable(String usuarioResponsable) { this.usuarioResponsable = usuarioResponsable; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
