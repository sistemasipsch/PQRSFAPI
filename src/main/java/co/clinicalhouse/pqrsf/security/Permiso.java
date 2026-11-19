package co.clinicalhouse.pqrsf.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * Funcionalidades del panel que se pueden habilitar o quitar por rol desde Configuración → Permisos.
 * El SUPER_ADMIN siempre tiene todas; Usuarios, Áreas, Cargos y Permisos son exclusivas del SUPER_ADMIN.
 */
public enum Permiso {
    VER_REPORTES("Ver reportes y SLA", "Reportes"),
    EXPORTAR_REPORTES("Exportar reportes (CSV)", "Reportes"),
    VER_PQRSF("Ver la bandeja de PQRSF", "PQRSF"),
    CAMBIAR_ESTADO("Cambiar el estado de una PQRSF", "PQRSF"),
    ASIGNAR_RESPONSABLE("Asignar responsable y área", "PQRSF"),
    RESPONDER_CORREO("Responder por correo", "PQRSF"),
    RESPONDER_WHATSAPP("Responder por WhatsApp", "PQRSF"),
    IMPRIMIR_PQRSF("Imprimir / PDF de la PQRSF", "PQRSF"),
    VER_AUDITORIA("Ver la auditoría", "Sistema"),
    CONFIGURAR_TIEMPOS("Configurar tiempos de respuesta", "Sistema");

    private final String descripcion;
    private final String grupo;

    Permiso(String descripcion, String grupo) {
        this.descripcion = descripcion;
        this.grupo = grupo;
    }

    public String getDescripcion() { return descripcion; }
    public String getGrupo() { return grupo; }

    /** Roles cuyas funcionalidades se configuran (el SUPER_ADMIN no se configura: tiene todo). */
    public static final String[] ROLES_CONFIGURABLES = {"JEFE_SIAU", "FUNCIONARIO_SIAU"};

    /** Permisos con los que arranca cada rol la primera vez. */
    public static Set<Permiso> predeterminados(String rol) {
        if ("JEFE_SIAU".equals(rol)) return EnumSet.allOf(Permiso.class);
        if ("FUNCIONARIO_SIAU".equals(rol)) {
            return EnumSet.of(VER_REPORTES, VER_PQRSF, CAMBIAR_ESTADO, RESPONDER_CORREO, RESPONDER_WHATSAPP, IMPRIMIR_PQRSF);
        }
        return EnumSet.noneOf(Permiso.class);
    }
}
