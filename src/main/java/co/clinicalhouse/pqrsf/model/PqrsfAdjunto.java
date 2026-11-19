package co.clinicalhouse.pqrsf.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pqrsf_adjuntos")
public class PqrsfAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pqrsf_id", nullable = false)
    @JsonBackReference(value = "pqrsf-adjuntos")
    private Pqrsf pqrsf;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "nombre_almacenado", nullable = false)
    private String nombreAlmacenado;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 500)
    private String rutaAlmacenamiento;

    @Column(name = "tipo_mime", nullable = false, length = 120)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "subido_el", nullable = false, updatable = false)
    private LocalDateTime subidoEl = LocalDateTime.now();

    public PqrsfAdjunto() {}

    public PqrsfAdjunto(Pqrsf pqrsf, String nombreOriginal, String nombreAlmacenado, 
                        String rutaAlmacenamiento, String tipoMime, Long tamanoBytes) {
        this.pqrsf = pqrsf;
        this.nombreOriginal = nombreOriginal;
        this.nombreAlmacenado = nombreAlmacenado;
        this.rutaAlmacenamiento = rutaAlmacenamiento;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.subidoEl = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Pqrsf getPqrsf() { return pqrsf; }
    public void setPqrsf(Pqrsf pqrsf) { this.pqrsf = pqrsf; }

    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String nombreOriginal) { this.nombreOriginal = nombreOriginal; }

    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String nombreAlmacenado) { this.nombreAlmacenado = nombreAlmacenado; }

    public String getRutaAlmacenamiento() { return rutaAlmacenamiento; }
    public void setRutaAlmacenamiento(String rutaAlmacenamiento) { this.rutaAlmacenamiento = rutaAlmacenamiento; }

    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }

    public Long getTamanoBytes() { return tamanoBytes; }
    public void setTamanoBytes(Long tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public LocalDateTime getSubidoEl() { return subidoEl; }
    public void setSubidoEl(LocalDateTime subidoEl) { this.subidoEl = subidoEl; }
}
