package co.clinicalhouse.pqrsf.service.impl;

import co.clinicalhouse.pqrsf.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path rootLocation;
    /** Almacenamiento máximo para los adjuntos de una solicitud: 20MB en total. */
    public static final long MAX_TOTAL_SIZE = 20L * 1024 * 1024;
    private static final int MAX_FILE_COUNT = 3;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png"
    );

    public FileStorageServiceImpl(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de almacenamiento para adjuntos", e);
        }
    }

    @Override
    public void validarArchivos(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        if (files.size() > MAX_FILE_COUNT) {
            throw new IllegalArgumentException("Solo se permite adjuntar un máximo de " + MAX_FILE_COUNT + " archivos.");
        }

        long total = files.stream().filter(f -> f != null && !f.isEmpty()).mapToLong(MultipartFile::getSize).sum();
        if (total > MAX_TOTAL_SIZE) {
            throw new IllegalArgumentException(String.format(
                    "Los adjuntos suman %.1f MB y superan el máximo permitido de 20 MB por solicitud.",
                    total / (1024.0 * 1024.0)));
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String filename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
            String ext = "";
            int dotIdx = filename.lastIndexOf(".");
            if (dotIdx >= 0) {
                ext = filename.substring(dotIdx).toLowerCase();
            }

            if (!EXTENSIONES_PERMITIDAS.contains(ext)) {
                throw new IllegalArgumentException("El formato del archivo '" + filename + 
                        "' no es compatible. Formatos permitidos: Word (.doc, .docx), PDF (.pdf), Imágenes (.jpg, .jpeg, .png).");
            }
        }
    }

    @Override
    public String guardarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo");
        String extension = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex);
        }

        // Generar un nombre único seguro: fecha-uuid-extension
        String storedName = "pqrs_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        try {
            if (storedName.contains("..")) {
                throw new RuntimeException("Ruta inválida en nombre de archivo: " + storedName);
            }

            Path destination = this.rootLocation.resolve(storedName);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
            }

            return storedName;
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar archivo en el servidor: " + originalName, e);
        }
    }

    @Override
    public Resource cargarComoRecurso(String nombreArchivo) {
        try {
            Path file = rootLocation.resolve(nombreArchivo).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("No se pudo encontrar o leer el archivo: " + nombreArchivo);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error en la URL del archivo: " + nombreArchivo, e);
        }
    }

    @Override
    public Path getRutaAlmacenamiento() {
        return rootLocation;
    }
}
