package co.clinicalhouse.pqrsf.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface FileStorageService {
    void init();
    String guardarArchivo(MultipartFile file);
    Resource cargarComoRecurso(String nombreArchivo);
    void validarArchivos(List<MultipartFile> files);
    Path getRutaAlmacenamiento();
}
