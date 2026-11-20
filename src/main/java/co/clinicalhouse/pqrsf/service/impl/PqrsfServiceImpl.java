package co.clinicalhouse.pqrsf.service.impl;

import co.clinicalhouse.pqrsf.dto.*;
import co.clinicalhouse.pqrsf.model.Pqrsf;
import co.clinicalhouse.pqrsf.model.PqrsfAdjunto;
import co.clinicalhouse.pqrsf.model.TrazaPqrsf;
import co.clinicalhouse.pqrsf.repository.PqrsfAdjuntoRepository;
import co.clinicalhouse.pqrsf.repository.PqrsfRepository;
import co.clinicalhouse.pqrsf.repository.TrazaPqrsfRepository;
import co.clinicalhouse.pqrsf.repository.UsuarioRepository;
import co.clinicalhouse.pqrsf.sla.SlaPolicy;
import co.clinicalhouse.pqrsf.service.AuditoriaService;
import co.clinicalhouse.pqrsf.service.EmailService;
import co.clinicalhouse.pqrsf.service.FileStorageService;
import co.clinicalhouse.pqrsf.service.PqrsfService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PqrsfServiceImpl implements PqrsfService {

    private final PqrsfRepository pqrsfRepository;
    private final PqrsfAdjuntoRepository adjuntoRepository;
    private final TrazaPqrsfRepository trazaRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final AuditoriaService auditoriaService;
    private final UsuarioRepository usuarioRepository;
    private final co.clinicalhouse.pqrsf.service.ConfiguracionService configuracionService;

    public static final String SIN_ASIGNAR = "Sin asignar";

    private static final List<String> ESTADOS_VALIDOS = List.of("radicado", "revisado", "en proceso", "solucionado");

    @Autowired
    public PqrsfServiceImpl(PqrsfRepository pqrsfRepository,
                            PqrsfAdjuntoRepository adjuntoRepository,
                            TrazaPqrsfRepository trazaRepository,
                            FileStorageService fileStorageService,
                            EmailService emailService,
                            AuditoriaService auditoriaService,
                            UsuarioRepository usuarioRepository,
                            co.clinicalhouse.pqrsf.service.ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
        this.pqrsfRepository = pqrsfRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.trazaRepository = trazaRepository;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.auditoriaService = auditoriaService;
        this.usuarioRepository = usuarioRepository;
    }

    /** Normaliza "en_proceso" / "En Proceso" → "en proceso". */
    private static String normalizarEstado(String estado) {
        return estado == null ? "" : estado.trim().toLowerCase().replace('_', ' ');
    }

    /**
     * Una solicitud marcada como SOLUCIONADA queda cerrada: su estado ya no se puede cambiar
     * (la traza conserva quién y cuándo la solucionó).
     */
    private static void validarNoSolucionada(Pqrsf p) {
        if ("solucionado".equals(normalizarEstado(p.getEstado()))) {
            throw new IllegalArgumentException("La solicitud " + p.getConsecutivo()
                    + " ya fue marcada como Solucionada y su estado no se puede modificar.");
        }
    }

    private static String resumir(String texto, int max) {
        String t = texto == null ? "" : texto.trim();
        return t.length() > max ? t.substring(0, max) + "…" : t;
    }

    @Override
    @Transactional
    public PqrsfResponseDTO asignarArea(Long id, String area, String usuarioResponsable, String ipOrigen, String userAgent) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRSF no encontrada con ID: " + id));
        String nueva = configuracionService.validarArea(area);
        String anterior = pqrsf.getArea();
        if (java.util.Objects.equals(anterior, nueva)) return mapearADTO(pqrsf);

        pqrsf.setArea(nueva);
        pqrsf.setFechaActualizacion(LocalDateTime.now());
        pqrsf = pqrsfRepository.save(pqrsf);

        TrazaPqrsf traza = new TrazaPqrsf(pqrsf, pqrsf.getEstado(), pqrsf.getEstado(),
                nueva == null ? "Se quitó el área involucrada (" + anterior + ")." : "Área involucrada: " + nueva
                        + (anterior != null ? " (antes: " + anterior + ")" : ""),
                usuarioResponsable);
        trazaRepository.save(traza);
        pqrsf.addTraza(traza);

        auditoriaService.registrar("PQRSF", pqrsf.getConsecutivo(), "ASIGNACION_AREA", usuarioResponsable, ipOrigen, userAgent,
                String.format("{\"areaAnterior\":\"%s\",\"nuevaArea\":\"%s\"}", anterior == null ? "" : anterior, nueva == null ? "" : nueva));
        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional
    public PqrsfResponseDTO registrarRespuestaWhatsapp(Long id, String telefono, String mensaje, boolean marcarSolucionado,
                                                      String usuarioResponsable, String ipOrigen, String userAgent) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRSF no encontrada con ID: " + id));
        String numero = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (numero.length() < 7 || numero.length() > 15) {
            throw new IllegalArgumentException("El número de WhatsApp no es válido.");
        }
        if (mensaje == null || mensaje.trim().length() < 5) {
            throw new IllegalArgumentException("El mensaje de WhatsApp es demasiado corto.");
        }

        String estadoAnterior = pqrsf.getEstado();
        String estadoFinal = marcarSolucionado ? "solucionado" : estadoAnterior;
        pqrsf.setEstado(estadoFinal);
        pqrsf.setFechaActualizacion(LocalDateTime.now());
        pqrsf = pqrsfRepository.save(pqrsf);

        TrazaPqrsf traza = new TrazaPqrsf(pqrsf, estadoAnterior, estadoFinal,
                "Respuesta enviada por WhatsApp al +" + numero + " | " + resumir(mensaje, 500), usuarioResponsable);
        trazaRepository.save(traza);
        pqrsf.addTraza(traza);

        auditoriaService.registrar("PQRSF", pqrsf.getConsecutivo(), "RESPUESTA_WHATSAPP", usuarioResponsable, ipOrigen, userAgent,
                String.format("{\"telefono\":\"+%s\",\"marcarSolucionado\":%s}", numero, marcarSolucionado));
        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PqrsfResponseDTO> exportar(String fechaDesdeStr, String fechaHastaStr) {
        LocalDate desde = fechaDesdeStr != null && !fechaDesdeStr.isBlank() ? LocalDate.parse(fechaDesdeStr.trim()) : null;
        LocalDate hasta = fechaHastaStr != null && !fechaHastaStr.isBlank() ? LocalDate.parse(fechaHastaStr.trim()) : null;
        return pqrsfRepository.findAll().stream()
                .filter(p -> {
                    LocalDate f = p.getFechaRadicacion().toLocalDate();
                    return (desde == null || !f.isBefore(desde)) && (hasta == null || !f.isAfter(hasta));
                })
                .sorted((a, b) -> b.getFechaRadicacion().compareTo(a.getFechaRadicacion()))
                .map(this::mapearADTO)
                .toList();
    }

    @Override
    @Transactional
    public synchronized PqrsfResponseDTO radicar(PqrsfRequestDTO dto, List<MultipartFile> files, 
                                                 String ipOrigen, String userAgent) {
        // 0. Sin autorización de tratamiento de datos no se puede radicar
        if (!Boolean.TRUE.equals(dto.getAceptaTratamientoDatos())) {
            throw new IllegalArgumentException(
                    "Para radicar su solicitud debe aceptar la política de tratamiento de datos personales.");
        }

        // 1. Validar archivos adjuntos si existen
        fileStorageService.validarArchivos(files);

        // 2. Generar consecutivo anual: PQRSF-CH-2026-00001
        String consecutivo = generarSiguienteConsecutivo();

        // 3. Mapear y construir entidad Pqrsf
        Pqrsf pqrsf = new Pqrsf();
        pqrsf.setConsecutivo(consecutivo);
        pqrsf.setFechaRecepcion(dto.getFechaRecepcion());
        pqrsf.setFechaRadicacion(LocalDateTime.now());
        pqrsf.setAceptaTratamientoDatos(true);
        pqrsf.setFechaAceptacionDatos(LocalDateTime.now());
        pqrsf.setVersionPoliticaDatos(dto.getVersionPoliticaDatos() != null && !dto.getVersionPoliticaDatos().isBlank()
                ? dto.getVersionPoliticaDatos().trim() : "sin versión");
        pqrsf.setIpAceptacionDatos(ipOrigen);

        // Familiar / Acompañante
        pqrsf.setFamPrimerApellido(dto.getFamPrimerApellido().trim());
        pqrsf.setFamSegundoApellido(dto.getFamSegundoApellido().trim());
        pqrsf.setFamPrimerNombre(dto.getFamPrimerNombre().trim());
        pqrsf.setFamSegundoNombre(dto.getFamSegundoNombre() != null ? dto.getFamSegundoNombre().trim() : null);
        pqrsf.setFamTipoDocumento(dto.getFamTipoDocumento().trim());
        pqrsf.setFamNumeroDocumento(dto.getFamNumeroDocumento().trim());
        pqrsf.setFamEmail(dto.getFamEmail().trim().toLowerCase());
        pqrsf.setFamDireccion(dto.getFamDireccion().trim());
        pqrsf.setFamTelefono(dto.getFamTelefono().trim());

        // Paciente
        pqrsf.setPacPrimerApellido(dto.getPacPrimerApellido().trim());
        pqrsf.setPacSegundoApellido(dto.getPacSegundoApellido().trim());
        pqrsf.setPacPrimerNombre(dto.getPacPrimerNombre().trim());
        pqrsf.setPacSegundoNombre(dto.getPacSegundoNombre() != null ? dto.getPacSegundoNombre().trim() : null);
        pqrsf.setPacTipoDocumento(dto.getPacTipoDocumento().trim());
        pqrsf.setPacNumeroDocumento(dto.getPacNumeroDocumento().trim());
        pqrsf.setPacEmail(dto.getPacEmail().trim().toLowerCase());
        pqrsf.setPacDireccion(dto.getPacDireccion().trim());
        pqrsf.setPacTelefono(dto.getPacTelefono().trim());
        pqrsf.setPacRegimen(dto.getPacRegimen().trim());
        pqrsf.setPacEntidad(dto.getPacEntidad().trim());
        pqrsf.setPacPoblacion(dto.getPacPoblacion().trim());

        // Solicitud
        pqrsf.setTipoSolicitud(dto.getTipoPeticion().trim());
        pqrsf.setDescripcion(dto.getDescripcionPeticion().trim());

        // Canal de respuesta (A, B, o C)
        String canal = dto.getMedioRespuesta().trim().toUpperCase();
        pqrsf.setMedioRespuesta(canal);
        if ("A".equals(canal)) {
            pqrsf.setRespDireccion(dto.getRespDireccion());
            pqrsf.setRespBarrio(dto.getRespBarrio());
            pqrsf.setRespMunicipio(dto.getRespMunicipio());
        } else if ("B".equals(canal)) {
            pqrsf.setRespCelular(dto.getRespCelular());
            pqrsf.setRespTelefonoFijo(dto.getRespTelefono());
        } else if ("C".equals(canal)) {
            pqrsf.setRespCorreo(dto.getRespCorreo() != null ? dto.getRespCorreo().trim().toLowerCase() : pqrsf.getFamEmail());
        }

        pqrsf.setEstado("radicado");
        pqrsf.setResponsableAsignado(SIN_ASIGNAR);
        pqrsf.setFechaActualizacion(LocalDateTime.now());

        // Guardar la entidad principal
        pqrsf = pqrsfRepository.save(pqrsf);

        // 4. Guardar adjuntos
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String storedName = fileStorageService.guardarArchivo(file);
                    PqrsfAdjunto adj = new PqrsfAdjunto(
                            pqrsf,
                            file.getOriginalFilename(),
                            storedName,
                            fileStorageService.getRutaAlmacenamiento().resolve(storedName).toString(),
                            file.getContentType() != null ? file.getContentType() : "application/octet-stream",
                            file.getSize()
                    );
                    adjuntoRepository.save(adj);
                    pqrsf.addAdjunto(adj);
                }
            }
        }

        // 5. Crear primer registro de TRAZA
        TrazaPqrsf trazaInicial = new TrazaPqrsf(
                pqrsf,
                null,
                "radicado",
                "Radicación inicial exitosa a través del formulario web oficial SIAU.",
                "PORTAL_WEB_PUBLICO"
        );
        trazaRepository.save(trazaInicial);
        pqrsf.addTraza(trazaInicial);

        // 6. Registrar en AUDITORÍA del sistema
        String detalleAuditoria = String.format(
                "{\"consecutivo\":\"%s\",\"paciente\":\"%s %s\",\"documento\":\"%s\",\"tipo\":\"%s\",\"adjuntos\":%d}",
                pqrsf.getConsecutivo(),
                pqrsf.getPacPrimerNombre(),
                pqrsf.getPacPrimerApellido(),
                pqrsf.getPacNumeroDocumento(),
                pqrsf.getTipoSolicitud(),
                pqrsf.getAdjuntos().size()
        );
        auditoriaService.registrar(
                "PQRSF",
                pqrsf.getConsecutivo(),
                "RADICACION",
                pqrsf.getFamEmail(),
                ipOrigen,
                userAgent,
                detalleAuditoria
        );

        // 7. Notificaciones por correo
        emailService.enviarReciboRadicacionUsuario(pqrsf);
        emailService.enviarNotificacionTrazaJefeSiau(pqrsf);

        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PqrsfResponseDTO> listar(String estado, String responsable, String area, String termino, Pageable pageable) {
        Specification<Pqrsf> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro por estado
            if (estado != null && !estado.trim().isEmpty() && !"todos".equalsIgnoreCase(estado)) {
                predicates.add(cb.equal(cb.lower(root.get("estado")), normalizarEstado(estado)));
            }

            // Filtro por responsable asignado
            if (responsable != null && !responsable.trim().isEmpty() && !"todos".equalsIgnoreCase(responsable)) {
                predicates.add(cb.equal(cb.lower(root.get("responsableAsignado")), responsable.trim().toLowerCase()));
            }

            // Filtro por área ("sin-area" = solicitudes que aún no tienen área)
            if (area != null && !area.trim().isEmpty() && !"todos".equalsIgnoreCase(area)) {
                if ("sin-area".equalsIgnoreCase(area.trim())) {
                    predicates.add(cb.or(cb.isNull(root.get("area")), cb.equal(cb.trim(root.get("area")), "")));
                } else {
                    predicates.add(cb.equal(cb.lower(root.get("area")), area.trim().toLowerCase()));
                }
            }

            // Buscador por término
            if (termino != null && !termino.trim().isEmpty()) {
                String pattern = "%" + termino.trim().toLowerCase() + "%";
                Predicate porConsecutivo = cb.like(cb.lower(root.get("consecutivo")), pattern);
                Predicate porNombre = cb.like(cb.lower(root.get("pacPrimerNombre")), pattern);
                Predicate porApellido = cb.like(cb.lower(root.get("pacPrimerApellido")), pattern);
                Predicate porDoc = cb.like(cb.lower(root.get("pacNumeroDocumento")), pattern);
                Predicate porEmail = cb.like(cb.lower(root.get("famEmail")), pattern);
                Predicate porSegundoApellido = cb.like(cb.lower(root.get("pacSegundoApellido")), pattern);
                Predicate porDocFamiliar = cb.like(cb.lower(root.get("famNumeroDocumento")), pattern);
                Predicate porNombreFamiliar = cb.like(cb.lower(root.get("famPrimerNombre")), pattern);
                Predicate porResponsable = cb.like(cb.lower(root.get("responsableAsignado")), pattern);
                Predicate porTipo = cb.like(cb.lower(root.get("tipoSolicitud")), pattern);
                Predicate porDescripcion = cb.like(cb.lower(root.get("descripcion")), pattern);
                predicates.add(cb.or(porConsecutivo, porNombre, porApellido, porDoc, porEmail, porSegundoApellido,
                        porDocFamiliar, porNombreFamiliar, porResponsable, porTipo, porDescripcion));
            }

            query.orderBy(cb.desc(root.get("fechaRadicacion")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return pqrsfRepository.findAll(spec, pageable).map(this::mapearADTO);
    }

    @Override
    @Transactional // no es readOnly: registra la consulta en la auditoría
    public PqrsfResponseDTO obtenerPorId(Long id, String usuarioAuditoria, String ipOrigen) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PQRSF no encontrada con ID: " + id));

        if (usuarioAuditoria != null) {
            auditoriaService.registrar(
                    "PQRSF",
                    pqrsf.getConsecutivo(),
                    "CONSULTA_DETALLE",
                    usuarioAuditoria,
                    ipOrigen,
                    null,
                    "{\"accion\":\"Consulta de detalle de requerimiento\"}"
            );
        }

        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional(readOnly = true)
    public PqrsfResponseDTO obtenerPorConsecutivo(String consecutivo) {
        Pqrsf pqrsf = pqrsfRepository.findByConsecutivo(consecutivo.toUpperCase())
                .orElseThrow(() -> new RuntimeException("No se encontró solicitud con el consecutivo: " + consecutivo));
        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional
    public PqrsfResponseDTO cambiarEstado(Long id, PqrsfStatusChangeDTO dto, 
                                          String usuarioResponsable, String ipOrigen, String userAgent) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PQRSF no encontrada con ID: " + id));

        String estadoAnterior = pqrsf.getEstado();
        String nuevoEstado = normalizarEstado(dto.getNuevoEstado());

        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado no reconocido: " + nuevoEstado);
        }
        if (nuevoEstado.equals(normalizarEstado(estadoAnterior))) {
            return mapearADTO(pqrsf); // sin cambios: no se duplica la traza
        }
        validarNoSolucionada(pqrsf);

        pqrsf.setEstado(nuevoEstado);
        pqrsf.setFechaActualizacion(LocalDateTime.now());
        pqrsf = pqrsfRepository.save(pqrsf);

        // Crear registro en TRAZA
        TrazaPqrsf traza = new TrazaPqrsf(
                pqrsf,
                estadoAnterior,
                nuevoEstado,
                dto.getObservacion() != null ? dto.getObservacion().trim() : "Cambio de estado por la Jefe SIAU",
                usuarioResponsable
        );
        trazaRepository.save(traza);
        pqrsf.addTraza(traza);

        // Registrar en AUDITORÍA
        String detalle = String.format(
                "{\"estadoAnterior\":\"%s\",\"nuevoEstado\":\"%s\",\"observacion\":\"%s\"}",
                estadoAnterior,
                nuevoEstado,
                dto.getObservacion() != null ? dto.getObservacion() : ""
        );
        auditoriaService.registrar(
                "PQRSF",
                pqrsf.getConsecutivo(),
                "CAMBIO_ESTADO",
                usuarioResponsable,
                ipOrigen,
                userAgent,
                detalle
        );

        emailService.enviarNotificacionCambioEstado(pqrsf, estadoAnterior, nuevoEstado, dto.getObservacion());

        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional
    public PqrsfResponseDTO asignarResponsable(Long id, String responsable, String observacion,
                                               String usuarioResponsable, String ipOrigen, String userAgent) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PQRSF no encontrada con ID: " + id));

        // El responsable debe ser un funcionario activo del sistema (o "Sin asignar")
        String nombre = responsable == null ? "" : responsable.trim();
        boolean valido = SIN_ASIGNAR.equalsIgnoreCase(nombre) || usuarioRepository
                .findByActivoTrueOrderByNombreAsc().stream()
                .anyMatch(u -> u.getNombre().equalsIgnoreCase(nombre));
        if (!valido) {
            throw new IllegalArgumentException("El responsable debe ser un funcionario activo registrado en Usuarios.");
        }

        String anterior = pqrsf.getResponsableAsignado();
        pqrsf.setResponsableAsignado(nombre);
        pqrsf.setFechaActualizacion(LocalDateTime.now());
        pqrsf = pqrsfRepository.save(pqrsf);

        // Registro de traza
        TrazaPqrsf traza = new TrazaPqrsf(
                pqrsf,
                pqrsf.getEstado(),
                pqrsf.getEstado(),
                "Reasignación de responsable a: " + responsable + (observacion != null && !observacion.isEmpty() ? " | Nota: " + observacion : ""),
                usuarioResponsable
        );
        trazaRepository.save(traza);
        pqrsf.addTraza(traza);

        // Auditoría
        auditoriaService.registrar(
                "PQRSF",
                pqrsf.getConsecutivo(),
                "ASIGNACION_RESPONSABLE",
                usuarioResponsable,
                ipOrigen,
                userAgent,
                String.format("{\"responsableAnterior\":\"%s\",\"nuevoResponsable\":\"%s\"}", anterior, responsable)
        );

        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional
    public PqrsfResponseDTO responderPorCorreo(Long id, RespuestaCorreoDTO dto, String usuarioResponsable,
                                               String ipOrigen, String userAgent) {
        Pqrsf pqrsf = pqrsfRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRSF no encontrada con ID: " + id));

        String destino = dto.getCorreoDestino() != null && !dto.getCorreoDestino().isBlank()
                ? dto.getCorreoDestino().trim().toLowerCase()
                : (pqrsf.getRespCorreo() != null && !pqrsf.getRespCorreo().isBlank() ? pqrsf.getRespCorreo() : pqrsf.getFamEmail());

        if (destino == null || !destino.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El correo de destino no es válido.");
        }

        String nombreFuncionario = "Jefe SIAU";
        String cargoFuncionario = "Servicio de Información y Atención al Usuario";
        var funcionario = usuarioRepository.findByCorreo(usuarioResponsable).orElse(null);
        if (funcionario != null) {
            nombreFuncionario = funcionario.getNombre();
            if (funcionario.getCargo() != null) cargoFuncionario = funcionario.getCargo();
        }

        boolean enviadoSmtp = emailService.enviarRespuestaCiudadano(
                pqrsf, destino, dto.getAsunto().trim(), dto.getMensaje().trim(), nombreFuncionario, cargoFuncionario);

        String estadoAnterior = pqrsf.getEstado();
        // Si ya estaba solucionada se mantiene así (el estado solucionado no se revierte)
        String estadoFinal = dto.isMarcarSolucionado() ? "solucionado" : estadoAnterior;
        pqrsf.setEstado(estadoFinal);
        pqrsf.setFechaActualizacion(LocalDateTime.now());
        pqrsf = pqrsfRepository.save(pqrsf);

        String resumen = dto.getMensaje().trim();
        if (resumen.length() > 500) resumen = resumen.substring(0, 500) + "…";
        TrazaPqrsf traza = new TrazaPqrsf(
                pqrsf,
                estadoAnterior,
                estadoFinal,
                "Respuesta enviada por correo a " + destino + (enviadoSmtp ? "" : " (modo simulación)") +
                        " | Asunto: " + dto.getAsunto().trim() + " | " + resumen,
                usuarioResponsable
        );
        trazaRepository.save(traza);
        pqrsf.addTraza(traza);

        auditoriaService.registrar(
                "PQRSF",
                pqrsf.getConsecutivo(),
                "RESPUESTA_CORREO",
                usuarioResponsable,
                ipOrigen,
                userAgent,
                String.format("{\"destino\":\"%s\",\"smtp\":%s,\"marcarSolucionado\":%s}",
                        destino, enviadoSmtp, dto.isMarcarSolucionado())
        );

        return mapearADTO(pqrsf);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDTO obtenerEstadisticas() {
        long total = pqrsfRepository.countTotal();
        long radicados = pqrsfRepository.countByEstadoIgnoreCase("radicado");
        long revisados = pqrsfRepository.countByEstadoIgnoreCase("revisado");
        long enProceso = pqrsfRepository.countByEstadoIgnoreCase("en proceso");
        long solucionados = pqrsfRepository.countByEstadoIgnoreCase("solucionado");

        return new DashboardStatsDTO(total, radicados, revisados, enProceso, solucionados);
    }

    private static final String SIN_AREA = "Sin área";

    /** Agrupa las solicitudes del periodo por área, con su semáforo, tipos y estados. */
    private List<ReporteSiauDTO.AreaReporteItemDTO> construirReportePorArea(List<Pqrsf> filtradas,
                                                                         Map<Long, SlaPolicy.Evaluacion> evaluaciones) {
        // Las áreas activas del catálogo aparecen aunque no tengan solicitudes en el periodo
        Map<String, List<Pqrsf>> porArea = new LinkedHashMap<>();
        configuracionService.listarAreas().stream()
                .filter(a -> Boolean.TRUE.equals(a.getActivo()))
                .forEach(a -> porArea.put(a.getNombre(), new ArrayList<>()));
        for (Pqrsf p : filtradas) {
            String area = p.getArea() != null && !p.getArea().isBlank() ? p.getArea().trim() : SIN_AREA;
            String clave = porArea.keySet().stream().filter(k -> k.equalsIgnoreCase(area)).findFirst().orElse(area);
            porArea.computeIfAbsent(clave, k -> new ArrayList<>()).add(p);
        }

        List<ReporteSiauDTO.AreaReporteItemDTO> lista = new ArrayList<>();
        porArea.forEach((area, ps) -> {
            long verde = 0, amarillo = 0, rojo = 0, sol = 0;
            Map<String, Long> tipos = new LinkedHashMap<>();
            Map<String, Long> estados = new LinkedHashMap<>();
            for (Pqrsf p : ps) {
                switch (evaluaciones.get(p.getId()).semaforo()) {
                    case VERDE -> verde++;
                    case AMARILLO -> amarillo++;
                    case ROJO -> rojo++;
                    case FINALIZADO -> sol++;
                }
                tipos.merge(p.getTipoSolicitud() != null ? p.getTipoSolicitud() : "(P) Petición", 1L, Long::sum);
                estados.merge(normalizarEstado(p.getEstado()), 1L, Long::sum);
            }
            double cumplimiento = ps.isEmpty() ? 100.0 : Math.round(((double) (ps.size() - rojo) / ps.size()) * 1000.0) / 10.0;
            lista.add(new ReporteSiauDTO.AreaReporteItemDTO(area, ps.size(), verde, amarillo, rojo, sol, cumplimiento, tipos, estados));
        });
        // Más solicitudes primero; "Sin área" siempre al final
        lista.sort(Comparator.comparing((ReporteSiauDTO.AreaReporteItemDTO a) -> SIN_AREA.equals(a.area()))
                .thenComparing(Comparator.comparingLong(ReporteSiauDTO.AreaReporteItemDTO::total).reversed())
                .thenComparing(ReporteSiauDTO.AreaReporteItemDTO::area));
        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteSiauDTO generarReporte(String fechaDesdeStr, String fechaHastaStr) {
        List<Pqrsf> todas = pqrsfRepository.findAll();

        LocalDate desde = (fechaDesdeStr != null && !fechaDesdeStr.trim().isEmpty()) 
                ? LocalDate.parse(fechaDesdeStr.trim()) : null;
        LocalDate hasta = (fechaHastaStr != null && !fechaHastaStr.trim().isEmpty()) 
                ? LocalDate.parse(fechaHastaStr.trim()) : null;

        List<Pqrsf> filtradas = todas.stream().filter(p -> {
            LocalDate f = p.getFechaRadicacion().toLocalDate();
            if (desde != null && f.isBefore(desde)) return false;
            if (hasta != null && f.isAfter(hasta)) return false;
            return true;
        }).toList();

        ReporteSiauDTO reporte = new ReporteSiauDTO();
        reporte.setTotalRequerimientos(filtradas.size());

        // Las series temporales se calculan siempre (aunque no haya datos) para que los gráficos tengan eje
        reporte.setSerieMensual(construirSerieMensual(todas));
        reporte.setSerieDiaria(construirSerieDiaria(todas));

        if (filtradas.isEmpty()) {
            reporte.setCumplimientoPorcentaje(100.0);
            return reporte;
        }

        long verde = 0;
        long amarillo = 0;
        long rojo = 0;
        long solucionados = 0;
        long totalDias = 0;

        Map<String, Long> distEstados = new HashMap<>();
        Map<String, Long> distTipos = new HashMap<>();
        Map<String, Long> distCanales = new HashMap<>();
        Map<String, Long> distRiesgo = new LinkedHashMap<>();
        for (SlaPolicy.NivelRiesgo r : SlaPolicy.NivelRiesgo.values()) distRiesgo.put(r.name(), 0L);
        Map<String, List<Pqrsf>> porResp = new HashMap<>();
        Map<Long, SlaPolicy.Evaluacion> evaluaciones = new HashMap<>();

        for (Pqrsf p : filtradas) {
            SlaPolicy.Evaluacion sla = SlaPolicy.evaluar(p);
            evaluaciones.put(p.getId(), sla);

            String est = p.getEstado() != null ? p.getEstado().toLowerCase() : "radicado";
            distEstados.merge(est, 1L, Long::sum);
            distTipos.merge(p.getTipoSolicitud() != null ? p.getTipoSolicitud() : "(P) Petición", 1L, Long::sum);
            distCanales.merge(p.getMedioRespuesta() != null ? p.getMedioRespuesta() : "C", 1L, Long::sum);
            distRiesgo.merge(sla.riesgo().name(), 1L, Long::sum);

            String resp = p.getResponsableAsignado() != null && !p.getResponsableAsignado().trim().isEmpty()
                    ? p.getResponsableAsignado().trim() : "Sin asignar";
            porResp.computeIfAbsent(resp, k -> new ArrayList<>()).add(p);

            totalDias += Duration.between(p.getFechaRadicacion(), LocalDateTime.now()).toDays();

            switch (sla.semaforo()) {
                case FINALIZADO -> solucionados++;
                case VERDE -> verde++;
                case AMARILLO -> amarillo++;
                case ROJO -> rojo++;
            }
        }

        reporte.setEnTerminoVerde(verde);
        reporte.setAlertaAmarillo(amarillo);
        reporte.setVencidasRojo(rojo);
        reporte.setSolucionados(solucionados);

        // Cumplimiento: todo lo que no está vencido (verde, amarillo o solucionado)
        double pctCumplimiento = ((double) (filtradas.size() - rojo) / filtradas.size()) * 100.0;
        reporte.setCumplimientoPorcentaje(Math.round(pctCumplimiento * 10.0) / 10.0);

        double tiempoPromedio = (double) totalDias / filtradas.size();
        reporte.setTiempoPromedioDias(Math.round(tiempoPromedio * 10.0) / 10.0);

        reporte.setDistribucionEstados(distEstados);
        reporte.setDistribucionTipos(distTipos);
        reporte.setDistribucionCanales(distCanales);
        reporte.setDistribucionRiesgo(distRiesgo);

        List<ReporteSiauDTO.ResponsableReporteItemDTO> respList = new ArrayList<>();
        for (Map.Entry<String, List<Pqrsf>> entry : porResp.entrySet()) {
            List<Pqrsf> listaResp = entry.getValue();

            long totalAsig = listaResp.size();
            long solResp = listaResp.stream().filter(p -> "solucionado".equalsIgnoreCase(p.getEstado())).count();
            long procResp = listaResp.stream().filter(p -> "en proceso".equalsIgnoreCase(p.getEstado())).count();
            long pendResp = listaResp.stream().filter(p -> List.of("radicado", "revisado").contains(normalizarEstado(p.getEstado()))).count();
            long vencResp = listaResp.stream()
                    .filter(p -> evaluaciones.get(p.getId()).semaforo() == SlaPolicy.Semaforo.ROJO)
                    .count();

            double efectividad = totalAsig > 0 ? ((double) solResp / totalAsig) * 100.0 : 0.0;

            respList.add(new ReporteSiauDTO.ResponsableReporteItemDTO(
                    entry.getKey(), totalAsig, solResp, procResp, pendResp, vencResp,
                    Math.round(efectividad * 10.0) / 10.0
            ));
        }

        respList.sort((a, b) -> Long.compare(b.getTotalAsignadas(), a.getTotalAsignadas()));
        reporte.setReportePorResponsable(respList);
        reporte.setReportePorArea(construirReportePorArea(filtradas, evaluaciones));

        return reporte;
    }

    /** Últimos 12 meses: radicadas por fecha de radicación, solucionadas por fecha de última actualización. */
    private ReporteSiauDTO.SerieTemporalDTO construirSerieMensual(List<Pqrsf> todas) {
        Locale es = Locale.forLanguageTag("es-CO");
        YearMonth actual = YearMonth.now();
        Map<YearMonth, long[]> buckets = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) buckets.put(actual.minusMonths(i), new long[2]);

        for (Pqrsf p : todas) {
            long[] b = buckets.get(YearMonth.from(p.getFechaRadicacion()));
            if (b != null) b[0]++;
            if ("solucionado".equalsIgnoreCase(p.getEstado()) && p.getFechaActualizacion() != null) {
                long[] s = buckets.get(YearMonth.from(p.getFechaActualizacion()));
                if (s != null) s[1]++;
            }
        }

        ReporteSiauDTO.SerieTemporalDTO serie = new ReporteSiauDTO.SerieTemporalDTO();
        buckets.forEach((mes, v) -> {
            String nombre = mes.getMonth().getDisplayName(TextStyle.SHORT, es).replace(".", "");
            serie.getEtiquetas().add(Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1));
            serie.getRadicadas().add(v[0]);
            serie.getSolucionadas().add(v[1]);
        });
        return serie;
    }

    /** Últimos 30 días. */
    private ReporteSiauDTO.SerieTemporalDTO construirSerieDiaria(List<Pqrsf> todas) {
        LocalDate hoy = LocalDate.now();
        Map<LocalDate, long[]> buckets = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) buckets.put(hoy.minusDays(i), new long[2]);

        for (Pqrsf p : todas) {
            long[] b = buckets.get(p.getFechaRadicacion().toLocalDate());
            if (b != null) b[0]++;
            if ("solucionado".equalsIgnoreCase(p.getEstado()) && p.getFechaActualizacion() != null) {
                long[] s = buckets.get(p.getFechaActualizacion().toLocalDate());
                if (s != null) s[1]++;
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        ReporteSiauDTO.SerieTemporalDTO serie = new ReporteSiauDTO.SerieTemporalDTO();
        buckets.forEach((dia, v) -> {
            serie.getEtiquetas().add(dia.format(fmt));
            serie.getRadicadas().add(v[0]);
            serie.getSolucionadas().add(v[1]);
        });
        return serie;
    }

    /**
     * Consecutivo anual: PQRSF-CH-2026-00001. La numeración vuelve a 00001 cada año.
     */
    private synchronized String generarSiguienteConsecutivo() {
        String prefijo = "PQRSF-CH-" + LocalDateTime.now().getYear() + "-";
        int siguiente = 1;

        Optional<String> ultimo = pqrsfRepository.findLastConsecutivo();
        if (ultimo.isPresent()) {
            String str = ultimo.get().toUpperCase();
            if (str.startsWith(prefijo)) {
                try {
                    siguiente = Integer.parseInt(str.substring(prefijo.length())) + 1;
                } catch (NumberFormatException e) {
                    siguiente = 1;
                }
            }
        }

        String consecutivo = String.format("%s%05d", prefijo, siguiente);
        // Salvaguarda ante colisiones (p. ej. datos editados a mano)
        while (pqrsfRepository.findByConsecutivo(consecutivo).isPresent()) {
            consecutivo = String.format("%s%05d", prefijo, ++siguiente);
        }
        return consecutivo;
    }

    private PqrsfResponseDTO mapearADTO(Pqrsf p) {
        PqrsfResponseDTO dto = new PqrsfResponseDTO();
        dto.setId(p.getId());
        dto.setConsecutivo(p.getConsecutivo().toUpperCase());
        dto.setFechaRecepcion(p.getFechaRecepcion());
        dto.setFechaRadicacion(p.getFechaRadicacion());

        // Familiar
        dto.setFamPrimerApellido(p.getFamPrimerApellido());
        dto.setFamSegundoApellido(p.getFamSegundoApellido());
        dto.setFamPrimerNombre(p.getFamPrimerNombre());
        dto.setFamSegundoNombre(p.getFamSegundoNombre());
        dto.setFamNombreCompleto(p.getFamPrimerNombre() + " " + 
                (p.getFamSegundoNombre() != null ? p.getFamSegundoNombre() + " " : "") + 
                p.getFamPrimerApellido() + " " + p.getFamSegundoApellido());
        dto.setFamTipoDocumento(p.getFamTipoDocumento());
        dto.setFamNumeroDocumento(p.getFamNumeroDocumento());
        dto.setFamEmail(p.getFamEmail());
        dto.setFamDireccion(p.getFamDireccion());
        dto.setFamTelefono(p.getFamTelefono());

        // Paciente
        dto.setPacPrimerApellido(p.getPacPrimerApellido());
        dto.setPacSegundoApellido(p.getPacSegundoApellido());
        dto.setPacPrimerNombre(p.getPacPrimerNombre());
        dto.setPacSegundoNombre(p.getPacSegundoNombre());
        dto.setPacNombreCompleto(p.getPacPrimerNombre() + " " + 
                (p.getPacSegundoNombre() != null ? p.getPacSegundoNombre() + " " : "") + 
                p.getPacPrimerApellido() + " " + p.getPacSegundoApellido());
        dto.setPacTipoDocumento(p.getPacTipoDocumento());
        dto.setPacNumeroDocumento(p.getPacNumeroDocumento());
        dto.setPacEmail(p.getPacEmail());
        dto.setPacDireccion(p.getPacDireccion());
        dto.setPacTelefono(p.getPacTelefono());
        dto.setPacRegimen(p.getPacRegimen());
        dto.setPacEntidad(p.getPacEntidad());
        dto.setPacPoblacion(p.getPacPoblacion());

        // Solicitud
        dto.setTipoSolicitud(p.getTipoSolicitud());
        dto.setDescripcion(p.getDescripcion());

        // Respuesta
        dto.setMedioRespuesta(p.getMedioRespuesta());
        dto.setRespDireccion(p.getRespDireccion());
        dto.setRespBarrio(p.getRespBarrio());
        dto.setRespMunicipio(p.getRespMunicipio());
        dto.setRespCelular(p.getRespCelular());
        dto.setRespTelefonoFijo(p.getRespTelefonoFijo());
        dto.setRespCorreo(p.getRespCorreo());

        // Estado y Responsable
        dto.setEstado(p.getEstado());
        dto.setResponsableAsignado(p.getResponsableAsignado() != null && !p.getResponsableAsignado().isBlank() ? p.getResponsableAsignado() : SIN_ASIGNAR);
        dto.setArea(p.getArea());
        dto.setFechaActualizacion(p.getFechaActualizacion());

        // ==================== SEMAFORIZACIÓN AUTOMÁTICA POR NIVEL DE RIESGO ====================
        long diasTranscurridos = Duration.between(p.getFechaRadicacion(), LocalDateTime.now()).toDays();
        dto.setDiasTranscurridos(diasTranscurridos);

        SlaPolicy.Evaluacion sla = SlaPolicy.evaluar(p);
        dto.setNivelRiesgo(sla.riesgo().name());
        dto.setNivelRiesgoEtiqueta(sla.riesgo().getEtiqueta());
        dto.setPlazoHoras(sla.riesgo().getPlazoHoras());
        dto.setFechaLimite(sla.fechaLimite());
        dto.setMinutosRestantes(sla.minutosRestantes());
        dto.setNivelSemaforo(sla.semaforo().name());
        dto.setTiempoRespuestaTexto(sla.texto());
        dto.setDiasRestantes(Math.max(0, sla.minutosRestantes() / (60 * 24)));

        // Adjuntos
        if (p.getAdjuntos() != null) {
            dto.setAdjuntos(p.getAdjuntos().stream().map(a -> new AdjuntoResponseDTO(
                    a.getId(),
                    a.getNombreOriginal(),
                    a.getNombreAlmacenado(),
                    a.getTipoMime(),
                    a.getTamanoBytes(),
                    "/api/files/" + a.getNombreAlmacenado(),
                    a.getSubidoEl()
            )).collect(Collectors.toList()));
        }

        // Trazas
        if (p.getTrazas() != null) {
            dto.setTrazas(p.getTrazas().stream().map(t -> new TrazaResponseDTO(
                    t.getId(),
                    p.getId(),
                    t.getEstadoAnterior(),
                    t.getEstadoNuevo(),
                    t.getObservacion(),
                    t.getUsuarioResponsable(),
                    t.getFechaRegistro()
            )).collect(Collectors.toList()));
        }

        return dto;
    }
}
