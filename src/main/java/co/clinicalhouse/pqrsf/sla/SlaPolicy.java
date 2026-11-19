package co.clinicalhouse.pqrsf.sla;

import co.clinicalhouse.pqrsf.model.Pqrsf;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Política institucional de semaforización automática por nivel de riesgo.
 *
 *  - Riesgo SIMPLE      (Petición, Sugerencia, Felicitación) → 2 días (48 horas)
 *  - Riesgo PRIORIZADO  (Queja, Reclamo)                     → 1 día  (24 horas)
 *  - Riesgo VITAL       (Tutela)                             → 24 horas
 *
 * Semáforo:
 *  - VERDE      → se ha consumido menos del 50% del plazo
 *  - AMARILLO   → se ha consumido el 50% o más del plazo, aún sin vencer
 *  - ROJO       → plazo vencido sin solución
 *  - FINALIZADO → solicitud solucionada
 */
public final class SlaPolicy {

    public static final double UMBRAL_ALERTA_PREDETERMINADO = 0.5;

    /** Plazos vigentes (horas) configurados desde el panel. Si un nivel no está, se usa el predeterminado. */
    private static final Map<NivelRiesgo, Long> PLAZOS = new ConcurrentHashMap<>();
    private static volatile double umbralAlerta = UMBRAL_ALERTA_PREDETERMINADO;

    private SlaPolicy() {}

    /**
     * Aplica la configuración guardada desde el panel (Configuración → Tiempos de respuesta).
     * Con un mapa vacío / umbral nulo se restauran los valores predeterminados.
     */
    public static void configurar(Map<NivelRiesgo, Long> plazosHoras, Double umbral) {
        PLAZOS.clear();
        if (plazosHoras != null) {
            plazosHoras.forEach((nivel, horas) -> {
                if (nivel != null && horas != null && horas > 0) PLAZOS.put(nivel, horas);
            });
        }
        umbralAlerta = umbral != null && umbral > 0 && umbral < 1 ? umbral : UMBRAL_ALERTA_PREDETERMINADO;
    }

    public static double getUmbralAlerta() { return umbralAlerta; }

    public enum NivelRiesgo {
        SIMPLE("Riesgo simple", 48),
        PRIORIZADO("Riesgo priorizado", 24),
        VITAL("Riesgo vital", 24);

        private final String etiqueta;
        private final long plazoPredeterminadoHoras;

        NivelRiesgo(String etiqueta, long plazoHoras) {
            this.etiqueta = etiqueta;
            this.plazoPredeterminadoHoras = plazoHoras;
        }

        public String getEtiqueta() { return etiqueta; }
        public long getPlazoPredeterminadoHoras() { return plazoPredeterminadoHoras; }

        /** Plazo vigente: el configurado desde el panel o, si no hay, el predeterminado. */
        public long getPlazoHoras() { return PLAZOS.getOrDefault(this, plazoPredeterminadoHoras); }
    }

    public enum Semaforo { VERDE, AMARILLO, ROJO, FINALIZADO }

    /** Determina el nivel de riesgo a partir del tipo de solicitud ("(Q) Queja", "Tutela", etc.). */
    public static NivelRiesgo riesgoPara(String tipoSolicitud) {
        String t = tipoSolicitud == null ? "" : tipoSolicitud.trim().toLowerCase();
        if (t.contains("tutela") || t.startsWith("(t)")) {
            return NivelRiesgo.VITAL;
        }
        if (t.contains("queja") || t.contains("reclamo") || t.startsWith("(q)") || t.startsWith("(r)")) {
            return NivelRiesgo.PRIORIZADO;
        }
        return NivelRiesgo.SIMPLE;
    }

    public static Evaluacion evaluar(Pqrsf p) {
        return evaluar(p.getTipoSolicitud(), p.getEstado(), p.getFechaRadicacion(), LocalDateTime.now());
    }

    public static Evaluacion evaluar(String tipo, String estado, LocalDateTime radicacion, LocalDateTime ahora) {
        NivelRiesgo riesgo = riesgoPara(tipo);
        LocalDateTime inicio = radicacion != null ? radicacion : ahora;
        LocalDateTime limite = inicio.plusHours(riesgo.getPlazoHoras());
        long minutosTranscurridos = Math.max(0, Duration.between(inicio, ahora).toMinutes());
        long minutosPlazo = riesgo.getPlazoHoras() * 60;
        long minutosRestantes = minutosPlazo - minutosTranscurridos;

        boolean solucionado = estado != null && "solucionado".equalsIgnoreCase(estado.trim());
        Semaforo semaforo;
        String texto;

        if (solucionado) {
            semaforo = Semaforo.FINALIZADO;
            texto = "Solucionado";
        } else if (minutosRestantes <= 0) {
            semaforo = Semaforo.ROJO;
            texto = "Vencida hace " + formatearDuracion(-minutosRestantes);
        } else if ((double) minutosTranscurridos / minutosPlazo >= umbralAlerta) {
            semaforo = Semaforo.AMARILLO;
            texto = "Vence en " + formatearDuracion(minutosRestantes);
        } else {
            semaforo = Semaforo.VERDE;
            texto = "Vence en " + formatearDuracion(minutosRestantes);
        }

        return new Evaluacion(riesgo, semaforo, limite, minutosTranscurridos, minutosRestantes, texto);
    }

    public static String formatearDuracion(long minutos) {
        long m = Math.abs(minutos);
        long dias = m / (60 * 24);
        long horas = (m % (60 * 24)) / 60;
        long mins = m % 60;
        if (dias > 0) return dias + "d " + horas + "h";
        if (horas > 0) return horas + "h " + mins + "m";
        return mins + "m";
    }

    public record Evaluacion(
            NivelRiesgo riesgo,
            Semaforo semaforo,
            LocalDateTime fechaLimite,
            long minutosTranscurridos,
            long minutosRestantes,
            String texto
    ) {}
}
