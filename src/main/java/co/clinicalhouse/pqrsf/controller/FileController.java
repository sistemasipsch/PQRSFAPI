package co.clinicalhouse.pqrsf.controller;

import co.clinicalhouse.pqrsf.repository.PqrsfAdjuntoRepository;
import co.clinicalhouse.pqrsf.security.Permiso;
import co.clinicalhouse.pqrsf.security.PermisoService;
import org.springframework.security.core.Authentication;
import co.clinicalhouse.pqrsf.service.FileStorageService;
import org.springframework.http.ContentDisposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final PqrsfAdjuntoRepository adjuntoRepository;

    /** Orígenes del panel que pueden mostrar los adjuntos dentro de un iframe (visor de PDF). */
    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String origenesPermitidos;

    private final PermisoService permisos;

    @Autowired
    public FileController(FileStorageService fileStorageService, PqrsfAdjuntoRepository adjuntoRepository, PermisoService permisos) {
        this.fileStorageService = fileStorageService;
        this.adjuntoRepository = adjuntoRepository;
        this.permisos = permisos;
    }

    /** Adjuntos de las solicitudes: solo funcionarios con sesión y permiso para ver PQRSF. */
    @GetMapping("/{nombreArchivo:.+}")
    public ResponseEntity<Resource> descargarArchivo(
            @PathVariable String nombreArchivo,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean descargar,
            Authentication auth) {
        permisos.exigir(auth, Permiso.VER_PQRSF);
        if (nombreArchivo.contains("..") || nombreArchivo.contains("/") || nombreArchivo.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido.");
        }
        // Solo se sirven archivos que pertenecen a una solicitud registrada
        if (adjuntoRepository.findByNombreAlmacenado(nombreArchivo).isEmpty()) {
            throw new RuntimeException("No se encontró el archivo solicitado.");
        }
        Resource recurso = fileStorageService.cargarComoRecurso(nombreArchivo);

        String contentType = "application/octet-stream";
        try {
            Path path = recurso.getFile().toPath();
            String detectedType = Files.probeContentType(path);
            if (detectedType != null) {
                contentType = detectedType;
            }
        } catch (IOException e) {
            // fallback a octet-stream
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion(descargar, nombreOriginal(nombreArchivo)))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "frame-ancestors 'self' " + origenesPermitidos.replace(",", " "))
                .body(recurso);
    }

    /** Nombre con el que el ciudadano subió el archivo (si existe en la base de datos). */
    private String nombreOriginal(String nombreAlmacenado) {
        return adjuntoRepository.findByNombreAlmacenado(nombreAlmacenado)
                .map(a -> a.getNombreOriginal())
                .filter(n -> n != null && !n.isBlank())
                .orElse(nombreAlmacenado);
    }

    private static String disposicion(boolean descargar, String nombre) {
        ContentDisposition.Builder b = descargar ? ContentDisposition.attachment() : ContentDisposition.inline();
        return b.filename(nombre, StandardCharsets.UTF_8).build().toString();
    }
}
