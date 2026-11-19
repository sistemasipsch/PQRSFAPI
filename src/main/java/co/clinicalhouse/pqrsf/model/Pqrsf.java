package co.clinicalhouse.pqrsf.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pqrsf")
public class Pqrsf {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String consecutivo;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    @Column(name = "fecha_radicacion", nullable = false, updatable = false)
    private LocalDateTime fechaRadicacion = LocalDateTime.now();

    // ==================== DATOS DEL FAMILIAR / ACOMPAÑANTE ====================
    @Column(name = "fam_primer_apellido", nullable = false, length = 80)
    private String famPrimerApellido;

    @Column(name = "fam_segundo_apellido", nullable = false, length = 80)
    private String famSegundoApellido;

    @Column(name = "fam_primer_nombre", nullable = false, length = 80)
    private String famPrimerNombre;

    @Column(name = "fam_segundo_nombre", length = 80)
    private String famSegundoNombre;

    @Column(name = "fam_tipo_documento", nullable = false, length = 50)
    private String famTipoDocumento;

    @Column(name = "fam_numero_documento", nullable = false, length = 30)
    private String famNumeroDocumento;

    @Column(name = "fam_email", nullable = false, length = 150)
    private String famEmail;

    @Column(name = "fam_direccion", nullable = false, length = 200)
    private String famDireccion;

    @Column(name = "fam_telefono", nullable = false, length = 30)
    private String famTelefono;

    // ==================== DATOS DEL PACIENTE ====================
    @Column(name = "pac_primer_apellido", nullable = false, length = 80)
    private String pacPrimerApellido;

    @Column(name = "pac_segundo_apellido", nullable = false, length = 80)
    private String pacSegundoApellido;

    @Column(name = "pac_primer_nombre", nullable = false, length = 80)
    private String pacPrimerNombre;

    @Column(name = "pac_segundo_nombre", length = 80)
    private String pacSegundoNombre;

    @Column(name = "pac_tipo_documento", nullable = false, length = 50)
    private String pacTipoDocumento;

    @Column(name = "pac_numero_documento", nullable = false, length = 30)
    private String pacNumeroDocumento;

    @Column(name = "pac_email", nullable = false, length = 150)
    private String pacEmail;

    @Column(name = "pac_direccion", nullable = false, length = 200)
    private String pacDireccion;

    @Column(name = "pac_telefono", nullable = false, length = 30)
    private String pacTelefono;

    @Column(name = "pac_regimen", nullable = false, length = 50)
    private String pacRegimen;

    @Column(name = "pac_entidad", nullable = false, length = 120)
    private String pacEntidad;

    @Column(name = "pac_poblacion", nullable = false, length = 50)
    private String pacPoblacion;

    // ==================== DETALLE DE LA SOLICITUD ====================
    @Column(name = "tipo_solicitud", nullable = false, length = 30)
    private String tipoSolicitud;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // ==================== FORMA DE RESPUESTA (A, B, o C) ====================
    @Column(name = "medio_respuesta", nullable = false, length = 2)
    private String medioRespuesta; // 'A', 'B', 'C'

    // Opción A: Domicilio
    @Column(name = "resp_direccion", length = 200)
    private String respDireccion;

    @Column(name = "resp_barrio", length = 100)
    private String respBarrio;

    @Column(name = "resp_municipio", length = 100)
    private String respMunicipio;

    // Opción B: Personalmente / Telefónica
    @Column(name = "resp_celular", length = 30)
    private String respCelular;

    @Column(name = "resp_telefono_fijo", length = 30)
    private String respTelefonoFijo;

    // Opción C: Correo Electrónico
    @Column(name = "resp_correo", length = 150)
    private String respCorreo;

    // ==================== ESTADO Y TRAZA ====================
    @Column(nullable = false, length = 30)
    private String estado = "radicado";

    @Column(name = "responsable_asignado", length = 150)
    private String responsableAsignado = "Sin asignar";

    @Column(length = 120)
    private String area;

    /** Evidencia de la autorización de tratamiento de datos dada por el ciudadano al radicar. */
    @Column(name = "acepta_tratamiento_datos")
    private Boolean aceptaTratamientoDatos;

    @Column(name = "fecha_aceptacion_datos")
    private LocalDateTime fechaAceptacionDatos;

    @Column(name = "version_politica_datos", length = 30)
    private String versionPoliticaDatos;

    @Column(name = "ip_aceptacion_datos", length = 60)
    private String ipAceptacionDatos;

    public Boolean getAceptaTratamientoDatos() { return aceptaTratamientoDatos; }
    public void setAceptaTratamientoDatos(Boolean v) { this.aceptaTratamientoDatos = v; }
    public LocalDateTime getFechaAceptacionDatos() { return fechaAceptacionDatos; }
    public void setFechaAceptacionDatos(LocalDateTime v) { this.fechaAceptacionDatos = v; }
    public String getVersionPoliticaDatos() { return versionPoliticaDatos; }
    public void setVersionPoliticaDatos(String v) { this.versionPoliticaDatos = v; }
    public String getIpAceptacionDatos() { return ipAceptacionDatos; }
    public void setIpAceptacionDatos(String v) { this.ipAceptacionDatos = v; }

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @OneToMany(mappedBy = "pqrsf", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "pqrsf-adjuntos")
    private List<PqrsfAdjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "pqrsf", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "pqrsf-trazas")
    @OrderBy("fechaRegistro DESC")
    private List<TrazaPqrsf> trazas = new ArrayList<>();

    public Pqrsf() {}

    @PrePersist
    public void prePersist() {
        if (this.fechaRadicacion == null) this.fechaRadicacion = LocalDateTime.now();
        if (this.fechaActualizacion == null) this.fechaActualizacion = LocalDateTime.now();
        if (this.estado == null) this.estado = "radicado";
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConsecutivo() { return consecutivo; }
    public void setConsecutivo(String consecutivo) { this.consecutivo = consecutivo; }

    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

    public LocalDateTime getFechaRadicacion() { return fechaRadicacion; }
    public void setFechaRadicacion(LocalDateTime fechaRadicacion) { this.fechaRadicacion = fechaRadicacion; }

    public String getFamPrimerApellido() { return famPrimerApellido; }
    public void setFamPrimerApellido(String famPrimerApellido) { this.famPrimerApellido = famPrimerApellido; }

    public String getFamSegundoApellido() { return famSegundoApellido; }
    public void setFamSegundoApellido(String famSegundoApellido) { this.famSegundoApellido = famSegundoApellido; }

    public String getFamPrimerNombre() { return famPrimerNombre; }
    public void setFamPrimerNombre(String famPrimerNombre) { this.famPrimerNombre = famPrimerNombre; }

    public String getFamSegundoNombre() { return famSegundoNombre; }
    public void setFamSegundoNombre(String famSegundoNombre) { this.famSegundoNombre = famSegundoNombre; }

    public String getFamTipoDocumento() { return famTipoDocumento; }
    public void setFamTipoDocumento(String famTipoDocumento) { this.famTipoDocumento = famTipoDocumento; }

    public String getFamNumeroDocumento() { return famNumeroDocumento; }
    public void setFamNumeroDocumento(String famNumeroDocumento) { this.famNumeroDocumento = famNumeroDocumento; }

    public String getFamEmail() { return famEmail; }
    public void setFamEmail(String famEmail) { this.famEmail = famEmail; }

    public String getFamDireccion() { return famDireccion; }
    public void setFamDireccion(String famDireccion) { this.famDireccion = famDireccion; }

    public String getFamTelefono() { return famTelefono; }
    public void setFamTelefono(String famTelefono) { this.famTelefono = famTelefono; }

    public String getPacPrimerApellido() { return pacPrimerApellido; }
    public void setPacPrimerApellido(String pacPrimerApellido) { this.pacPrimerApellido = pacPrimerApellido; }

    public String getPacSegundoApellido() { return pacSegundoApellido; }
    public void setPacSegundoApellido(String pacSegundoApellido) { this.pacSegundoApellido = pacSegundoApellido; }

    public String getPacPrimerNombre() { return pacPrimerNombre; }
    public void setPacPrimerNombre(String pacPrimerNombre) { this.pacPrimerNombre = pacPrimerNombre; }

    public String getPacSegundoNombre() { return pacSegundoNombre; }
    public void setPacSegundoNombre(String pacSegundoNombre) { this.pacSegundoNombre = pacSegundoNombre; }

    public String getPacTipoDocumento() { return pacTipoDocumento; }
    public void setPacTipoDocumento(String pacTipoDocumento) { this.pacTipoDocumento = pacTipoDocumento; }

    public String getPacNumeroDocumento() { return pacNumeroDocumento; }
    public void setPacNumeroDocumento(String pacNumeroDocumento) { this.pacNumeroDocumento = pacNumeroDocumento; }

    public String getPacEmail() { return pacEmail; }
    public void setPacEmail(String pacEmail) { this.pacEmail = pacEmail; }

    public String getPacDireccion() { return pacDireccion; }
    public void setPacDireccion(String pacDireccion) { this.pacDireccion = pacDireccion; }

    public String getPacTelefono() { return pacTelefono; }
    public void setPacTelefono(String pacTelefono) { this.pacTelefono = pacTelefono; }

    public String getPacRegimen() { return pacRegimen; }
    public void setPacRegimen(String pacRegimen) { this.pacRegimen = pacRegimen; }

    public String getPacEntidad() { return pacEntidad; }
    public void setPacEntidad(String pacEntidad) { this.pacEntidad = pacEntidad; }

    public String getPacPoblacion() { return pacPoblacion; }
    public void setPacPoblacion(String pacPoblacion) { this.pacPoblacion = pacPoblacion; }

    public String getTipoSolicitud() { return tipoSolicitud; }
    public void setTipoSolicitud(String tipoSolicitud) { this.tipoSolicitud = tipoSolicitud; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getMedioRespuesta() { return medioRespuesta; }
    public void setMedioRespuesta(String medioRespuesta) { this.medioRespuesta = medioRespuesta; }

    public String getRespDireccion() { return respDireccion; }
    public void setRespDireccion(String respDireccion) { this.respDireccion = respDireccion; }

    public String getRespBarrio() { return respBarrio; }
    public void setRespBarrio(String respBarrio) { this.respBarrio = respBarrio; }

    public String getRespMunicipio() { return respMunicipio; }
    public void setRespMunicipio(String respMunicipio) { this.respMunicipio = respMunicipio; }

    public String getRespCelular() { return respCelular; }
    public void setRespCelular(String respCelular) { this.respCelular = respCelular; }

    public String getRespTelefonoFijo() { return respTelefonoFijo; }
    public void setRespTelefonoFijo(String respTelefonoFijo) { this.respTelefonoFijo = respTelefonoFijo; }

    public String getRespCorreo() { return respCorreo; }
    public void setRespCorreo(String respCorreo) { this.respCorreo = respCorreo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getResponsableAsignado() { return responsableAsignado; }
    public void setResponsableAsignado(String responsableAsignado) { this.responsableAsignado = responsableAsignado; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public List<PqrsfAdjunto> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<PqrsfAdjunto> adjuntos) { this.adjuntos = adjuntos; }

    public List<TrazaPqrsf> getTrazas() { return trazas; }
    public void setTrazas(List<TrazaPqrsf> trazas) { this.trazas = trazas; }

    public void addAdjunto(PqrsfAdjunto adjunto) {
        adjuntos.add(adjunto);
        adjunto.setPqrsf(this);
    }

    public void addTraza(TrazaPqrsf traza) {
        trazas.add(traza);
        traza.setPqrsf(this);
    }
}
