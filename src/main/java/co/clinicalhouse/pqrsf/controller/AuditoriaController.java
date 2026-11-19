package co.clinicalhouse.pqrsf.controller;

import co.clinicalhouse.pqrsf.dto.AuditoriaResponseDTO;
import co.clinicalhouse.pqrsf.security.Permiso;
import co.clinicalhouse.pqrsf.security.PermisoService;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final PermisoService permisos;

    @Autowired
    public AuditoriaController(AuditoriaService auditoriaService, PermisoService permisos) {
        this.auditoriaService = auditoriaService;
        this.permisos = permisos;
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<AuditoriaResponseDTO>> obtenerRecientes(Authentication auth) {
        permisos.exigir(auth, Permiso.VER_AUDITORIA);
        return ResponseEntity.ok(auditoriaService.obtenerUltimosEventos());
    }

    /**
     * Bitácora de UN día (por defecto, hoy). El panel muestra solo el día en curso:
     * al pasar la medianoche los eventos del día anterior dejan de mostrarse y se consultan
     * eligiendo la fecha ("Consultar por día"). Nada se borra de la base de datos.
     */
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponseDTO>> obtenerPorDia(
            @RequestParam(required = false) String fecha,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            Authentication auth) {
        permisos.exigir(auth, Permiso.VER_AUDITORIA);
        LocalDate dia;
        try {
            dia = fecha == null || fecha.isBlank() ? LocalDate.now() : LocalDate.parse(fecha.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha inválida. Use el formato AAAA-MM-DD.");
        }
        if (dia.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede consultar una fecha futura.");
        }
        return ResponseEntity.ok(auditoriaService.obtenerPorDia(dia, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 500))));
    }
}
