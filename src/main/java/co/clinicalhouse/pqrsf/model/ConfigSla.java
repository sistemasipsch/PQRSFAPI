package co.clinicalhouse.pqrsf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Tiempo de respuesta por nivel de riesgo, editable desde el panel.
 * Una fila por nivel (SIMPLE, PRIORIZADO, VITAL) + una fila "UMBRAL" cuyo valor es el % de alerta.
 */
@Entity
@Table(name = "config_sla")
public class ConfigSla {

    @Id
    @Column(length = 30)
    private String clave; // SIMPLE | PRIORIZADO | VITAL | UMBRAL

    @Column(nullable = false)
    private Long valor; // horas para los niveles; porcentaje (1-99) para UMBRAL

    @Column(name = "actualizado_por", length = 150)
    private String actualizadoPor;

    @Column(name = "actualizado_el")
    private LocalDateTime actualizadoEl;

    public ConfigSla() {}

    public ConfigSla(String clave, Long valor) {
        this.clave = clave;
        this.valor = valor;
        this.actualizadoEl = LocalDateTime.now();
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public Long getValor() { return valor; }
    public void setValor(Long valor) { this.valor = valor; }

    public String getActualizadoPor() { return actualizadoPor; }
    public void setActualizadoPor(String actualizadoPor) { this.actualizadoPor = actualizadoPor; }

    public LocalDateTime getActualizadoEl() { return actualizadoEl; }
    public void setActualizadoEl(LocalDateTime actualizadoEl) { this.actualizadoEl = actualizadoEl; }
}
