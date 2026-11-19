package co.clinicalhouse.pqrsf.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PqrsfResponseDTO {
    private Long id;
    private String consecutivo;
    private LocalDate fechaRecepcion;
    private LocalDateTime fechaRadicacion;

    // Familiar
    private String famPrimerApellido;
    private String famSegundoApellido;
    private String famPrimerNombre;
    private String famSegundoNombre;
    private String famNombreCompleto;
    private String famTipoDocumento;
    private String famNumeroDocumento;
    private String famEmail;
    private String famDireccion;
    private String famTelefono;

    // Paciente
    private String pacPrimerApellido;
    private String pacSegundoApellido;
    private String pacPrimerNombre;
    private String pacSegundoNombre;
    private String pacNombreCompleto;
    private String pacTipoDocumento;
    private String pacNumeroDocumento;
    private String pacEmail;
    private String pacDireccion;
    private String pacTelefono;
    private String pacRegimen;
    private String pacEntidad;
    private String pacPoblacion;

    // Detalle Solicitud
    private String tipoSolicitud;
    private String descripcion;

    // Forma Respuesta
    private String medioRespuesta;
    private String respDireccion;
    private String respBarrio;
    private String respMunicipio;
    private String respCelular;
    private String respTelefonoFijo;
    private String respCorreo;

    // Estado y Fechas
    private String estado;
    private String responsableAsignado;
    private String area;
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    private long diasTranscurridos;
    private long diasRestantes;
    private String nivelSemaforo;
    private String tiempoRespuestaTexto;
    private LocalDateTime fechaActualizacion;

    // Listas asociadas
    private List<AdjuntoResponseDTO> adjuntos = new ArrayList<>();
    private List<TrazaResponseDTO> trazas = new ArrayList<>();

    public PqrsfResponseDTO() {}

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

    public String getFamNombreCompleto() { return famNombreCompleto; }
    public void setFamNombreCompleto(String famNombreCompleto) { this.famNombreCompleto = famNombreCompleto; }

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

    public String getPacNombreCompleto() { return pacNombreCompleto; }
    public void setPacNombreCompleto(String pacNombreCompleto) { this.pacNombreCompleto = pacNombreCompleto; }

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

    public long getDiasTranscurridos() { return diasTranscurridos; }
    public void setDiasTranscurridos(long diasTranscurridos) { this.diasTranscurridos = diasTranscurridos; }

    public long getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(long diasRestantes) { this.diasRestantes = diasRestantes; }

    public String getNivelSemaforo() { return nivelSemaforo; }
    public void setNivelSemaforo(String nivelSemaforo) { this.nivelSemaforo = nivelSemaforo; }

    public String getTiempoRespuestaTexto() { return tiempoRespuestaTexto; }
    public void setTiempoRespuestaTexto(String tiempoRespuestaTexto) { this.tiempoRespuestaTexto = tiempoRespuestaTexto; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public List<AdjuntoResponseDTO> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<AdjuntoResponseDTO> adjuntos) { this.adjuntos = adjuntos; }

    public List<TrazaResponseDTO> getTrazas() { return trazas; }
    public void setTrazas(List<TrazaResponseDTO> trazas) { this.trazas = trazas; }

    // ==================== SEMAFORIZACIÓN POR NIVEL DE RIESGO ====================
    private String nivelRiesgo;          // SIMPLE | PRIORIZADO | VITAL
    private String nivelRiesgoEtiqueta;  // "Riesgo simple", ...
    private long plazoHoras;
    private long minutosRestantes;
    private LocalDateTime fechaLimite;

    public String getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(String nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }

    public String getNivelRiesgoEtiqueta() { return nivelRiesgoEtiqueta; }
    public void setNivelRiesgoEtiqueta(String nivelRiesgoEtiqueta) { this.nivelRiesgoEtiqueta = nivelRiesgoEtiqueta; }

    public long getPlazoHoras() { return plazoHoras; }
    public void setPlazoHoras(long plazoHoras) { this.plazoHoras = plazoHoras; }

    public long getMinutosRestantes() { return minutosRestantes; }
    public void setMinutosRestantes(long minutosRestantes) { this.minutosRestantes = minutosRestantes; }

    public LocalDateTime getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDateTime fechaLimite) { this.fechaLimite = fechaLimite; }
}
