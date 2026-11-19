package co.clinicalhouse.pqrsf.dto;

import java.time.LocalDateTime;

public class AdjuntoResponseDTO {
    private Long id;
    private String nombreOriginal;
    private String nombreAlmacenado;
    private String tipoMime;
    private Long tamanoBytes;
    private String urlDescarga;
    private LocalDateTime subidoEl;

    public AdjuntoResponseDTO() {}

    public AdjuntoResponseDTO(Long id, String nombreOriginal, String nombreAlmacenado, 
                              String tipoMime, Long tamanoBytes, String urlDescarga, LocalDateTime subidoEl) {
        this.id = id;
        this.nombreOriginal = nombreOriginal;
        this.nombreAlmacenado = nombreAlmacenado;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.urlDescarga = urlDescarga;
        this.subidoEl = subidoEl;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public String getUrlDescarga() { return urlDescarga; }
    public void setUrlDescarga(String urlDescarga) { this.urlDescarga = urlDescarga; }

    public LocalDateTime getSubidoEl() { return subidoEl; }
    public void setSubidoEl(LocalDateTime subidoEl) { this.subidoEl = subidoEl; }
}
