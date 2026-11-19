package co.clinicalhouse.pqrsf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PqrsfRequestDTO {

    @NotNull(message = "La fecha de recepción es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaRecepcion;

    // Familiar
    @NotBlank(message = "El primer apellido del familiar es obligatorio")
    private String famPrimerApellido;

    @NotBlank(message = "El segundo apellido del familiar es obligatorio")
    private String famSegundoApellido;

    @NotBlank(message = "El primer nombre del familiar es obligatorio")
    private String famPrimerNombre;

    private String famSegundoNombre;

    @NotBlank(message = "El tipo de documento del familiar es obligatorio")
    private String famTipoDocumento;

    @NotBlank(message = "El número de documento del familiar es obligatorio")
    @Pattern(regexp = "^[0-9]{3,15}$", message = "El número de documento del familiar solo puede contener números (entre 3 y 15 dígitos)")
    private String famNumeroDocumento;

    @NotBlank(message = "El correo electrónico del familiar es obligatorio")
    private String famEmail;

    @NotBlank(message = "La dirección del familiar es obligatoria")
    private String famDireccion;

    @NotBlank(message = "El teléfono del familiar es obligatorio")
    private String famTelefono;

    // Paciente
    @NotBlank(message = "El primer apellido del paciente es obligatorio")
    private String pacPrimerApellido;

    @NotBlank(message = "El segundo apellido del paciente es obligatorio")
    private String pacSegundoApellido;

    @NotBlank(message = "El primer nombre del paciente es obligatorio")
    private String pacPrimerNombre;

    private String pacSegundoNombre;

    @NotBlank(message = "El tipo de documento del paciente es obligatorio")
    private String pacTipoDocumento;

    @NotBlank(message = "El número de documento del paciente es obligatorio")
    @Pattern(regexp = "^[0-9]{3,15}$", message = "El número de documento del paciente solo puede contener números (entre 3 y 15 dígitos)")
    private String pacNumeroDocumento;

    @NotBlank(message = "El correo electrónico del paciente es obligatorio")
    private String pacEmail;

    @NotBlank(message = "La dirección del paciente es obligatoria")
    private String pacDireccion;

    @NotBlank(message = "El teléfono del paciente es obligatorio")
    private String pacTelefono;

    @NotBlank(message = "El régimen de afiliación es obligatorio")
    private String pacRegimen;

    @NotBlank(message = "La entidad (EPS) es obligatoria")
    private String pacEntidad;

    @NotBlank(message = "El tipo de población es obligatorio")
    private String pacPoblacion;

    // Detalle Solicitud
    @NotBlank(message = "El tipo de solicitud es obligatorio")
    private String tipoPeticion;

    @NotBlank(message = "La descripción de la solicitud es obligatoria")
    private String descripcionPeticion;

    // Forma Respuesta (A, B, C)
    @NotBlank(message = "Debe seleccionar un medio de respuesta (A, B, o C)")
    @Pattern(regexp = "^[ABC]$", message = "El medio de respuesta debe ser A, B o C")
    private String medioRespuesta;

    // Canales
    private String respDireccion;
    private String respBarrio;
    private String respMunicipio;
    private String respCelular;
    private String respTelefono;
    private String respCorreo;

    public PqrsfRequestDTO() {}

    // Getters y Setters
    public LocalDate getFechaRecepcion() { return fechaRecepcion; }
    public void setFechaRecepcion(LocalDate fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }

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

    public String getTipoPeticion() { return tipoPeticion; }
    public void setTipoPeticion(String tipoPeticion) { this.tipoPeticion = tipoPeticion; }

    public String getDescripcionPeticion() { return descripcionPeticion; }
    public void setDescripcionPeticion(String descripcionPeticion) { this.descripcionPeticion = descripcionPeticion; }

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

    public String getRespTelefono() { return respTelefono; }
    public void setRespTelefono(String respTelefono) { this.respTelefono = respTelefono; }

    public String getRespCorreo() { return respCorreo; }
    public void setRespCorreo(String respCorreo) { this.respCorreo = respCorreo; }

    // Autorización de tratamiento de datos personales (obligatoria para radicar)
    private Boolean aceptaTratamientoDatos;
    private String versionPoliticaDatos;

    public Boolean getAceptaTratamientoDatos() { return aceptaTratamientoDatos; }
    public void setAceptaTratamientoDatos(Boolean v) { this.aceptaTratamientoDatos = v; }
    public String getVersionPoliticaDatos() { return versionPoliticaDatos; }
    public void setVersionPoliticaDatos(String v) { this.versionPoliticaDatos = v; }
}
