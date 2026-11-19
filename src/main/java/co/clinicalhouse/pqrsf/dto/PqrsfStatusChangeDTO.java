package co.clinicalhouse.pqrsf.dto;

import jakarta.validation.constraints.NotBlank;

public class PqrsfStatusChangeDTO {

    @NotBlank(message = "El nuevo estado es obligatorio")
    private String nuevoEstado;

    private String observacion;

    public PqrsfStatusChangeDTO() {}

    public PqrsfStatusChangeDTO(String nuevoEstado, String observacion) {
        this.nuevoEstado = nuevoEstado;
        this.observacion = observacion;
    }

    public String getNuevoEstado() { return nuevoEstado; }
    public void setNuevoEstado(String nuevoEstado) { this.nuevoEstado = nuevoEstado; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}
